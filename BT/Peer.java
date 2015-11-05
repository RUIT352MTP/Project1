/* Timothy Choi
 * Prasant Sinha
 * Michael Shafran
 * 
 * */

package BT;

import GivenTools.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;

public class Peer {
    String ip;
    int port;
    byte[] peer_id;
	byte[] info_hash;
	DataInputStream in;
	DataOutputStream out;
	byte[] my_id;
    TorrentInfo ti;
	Parser p;
	Socket socket = null;
	
    public Peer(String ip, int port, byte[] peer_id){
        this.ip = ip;
        this.port = port;
        this.peer_id = peer_id;
        
    }

	private byte[] beginMessaging() {
		// Optional: send bitfield message, but I didn't
		byte[] thefile = new byte[ti.file_length];
		
		try {
			
			int length = this.in.readInt();
			this.in.readByte();
			readMessage(length-1);
			
			writeMessage(new PeerMsg(0,PeerMsg.Interested));
			
			while(readMessage(5)[4] != 1){ // loop until peer unchokes
				writeMessage(new PeerMsg(0,PeerMsg.Interested));
			}
			
			int rLen = 16384;
			int limit = ti.piece_hashes.length * (ti.piece_length/rLen);
			int bytesWritten = 0;
			
			for(int counter = 0; counter < limit; counter++){
				
				if(counter == limit-1)
					rLen = ti.file_length 
					- (ti.piece_length * (ti.piece_hashes.length - 1)) 
					- ( (ti.piece_length / rLen) - 1 ) * 16384;
				
				int start = (counter%2) * rLen;
				
				PeerMsg m = new PeerMsg(0,PeerMsg.Request);
				m.Payload(rLen, start, counter/2);
				writeMessage(m);
				readMessage(13); // don't care about <length-prefix><7> and <index><begin>
				System.arraycopy(readMessage(rLen), 0, thefile, bytesWritten, rLen);
				this.p.DownloadedParts(bytesWritten);
				this.p.sendGet();
				bytesWritten += rLen;
				
			}
			
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
		
		return thefile;
		
	}
	
	private void writeMessage(PeerMsg pm) throws IOException{
		this.out.write(pm.getMessage());
		this.out.flush();//sends message
		
	}
	
	private byte[] readMessage(int len) throws IOException{
		byte[] bArr = new byte[len];
		for(int i=0; i < len; i++)//reads response from peer
			bArr[i] = this.in.readByte();
		return bArr;
	}
}

