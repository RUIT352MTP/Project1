package RUBTClient;

import java.io.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.nio.ByteBuffer;
import java.net.ServerSocket;
import java.net.Socket;

import GivenTools.BencodingException;
import GivenTools.TorrentInfo;

public class RUBTClient extends Thread{
	public Tracker tracker;					
	public Destination destfile;
	public int uploaded;					
	public boolean keepRunning = true;			
	public final TorrentInfo torrentinfo;		
	public final List<Peer> peers = Collections.synchronizedList(new LinkedList<Peer>());			 
	public final List<Peer> blocking_peers = Collections.synchronizedList(new LinkedList<Peer>());
	ExecutorService workers = Executors.newCachedThreadPool();	
	
	protected Socket incomingSocket;
	protected ServerSocket serverSocket;
	protected DataInputStream listenInput;
	protected DataOutputStream listenOutput;
	protected Handshake listener;
	
	private int	port = 0;					
	private int downloaded = 0;					
	private final int max_request = 16384;		
	private static boolean seeding;

	private int	unchokeLimit = 3;
	private volatile int unchokedPeers = 0;
	

	private final Timer trackerTimer = new Timer("trackerTimer",true);						
	private TrackerAnnounceTask trackerTask;

	private final Timer optimisticTimer = new Timer("optimisticTimer",true);
	private OptimisticChokeTask optimisticTask;
	
	private final LinkedBlockingQueue<PeerMsg> tasks = new LinkedBlockingQueue<PeerMsg>();   

	public RUBTClient(Destination destfile){
		this.destfile = destfile;
		this.torrentinfo = destfile.tinfo();
		this.tracker = new Tracker(this.torrentinfo.file_length);
	}
	
	public static void main(String[] args){
		
		
		if (args.length != 2){
			System.err.println("Usage: java RUBT <torrent> <destination>");//2 args
			return;
		}
		
		
		String torrentname = args[0];
		String destination = args[1];
		
		FileInputStream fileInputStream = null;
		File torrent = new File(torrentname);
		
		
		TorrentInfo torrentinfo = null;
		
		byte[] torrentbytes = new byte[(int)torrent.length()];
		
		try {
			fileInputStream = new FileInputStream(torrent);
			fileInputStream.read(torrentbytes);
			fileInputStream.close();
		}catch (Exception e){
			e.printStackTrace();
		}
		
		try {
			torrentinfo = new TorrentInfo(torrentbytes);
		} catch (BencodingException e) {
			System.err.println("Beencoding Exception!");
			e.printStackTrace();
		}
		
		Destination destfile = new Destination(torrentinfo, destination);
		
		File mp4 = new File(destination);
		boolean file_complete= false;
		
		if (mp4.exists()){
			file_complete = destfile.checkF();
		}else {
			destfile.fileS();
		}
		
		if (file_complete){
			seeding = true;
		}
		
		destfile.bitupdate(); 
		RUBTClient client = new RUBTClient(destfile); 
		
		destfile.initclient(client);		
		System.out.println(torrentinfo.file_length);
		System.out.println(torrentinfo.piece_length);
		System.out.println(torrentinfo.file_length/torrentinfo.piece_length);
		client.start();			
	}
	
	private static class TrackerAnnounceTask extends TimerTask {
		
		private final RUBTClient client;
		public TrackerAnnounceTask(final RUBTClient client){
			this.client = client;
		}
		
		public void run(){
						Communicator peer_list = this.client.contactTracker(null);
			List<Peer> newPeers = peer_list.cPeer();
			
						this.client.addPeers(newPeers);  
			
			System.out.println("*tracker timer*");
			System.out.println("interval: " + this.client.tracker.getInterval());
			
			int interval = this.client.tracker.getInterval();
			
			if(interval > 180  || interval < 60){
				interval = 180;
			}
			this.client.trackerTimer.schedule(new TrackerAnnounceTask(this.client), interval * 1000);
		}
	}
	
	
	private static class OptimisticChokeTask extends TimerTask{	
		private final RUBTClient client;

		public OptimisticChokeTask(final RUBTClient client){
			this.client = client;
		}

		public void run(){
			
			List<Peer> choked_peers = new LinkedList<Peer>();
			
			double bytes_per_second = 0;
			double lowest_bps = Integer.MAX_VALUE;
			
			if (client.peers.size() < 1) return;
			
			Peer dropped_peer = client.peers.get(0);
			Peer picked_up_peer = null;
			Message message = new Message();
			
			boolean seeding = client.getSeeding();  
			System.out.println(“Unchoking");
			System.out.println("seeding: " + seeding);
			for (Peer peer: client.peers){
				if (!peer.isChoking()){
					if (seeding){
						bytes_per_second = peer.sent_bps;
					}else {
						bytes_per_second = peer.received_bps;
					}
					
					System.out.println(peer.getPeer_id() + " performance: " + bytes_per_second + "bps");
					
					if(bytes_per_second <  lowest_bps){
						lowest_bps = bytes_per_second;
						dropped_peer = peer;
					}
				}
			}
			
			for (Peer peer: client.peers){
				if (peer.isChoking()){
					choked_peers.add(peer);
				}
			}
			
			if(client.unchokedPeers > 1){
				dropped_peer.sendMessage(message.getChoke());
				dropped_peer.setChoking(true);
				client.decrementUnchoked();
			}
			
			System.out.println("Peer: " + dropped_peer.getPeer_id() + " has been choked");
			
			Random randomGenerator = new Random();
			
			if (choked_peers.size() > 0){ 
				picked_up_peer = choked_peers.get(randomGenerator.nextInt(choked_peers.size()));
				picked_up_peer.sendMessage(message.getUnchoke());
				picked_up_peer.setChoking(false);
				client.incrementUnchoked();
				System.out.println("Peer: " + picked_up_peer.getPeer_id() + " has been unchoked");
			}
			System.out.println("downloaded "+ client.downloaded);
			System.out.println("@@@@@@@@@@@@@  Optimizely Time done  @@@@@@@@@@@@");
		}
	}
	
	public void run(){
		
		listener = new Handshake(this);
		listener.start();
		
		
	

		ShutdownHook hook = new ShutdownHook(this);
		hook.attachShutdownHook();
		startInputListener();
		
		final Message message = new Message();
		
		while(this.port == 0){
		}
		Communicator peer_list = contactTracker("started");
		addPeers(peer_list.getValidPeers());
		{	
			int interval = peer_list.interval;
			if(interval <= 0 || interval >= 180){
				interval = 120;
			}
			System.out.println("tracker announce interval: " + interval);
			this.tracker.setInterval(interval);
			trackerTask = new TrackerAnnounceTask(this);
			this.trackerTimer.schedule(trackerTask, interval * 1000);
		}
		
		while(keepRunning){

			try{
				final PeerMsg task = this.tasks.take();
				
				this.workers.execute(new Runnable() {
					public void run(){
						byte[] msg = task.getMessage();
						Peer peer = task.getPeer();
						if (peer!= null && !peers.contains(peer)){
							return;
						}
						switch(msg[0]){  

							case Message.CHOKE:
								peer.setChoked(true);
								eraser(peer);   
								break;
							case Message.UNCHOKE:
								peer.setChoked(false);
								chooseAndRequestPiece(peer);
								break;			
							case Message.INTERESTED:
								System.out.println("Peer " + peer.getPeer_id() + " sent interested");
								peer.setRemoteInterested(true);
								if (unchokedPeers < unchokeLimit){ 
									peer.sendMessage(message.getUnchoke());   
									peer.setChoking(false);
									incrementUnchoked();   
									}
								break;
							case Message.HAVE:  
								if (peer.isChoked()){
									byte[] piece_bytes = new byte[4];
									System.arraycopy(msg, 1, piece_bytes, 0, 4);
									int piece = ByteBuffer.wrap(piece_bytes).getInt();
		
									destfile.divC(peer.getBitfield(), piece, true);
									
									if(destfile.pieceAdd(peer.getBitfield()) != -1 && !peer.getFirstSent()){
										peer.setInterested(true);
										peer.setFirstSent(true);
										peer.sendMessage(message.getInterested());
									}
									else{
										
									}
								}
								break;
							case Message.BITFIELD:  
								if (!peer.getFirstSent()){
									peer.setFirstSent(true);
									
								}else {
									peer.setConnected(false);
									removePeer(peer);
									return;
								}
								if (destfile.pieceAdd(peer.getBitfield()) != -1){ 
									peer.setInterested(true);
									peer.sendMessage(message.getInterested());
								}
								break;
							case Message.REQUEST:
								if(!isValidRequest(msg,peer)){  
									if(!peer.isChoking()){
									peer.setConnected(false);
									System.out.println("REQUEST CLOSING CONNECTION");
									removePeer(peer);
									}
								}
								break;
							case Message.PIECE:		
								if (!peer.isInterested()){ 
									peer.setConnected(false);
									removePeer(peer);
								}else {
									peer.received_bytes += msg.length;
									getNextBlock(msg,peer);
								}
								break;
							case Message.QUIT:	 	
								endEventLoop();
								break;
						}
					}
				});
			}catch (InterruptedException ie){
				System.err.println("caught interrupt. continuing anyway");
			}
		}
		cleanUp();
		return;
	}
	
	public void addPeers(List<Peer> newPeers){
		
		for (Peer peer: newPeers){
			peer.initclient(this);
			peer.start();
		}
		
		if(optimisticTask == null){ 
			optimisticTask = new OptimisticChokeTask(this);
			optimisticTimer.scheduleAtFixedRate(optimisticTask, 30 * 1000, 30 * 1000);
		}
	}
	
	public synchronized boolean alreadyConnected(byte[] peer_id){
		
		for (Peer peer: this.peers){
			if (Arrays.equals(peer.getPeer_id(),peer_id)){
				return true;
			}
		}
		return false;
	}
	
	
	public synchronized  void addMessageTask(PeerMsg task){
		tasks.add(task);
	}
	

	public synchronized void chooseAndRequestPiece(final Peer peer){
		int current_piece = 0;
	   	int offset_counter = 0;
	   	Message current_message = new Message();
	   	byte[] request_message;
		if (!peer.isChoked() && peer.isInterested()){ 
			current_piece = destfile.pieceAdd(peer.getBitfield());
			if (current_piece == -1){
				peer.setInterested(false);
				return;
			}
			destfile.progtake(current_piece);  //take the progress piece
			peer.setLastRequestedPiece(current_piece);
	 	   	offset_counter = destfile.pieces[current_piece].getOffset();
			if (offset_counter != -1){
				offset_counter += max_request;
			}else {
				offset_counter = 0;
			}
	   		request_message = current_message.request(current_piece, offset_counter, max_request);
	   		
	   		System.out.println("requesting piece " + current_piece);
			peer.sendMessage(request_message);
	   	}
	}
	
	private synchronized void addChunk(int piece, int offset,byte[] data){
		byte[] chunk = new byte[data.length-9];
		System.arraycopy(data, 9, chunk, 0, data.length-9);
		destfile.pieces[piece].assemble(chunk,offset);
	}
	
	private void getNextBlock(byte[] block,Peer peer){
		Message message = new Message();
		byte[] request;
		byte[] piece_bytes = new byte[4];
		byte[] offset_bytes = new byte[4];
		int small_request;
		System.arraycopy(block, 5, offset_bytes, 0, 4); //offset from piece
		System.arraycopy(block, 1, piece_bytes, 0, 4); //bytes from piece
		int offset = ByteBuffer.wrap(offset_bytes).getInt();  //make buffer
		int piece = ByteBuffer.wrap(piece_bytes).getInt();
		
		addChunk(piece,offset,block);  
		if ((piece == torrentinfo.file_length / torrentinfo.piece_length) && (offset + 2 * max_request > torrentinfo.file_length % torrentinfo.piece_length)){//end
			small_request = (torrentinfo.file_length%torrentinfo.piece_length)%max_request;
			
			if (small_request + offset == torrentinfo.file_length % torrentinfo.piece_length){
				if(destfile.addbit(piece)){ 
					this.downloaded += destfile.pieces[piece].data.length;
					System.out.println("Giving the last piece");
					
					System.out.println("Downloaded "+ downloaded);
					
					Peer[] array = peers.toArray(new Peer[peers.size()]);
					
					for(int i = 0; i < array.length; i++){
						array[i].sendMessage(message.getHaveMessage(piece_bytes));
					}
					
					chooseAndRequestPiece(peer);
				}
				else{
					removePeer(peer);
				}
			}else {
				small_request = (torrentinfo.file_length%torrentinfo.piece_length) % max_request;
				request = message.request(piece, offset + max_request, small_request);
				if (peer.isChoked()){			
	   				System.out.println("got choked out");
	   				return;
	   			}else {
					peer.sendMessage(request);
				}
			}
			
		}else if (offset + max_request == torrentinfo.piece_length){ 	//have last piece?
			if (destfile.addbit(piece)){
				this.downloaded += destfile.pieces[piece].data.length;
				
				for (Peer all_peer: this.peers){
					all_peer.sendMessage(message.getHaveMessage(piece_bytes));
				}
				chooseAndRequestPiece(peer); 		
			}
			else {
				removePeer(peer);
			}
		}else {
			if (peer.isChoked()){			
   				System.out.println("got choked out");
   				return;
   			}else {
				request = message.request(piece, offset + max_request, max_request);
				peer.sendMessage(request);
			}
		}
	}
	
	public Communicator contactTracker(String event){
		this.tracker.updateProgress(this.torrentinfo.file_length - this.destfile.moreC, this.uploaded);
		this.tracker.constructURL(this.torrentinfo.announce_url.toString(), this.torrentinfo.info_hash, this.port);
		byte[] response_string = null;
		try{
			response_string = this.tracker.requestPeerList(event);
		}catch (Exception e){
			System.err.println("exception thrown requesting peer list from tracker");
			e.printStackTrace();

			if (event == null || event.equals("stopped")  || event.equals("completed") ){
				System.err.println("RUBTClient contactTracker(): ");
			}else if (event.equals("started")){
				System.err.println("RUBTClient contactTracker(): failed to contact tracker on startup. quitting ...");
				quitClientLoop();
			}
		}
		
		if (response_string == null){
			System.err.println("RUBTClient contactTracker(): null response from tracker");
			quitClientLoop();
		}
		
		if(event != null && event.equals("completed")){
			System.out.println("\n  \n  ***************completed*************  \n \n ");
			System.out.println("incomplete: " + this.destfile.incomplete);
		}
		return (new Communicator(response_string));
	}
	
	public synchronized void addPeerToList(Peer peer){
		peers.add(peer);
	}
	          
	public void removePeer(Peer peer){
		if (peers.contains(peer)){
			System.out.println("closing connections for peer " + peer.getPeer_id());
			eraser(peer);
			peer.closeConnections();
			peers.remove(peer);
		}
	}
	
	private boolean isValidRequest(byte[] message,Peer peer){
		Message piece_message = new Message();
		byte[] index_bytes= new byte[4];
		byte[] begin_bytes = new byte[4];
		byte[] length_bytes = new byte[4];
		byte[] piece;
		System.arraycopy(message, 1, index_bytes, 0, 4);
		System.arraycopy(message, 5, begin_bytes, 0, 4);
		System.arraycopy(message, 9, length_bytes, 0, 4); 
		int index = ByteBuffer.wrap(index_bytes).getInt();  
		int begin = ByteBuffer.wrap(begin_bytes).getInt();
		int length = ByteBuffer.wrap(length_bytes).getInt();
		if((length > max_request || length <= 0)|| (index > destfile.pieces.length || index < 0) || (begin < 0 || begin > torrentinfo.piece_length)){
			//confirm pieces
			return false;
		}
		piece = piece_message.getPieceMessage(destfile, index_bytes, length, begin_bytes); 
		peer.sent_bytes += piece.length;
		uploaded += piece.length;
		peer.sendMessage(piece);  
		return true;
	}
	
	public class ShutdownHook{
		
		private RUBTClient client;  
		
		/**
		 * @param client RUBTClient thread that spawns shutdown hook and whose cleanUp method is used
		 */
		public ShutdownHook(RUBTClient client){
			this.client = client;
		}
		
		/**
		 * Calls client's cleanup method if client is unexpectedly shutdown and client thread
		 * no longer alive 
		 */
		public void attachShutdownHook(){
			Runtime.getRuntime().addShutdownHook(new Thread(){
				public void run(){
					if(client.isAlive()) 
						client.cleanUp();
				}
			});
		}
	}
	
	public void closeAllConnections(){
		for(Peer peer: this.peers){
			peer.closeConnections();
		}
		for(Peer peer: this.blocking_peers){
			peer.closeConnections();
		}
		
		try {
			if(serverSocket != null) serverSocket.close();
			if(incomingSocket != null) incomingSocket.close();
			if(listenOutput != null) listenOutput.close();
			if(listenInput  != null) listenInput.close();
			
		} catch (IOException e) {
			System.err.println("RUBTClient.java closeAllConnections: error while shuting down listener port");
		}
		System.out.println("All connections closed");
	}
	
	public void quitClientLoop(){
		endEventLoop();
		Message quit_message = new Message();
		PeerMsg quit_task = new PeerMsg(null, quit_message.getQuitMessage());
		addMessageTask(quit_task);
	}
	
	public void cleanUp(){
		closeAllConnections();
		contactTracker("stopped");
		
		trackerTask.cancel();
		trackerTimer.cancel();
		optimisticTask.cancel();
		optimisticTimer.cancel();
		
		this.workers.shutdownNow();
		System.out.println("Ending Client Program");
	}
	
	private void startInputListener(){
		this.workers.execute(new Runnable(){
			/** 
			 * Worker threads that listens in console for user input to quit program
			 */
			public void run(){
				Scanner scanner = new Scanner(System.in);
				while(true){
					if(scanner.nextLine().equals("quit")){
						quitClientLoop();
						break;
					}else{
						System.out.println("incorrect input. try typing \"quit\"");
					}
				}
			}
		});
	}
	
	public byte[] getbitfield(){
		return this.destfile.getMybitfield();
	}
	public int getPort(){
		return this.port;
	}
	public void setPort(int port){
		this.port = port;
	}
	public boolean getSeeding(){
		return seeding;
	}
	public void setSeeding(){
		seeding = true;
	}
	private synchronized void endEventLoop(){
		this.keepRunning = false;
	}
	private synchronized void incrementUnchoked(){
		unchokedPeers++;
	}
	private synchronized void decrementUnchoked(){
		unchokedPeers--;
	}	
	private void eraser(Peer peer){
		destfile.eraser(peer.getLastRequestedPiece());
	}
}
