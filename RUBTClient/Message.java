//Timothy Choi

package RUBTClient;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.ByteBuffer;

public class Message 

{

	public static final  byte CHOKE = 0;
	public static final byte UNCHOKE = 1;
	public static final byte INTERESTED = 2;
	public static final byte HAVE = 4;
	public static final byte BITFIELD = 5;
	public static final byte REQUEST = 6;
	public static final byte PIECE = 7;
	public static final byte QUIT = 25;

	private final byte[] choke_ = { 0,0,0,1,0};
	private final byte[] unchoke_ = {0,0,0,1,1};
	private final byte[] interested = {0,0,0,1,2};
	private final byte[] not_interested = {0,0,0,1,3};

	private final byte[] hs = {0x13,'B','i','t','T','o','r','r','e','n','t',' ','p','r','o','t','o','c','o','l',0,0,0,0,0,0,0,0};
	private final byte[] hs_have = {0,0,0,5,4};
	private final byte[] hs_request = {0,0,0,0xD,6};
	private final byte[] keep_alive = {0,0,0,0};

	private final int request_ = 0xD;
	public byte[] getKeepAlive_()
	{
		return keep_alive;
	}
	public byte[] getChoke_()
	{
		return choke_;
	}
	public byte[] getUnchoke_()
	{
		return unchoke_;
	}
	public byte[] getInterested_()
	{
		return interested;
	}
	public byte[] getNotInterested_()
	{
		return not_interested;
	}
	public int getRequestPrefix_()
	{
		return request_;
	}
	

	public byte[] makeBitfieldMsg(byte[] bitfield_)
	{
		int field_length = bitfield_.length+1;
		ByteBuffer buffer = ByteBuffer.allocate(4);
		buffer.putInt(field_length);

		byte[] bitfield = new byte[5+field_length-1];
		System.arraycopy(buffer.array(), 0, bitfield, 0, 4);
		bitfield[4]=BITFIELD;
		System.arraycopy(bitfield_,0,bitfield,5,bitfield_.length);

		return bitfield;
	}

	public byte[] handshake_ (byte[] info_hash, byte[] userid)
	{
		byte[] handshake = new byte[68];
		System.arraycopy(hs,0,handshake,0,28);
		System.arraycopy(info_hash, 0, handshake,28 , 20);
		System.arraycopy(userid, 0, handshake,48 , 20);

		return handshake;
	}

	public byte[] getPiece(Destination file, byte[] request_index, int reqest_length, byte[] request_begin)
	{
		byte[] index_ = request_index;
		byte[] begin_ = request_begin;
		byte[] piece_message = new byte[reqest_length+13];
		byte[] block = new byte[reqest_length];
		int length = reqest_length+9;
		int index = ByteBuffer.wrap(index_).getInt();
		int begin = ByteBuffer.wrap(begin_).getInt();
		ByteBuffer buffer = ByteBuffer.allocate(4);
		buffer.putInt(length);
		block = file.getPieceData(index, begin, length);
		System.arraycopy(buffer.array(), 0, piece_message, 0, 4);
		piece_message[4]=PIECE;
		System.arraycopy(index_, 0, piece_message, 5, 4);
		System.arraycopy(begin_,0,piece_message,9,4);
		System.arraycopy(block, 0, piece_message, 13, reqest_length);
		return piece_message;
	}

	public byte[] doesHave(byte[] i)
	{
		byte[] have = new byte[9];
		System.arraycopy(hs_have, 0, have, 0, 5);
		System.arraycopy(i,0,have,5,4);
		return have;
	}

	public static Message read(DataInputStream in)
	{
		return null;
	}

	public byte[] request(int index, int begin, int length)
	{
		ByteBuffer request = ByteBuffer.allocate(17);//allocates a byte buffer to compose our request in
		request.put(hs_request);
		request.putInt(index);
		request.putInt(begin);
		request.putInt(length);
		return request.array();//returns the buffer as a byte array
	}

	public byte[] quit()
	{
		byte[] msg = {QUIT};
		return msg;
	}



}
