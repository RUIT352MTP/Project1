package RUBTClient;

import java.net.URL;
import java.net.URLConnection;
import java.util.Random;
import java.util.UUID;
import java.nio.ByteBuffer;
import java.io.DataInputStream;
import java.io.ByteArrayOutputStream;

public class Tracker {

	private int port; 				//port number of client is listening on
	private int file_length;		//file length of target file held by peer
	private int downloaded;			//number of bytes downloaded from peer
	private int	uploaded;			//number of bytes uploaded to peers
	private int interval;			//milliseconds expected between tracker announcements
	private String url; 				//url contructed for annoucning to the
	private String encodedInfoHash;	//escaped info hash of torrent info 
	private byte[] usrid;				//identifying peer id for client
	

	public Tracker(int file_length){
		this.downloaded = 0;
		this.uploaded = 0;
		this.file_length = file_length;
		randomID();
	}
	
	
	public void updateProgress(int downloaded, int uploaded){
		this.downloaded = downloaded;
		this.uploaded = uploaded;
	}

	
	public void constructURL(String announce_url, ByteBuffer info_hash, int port){   //construct url key/value pairs
		
		this.port = port;
		String info_hash_encoded = "?info_hash=" + encodeHash(info_hash);
		String peer_id = "&peer_id=" + usrid;
		String port_field = "&port=" + port;
		String download_field = "&downloaded=" + downloaded;
		String upload_field = "&uploaded=" + uploaded;
		String left =  "&left=" + (file_length - downloaded);
	
		//setUrl(announce_url + info_hash_encoded + peer_id + port + download_field + upload_field+ left);
		this.url = (announce_url + info_hash_encoded + peer_id + port_field + download_field + upload_field+ left);
	}
	
	
	public String encodeHash(ByteBuffer info_hash){
		String hash = "";
		for(int i =0; i < 20; i++){
			hash = hash + "%" + String.format("%02x", info_hash.get(i));
		}
		setEncodedInfoHash(hash);
		return hash;
	}
	
	
	public int sendEvent(String event, int current_downloaded) throws Exception{
		
		setDownloaded(current_downloaded);
		URL obj;
		if(event.equals("started")){
			obj = new URL(getUrl() + "&event=" + event);
		}else if(event.equals("completed") || event.equals("stopped")){
			obj = new URL(getUrl().substring(0,getUrl().indexOf("downloaded")) + "downloaded=" + downloaded + "&uploaded=" + getUploaded() + "&left=" + (getFile_length()-getDownloaded()) + "&event="+event);
		}else{
			return 0;
		}
		
		URLConnection connection = obj.openConnection();
		System.out.println(event + " event sent to tracker");
		return 1;
	}
	
	
	public byte[] requestPeerList(String event) throws Exception{   
		
		URL obj;
		if(event == null){
			obj = new URL(this.url); 
		}else{
			obj = new URL(this.url + "&event=" + event); 
		}
		URLConnection connection = obj.openConnection(); //sends request

		//int contentLength = connection.getContentLength();
		
		DataInputStream datastream = new DataInputStream(connection.getInputStream());
		ByteArrayOutputStream encoded_response = new ByteArrayOutputStream();
		int tracker_response = datastream.read();
		//String bencoded_response = "";
		while(tracker_response != -1){
			encoded_response.write(tracker_response);
			tracker_response=datastream.read();
		}
		encoded_response.close();
		return encoded_response.toByteArray();
	}
	
	/** uses UUID truncated for 18 characters, MS added at front
	 */
	 public final String uniqueID = UUID.randomUUID().toString();
	 
	public void randomID(){
	
		byte id[] = new byte[20];
		id= ("MS"+uniqueID.substring(0, 18)).getBytes();
		this.usrid = id;
		
	}
	
	
	public byte[] getUser_id(){
		return usrid;
	}
	
	
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public int getPort() {
		return this.port;
	}

	
	public void setPort_num(int port) {
		this.port = port;
	}

	
	public String getEncodedInfoHash() {
		return encodedInfoHash;
	}
	
	
	
	public void setEncodedInfoHash(String encodedInfoHash) {
		this.encodedInfoHash = encodedInfoHash;
	}

	
	public int getFile_length() {
		return file_length;
	}

	
	public void setFile_length(int file_length) {
		this.file_length = file_length;
	}

	
	public int getDownloaded() {
		return downloaded;
	}

	
	public void setDownloaded(int downloaded) {
		this.downloaded = downloaded;
	}

	
	public int getUploaded() {
		return uploaded;
	}

	
	public void setUploaded(int uploaded) {
		this.uploaded = uploaded;
	}
	
	
	public int getInterval() {
		return this.interval;
	}	
	
	
	public void setInterval(int interval) {
		this.interval = interval;
	}
}