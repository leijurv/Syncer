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
import java.util.*;
import javax.sound.sampled.*;
import java.io.*;
import java.net.*;

/**
 *
 * @author leijurv
 */
public class Syncer {

    static final int SIZE = 4096;
    static final int BYTES_PER_SEC = 44100 * 4;
    static boolean gui = true;
    static  boolean isopen=false;
    static String ip="45.55.31.91";
    static float target=0.5F;
    static float range=0.05F;
    static boolean avg=false;
    static float maxcache=target+range/2;
    static float mincache=target-range/2;

    static float[] rollingAvg=new float[100];
    static int indexInRoll=0;
    static boolean fullroll=false;

    static String currentlyPlaying = "";

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
        // if (!new File("/usr/local/bin/sox").exists()) {
        //     JOptionPane.showInputDialog("You need sox. brew install sox");
        //     System.out.println("You need sox. brew install sox");
        //     return;
        // }

        boolean verbose = Arrays.asList(args).contains("verbose");
        boolean noGui = Arrays.asList(args).contains("nogui");
        boolean yesGui = Arrays.asList(args).contains("gui");
        yesGui=true;
        boolean customIP = Arrays.asList(args).contains("ip");
        boolean dontcut = Arrays.asList(args).contains("nocut");
        if (!noGui && !yesGui) {
            gui = 0 == JOptionPane.showConfirmDialog(null, "Would you a GUI or nah?", "It'll work either way", JOptionPane.YES_NO_OPTION);
        } else {
            if (yesGui && noGui) {
                throw new ArrayIndexOutOfBoundsException("no");
            }
            gui = yesGui;
        }
         System.out.println(customIP);
        if (customIP) {
            for (int i = 0; i < args.length; i++) {
                System.out.println(args[i]);
              if (args[i].contains("ip")) {
               ip = args[i+1];
              }
            }
        }
        System.out.println("Connecting to " + ip);
        InputStream in = new Socket(ip, 5021).getInputStream();
        new CacheSource(new DataInputStream(in), cache).start();
        cache.sleepUntilSize(30);
        //Process sox = new ProcessBuilder("/usr/local/bin/sox -t raw -r 44100 -b 16 -c 2 --buffer 4096 -e signed-integer - -tcoreaudio --buffer 4096".split(" ")).start();
        if (verbose) {
            System.out.println("Starting to play");
        }
        /*new Thread() {
            @Override
            public void run() {
                try {
                    int j;
                    while ((j = sox.getInputStream().read()) >= 0) {
                        System.out.write(j);
                    }
                } catch (IOException ex) {
                    Logger.getLogger(Syncer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }.start();
        new Thread() {
            @Override
            public void run() {
                try {
                    while (sox.getErrorStream().read() >= 0) {//gotta do this beacuse otherwise
                    }
                    int exitCode = sox.waitFor();
                    System.out.println("SOX EXITED WITH CODE " + exitCode);
                    Thread.sleep(1000);
                    System.exit(exitCode);//this is a sketchy way to just make it exit once the sox subprocess stops
                } catch (InterruptedException | IOException ex) {
                    Logger.getLogger(Syncer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }.start();*/
        final AudioFormat af=new AudioFormat(44100,16,2,true,false);
        SourceDataLine linee=AudioSystem.getSourceDataLine(af);
        
        linee.open(af,4096);
                            linee.start();
                            System.out.println("Buffer size: "+linee.getBufferSize());
       
        //byte[] y=temp.toByteArray();
        //linee.write(y,0,y.length);
        //linee.drain();
        //linee.close();
        new Thread() {
            @Override
            public void run() {
                try {
                    while (true) {
                        Chunk chunk = cache.getChunk();
                        if (!isopen){
                            
                            isopen=true;
                        }
                        //linee.drain();
                        
                        //sox.getOutputStream().write(chunk.contents);
                        if (gui) {
                            GUI.onData(chunk);
                        }
                        long lad = System.currentTimeMillis();
                        offset = lad - chunk.multiplexed / 1000000;
                        offset2 = chunk.beganToReceive - chunk.multiplexed / 1000000;
                        offset3 = chunk.received - chunk.multiplexed / 1000000;
                        linee.write(GUI.rec?new byte[chunk.contents.length]:chunk.contents,0,chunk.len);
                        //sox.getOutputStream().flush();
                       
                        long wew = System.currentTimeMillis();
                        offset4 = wew - lastWrite;
                        lastWrite = wew;
                        
                        offset5 = wew - chunk.multiplexed / 1000000;
                        //System.out.println("wew");
                    }
                } catch (Exception ex) {
                    Logger.getLogger(Syncer.class.getName()).log(Level.SEVERE, null, ex);
                    System.out.println("Wow i'm bad");
                    System.out.println("Wow i'm bad");
                    System.out.println("Wow i'm bad");
                    System.out.println("Wow i'm bad");
                    System.out.println("Wow i'm bad");

                }
            }
        }.start();
        new Thread() {
            public void run() {
                while (true){
                try {
                    System.out.println("UMM");
                    Socket querySocket = new Socket(ip, 5023);
                    System.out.println("UMM");
                    DataOutputStream outToServer = new DataOutputStream(querySocket.getOutputStream());
                    System.out.println("UMM");
                    //BufferedReader inFromServer = new BufferedReader(new InputStreamReader(querySocket.getInputStream()));
                    String query = "current";
                    System.out.println("UMM");
                    outToServer.writeBytes(query + '\n');
                    InputStream in=querySocket.getInputStream();
                  // querySocket.getOutputStream().close();
                    System.out.println("UMM");
                     String resp = "";
                       resp = new Scanner(in).nextLine();
                       System.out.println("meme: " + resp);
                       currentlyPlaying=resp;
                    System.out.println("FROM SERVER: " + resp);
                    querySocket.close();
                    
                } catch (Exception e) {
                    Logger.getLogger(Syncer.class.getName()).log(Level.SEVERE, null, e);
                }
                try{
                Thread.sleep(5000);
                } catch (Exception e) {
                    Logger.getLogger(Syncer.class.getName()).log(Level.SEVERE, null, e);
                }
            }
            }
        }.start();
        if (gui) {
            GUI.begin();
        }
        long startAbove = 0;


        new Thread(){
            public void run(){
               
                try{
                     while (true ){

                        if (GUI.dofft%3!=0)
                GUI.M.repaint();
                Thread.sleep(17);
            }
                } catch (Exception e) {
                    Logger.getLogger(Syncer.class.getName()).log(Level.SEVERE, null, e);
                }
                
            }
        }.start();


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

                rollingAvg[(indexInRoll++)%rollingAvg.length]=seconds;
                if(indexInRoll==rollingAvg.length){
                    fullroll=true;
                }

                float sum=0;
                if (avg){
                    for (int i=0; i<rollingAvg.length; i++){
                        sum+=rollingAvg[i];
                    }
                    sum/=rollingAvg.length;
                }else{
                    sum=rollingAvg[0];
                    for (int i=0; i<rollingAvg.length; i++){
                        if(rollingAvg[i]>sum){
                            sum=rollingAvg[i];
                        }
                    }
                }
                temp+=("Full roll: "+fullroll);
                temp+="\n";
                temp+=("Rolling average of cache size: "+sum);
                temp+="\n";

                temp += ("Number of seconds currently in cache: " + seconds);
                temp += "\n";
                boolean aboveLong = false;
                if (seconds > 2) {
                    if (startAbove == 0) {
                        startAbove = System.currentTimeMillis();
                    }

                    if (startAbove + 2000 < System.currentTimeMillis()) {

                        aboveLong = true;
                    }
                } else {
                    startAbove = 0;
                }
                float ratio=0.9F;

            if (!dontcut){
                if (fullroll && sum>maxcache){
                    System.out.println("Would cut");
                    ratio = (float)maxcache/(float)sum;

                    aboveLong=true;
                    fullroll=false;
                    indexInRoll=0;
                }
                if (fullroll && sum<mincache){
                    System.out.println("dupeing");
                    new Thread(){
                        public void run(){
                            Syncer.cache.dupe();
                        }
                    }.start();
                    
                    fullroll=false;
                    indexInRoll=0;
                }
            }
                temp += ("ms since zero cache: " + (size > 1 ? (System.currentTimeMillis() - startAbove) : 0));
                temp += "\n";
               //temp += ("Cutting: " + aboveLong);
                //temp += "\n";
                temp += "Offset between server multiplex time and receive: " + offset2;
                temp += "\n";
                //temp += "Offset between server multiplex time and fully received: " + offset3;
                //temp += "\n";
                temp += "Subprocess flush time: " + offset4;
                temp += "\n";
                /*temp += "Offset between server multiplex time and end write to sox subprocess: " + offset5;
                temp += "\n";
                temp += "Offset between server multiplex time and began write to sox subprocess: " + offset;
                temp += "\n";*/
                temp += "Currently playing: " + currentlyPlaying;
                temp += "\n";
                temp += "Hit p to search for a song on youtube";
                if (gui && GUI.M != null) {
                    GUI.info = temp;
                    GUI.M.repaint();
                }
                if (!dontcut && aboveLong) {
                    int target = (int) Math.floor(Math.max(1, size * ratio));
                    System.out.println("Cutting "+(Syncer.cache.getSize()-target)+" samples");
                    while (Syncer.cache.getSize() > target) {
                        Syncer.cache.getChunk();//pop from the end not the beginning
                    }
                    if (seconds > 2) {
                        //beginning = null;
                    }
                    /*if (System.currentTimeMillis() - startAbove > 20000) {
                        System.out.println("Exiting because the cache has been nonzero for more than 20 seconds");
                        Thread.sleep(1000);
                        System.exit(1);
                    }*/
                }
                long dank = System.currentTimeMillis() - lastWrite;
                if (dank > 5000 && dank < 50000) {
                    System.out.println("Haven't written in 5s, something's wrong");
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
