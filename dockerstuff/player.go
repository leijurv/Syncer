package main

import (
	"bytes"
	"fmt"
	"io"
	"io/ioutil"
	"bufio"
	"net"
	"os"
	"os/exec"
	"regexp"
	"strings"
	"syscall"
)

type QueueItem struct {
	data []byte
	name string
}

var qch chan *QueueItem

var PLEASECLAP bool
var current *QueueItem

func main() {
	qch = make(chan *QueueItem, 1)
	go consumeQueue()
	go urlServer()
	pcmServer()
}

func pcmServer() {
	ln, err := net.Listen("tcp", ":5022")
	if err != nil {
		panic(err)
	}
	fmt.Println("PCM player is listening on 5022")
	for {
		connection, err := ln.Accept()
		if err != nil {
			panic(err)
		}
		fmt.Println("WAV player got connection", connection)
		go func() {
			connection.Write([]byte("starting to receive...\n"))
			fmt.Println("hello?")
			aoeu, err := ioutil.ReadAll(connection)
			if err != nil {
				panic(err)
			}
			connection.Write([]byte("ok got it. added to queue.\n"))
			connection.Close()
			fmt.Println("Copying data of len", len(aoeu), "from connection to ffmpeg")
			fmt.Println("kd", len(aoeu))
			qch <- &QueueItem{data: aoeu, name: "fun pcm data we got from " + connection.RemoteAddr().String()}
			fmt.Println("Wrote to queue")
		}()
	}
}

func urlServer() {
	ln, err := net.Listen("tcp", ":5023")
	if err != nil {
		panic(err)
	}
	fmt.Println("youtube-dl server is listening on 5023")
	for {
		connection, err := ln.Accept()
		if err != nil {
			panic(err)
		}
		fmt.Println("youtube-dl server got connection", connection)
		go func() {

			aoeu, err := bufio.NewReader(connection).ReadString('\n')
			fmt.Println(string(aoeu))
			text := strings.Trim(string(aoeu), "\n")
			if text == "skip" {
				PLEASECLAP = true
				connection.Close()
				return
			}
			if text == "current" {
				if current == nil {
					connection.Write([]byte("Nothing playing\n"))
					connection.Close()
					return
				}
				connection.Write([]byte("Currently playing " + current.name + "\n\n"))
				connection.Close()
				return
			}
			urlRe := regexp.MustCompile(`https?://*.*`)
			dlUrl := text
			if !urlRe.MatchString(text) {
				// search for string
				dlUrl = "ytsearch:" + text
			}
			fmt.Println("downloading", dlUrl)
			nameCmd := exec.Command("youtube-dl", "--get-filename", "-o", "%(title)s", dlUrl)
			var outb, errb bytes.Buffer
			nameCmd.Stdout = &outb
			nameCmd.Stderr = &errb

			err = nameCmd.Run()
			name := outb.String()
			stderr := errb.String()
			if err != nil {
				connection.Write([]byte("Error downloading " + text + "\n"))
				connection.Close()
				fmt.Println(err, stderr)
				return
			}
			connection.Write([]byte("Downloading " + string(name) + "...\n"))
			dlCmd := exec.Command("youtube-dl", "-f", "bestaudio", "-o", "-", dlUrl)
			dlCmd.SysProcAttr = &syscall.SysProcAttr{Setpgid: true} //this is needed because stack overflow says so
			var outbc, errbc bytes.Buffer
			dlCmd.Stdout = &outbc
			dlCmd.Stderr = &errbc

			err = dlCmd.Run()
			audio := outbc.Bytes()
			stderr = errbc.String()
			if err != nil {
				connection.Write([]byte("Error downloading " + string(name) + "\n" + stderr))
				connection.Close()
				fmt.Println(err, stderr)
				return
			}

			qch <- &QueueItem{data: audio, name: string(name)}
			connection.Write([]byte("Done! Your song will play soon...\n\n"))
			connection.Close()
		}()
	}
}

func consumeQueue() {
	for {
		fmt.Println("Queue waiting")
		data := <-qch
		fmt.Println("Time to play", data.name)
		fmt.Println("Len:", len(data.data))
		playAndWait(data)
		current=nil
		fmt.Println("and done")
	}
}
func playAndWait(qi *QueueItem) {
	data := qi.data
	current = qi
	cmd := exec.Command("ffmpeg", "-i", "-", "-f", "wav", "-ar", "44100", "-ac", "2", "-")
	cmd.SysProcAttr = &syscall.SysProcAttr{Setpgid: true} //this is needed because stack overflow says so

	in, err := cmd.StdinPipe()
	if err != nil {
		panic(err)
	}
	cmd.Stderr = os.Stdout

	fmt.Println("Creating pacat")
	pac := exec.Command("pacat")
	pac.SysProcAttr = &syscall.SysProcAttr{Setpgid: true} //this is needed because stack overflow says so

	ffout, err := cmd.StdoutPipe()
	if err != nil {
		panic(err)
	}
	pacin, err := pac.StdinPipe()
	if err != nil {
		panic(err)
	}
	go func() {
		fmt.Println("Starting to read from cmd")
		b := make([]byte, 4096)
		for {
			n, err := io.ReadFull(ffout, b)
			if !PLEASECLAP {
				pacin.Write(b[:n])
			}
			if err != nil {
				fmt.Println("copy err", err)
				pacin.Close()
				return
			}
		}
	}()
	pac.Stderr = os.Stdout
	pac.Stdout = os.Stdout
	fmt.Println("Starting pac")
	err = cmd.Start()
	if err != nil {
		panic(err)
	}
	err = pac.Start()
	if err != nil {
		panic(err)
	}
	fmt.Println("Started both")

	in.Write(data)
	in.Close()
	fmt.Println("Wrote to ffmpeg")
	cmd.Wait()
	fmt.Println("ffmpeg over")
	pac.Wait()
	fmt.Println("HEY DUDE IM ACTUALLY DONE")
	PLEASECLAP = false
	pgid, err := syscall.Getpgid(cmd.Process.Pid) //and kill the process
	if err == nil {
		syscall.Kill(-pgid, 15) // note the minus sign
	}
	pgid, err = syscall.Getpgid(pac.Process.Pid) //and kill the process
	if err == nil {
		syscall.Kill(-pgid, 15) // note the minus sign
	}
}
func copyAndNotifyWhenDone(dst io.Writer, src io.Reader, doneChan chan int) {
	go func() {
		io.Copy(dst, src)
		doneChan <- 1
	}()
}
