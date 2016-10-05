/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package syncer;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author leijurv
 */
public class Syncer {

    static final int SIZE = 4096;
    static final int BYTES_PER_SEC = 44100 * 4;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        /*if (args.length == 0) {
            System.out.println("Give me an argument. Either 'stdin' or 'connect'");
            return;
        }
        InputStream in;
        if (args[0].equals("stdin")) {
            in = System.in;
        } else if (args[0].equals("connect")) {
            in = new Socket("207.47.5.28", 5021).getInputStream();
        } else {
            System.out.println("What is " + args[0]);
            return;
        }*/
        if (!new File("/usr/local/bin/sox").exists()) {
            System.out.println("You need sox. brew install sox");
            return;
        }

        InputStream in = new Socket("207.47.5.28", 5021).getInputStream();
        boolean verbose = args.length > 0 && args[0].equals("verbose");
        new CacheSource(new DataInputStream(in), cache).start();
        cache.sleepUntilSize(20);
        Process sox = new ProcessBuilder("/usr/local/bin/sox -t raw -r 44100 -b32 -e signed-integer - -tcoreaudio".split(" ")).start();
        if (verbose) {
            System.out.println("Starting to play");
        }
        new Thread() {
            public void run() {
                try {
                    int exitCode = sox.waitFor();
                    System.out.println("SOX EXITED WITH CODE " + exitCode);
                    int j;
                    while ((j = sox.getErrorStream().read()) >= 0) {
                        System.out.write(j);
                    }
                    Thread.sleep(1000);
                    System.exit(exitCode);//this is a sketchy way to just make it exit once the sox subprocess stops
                } catch (InterruptedException | IOException ex) {
                    Logger.getLogger(Syncer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }.start();
        new Thread() {
            public void run() {
                try {
                    while (true) {
                        Chunk toWrite = null;
                        while (toWrite == null) {
                            toWrite = cache.getBytes();
                        }
                        sox.getOutputStream().write(toWrite.contents);
                        GUI.onData(toWrite);
                        sox.getOutputStream().flush();
                        //System.out.println("wew");
                    }
                } catch (IOException ex) {
                    Logger.getLogger(Syncer.class.getName()).log(Level.SEVERE, null, ex);

                }
            }
        }.start();

        GUI.begin();
    }

    static AudioCache cache = new AudioCache();

}
