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
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;


public class Peer {
	static final byte[] BT_PROTOCOL = { 'B', 'i', 't', 'T', 'o',
		'r', 'r', 'e', 'n', 't', ' ', 'p', 'r', 'o', 't', 'o', 'c', 'o', 'l' 
	};
    String ip;
    int port;
    byte[] peer_id;
	byte[] info_hash;
	DataInputStream in;
	DataOutputStream out;
    TorrentInfo ti;
	Parser p;
	Socket socket = null;
	
    public Peer(String ip, int port, byte[] peer_id, Parser p) throws UnknownHostException, IOException{
        this.ip = ip;
        this.port = port;
        this.peer_id = peer_id;
		this.ti = p.ti;
		this.p = p;
		this.info_hash = ti.info_hash.array();

        
		
		// create a connection
		this.socket = new Socket(this.ip, this.port);
		this.socket.setSoTimeout(Parser.interval*1000);
		this.in = new DataInputStream(this.socket.getInputStream());
		this.out = new DataOutputStream(this.socket.getOutputStream());
		
		// validate the info hash and then start messaging 
		if(isValid(handshake())) 
			RUBTClient.writeToFile(beginMessaging());
		
		// close everything
		this.in.close();
		this.out.close();
		this.socket.close();
    }
    

	public byte[] handshake() throws IOException{
		
		byte[] send_message = new byte[68];
		byte[] receive_message = new byte[68];
		
		send_message[0] = 19;
		System.arraycopy(BT_PROTOCOL, 0, send_message, 1, BT_PROTOCOL.length);
		System.arraycopy(this.info_hash, 0, send_message, 28, 20);
		System.arraycopy(peer_id, 0, send_message, 48, 20);
		
		this.out.write(send_message);//make handshake
		this.out.flush();
		this.in.read(receive_message);
		
		System.out.println("Sent_Message:" + new String(send_message, "UTF-8"));
		System.out.println("Received_Message:" + new String(receive_message, "UTF-8"));
		
		return receive_message;
		
	}
	private boolean isValid(byte[] handshake) {
		
		if(handshake == null || handshake.length != 68 || handshake[0] != 19)//handshake validation
			return false;
		
		
		byte[] spliced_arr = new byte[19];
		System.arraycopy(handshake, 1, spliced_arr, 0, 19);
		if(!Arrays.equals(spliced_arr,Peer.BT_PROTOCOL))//handshake validation
			return false;
		
		spliced_arr = new byte[20];
		System.arraycopy(handshake, 28, spliced_arr, 0, 20);
		if(!Arrays.equals(spliced_arr,this.info_hash))
			return false;
		
		spliced_arr = new byte[20];
		System.arraycopy(handshake, 28, spliced_arr, 0, 20);
		return !Arrays.equals(spliced_arr,this.peer_id);
		
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

