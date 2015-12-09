package RUBTClient;

import java.net.*;
import java.io.*;
import java.util.Arrays;
import java.util.Date;
import java.util.TimerTask;
import java.util.Timer;

//methods for creating/maintaining connections between Client and its Peers

public class Peer extends Thread {
	
	private String  			ip;
	private int 				port;				
	private byte[] 				peer_id;			
	
	private Socket peer_socket;
	private DataInputStream peer_iStream;
	private DataOutputStream peer_oStream;
	
	private boolean 			choked; 			
	private boolean 			choking; 	
	private boolean 			incoming;
	private boolean			 	connected;			
	private boolean 			interested;
	private boolean 			remote_interested;
	
	private byte[] hs_response;
	private byte[] 				bitfield;
	
	
	private Date sentLast;
	private boolean sentFirst;
	private RUBTClient 			client;
	private PeerMsg 			message;
	
	private int reqLast;
	
	protected double upload_bps;
	protected double upload_bytes;
	protected double download_bps;
	protected double download_bytes;
	
	private Timer 				sendTimer; 		
	private SendTimerTask sendTimerTask;
	
	private Timer					performanceTimer;
	private PerformanceTimerTask performanceTimerTask;

	//constructor for when client connects to peers
	public Peer(String ip, byte[] peer_id, Integer port) {

		this.ip = ip;
		this.port = port;
		this.peer_id = peer_id;
		this.peer_socket = null;
		this.peer_iStream = null;
		this.peer_oStream = null;
		this.incoming = false;
		this.connected = false;
		this.interested = false;
		this.sentFirst = false;
		this.choked = true;
		this.choking = true;
		upload_bytes = 0;
		upload_bytes = 0;
		download_bps = 0;
		download_bytes = 0;
		sentLast = new Date();
		sendTimer = new Timer("timer",true);
		performanceTimer = new Timer("performance",true);
	}
	
	//constructor for when peers attempt to connect to client
	public Peer(Socket peer_socket, DataInputStream peer_iStream, DataOutputStream peer_oStream) {
		this.peer_socket = peer_socket;

		try {
			this.peer_socket.setSoTimeout(125 * 1000);
		} catch (SocketException e) {
			System.out.println("SocketException");
		}

		this.connected = false;
		this.interested = false;
		this.sentFirst = false;
		this.choked = true;
		this.choking = true;
		this.incoming = true;
		this.peer_iStream = peer_iStream;
		this.peer_oStream = peer_oStream;
		upload_bps = 0;
		upload_bytes = 0;
		download_bps = 0;
		download_bytes = 0;
		sentLast = new Date();
		sendTimer = new Timer("timer",true);
		performanceTimer = new Timer("performance",true);
	}

	public byte[] handshake(){

		byte[] phandshake = new byte[68];
		try {
			peer_iStream.readFully(phandshake);
		}catch (EOFException e){
			close();
			return null;
		}catch (IOException e1){
			System.err.println("error");
			this.client.blocking_peers.remove(this);
			close();
			return null;
		}
		return phandshake;
	}

	public void run(){
		
		byte[] client_bitfield;
		byte[] handshake;
		
		System.out.println("check peer: " + this.get_peer_id());
		if(this.peer_socket == null  && !this.peerConnect()){
			System.out.println("error");
			return;
		}
		if (this.client.alreadyConnected(this.peer_id)){
			System.out.println("error");
			if(incoming) this.close();
			return;
		}
		Message current_message = new Message();

		if(!incoming){
			this.client.blocking_peers.add(this);
			this.send(current_message.handShake(this.client.torrentinfo.info_hash.array(), this.client.tracker.getUser_id()));
			handshake = this.handshake();
			if(handshake == null){
				return;
			}else if(!handshakeCheck(handshake)){
				this.close();
				return;
			}
			this.client.blocking_peers.remove(this);
		}
		client_bitfield = current_message.getBitFieldMessage(this.client.destfile.getMybitfield());

		this.send(client_bitfield);
		
		this.client.addPeerToList(this);
		System.out.println("Added peer: " + this.peer_id);

		performanceTimerTask = new PerformanceTimerTask(this);
		this.performanceTimer.scheduleAtFixedRate(performanceTimerTask, 2*1000 ,2 * 1000);
		
		while (connected){ 
			try {
				Thread.sleep(1*50);
				try {
					if(peer_iStream.available() == 0){
						continue;   
					}
				}catch (IOException e){
					return;
				}
				
				int length_prefix = peer_iStream.readInt();
					if(length_prefix <=0){
					continue;
				}
				
				hs_response = new byte[length_prefix];
				peer_iStream.readFully(hs_response);
				
				if(hs_response[0] == Message.BITFIELD&& sentFirst ==false){
					bitfield = new byte[length_prefix-1];
					System.arraycopy(hs_response,1,this.bitfield,0,bitfield.length);
				}
				message = new PeerMsg(this, hs_response);
				client.addMessageTask(message); 
			}catch (EOFException e) { 
					continue;
			}catch (Exception e){
				System.err.println("error");
				e.printStackTrace();
			}
		}
		return;
	}

	private static class PerformanceTimerTask extends TimerTask{

		private Peer peer;

		public PerformanceTimerTask(Peer peer){
			this.peer = peer;
		}

		public void run(){

			if (!peer.choked){
				peer.download_bps = ((0.65 * peer.download_bps) + (0.35 * peer.download_bytes))/2;
				if(peer.download_bps < 100) peer.download_bps = 0;
				peer.download_bytes = 0;
			}
			if (!peer.choking){
				peer.upload_bps = ((0.65 * peer.upload_bps) + (0.35 * peer.upload_bytes))/2;
				if(peer.upload_bps < 100) peer.upload_bps = 0;
				peer.upload_bytes = 0;
			}
		}
	}

	private static class SendTimerTask extends TimerTask{
		private Peer peer;
		public SendTimerTask(Peer peer){
			this.peer = peer;
		}

		public void run() {

			if(peer.connected&&(System.currentTimeMillis()-peer.get_sent_last()>=(150*1000))){
				System.out.println("keep alive");
				byte[] keep_alive = {0,0,0,0};
				peer.send(keep_alive);
			}
		}
	}
		
	public boolean peerConnect(){
		
		try {
			this.peer_socket = new Socket(ip, port);
			this.peer_socket.setSoTimeout(125 * 1000); //set the socket timeout for 2 minutes and 10 seconds
			this.peer_oStream = new DataOutputStream(peer_socket.getOutputStream());
			this.peer_iStream = new DataInputStream(peer_socket.getInputStream());
			connected = true;
		}catch (UnknownHostException e){
			System.err.println("error");
			return false;
		}catch (IOException e){
			System.err.println("error");
			return false;
		}
		sendTimerTask = new SendTimerTask(this);
		sendTimer.scheduleAtFixedRate(sendTimerTask, 0, 10*1000);
		sentLast.setTime(System.currentTimeMillis());
		
		return true;
	}

	public synchronized void send(byte[] Message){
		if (this.peer_oStream == null){
		}else {
			try {
				peer_oStream.write(Message);
			} catch (IOException e) {
				System.err.println("error");
				client.removePeer(this);
			}
		}
		sentLast.setTime(System.currentTimeMillis());
	}

	private boolean handshakeCheck(byte[] peer_handshake){	
		
		byte[] peer_infohash = new byte [20];
		System.arraycopy(peer_handshake, 28, peer_infohash, 0, 20); //copies the peer's infohash
		byte[] peer_id = new byte[20];
		System.arraycopy(peer_handshake,48,peer_id,0,20);//copies the peer id.
		
		if (Arrays.equals(peer_infohash, this.client.torrentinfo.info_hash.array())){  //returns true if the peer id matches and the info hash matches
			return true;
		}else {
			return false;
		}
	}

	public void close(){
		try {
			if (peer_iStream != null)
				peer_iStream.close();
			if (peer_oStream != null)
				peer_oStream.close();
			if (peer_socket != null)
				peer_socket.close();
			
			connected = false;
			this.stop();
			clean();
		}catch (IOException e){
			System.out.println("error");
			e.printStackTrace();
			return;
		}
	}
	private void clean(){
		
		if(sendTimerTask != null) sendTimerTask.cancel();
		if(sendTimer != null) sendTimer.cancel();
		if(performanceTimer != null) performanceTimer.cancel();
		if(performanceTimerTask != null ) performanceTimerTask.cancel();
	}
	boolean validPort = false;
	public String get_ip() {
		return ip;
	}
	public void set_client(RUBTClient client){
		this.client = client;
		this.bitfield = new byte[client.getbitfield().length];
	}
	public int get_req_last(){
		return reqLast;
	}
	public void set_connected(boolean connected){
		this.connected = connected;
	}
	public byte[] get_bitfield(){
		return this.bitfield;
	}
	public byte[] get_peer_id() {
		return peer_id;
	}
	public void set_peer_id(byte[] peer_id){
		this.peer_id = peer_id;
	}
	public int get_port() {
		return port;
	}
	public boolean is_interested() {
		return interested;
	}
	public boolean is_choked() {
		return choked;
	}
	public boolean is_choking() {
		return choking;
	}
	public void set_choking(boolean choking) {
		this.choking = choking;
	}
	public void set_choked(boolean state){
		this.choked = state;
	}
	public void set_interested(boolean state){
		interested = state;
	}
	public void set_req_last(int last){
		this.reqLast = last;
	}
	public boolean get_sent_first(){
		return sentFirst;
	}
	public void set_remote_interested(boolean interested) {
		this.remote_interested = interested;
	}
	public void set_sent_first(boolean first_sent){
		this.sentFirst = first_sent;
	}
	public boolean same(Peer peer){
		return(this.ip == peer.get_ip() && this.peer_id.equals(peer.get_peer_id()));
	}
	public long get_sent_last(){
		return sentLast.getTime();
	}


}
