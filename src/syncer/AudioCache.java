/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package syncer;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author leijurv
 */
public class AudioCache {

    int waitMS = 0;
    final Object lock = new Object();
    LinkedList<Chunk> beginning = null;

    public int getSize() {
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
    public void dupe(){
        Chunk ch=getChunk();
        addChunk(ch);
        addChunk(ch);
    }
    public void addChunk(Chunk bytes) {
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

    public Chunk getChunk() {
        Chunk chunk;
        do {
            chunk = getChunk0();
        } while (chunk == null);
        return chunk;
    }

    private Chunk getChunk0() {
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
                return null;
            }
            LinkedList<Chunk> temp = beginning;
            LinkedList prev = null;
            while (temp.next != null) {
                prev = temp;
                temp = temp.next;
            }
            Chunk toReturn = temp.data;
            if (prev == null) {
                beginning = null;
                return toReturn;
            }
            prev.next = null;
            return toReturn;
        }
    }

    public void sleepUntilSize(int target) throws InterruptedException {
        while (getSize() < target) {
            Thread.sleep(1);
        }
    }

}
