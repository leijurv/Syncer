package main

import (
	"encoding/binary"
	"fmt"
	"io"
	"net"
	"os"
	"sync"
	"time"
)

var channels []chan []byte
var channelsLock sync.Mutex

const (
	CHUNK_SIZE   = 4096
	CHANNEL_SIZE = 250
)

func main() {
	ln, err := net.Listen("tcp", ":5021")
	if err != nil {
		panic(err)
	}
	fmt.Println("Multiplex is listening on 5021")
	go func() {
		for {
			data := make([]byte, CHUNK_SIZE)
			b := make([]byte, 8)
			_, err := io.ReadFull(os.Stdin, data)
			if err != nil {
				panic(err)
			}
			ts := time.Now().UnixNano()
			binary.BigEndian.PutUint64(b, uint64(ts))
			data = append(b, data...)
			channelsLock.Lock()
			for _, ch := range channels {
				//fmt.Println("Writing to channel, channel size:",len(ch))
				select {
				case ch <- data:
				default:
					//Silently allow this connection to skip this chunk of data,
					// because their connection is too slow to keep up and has backed up their channel
					fmt.Println("Channel", ch, "is blocking, skipped")
				}
			}
			channelsLock.Unlock()
		}
	}()
	ln0, err := net.Listen("tcp", ":5020")
	if err != nil {
		panic(err)
	}
	fmt.Println("Multiplex is listening on 5020")
	go func(){
		for {
			conn, err := ln0.Accept()
			if err != nil {
				panic(err)
			}
			go handleConnection(conn, false)
		}
	}()
	for {
		conn, err := ln.Accept()
		if err != nil {
			panic(err)
		}
		go handleConnection(conn, true)
	}
}
func handleConnection(connection net.Conn, wasThisOn5021 bool) {
	fmt.Println("Multiplex got connection", connection)
	ch := make(chan []byte, CHANNEL_SIZE)
	fmt.Println("Made channel", ch)
	channelsLock.Lock()
	channels = append(channels, ch)
	channelsLock.Unlock()
	duration, _ := time.ParseDuration("5s")
	connection.(*net.TCPConn).SetNoDelay(true)
	for {
		data := <-ch
		//fmt.Println("Reading from channel, channel size:",len(ch))
		connection.SetDeadline(time.Now().Add(duration))
		if !wasThisOn5021{
			data=data[:8]
		}
		_, err := connection.Write(data)
		if err != nil {
			fmt.Println("Removing connection", connection, "and channel", ch)
			connection.Close()
			remove(ch)
			return
		}
	}
}
func remove(ch chan []byte) {
	channelsLock.Lock()
	defer channelsLock.Unlock()
	for i := 0; i < len(channels); i++ {
		if channels[i] == ch {
			channels = append(channels[:i], channels[i+1:]...)
			close(ch)
			return
		}
	}
	close(ch)
	fmt.Println("IT WASNT THERE")
}
