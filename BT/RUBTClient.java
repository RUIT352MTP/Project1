package BT;

import GivenTools.*;

import java.io.*;
import java.net.*;
import java.util.*;

public class RUBTClient {

    public static void main(String[] args) {

        /* Check for valid arguments */
        if(args[0] == null)
            System.out.println("ERROR: Non-existing file path");

        if(args.length != 2)
            System.out.println("ERROR: Invalid arguments");

		/* Open torrent file then read data */
        byte[] torrentFileBytes = null;
        TorrentInfo torrentInfo = null;
        try{
            File tf = new File(args[0]);
            RandomAccessFile torrentFile = new RandomAccessFile(tf,"r");
            torrentFileBytes = new byte[(int)torrentFile.length()];
            torrentFile.read(torrentFileBytes);
            torrentInfo = new TorrentInfo(torrentFileBytes);
            torrentFile.close();
        }
        catch (BencodingException e){
            System.out.println("ERROR: Corrupted file");
        }
        catch(IOException e){
            System.out.println("ERROR: File not found");
        }
        System.out.println(torrentInfo.announce_url.toString());
        System.out.println(torrentInfo.file_name);
		
		
		/* GET request to tracker */
        Parser parser = new Parser(torrentInfo);
        try{
            URL peerUrl = new URL(parser.passUrl());
            HttpURLConnection http_conn = (HttpURLConnection)peerUrl.openConnection();
            http_conn.setRequestMethod("GET");
            int size = http_conn.getContentLength();
            InputStream input = http_conn.getInputStream();
            DataInputStream output = new DataInputStream(input);
            torrentFileBytes = new byte[size];
            output.readFully(torrentFileBytes);
            input.close();
            output.close();
        }
        catch (IOException e) {
            System.out.println("ERROR: Program failed in RUBTClient");
        }

		/* Retrieve peer list */
        List<Peer> peers = null;
        try {
            peers = parser.parseResponse(torrentFileBytes);
        } catch (BencodingException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e){
        	e.printStackTrace();
        }

        if(peers == null)
            System.out.println("ERROR: No peers were found");

        /* Find peer that has peer_id starting with RU11 or RUBT11 */
        String selectedIP="";
        int selectedPort=2000;
        for(Peer p : peers){
            String temp = new String(p.peer_id);
            if(temp.startsWith("-RU11") || temp.startsWith("-RUBT11")) {
                selectedIP = p.ip;
                selectedPort = p.port;
                break;
            }
        }
        //handshake
        Socket s = null;
		try {
			s = new Socket(selectedIP,selectedPort);
		
        
        Handshake hs = new Handshake(s);
        byte[] hand = hs.buildHandshake(torrentInfo.info_hash.array(), parser.peer_id);
        System.out.println("Built HS");
        hs.sendHandshake(hand, 0, 68);
        System.out.println("sent");
        byte[] handout = hs.readHandshake();
        System.out.println(handout);
        for(int x= 0 ; x < handout.length; x++) {
            // printing the characters
            System.out.print((char)handout[x]  + "   "); 
         }
        System.out.println("Received");
        s.close();
		} catch (UnknownHostException e) {
			
			e.printStackTrace();
		} catch (IOException e) {
		
			e.printStackTrace();
		}
    }
}