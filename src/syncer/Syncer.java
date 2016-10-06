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
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 *
 * @author leijurv
 */
public class Syncer {

    static final int SIZE = 4096;
    static final int BYTES_PER_SEC = 44100 * 4;
    static boolean gui = true;

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
            JOptionPane.showInputDialog("You need sox. brew install sox");
            System.out.println("You need sox. brew install sox");
            return;
        }

        boolean verbose = Arrays.asList(args).contains("verbose");
        boolean noGui = Arrays.asList(args).contains("nogui");
        boolean yesGui = Arrays.asList(args).contains("gui");
        if (!noGui && !yesGui) {
            gui = 0 == JOptionPane.showConfirmDialog(null, "Would you a GUI or nah?", "It'll work either way", JOptionPane.YES_NO_OPTION);
        } else {
            if (yesGui && noGui) {
                throw new ArrayIndexOutOfBoundsException("no");
            }
            gui = yesGui;
        }
        InputStream in = new Socket("207.47.5.28", 5021).getInputStream();
        new CacheSource(new DataInputStream(in), cache).start();
        cache.sleepUntilSize(20);
        Process sox = new ProcessBuilder("/usr/local/bin/sox -t raw -r 44100 -b 16 -c 2 --buffer 4096 -e signed-integer - -tcoreaudio --buffer 4096".split(" ")).start();
        if (verbose) {
            System.out.println("Starting to play");
        }
        new Thread() {
            @Override
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
            @Override
            public void run() {
                try {
                    while (true) {
                        Chunk chunk = cache.getChunk();
                        sox.getOutputStream().write(chunk.contents);
                        if (gui) {
                            GUI.onData(chunk);
                        }
                        long lad = System.currentTimeMillis();
                        offset = lad - chunk.multiplexed / 1000000;
                        offset2 = chunk.beganToReceive - chunk.multiplexed / 1000000;
                        offset3 = chunk.received - chunk.multiplexed / 1000000;
                        sox.getOutputStream().flush();
                        long wew = System.currentTimeMillis();
                        lastWrite = wew;
                        offset4 = wew - lad;
                        offset5 = wew - chunk.multiplexed / 1000000;
                        //System.out.println("wew");
                    }
                } catch (IOException ex) {
                    Logger.getLogger(Syncer.class.getName()).log(Level.SEVERE, null, ex);
                    System.out.println("Wow i'm bad");
                    System.out.println("Wow i'm bad");
                    System.out.println("Wow i'm bad");
                    System.out.println("Wow i'm bad");
                    System.out.println("Wow i'm bad");

                }
            }
        }.start();
        if (gui) {
            GUI.begin();
        }
        long startAbove = 0;
        while (true) {
            synchronized (cache.lock) {
                int size = cache.getSize();
                String temp = "";
                temp += ("Number of seconds output thread spent waiting for data: " + cache.waitMS / 1000F);
                temp += "\n";
                cache.waitMS = 0;
                temp += ("Cache size: " + size);
                temp += "\n";
                float seconds = (size * SIZE) / ((float) BYTES_PER_SEC);

                temp += ("Number of seconds currently in cache: " + seconds);
                temp += "\n";
                boolean aboveLong = false;
                if (size > 1) {
                    if (startAbove == 0) {
                        startAbove = System.currentTimeMillis();
                    }

                    if (startAbove + 2000 < System.currentTimeMillis()) {
                        aboveLong = true;
                    }
                } else {
                    startAbove = 0;
                }
                temp += ("ms since zero cache: " + (size > 1 ? (System.currentTimeMillis() - startAbove) : 0));
                temp += "\n";
                temp += ("Cutting: " + aboveLong);
                temp += "\n";
                temp += "Offset between server multiplex time and receive: " + offset2;
                temp += "\n";
                temp += "Offset between server multiplex time and fully received: " + offset3;
                temp += "\n";
                temp += "Subprocess flush time: " + offset4;
                temp += "\n";
                temp += "Offset between server multiplex time and end write to sox subprocess: " + offset5;
                temp += "\n";
                temp += "Offset between server multiplex time and began write to sox subprocess (this is the important one): " + offset;
                if (gui && GUI.M != null) {
                    GUI.info = temp;
                    GUI.M.repaint();
                }
                if (aboveLong) {
                    int target = (int) Math.floor(Math.max(0, size * 0.98F));
                    while (Syncer.cache.getSize() > target) {
                        Syncer.cache.beginning = Syncer.cache.beginning.next;
                    }
                    if (seconds > 2) {
                        //beginning = null;
                    }
                    if (System.currentTimeMillis() - startAbove > 20000) {
                        System.out.println("Exiting because the cache has been nonzero for more than 20 seconds");
                        Thread.sleep(1000);
                        System.exit(1);
                    }
                }
                long dank = System.currentTimeMillis() - lastWrite;
                if (dank > 20000 && dank < 50000) {
                    System.out.println("Haven't written in 20s, something's wrong");
                    Thread.sleep(500);
                    System.exit(1);
                }
                /*if (verbose) {
                    System.out.println(temp);
                }*/

            }
            Thread.sleep(50);
        }
    }
    static long offset;
    static long offset2;
    static long offset3;
    static long offset4;
    static long offset5;
    static long lastWrite;
    static AudioCache cache = new AudioCache();

}
