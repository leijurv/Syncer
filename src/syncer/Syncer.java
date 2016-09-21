/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package syncer;

import java.awt.Color;
import java.awt.Graphics;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
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

    static final int SIZE = 16384;
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
                    System.exit(1);

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
                    System.exit(sox.waitFor());
                } catch (InterruptedException ex) {
                    Logger.getLogger(Syncer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }.start();
        new Thread() {
            public void run() {
                try {
                    while (true) {
                        byte[] toWrite = getBytes();
                        sox.getOutputStream().write(toWrite);
                        long start = System.currentTimeMillis();
                        float[] data = new float[toWrite.length / 4];
                        IntBuffer d = ByteBuffer.wrap(toWrite).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
                        for (int i = 0; i < data.length; i++) {
                            data[i] = ((float) d.get()) / Integer.MAX_VALUE;
                        }
                        /* float minValue = data[0];
                        int minPos = 0;
                        for (int i = 1; i < data.length; i++) {
                            if (data[i] < minValue) {
                                minValue = data[i];
                                minPos = i;
                            }
                        }
                        float[] n = new float[data.length];
                        for (int i = 0; i < data.length; i++) {
                            if (i < minPos) {
                                n[i - minPos + n.length] = data[i];
                            } else {
                                n[i - minPos] = data[i];
                            }
                        }
                        data = n;*/
                        Complex[] input = new Complex[data.length];
                        for (int i = 0; i < input.length; i++) {
                            input[i] = new Complex(data[i], 0);
                        }
                        fft = FFT.fft(input);
                        mostRecentSample = data;
                        System.out.println(System.currentTimeMillis() - start);
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
                float[] bleh = mostRecentSample;
                if (bleh == null) {
                    return;
                }
                for (int i = 0; i < bleh.length; i++) {
                    int x = i / 4;
                    int y = (int) (bleh[i] * 100 + 400);
                    g.drawLine(x, y, x, y);
                }
                g.setColor(Color.RED);
                for (int i = 0; i < fft.length; i++) {
                    int x = i / 4;
                    int y = (int) (fft[i].re() * 100 + 400);
                    g.drawLine(x, y, x, y);
                }
                g.setColor(Color.BLUE);
                for (int i = 0; i < fft.length; i++) {
                    int x = i / 4;
                    int y = (int) (fft[i].im() * 100 + 400);
                    g.drawLine(x, y, x, y);
                }
                ArrayList<Integer> signChanges = new ArrayList<>();
                ArrayList<Double> possibleFreq = new ArrayList<>();
                g.setColor(Color.GREEN);
                for (int i = 0; i < fft.length / 2 - 1; i++) {
                    int x = i / 4;
                    double t = fft[i].re();
                    double n = fft[i + 1].re();
                    if (Math.signum(n) != Math.signum(t)) {
                        g.drawLine(x, 0, x, M.getHeight());
                        signChanges.add(i);
                        double secondsInThisSample = (((double) SIZE) / BYTES_PER_SEC);
                        possibleFreq.add(((double) i) / secondsInThisSample);
                    }
                }
                g.drawString("Sign changes: " + signChanges, 200, 200);
                g.drawString("Possible frequencies: " + possibleFreq, 200, 220);

                /*double max = 0;
                int pos = 0;
                for (int i = 0; i < fft.length / 2; i++) {
                    if (fft[i].re() > max) {
                        max = fft[i].re();
                        pos = i / 4;
                    }
                }*/
                //g.drawString(fft.length + " " + pos + " " + max + "", 100, 100);
            }
        };
        JFrame frame = new JFrame("Spotify");
        frame.setContentPane(M);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        //frame.setSize(500, 200);
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

                    if (startAbove + 2000 < System.currentTimeMillis()) {
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
                    int target = (int) Math.floor(Math.max(0, size * 0.98F));
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
    static float[] mostRecentSample = null;
    static Complex[] fft = null;
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
