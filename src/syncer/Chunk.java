/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package syncer;

import java.io.DataInputStream;
import java.io.IOException;
import static syncer.Syncer.SIZE;

/**
 *
 * @author leijurv
 */
public class Chunk {

    final long multiplexed;
    final long beganToReceive;
    final long received;
    final byte[] contents = new byte[SIZE];
    float[] floatVersion;

    public Chunk(DataInputStream in) throws IOException {
        multiplexed = in.readLong();
        beganToReceive = System.currentTimeMillis();
        in.readFully(contents);
        received = System.currentTimeMillis();
    }
}
