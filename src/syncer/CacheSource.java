/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package syncer;

import java.io.DataInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import static syncer.Syncer.cache;

/**
 *
 * @author leijurv
 */
public class CacheSource extends Thread {

    final DataInputStream in;
    final AudioCache out;

    public CacheSource(DataInputStream in, AudioCache cache) {
        this.out = cache;
        this.in = in;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Chunk chunk = new Chunk(in);
                cache.addChunk(chunk);
            }
        } catch (Exception ex) {
            try {
                Logger.getLogger(Syncer.class.getName()).log(Level.SEVERE, null, ex);
                Thread.sleep(1000);
                System.out.println("LOL RIP");
                Thread.sleep(1000);
                System.exit(1);
            } catch (InterruptedException ex1) {
                Logger.getLogger(CacheSource.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }
    }
}
