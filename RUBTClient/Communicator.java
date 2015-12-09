package RUBTClient;

import java.util.*;
import java.nio.ByteBuffer;
import GivenTools.Bencoder2;
import GivenTools.BencodingException;

public class Communicator {
	String msg;
	ArrayList<Peer> peers = new ArrayList<Peer>();
	List peerL;
	Integer lowI, interval;
	int c = 0;

	public Communicator(byte[] getrequest) {
		super();
		Map peerL=null;
        Object k = null, ERRORk = null;

		try { peerL = (Map) Bencoder2.decode(getrequest);
		} 
		catch (BencodingException e) {
			System.err.println("could not get request");
			e.printStackTrace();
		}
		final Iterator iE = peerL.keySet().iterator();
        while ((ERRORk = iE.next()) != null && iE.hasNext())
        {
        	String stringerror = ss((ByteBuffer) ERRORk);
        	if(stringerror.equals("failure reason")){
        		System.err.println("reason " + (Integer)peerL.get(ERRORk));
        	}
        }
		final Iterator Pitr = peerL.keySet().iterator();
        while (Pitr.hasNext() && (k = Pitr.next()) != null)//iterator
        {
        	String stkey = ss((ByteBuffer) k);
            if (stkey.equals("peers")){
            	ArrayList pL1 = (ArrayList) peerL.get(k);
            	Iterator pI1 = pL1.iterator();
            	
            	while(pI1.hasNext()){
            		HashMap peer=(HashMap) pI1.next();
            		Iterator morepeer=peer.keySet().iterator();
            		byte[] tID = null;
        			String tIP = null;
        			Integer tP = null;
            		
            		while(morepeer.hasNext()){
            			Object next = morepeer.next();
            			String temp_info = ss((ByteBuffer) next);
            			if(temp_info.equals("peer id")){
            				tID = ((ByteBuffer) peer.get(next)).array();
            			}else if(temp_info.equals("port")){
            				tP = (Integer) peer.get(next);
            			}else if(temp_info.equals("ip")){
            				tIP = ss((ByteBuffer) peer.get(next));
            			}
            		}
        			this.peers.add(new Peer(tIP, tID, tP));
            	}
            }else if(stkey.equals("interval")){
            	this.interval = (Integer) peerL.get(k);
            }else if(stkey.equals("min interval")){
            	this.lowI = (Integer) peerL.get(k);
            }
        }
	}
	public List<Peer> cPeer(){
		Iterator<Peer> itr = this.peers.iterator();
		List<Peer> cPeer = new ArrayList<Peer>();
		while(itr.hasNext()){
			Peer tmp1 = itr.next();
			if(((tmp1.get_ip().equals("128.6.171.130")) || (tmp1.get_ip().equals("128.6.171.131")))){
				cPeer.add(tmp1);
			}
		} return cPeer;
	}
	public static String ss(ByteBuffer buff){
		  StringBuilder s = new StringBuilder();
		  byte[] build=buff.array();
		  for(int i=0; i<build.length; ++i){
		    if(build[i] >= 32||build[i] <= 126){
		      s.append((char)build[i]);
		    }else {
		      s.append(String.format("%02x",build[i]));
		    }
		  }
		  return s.toString();
		}
	public void displayP() {
		Iterator<Peer> itr = this.peers.iterator();
		System.out.println("There are " + this.peers.size() + " peer(s):");
		while(itr.hasNext()){
			Peer tt = itr.next();
			System.out.println("Peer "+ ++c + "/"+this.peers.size()+":");
			System.out.println("IP: "+tt.get_ip());
			System.out.println("Peer ID: "+tt.get_peer_id());
			System.out.println("Port: "+tt.get_port());
		}
	}
}