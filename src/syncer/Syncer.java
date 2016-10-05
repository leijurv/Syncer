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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;

/**
 *
 * @author leijurv
 */
public class Syncer {

    static final int SIZE = 4096;
    static final int BYTES_PER_SEC = 44100 * 4;
    static boolean dofft = false;

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
                    System.exit(sox.waitFor());//this is a sketchy way to just make it exit once the sox subprocess stops
                } catch (InterruptedException ex) {
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
                        onData(toWrite.contents);
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
    static float[] mostRecentSample = null;
    static Complex[] fft = null;
    static JComponent M;
    public static String info = "";
    static AudioCache cache = new AudioCache();

    public static void onData(byte[] toWrite) {
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
        if (dofft) {
            Complex[] input = new Complex[data.length];
            for (int i = 0; i < input.length; i++) {
                input[i] = new Complex(data[i], 0);
            }
            fft = FFT.fft(input);
        }
        mostRecentSample = data;
        if (M != null) {
            M.repaint();
        }
        //System.out.println(System.currentTimeMillis() - start);
    }

}
