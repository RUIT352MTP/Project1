package BT;

import java.io.*;
import java.net.Socket;

public class communicator implements Runnable{
	 private Socket server;
	    private String line,input;

	    communicator(Socket server) {
	      this.server=server;
	    }

	    public void run () {

	      input="";

	      try {
	        // Get input from the client
	    	  BufferedReader in = new BufferedReader(new InputStreamReader(server.getInputStream()));
	       

	        while((line = in.readLine()) != null && !line.equals(".")) {
	          input=input + line;
	          System.out.println(input);
	          
	        }

	        // Now write to the client

	        System.out.println("Overall message is:" + input);
	     
	        server.close();
	      } catch (IOException ioe) {
	        System.out.println("IOException on socket listen: " + ioe);
	        ioe.printStackTrace();
	      }
	    }

}
