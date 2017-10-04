package main

import (
	"bytes"
	"fmt"
	"io"
	"io/ioutil"
	"bufio"
	"net"
	"strconv"
	"os"
	"time"
	"os/exec"
	"regexp"
	"strings"
	"sync"
	"syscall"
)

type QueueItem struct {
	data []byte
	name string
}

var qch chan *QueueItem

var PLEASECLAP bool
var l sync.Mutex
var voteskips []string
var onlines =make(map[string]int64)
var current *QueueItem

func main() {
	qch = make(chan *QueueItem, 1000)
	go consumeQueue()
	go urlServer()
	go liveServer()
	pcmServer()
}
func liveServer(){
	ln, err := net.Listen("tcp", ":5024")
	if err != nil {
		panic(err)
	}
	fmt.Println("PCM live player is listening on 5024")
	for {
		connection, err := ln.Accept()
		if err != nil {
			panic(err)
		}
		fmt.Println("PCM live player got connection", connection)
		go directlyPlayPCMLive(connection)
	}
}
func pcmServer() {
	ln, err := net.Listen("tcp", ":5022")
	if err != nil {
		panic(err)
	}
	fmt.Println("audio player is listening on 5022")
	for {
		connection, err := ln.Accept()
		if err != nil {
			panic(err)
		}
		fmt.Println("audio player got connection", connection)
		go func() {
			connection.Write([]byte("starting to receive...\n"))
			fmt.Println("hello?")
			aoeu, err := ioutil.ReadAll(connection)
			if err != nil {
				//panic(err)
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
			if len(text)>len("bkdtfeweaouaaaaaaaaa") && text[:len("bkdtfeweaouaaaaaaaaa")] == "bkdtfeweaouaaaaaaaaa" { // "skip" is too obvious
				l.Lock()
				n:=text[len("bkdtfeweaouaaaaaaaaa"):]
				for i:=0; i<len(voteskips); i++{
					if voteskips[i]==n{
						l.Unlock()
						connection.Close()
						return
					}
				}
				voteskips=append(voteskips,n)
				if 2*len(voteskips)>=len(onlines){
					PLEASECLAP=true
				}
				l.Unlock()
				connection.Close()
				return
			}
			if len(text)>len("kdtfeweaou") && text[:len("kdtfeweaou")] == "kdtfeweaou" { // "current" is too obvious
				l.Lock()
				onlines[text[len("kdtfeweaou"):]]=time.Now().UnixNano()//truly amazing
				a:=make([]string,0)
				for k,_:=range onlines{
					a=append(a,k)
				}
				for _,k:=range a{
					if onlines[k]<time.Now().UnixNano()-int64(10*time.Second){
						delete(onlines,k)
					}
				}
				onl:=""
				for k,_:=range onlines{
					onl+=k
					onl+=","
				}
				l.Unlock()
				if current == nil {
					connection.Write([]byte("Nothing playing\n"))
					connection.Close()
					return
				}
				connection.Write([]byte("Currently playing " + strings.Trim(current.name,"\n") + " for "+onl+" and "+strconv.Itoa(len(voteskips))+" votes to skip. Queue length "+strconv.Itoa(len(qch))+"\n\n"))
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
		voteskips=make([]string,0)
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
func directlyPlayPCMLive(conn net.Conn) {
	doneChan := make(chan int, 1)

	cmd := exec.Command("pacat")
	cmd.SysProcAttr = &syscall.SysProcAttr{Setpgid: true} //this is needed because stack overflow says so

	in, err := cmd.StdinPipe()
	if err != nil {
		panic(err)
	}
	copyAndNotifyWhenDone(in, conn, doneChan)

	out, err := cmd.StdoutPipe()
	if err != nil {
		panic(err)
	}
	copyAndNotifyWhenDone(conn, out, doneChan)

	err = cmd.Start()
	if err != nil {
		panic(err)
	}

	go func() {
		<-doneChan
		conn.Close()                                  //when we are done (for any reason), both close the connection
		pgid, err := syscall.Getpgid(cmd.Process.Pid) //and kill the process
		if err == nil {
			syscall.Kill(-pgid, 15) // note the minus sign
		}
	}()

	cmd.Wait()
	doneChan <- 1

	fmt.Println("Main func over")
}
func copyAndNotifyWhenDone(dst io.Writer, src io.Reader, doneChan chan int) {
	go func() {
		io.Copy(dst, src)
		doneChan <- 1
	}()
}
