package RUBTClient;

import java.util.Arrays;
import java.net.ServerSocket;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;

public class Handshake extends Thread{
	private final RUBTClient cMain;
	public Handshake(final RUBTClient cMain){
		this.cMain = cMain;
	}
	
	private byte[] hCheck(byte[] shake){	
		byte[] info = new byte [20];
		System.arraycopy(shake, 28, info, 0, 20); 
		
		byte[] ID = new byte[20];
		System.arraycopy(shake, 48, ID, 0, 20);

		if (Arrays.equals(info, this.cMain.torrentinfo.info_hash.array())){ 
			return ID;
		} else {
			return null;
		}
	}
	
	public void run(){
		boolean port = false;
		cMain.setPort(6881);
		cMain.serverSocket = null;
		
		while(cMain.getPort() < 6890 && !port){
			try {
				cMain.serverSocket = new ServerSocket(cMain.getPort());
				port = true;
			} 
			catch (IOException e) {
				cMain.setPort(cMain.getPort() + 1);
			}
		}
		
		if(cMain.getPort() > 6889){
			System.err.println("No port applicable, terminating");
			cMain.quitClientLoop();
			
			return;
		}
		
		while (cMain.keepRunning){
			try{
				if(Thread.currentThread().isInterrupted()){
					System.out.println("thread failure");
					
					break;
				}
				
				if(cMain.serverSocket == null){
					System.err.println("null connection");
					cMain.quitClientLoop();
				}

				cMain.incomingSocket = cMain.serverSocket.accept();
				cMain.listenInput = new DataInputStream(cMain.incomingSocket.getInputStream());
				cMain.listenOutput = new DataOutputStream(cMain.incomingSocket.getOutputStream());
				Peer peer = new Peer(cMain.incomingSocket, cMain.listenInput, cMain.listenOutput);
				Message msg = new Message();
				byte[] handshake;
				byte[] peerid;
				peer.sendMessage(msg.handShake(cMain.torrentinfo.info_hash.array(), cMain.tracker.getUser_id()));
				handshake = peer.handshake();
				
				if(handshake == null){
					continue;
				}
				
				peerid=hCheck(handshake);
				
				if(peerid == null){
					System.out.println("peer ID not received");
					peer.closeConnections();
					continue;
				}
				
				peer.setPeer_id(peerid);
				
				System.out.println("peer id: " +  peerid);
				
				peer.setClient(cMain);
				peer.setConnected(true);
				peer.start();
				
			}catch(EOFException e){
				System.err.println("Tracker connected to client");
			}catch(IOException ioe){
				System.out.println("IOexception");
			}catch(Exception e){
				System.err.println("Exception");
			}
		}
		System.out.println("Listener terminated");
		return;
	}
	
}	
