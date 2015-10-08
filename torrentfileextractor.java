import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;


public class torrentfileextractor {

	public static void main(String[] args) throws IOException, BencodingException {
		// TODO Auto-generated method stub
		
		InputStream inputStream = null;
		byte [] ba;
	    try 
	    {
	    	File f = new File("/Users/michaelshafran/Documents/Internet Technology/Project/Project1/Phase1.torrent");
	        inputStream = new FileInputStream(f);
	        ba=  readFully(inputStream,f.length());
	    } 
	    finally
	    {
	        if (inputStream != null)
	        {
	            inputStream.close();
	        }
	    }
	    TorrentInfo ti = new TorrentInfo(ba);
	    System.out.println(ti.announce_url);
	    System.out.println(ti.file_length/1000000 +"MB");
	    System.out.println(ti.file_name);

	}
	
	public static byte[] readFully(InputStream stream, long len) throws IOException
	{
	    byte[] buffer = new byte[(int) len];
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    
	    int bytesRead;
	    while ((bytesRead = stream.read(buffer)) != -1)
	    {
	        baos.write(buffer, 0, bytesRead);
	    }
	    return baos.toByteArray();
	}

}
