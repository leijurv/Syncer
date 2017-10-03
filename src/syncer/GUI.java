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
import java.awt.event.*;
import java.awt.geom.Line2D;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.sound.sampled.*;
import javax.swing.*;
import java.util.*;
import java.util.ArrayList;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import static syncer.Syncer.BYTES_PER_SEC;
import static syncer.Syncer.SIZE;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.io.*;
import java.net.*;

/**
 *
 * @author leijurv
 */
public class GUI {

    static float[] mostRecentSample = null;
    static Complex[] fft = null;
    static JComponent M;
    public static String info = "";
    static boolean rec=false;
    static int dofft = 1;

    public static void begin() throws InterruptedException {
        M = new JComponent() {
            @Override
            public void paintComponent(Graphics g) {
                if (rec){
                    g.setColor(Color.RED);
                    g.drawOval(0,0,M.getWidth(),M.getHeight());
                }
                long a = System.currentTimeMillis();
                paintComponent1(g);
                long b = System.currentTimeMillis();
                g.setColor(Color.BLACK);
                g.drawString("GUI render time: " + (b - a) + "ms", M.getWidth() - 200, M.getHeight() - 15);
            }

            public void paintComponent1(Graphics g) {
                g.setColor(Color.BLACK);
                ((Graphics2D) g).setStroke(new BasicStroke(0.01F));
                g.drawString("Click to toggle FFT", 400, 50);
                String[] lol = info.split("\n");
                for (int i = 0; i < lol.length; i++) {
                    g.drawString(lol[i], 10, 10 + i * 15);
                }

                {
                    int curpos=Syncer.indexInRoll;
                    int ind0=((curpos-1)+Syncer.rollingAvg.length)%Syncer.rollingAvg.length;
                    float currSec=Syncer.rollingAvg[ind0];
                float sum=0;
                float sum2=0;
                {
                float[] rollingAvg=Syncer.rollingAvg;
               
                    for (int i=0; i<rollingAvg.length; i++){
                        sum+=rollingAvg[i];
                    }
                    sum/=rollingAvg.length;
              
                    sum2=rollingAvg[0];
                    for (int i=0; i<rollingAvg.length; i++){
                        if(rollingAvg[i]>sum2){
                            sum2=rollingAvg[i];
                        }
                    }
                }
                if (Syncer.avg){
                    float tmp=sum;
                    sum=sum2;
                    sum2=tmp;
                }

                int cx=M.getWidth()/2;
                float mul=600;
                float memelord=(Syncer.maxcache+Syncer.mincache)/2;
                g.setColor(Color.BLACK);
                for (int i=-1; i<=1; i++){
                    float lm=memelord+i*0.25F;
                    int xxxxx=(int)(cx+(lm-memelord)*mul);
                    g.drawLine(xxxxx,0,xxxxx,40);
                    g.drawString(lm+"",xxxxx,50);
                }

                g.drawLine((int)(cx+(Syncer.maxcache-memelord)*mul),0,(int)(cx+(Syncer.maxcache-memelord)*mul),30);
                g.drawLine((int)(cx+(Syncer.mincache-memelord)*mul),0,(int)(cx+(Syncer.mincache-memelord)*mul),30);
                g.setColor(Color.BLUE);
                int zer=(int)(cx+(0-memelord)*mul);
                g.drawLine(zer,0,zer,30);
                g.drawString("0",zer,40);
                if (!Syncer.fullroll){
                    g.setColor(Color.RED);
                }else{
                    g.setColor(Color.BLACK);
                }
                int xxxxx=(int)(cx+(sum-memelord)*mul);
                g.drawLine(xxxxx,0,xxxxx,60);
                g.drawString(sum+"",xxxxx,70);

                int xxxxxx=(int)(cx+(sum2-memelord)*mul);
                g.drawLine(xxxxxx,0,xxxxxx,80);
                g.drawString(sum2+"",xxxxxx,90);

                int vomit=20;

                g.drawLine((int)(cx+(currSec-memelord)*mul),0,(int)(cx+(currSec-memelord)*mul),vomit);

                
                g.setColor(Color.GREEN);
                //float mult=300;
                //float prevY=-1;
                //int xof=M.getWidth()/2;
                //float mult2=1.0F;
                
                float prevX=-1;
                for (int i=1; i<=Syncer.rollingAvg.length; i++){
                    int ind=((curpos-i)+Syncer.rollingAvg.length)%Syncer.rollingAvg.length;
                    float x=cx+(Syncer.rollingAvg[ind]-memelord)*mul;
                    if (prevX!=-1){
                        g.drawLine((int)prevX,i-1+vomit,(int)x,i+vomit);
                    }
                    prevX=x;
                    /*float y=M.getHeight()-mult*Syncer.rollingAvg[i];if (prevY!=-1)
                    g.drawLine((int)(xof+(i-1)*mult2),(int)prevY,(int)(xof+i*mult2),(int)y);
                    prevY=y;*/
                }

            }
            {
                
            }
            if(dofft%3==0){
                return;
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
                       float start = 20;
                    g.setColor(Color.BLUE);
                   
                    g.setColor(Color.BLACK);

                    float maxRenderedY=-1;
                    float minRenderedY=-1;
                    float offset=0;
                    int rng=400;
                    o:
                    for(int i=0; i<mostRecentSample.length; i++){
                        for(int j=i-rng; j<=i+rng; j++){
                            if(j>=0 && j<mostRecentSample.length && mostRecentSample[j]>mostRecentSample[i]){
                                continue o;
                            }
                        }
                        offset=i;
                        break;
                    }
                    for (int i = 0; i <= size; i++) {
                        float location = size - i+0.5f;
                        float xStart = width * location + start-offset/dotsPerPixel;
                        if (xStart < M.getWidth()) {
                            float[] bleh = i == size ? mostRecentSample : curr.data.floatVersion;
                            /*
                                for (int j = 0; j < bleh.length; j++) {
                                    float x = j / dotsPerPixel + xStart;
                                    float y = bleh[j] * (M.getHeight() / 5) + M.getHeight() / 2;
                                    ((Graphics2D) g).draw(new Line2D.Float(x, y, x, y));
                                    //g.drawRect(x, y, 0, 0);
                                }*/
                                

                                  float sumthing = 0;
                            for (int j = 0; j < bleh.length - 1; j++) {
                                float x1 = j / dotsPerPixel + xStart;
                                float y1 = bleh[j] * visSize + visCenter;
                                float x2 = (j + 1) / dotsPerPixel + xStart;
                                float y2 = bleh[j + 1] * visSize + visCenter;
                                if (y1<minRenderedY || minRenderedY==-1){
                                    minRenderedY=y1;
                                }
                                if (y1>maxRenderedY || maxRenderedY==-1){
                                    maxRenderedY=y1;
                                }
                                ((Graphics2D) g).draw(new Line2D.Float(x1, y1, x2, y2));
                                //g.drawRect(x, y, 0, 0);
                                sumthing += bleh[j];
                            }
                            if(i==size){
                                g.setColor(Color.BLUE);
                                   ((Graphics2D) g).draw(new Line2D.Float(offset/dotsPerPixel+xStart, minRenderedY, offset/dotsPerPixel+xStart, maxRenderedY));
                                }
                            float avg = sumthing / bleh.length;
                            if (location == 0) {
                             //((Graphics2D) g).draw(new Line2D.Float(20, avg*100, 420, avg*100));
                            }                        }
                        if (i != size) {
                            curr = curr.next;
                        }
                    }
                    g.setColor(Color.BLUE);
                     for (int i = -1; i <= 1; i++) {
                        ((Graphics2D) g).draw(new Line2D.Float(0, visCenter + visSize * i, start, visCenter + visSize * i));
                    }
                        ((Graphics2D) g).draw(new Line2D.Float(0, minRenderedY, start*3, minRenderedY));
                        ((Graphics2D) g).draw(new Line2D.Float(0, maxRenderedY, start*3, maxRenderedY));

                    
                }
                if (fft == null || dofft%3<2) {
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
                    g.drawLine(x, y, x, y);
                    g.drawRect(x, y, 1, y);
                }
                int blocksize = 100;
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
        JSlider volumeBoost = new JSlider(JSlider.VERTICAL, 0, 2, 1);
        volumeBoost.setPaintLabels(true);
        
        class SliderListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            JSlider source = (JSlider)e.getSource();
            if (!source.getValueIsAdjusting()) {
               System.out.println(source.getValue());
                
            }    
        }
    }
        volumeBoost.addChangeListener(new SliderListener());
        final JFileChooser fc = new JFileChooser();
        JFrame frame = new JFrame("Syncer");
        frame.setContentPane(M);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        //frame.setSize(500, 200);
        frame.setSize(10000, 10000);
        frame.setVisible(true);
        frame.addKeyListener(new KeyListener(){
              public void keyTyped(KeyEvent e) {
        System.out.println("Typed"+e.getKeyChar());
        if(e.getKeyChar()=='d'){
            System.out.println("DUPING");
            new Thread(){
                        public void run(){
                            Syncer.cache.dupe();
                        }
                    }.start();
        }
        if (e.getKeyChar()=='c'){
            System.out.println("CUTTING");
             synchronized (Syncer.cache.lock) {
                if (Syncer.cache.beginning!=null)
                Syncer.cache.beginning = Syncer.cache.beginning.next;
            }
        }
         if (e.getKeyChar()=='h'){
            System.out.println("CUTTING HALF");
             synchronized (Syncer.cache.lock) {
                if (Syncer.cache.beginning!=null)
                Syncer.cache.beginning.data.len-=512;
            }
        }
        if (e.getKeyChar()=='p'){
            new Thread(){
                public void run(){
                    String pl=""+JOptionPane.showInputDialog("What would you like to play? (first youtube search result will be played)");
                     try {
                        if(pl.equals("null") ||pl.equals("")){
                            return;
                        }
                    Socket querySocket = new Socket(Syncer.ip, 5023);
                    DataOutputStream outToServer = new DataOutputStream(querySocket.getOutputStream());
                 
                    outToServer.writeBytes(pl + '\n');
                    
                    querySocket.close();
                    
                } catch (Exception e) {
                    Logger.getLogger(Syncer.class.getName()).log(Level.SEVERE, null, e);
                }
                }
            }.start();
            
        }
        if(e.getKeyChar()=='f'){

             int returnVal = fc.showOpenDialog(frame);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                new Thread(){
                    public void run(){
                        try{
                            Socket xd=new Socket(Syncer.ip,5022);
                        FileInputStream in=new FileInputStream(file);
                        byte[] memer=new byte[65536];
                        int j=in.read(memer);
                        while(j>=0){
                            xd.getOutputStream().write(memer,0,j);
                            j=in.read(memer);
                        }
                        xd.close();
                        in.close();
                    }catch(Exception e){
                         Logger.getLogger(Syncer.class.getName()).log(Level.SEVERE, null, e);
                    }
                    }
                }.start();
            }
        }
        if (e.getKeyChar()==' '){
           /* try{
            Thread.sleep(500);
             }catch(Exception ee){
                         Logger.getLogger(Syncer.class.getName()).log(Level.SEVERE, null, ee);
                    }*/
            rec=!rec;
        }

    }

    /** Handle the key-pressed event from the text field. */
    public void keyPressed(KeyEvent e) {
    }

    /** Handle the key-released event from the text field. */
    public void keyReleased(KeyEvent e) {
    }
});
        new Thread(){
            public void run(){
                try{
                    while(true){
                        if(!rec){
                            Thread.sleep(5);
                            continue;
                        }
                        Socket s=new Socket(Syncer.ip,5024);
                        final AudioFormat af=new AudioFormat(44100,16,2,true,false);
            TargetDataLine line;
            DataLine.Info info=new DataLine.Info(TargetDataLine.class,af);
          
            line=(TargetDataLine) AudioSystem.getLine(info);
            line.open(af);
            line.start();
            int j;
            byte[] d=new byte[16384];
            OutputStream o=s.getOutputStream();
            int c=20;
            while (true){
                j=line.read(d,0,d.length);
                o.write(d,0,j);
                if (!rec){
                    c--;
                    if (c<0){
                        break;
                    }
                }
            }
            line.close();s.close();


                    }
                    }catch(Exception e){
                         Logger.getLogger(Syncer.class.getName()).log(Level.SEVERE, null, e);
                    }
            }
        }.start();
        frame.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                dofft ++;
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
        if (dofft%3>1) {
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

