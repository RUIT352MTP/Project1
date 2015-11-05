/* Timothy Choi
 * Prasant Sinha
 * Michael Shafran
 * 
 * */

package BT;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

public class Handshake {
	private Socket s;
	public Handshake(Socket s){
		this.s = s;
	}
	
	public byte[] buildHandshake(byte[] info_hash, String peer){
		 //handshake
		ByteBuffer handshake =ByteBuffer.allocate(68);
        byte[] protocol_name = "BitTorrent protocol".getBytes();
        byte[] reserved = new byte[8];
        byte[] peer_id = peer.substring(0, 20).getBytes();
        byte len = 19;
        
        handshake.put(len);
        handshake.put(protocol_name);
        handshake.put(reserved);
        handshake.put(info_hash);
        handshake.put(peer_id);
        
        return handshake.array();
        
    
     
	}
	public byte[] readHandshake() throws IOException {
	    // Again, probably better to store these objects references in the support class
	    InputStream in = s.getInputStream();
	    DataInputStream dis = new DataInputStream(in);

	    
	    byte[] data = new byte[68];
	 
	        dis.readFully(data);
	    //in.close();
	    //dis.close();
	    return data;
	}
	public void sendHandshake(byte[] myByteArray, int start, int len) throws IOException {
	    if (len < 0)
	        throw new IllegalArgumentException("Negative length not allowed");
	    if (start < 0 || start >= myByteArray.length)
	        throw new IndexOutOfBoundsException("Out of bounds: " + start);
	    // Other checks if needed.

	    // May be better to save the streams in the support class;
	    // just like the socket variable.
	    OutputStream out = s.getOutputStream(); 
	    DataOutputStream dos = new DataOutputStream(out);

	    dos.writeInt(len);
	    if (len > 0) {
	        dos.write(myByteArray, start, len);
	    }
	    //out.close();
	    //dos.close();
	}
	 // optional
}
