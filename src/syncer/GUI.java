/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package syncer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import static syncer.Syncer.BYTES_PER_SEC;
import static syncer.Syncer.SIZE;

/**
 *
 * @author leijurv
 */
public class GUI {

    static float[] mostRecentSample = null;
    static Complex[] fft = null;
    static JComponent M;
    public static String info = "";
    static boolean dofft = false;

    public static void begin() throws InterruptedException {
        M = new JComponent() {
            @Override
            public void paintComponent(Graphics g) {
                long a = System.currentTimeMillis();
                paintComponent1(g);
                long b = System.currentTimeMillis();
                g.drawString("GUI render time: " + (b - a) + "ms", M.getWidth() - 200, M.getHeight() - 15);
            }

            public void paintComponent1(Graphics g) {
                ((Graphics2D) g).setStroke(new BasicStroke(0.01F));
                g.drawString("Click to toggle FFT", 400, 50);
                String[] lol = info.split("\n");
                for (int i = 0; i < lol.length; i++) {
                    g.drawString(lol[i], 10, 10 + i * 15);
                }
                double secondsInThisSample = (((double) SIZE) / BYTES_PER_SEC);
                /*for (int i = 0; i < bleh.length; i++) {
                    int x = i / 4;
                    int y = (int) (bleh[i] * 100 + 300);
                    g.drawRect(x, y, 0, 0);
                }*/

                synchronized (Syncer.cache.lock) {
                    int size = Syncer.cache.getSize();
                    LinkedList<Chunk> curr = Syncer.cache.beginning;

                    float dotsPerPixel = 4F;
                    final float visCenter = M.getHeight() / 2F;
                    final float visSize = M.getHeight() / 5F;
                    float width = mostRecentSample.length / dotsPerPixel;
                    float start = 10;
                    g.setColor(Color.BLUE);
                    for (int i = -1; i <= 1; i++) {
                        ((Graphics2D) g).draw(new Line2D.Float(0, visCenter + visSize * i, start, visCenter + visSize * i));
                    }
                    g.setColor(Color.BLACK);
                    for (int i = 0; i <= size; i++) {
                        int location = size - i;
                        float xStart = width * location + start;
                        if (xStart < M.getWidth()) {
                            float[] bleh = i == size ? mostRecentSample : curr.data.floatVersion;
                            /*
                                for (int j = 0; j < bleh.length; j++) {
                                    float x = j / dotsPerPixel + xStart;
                                    float y = bleh[j] * (M.getHeight() / 5) + M.getHeight() / 2;
                                    ((Graphics2D) g).draw(new Line2D.Float(x, y, x, y));
                                    //g.drawRect(x, y, 0, 0);
                                }*/
                            for (int j = 0; j < bleh.length - 1; j++) {
                                float x1 = j / dotsPerPixel + xStart;
                                float y1 = bleh[j] * visSize + visCenter;
                                float x2 = (j + 1) / dotsPerPixel + xStart;
                                float y2 = bleh[j + 1] * visSize + visCenter;
                                ((Graphics2D) g).draw(new Line2D.Float(x1, y1, x2, y2));
                                //g.drawRect(x, y, 0, 0);
                            }
                        }
                        if (i != size) {
                            curr = curr.next;
                        }
                    }
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

    }

    public static void onData(Chunk chunk) {

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
        float[] data = chunk.floatVersion;
        if (dofft) {
            Complex[] input = new Complex[data.length];
            for (int i = 0; i < input.length; i++) {
                input[i] = new Complex(data[i], 0);
            }
            fft = FFT.fft(input);
        }
        mostRecentSample = data;
        if (M != null) {
            //M.repaint();
        }
        //System.out.println(System.currentTimeMillis() - start);
    }
}
