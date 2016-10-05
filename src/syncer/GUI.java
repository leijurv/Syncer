/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package syncer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import static syncer.Syncer.BYTES_PER_SEC;
import static syncer.Syncer.SIZE;
import static syncer.Syncer.cache;
import static syncer.Syncer.dofft;

/**
 *
 * @author leijurv
 */
public class GUI {

    static float[] mostRecentSample = null;
    static Complex[] fft = null;
    static JComponent M;
    public static String info = "";

    public static void begin() throws InterruptedException {
        M = new JComponent() {
            public void paintComponent(Graphics g) {
                g.drawString("Click to toggle FFT", 400, 50);
                String[] lol = info.split("\n");
                for (int i = 0; i < lol.length; i++) {
                    g.drawString(lol[i], 10, 10 + i * 15);
                }
                float[] bleh = mostRecentSample;
                if (bleh == null) {
                    return;
                }
                double secondsInThisSample = (((double) SIZE) / BYTES_PER_SEC);
                for (int i = 0; i < bleh.length; i++) {
                    int x = i / 4;
                    int y = (int) (bleh[i] * 100 + 300);
                    g.drawLine(x, y, x, y);
                }
                if (fft == null || !dofft) {
                    return;
                }
                g.setColor(Color.RED);
                for (int i = 0; i < fft.length; i++) {
                    int x = i;
                    //int x = (int) (i / secondsInThisSample);
                    if (fft[i].re() > 0) {
                        g.setColor(Color.RED);
                    } else {
                        //g.setColor(Color.ORANGE);
                    }
                    int y = (int) (-fft[i].re() * 10 + M.getHeight() - 120);
                    //int othery = (int) (fft[i].re() * 100 + M.getHeight() - 120);
                    //g.drawLine(x, y, x, othery);
                    g.drawLine(x, y, x, y);
                }
                int blocksize = 10;
                for (int block = 0; block + blocksize < fft.length; block += blocksize) {
                    double sum = 0;
                    for (int i = block; i < block + blocksize; i++) {
                        sum += Math.abs(fft[i].re());
                    }
                    sum /= blocksize;
                    int startX = block;
                    int startY = M.getHeight() - 120;
                    int sizeY = (int) (sum * 10);
                    g.drawRect(startX, startY - sizeY, blocksize, sizeY);

                }
                g.setColor(Color.BLUE);
                for (int block = 0; block + blocksize < fft.length; block += blocksize) {
                    double sum = 0;
                    for (int i = block; i < block + blocksize; i++) {
                        sum += fft[i].re();
                    }
                    sum /= blocksize;
                    int startX = block;
                    int startY = M.getHeight() - 120;
                    int sizeY = (int) (sum * 10);
                    if (sizeY < 0) {
                        g.drawRect(startX, startY, blocksize, -sizeY);
                    } else {
                        g.drawRect(startX, startY - sizeY, blocksize, sizeY);
                    }

                }

                for (int i = 0; i < fft.length; i++) {
                    int x = i / 4;
                    int y = (int) (fft[i].im() * 100 + 400);
                    //g.drawLine(x, y, x, y);
                }
                ArrayList<Integer> signChanges = new ArrayList<>();
                ArrayList<Double> possibleFreq = new ArrayList<>();
                g.setColor(Color.GREEN);
                for (int i = 0; i < fft.length / 2 - 1; i++) {
                    int x = i;
                    double t = fft[i].re();
                    double n = fft[i + 1].re();
                    if (Math.signum(n) != Math.signum(t)) {
                        g.drawLine(x, M.getHeight() - 20, x, M.getHeight());
                        signChanges.add(i);

                        possibleFreq.add(((double) i) / secondsInThisSample);
                    }
                }
                g.setColor(Color.BLACK);
                g.drawString("Sign changes: " + signChanges, 200, 130);
                g.drawString("Possible frequencies: " + possibleFreq, 200, 150);
                g.setColor(Color.BLACK);
                g.drawLine(0, M.getHeight() - 120, M.getWidth(), M.getHeight() - 120);
                for (int x = 0; x < M.getWidth(); x += 50) {
                    int ind = x;
                    double freq = ind / secondsInThisSample;
                    int f = (int) freq;
                    g.drawString(f + "", x, M.getHeight() - 5);
                }
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
        frame.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                dofft = !dofft;
                //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void mousePressed(MouseEvent e) {
                //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void mouseExited(MouseEvent e) {
                //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        });
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
                temp += "Offset between server multiplex time and began write to sox subprocess (this is the important one): " + offset;
                info = temp;
                M.repaint();
                if (aboveLong) {
                    int target = (int) Math.floor(Math.max(0, size * 0.98F));
                    /*while (getSize() > target) {
                        beginning = beginning.next;
                    }*/
                    if (seconds > 2) {
                        //beginning = null;
                    }
                    if (System.currentTimeMillis() - startAbove > 20000) {
                        System.exit(1);
                    }
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

    public static void onData(Chunk chunk) {
        offset = System.currentTimeMillis() - chunk.multiplexed / 1000000;
        offset2 = chunk.beganToReceive - chunk.multiplexed / 1000000;
        offset3 = chunk.received - chunk.multiplexed / 1000000;

        byte[] toWrite = chunk.contents;
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
