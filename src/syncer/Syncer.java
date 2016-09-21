/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package syncer;

import java.awt.Graphics;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

/**
 *
 * @author leijurv
 */
public class Syncer {

    static final int SIZE = 1024;
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

        //InputStream in = new Socket("localhost", 5021).getInputStream();
        InputStream in = new ProcessBuilder("ssh", "b", "tail -c 0 -f /tmp/soundout/output.wav").start().getInputStream();
        boolean verbose = args.length > 0 && args[0].equals("verbose");
        DataInputStream wav = new DataInputStream(in);
        byte[] wewlad = new byte[SIZE];
        wav.readFully(wewlad);
        addBytes(wewlad);
        new Thread() {
            public void run() {

                try {

                    while (true) {
                        byte[] wewlad = new byte[SIZE];
                        wav.readFully(wewlad);
                        addBytes(wewlad);
                    }
                } catch (Exception ex) {
                    Logger.getLogger(Syncer.class.getName()).log(Level.SEVERE, null, ex);

                }
            }

        }.start();
        while (getSize() < 20) {
            Thread.sleep(1);

        }
        Process sox = new ProcessBuilder("/usr/local/bin/sox -t raw -r 44100 -b32 -e signed-integer - -tcoreaudio".split(" ")).start();
        if (verbose) {
            System.out.println("Starting to play");
        }
        new Thread() {
            public void run() {
                try {
                    while (true) {
                        sox.getOutputStream().write(getBytes());
                        sox.getOutputStream().flush();
                        //System.out.println("wew");
                    }
                } catch (IOException ex) {
                    Logger.getLogger(Syncer.class.getName()).log(Level.SEVERE, null, ex);

                }
            }
        }.start();
        M = new JComponent() {
            public void paintComponent(Graphics g) {
                String[] lol = info.split("\n");
                for (int i = 0; i < lol.length; i++) {
                    g.drawString(lol[i], 10, 10 + i * 15);
                }
            }
        };
        JFrame frame = new JFrame("Spotify");
        frame.setContentPane(M);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(10000, 10000);
        frame.setVisible(true);
        long startAbove = 0;
        while (true) {
            synchronized (lock) {
                int size = getSize();
                String temp = "";
                temp += ("Number of seconds output thread spent waiting for data: " + waitMS / 1000F);
                temp += "\n";
                waitMS = 0;
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

                    if (startAbove + 1000 < System.currentTimeMillis()) {
                        aboveLong = true;
                    }
                } else {
                    startAbove = 0;
                }
                temp += ("ms since zero cache: " + (size > 1 ? (System.currentTimeMillis() - startAbove) : 0));
                temp += "\n";
                temp += ("Cutting: " + aboveLong);
                info = temp;
                M.repaint();
                if (aboveLong) {
                    int target = (int) Math.floor(Math.max(0, size * 0.95F));
                    while (getSize() > target) {
                        beginning = beginning.next;
                    }
                    if (seconds > 2) {
                        //beginning = null;
                    }
                    if (System.currentTimeMillis() - startAbove > 20000) {
                        System.exit(1);
                    }
                }
                if (verbose) {
                    System.out.println(temp);
                }

            }
            Thread.sleep(50);
        }
    }
    static JComponent M;
    public static String info = "";
    static int waitMS = 0;
    public static final Object lock = new Object();
    static LinkedList beginning = null;

    public static int getSize() {
        synchronized (lock) {
            if (beginning == null) {
                return 0;
            }
            int count = 0;
            LinkedList temp = beginning;
            while (temp != null) {
                temp = temp.next;
                count++;
            }
            return count;
        }
    }

    public static void addBytes(byte[] bytes) {
        synchronized (lock) {
            if (beginning == null) {
                beginning = new LinkedList();
                beginning.data = bytes;
                //System.out.println("beginning add");
                return;
            }
            //System.out.println("normal add");
            LinkedList wew = new LinkedList();
            wew.next = beginning;
            wew.data = bytes;
            beginning = wew;
        }
    }

    public static byte[] getBytes() {
        //System.out.println("Getting bytes");
        while (beginning == null) {
            waitMS++;
            try {
                Thread.sleep(1);
            } catch (InterruptedException ex) {
                Logger.getLogger(Syncer.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        }
        synchronized (lock) {
            if (beginning == null) {
                return getBytes();
            }
            LinkedList temp = beginning;
            LinkedList prev = null;
            while (temp.next != null) {
                prev = temp;
                temp = temp.next;
            }
            byte[] toReturn = temp.data;
            if (prev == null) {
                beginning = null;
                return toReturn;
            }
            prev.next = null;
            return toReturn;
        }
    }

    public static class LinkedList {

        LinkedList next;
        byte[] data;
    }
}
