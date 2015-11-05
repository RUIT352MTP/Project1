/* Timothy Choi
 * Prasant Sinha
 * Michael Shafran
 * 
 * */

package BT;

import GivenTools.*;
import java.io.*;
import java.nio.*;
import java.util.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Parser {

    private static final ByteBuffer KEY_PEERS = ByteBuffer.wrap(new byte[] {'p','e','e','r','s'});
    private static final ByteBuffer KEY_IP = ByteBuffer.wrap(new byte[] {'i','p'});
    private static final ByteBuffer KEY_PORT = ByteBuffer.wrap(new byte[] {'p','o','r','t'});
    private static final ByteBuffer KEY_PEER_ID = ByteBuffer.wrap(new byte[] {'p','e','e','r',' ','i','d'});
    private static final ByteBuffer KEY_INTERVAL = ByteBuffer.wrap(new byte[] {'i','n','t','e','r','v','a','l'});
    private static final ByteBuffer KEY_MIN_INTERVAL = ByteBuffer.wrap(new byte[] {'m','i','n',' ','i','n','t','e','r','v','a','l'});
    private static final ByteBuffer KEY_FAILURE_REASON = ByteBuffer.wrap(new byte[] {'f','a','i','l','u','r','e',' ','r','e','a','s','o','n'});

	private static final String PEER_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"; 
	private SecureRandom random = new SecureRandom();

    public final String uniqueID = UUID.randomUUID().toString();
    public final String peer_id = Typechange.bytesToURL(("MS"+uniqueID.substring(0, 18)).getBytes());
    private int downloaded = 0;
    private int uploaded = 0;
    private int left;
    private String info_hash;
    private String ip_addr;
    public static final int port = 11598;
    TorrentInfo ti;
    
    public static int interval = 0;
    private int min_interval = 0;
	public final String my_peer_id = getPeerID();

    public Parser(TorrentInfo ti){
        ip_addr = ti.announce_url.toString();
        info_hash = Typechange.bytesToURL(ti.info_hash.array());
        left = ti.file_length;//TODO
        this.ti = ti;
    }

    public String passUrl(){
        return ip_addr + "?info_hash=" + info_hash + "&peer_id=" + peer_id
                + "&port=" + port + "&uploaded=" + uploaded + "&downloaded="
                + downloaded + "&left=" + left;
    }

	public byte[] sendGet() throws MalformedURLException, IOException {
		URL url = new URL(ip_addr + "?info_hash=" + info_hash + "&peer_id=" + my_peer_id 
				+ "&port=" + port + "&uploaded=" + uploaded + "&downloaded="
				+ downloaded + "&left=" + left);
		HttpURLConnection http_conn = (HttpURLConnection) url.openConnection();
		http_conn.setRequestMethod("GET");
		
		byte[] databyte = null;
		try{
			InputStreamReader in = new InputStreamReader(http_conn.getInputStream());
			BufferedReader inB = new BufferedReader(in);
			String dataline;
			String data="";
			while((dataline=inB.readLine())!=null){
				data+=dataline;
				System.out.println(data);
			}
			in.close();
			databyte = data.getBytes();
			return databyte;//we are able to see the interaction
			
		} catch(IOException ioe){
			System.out.println(ioe.getMessage());
			throw ioe;
		}
	}
    
    public List<Peer> parseResponse(byte[] resp) throws BencodingException, UnknownHostException, IOException{

        List<Peer> peers_list = new ArrayList<Peer>();

        Map<ByteBuffer, Object> tracker = (Map<ByteBuffer, Object>) Bencoder2.decode(resp);

        if(tracker.containsKey(KEY_FAILURE_REASON)){
            System.out.println("ERROR: Failure Reason Key present");
        }

        if(tracker.containsKey(KEY_INTERVAL)){
            this.interval = ((Integer)tracker.get(KEY_INTERVAL)).intValue();
        }else{
            System.out.println("ERROR: Interval Key not present");
        }

        if(tracker.containsKey(KEY_MIN_INTERVAL)){
            this.min_interval = ((Integer)tracker.get(KEY_INTERVAL)).intValue();
        }else{
            System.out.println("ERROR: Minimum Interval Key not present");
        }

        for( Object temp: (ArrayList<?>)tracker.get(KEY_PEERS) ) {

            Map<ByteBuffer, Object> pair = (Map<ByteBuffer,Object>)temp;
            int port = ((Integer) pair.get(KEY_PORT)).intValue();
            String ip = new String(((ByteBuffer)pair.get(KEY_IP)).array());
            byte[] peer_id = ((ByteBuffer) pair.get(KEY_PEER_ID)).array();
            peers_list.add(new Peer(ip, port, peer_id, this));
        }
        ToolKit.print(Bencoder2.decode(resp));
        return peers_list;

    }

    public static String objectToStr(Object o){

        if(o instanceof Integer){
            return String.valueOf(o);
        } else if(o instanceof ByteBuffer){
            try {
                return new String(((ByteBuffer) o).array(),"ASCII");
            } catch (UnsupportedEncodingException e) {
                return o.toString();
            }
        }else if(o instanceof Map<?,?>){

            String retStr = "";
            for (Object name: ((Map<?, ?>) o).keySet()){
                String value = objectToStr(((Map<?, ?>) o).get(name));
                retStr += objectToStr(name) + ": " + value + "\n";
            }

            return retStr;
        }else if(o instanceof List){

            String retStr = "";
            for(Object elem: (List<?>)o){
                retStr += objectToStr(elem) + "\n";
            }
            return retStr;
        }
        return o.toString();
    }

	
	private String getPeerID(){
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < 20; i++) {//generates random string
			int index = random.nextInt(PEER_STRING.length());
			builder.append(PEER_STRING.charAt(index));
			if(builder.toString().length()==2&&builder.toString().substring(0,1)=="RU"){//can not be RU or RUBT
				getPeerID();
			}
		}
		return builder.toString();
	}

	public void DownloadedParts(int num){
		this.downloaded = num;
		this.left = this.ti.file_length - num;
	}

}