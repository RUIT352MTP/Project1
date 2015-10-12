package BT;

import GivenTools.*;
import java.io.*;
import java.nio.*;
import java.util.*;

public class Parser {

    private static final ByteBuffer KEY_PEERS = ByteBuffer.wrap(new byte[] {'p','e','e','r','s'});
    private static final ByteBuffer KEY_IP = ByteBuffer.wrap(new byte[] {'i','p'});
    private static final ByteBuffer KEY_PORT = ByteBuffer.wrap(new byte[] {'p','o','r','t'});
    private static final ByteBuffer KEY_PEER_ID = ByteBuffer.wrap(new byte[] {'p','e','e','r',' ','i','d'});
    private static final ByteBuffer KEY_INTERVAL = ByteBuffer.wrap(new byte[] {'i','n','t','e','r','v','a','l'});
    private static final ByteBuffer KEY_MIN_INTERVAL = ByteBuffer.wrap(new byte[] {'m','i','n',' ','i','n','t','e','r','v','a','l'});
    private static final ByteBuffer KEY_FAILURE_REASON = ByteBuffer.wrap(new byte[] {'f','a','i','l','u','r','e',' ','r','e','a','s','o','n'});

   
    public final String uniqueID = UUID.randomUUID().toString();
    public final String peer_id = Typechange.bytesToURL(uniqueID.getBytes());
    private int downloaded = 0;
    private int uploaded = 0;
    private int left;
    private String info_hash;
    private String ip_addr;
    private final int port = 6881;

    private int interval = 0;
    private int min_interval = 0;

    public Parser(TorrentInfo ti){
        ip_addr = ti.announce_url.toString();
        info_hash = Typechange.bytesToURL(ti.info_hash.array());
        left = ti.file_length;//TODO
    }

    public String passUrl(){
        return ip_addr + "?info_hash=" + info_hash + "&peer_id=" + peer_id
                + "&port=" + port + "&uploaded=" + uploaded + "&downloaded="
                + downloaded + "&left=" + left;
    }

    public List<Peer> parseResponse(byte[] resp) throws BencodingException, UnsupportedEncodingException{

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
            peers_list.add(new Peer(ip, port, peer_id));
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




}