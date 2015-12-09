package RUBTClient;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.io.RandomAccessFile;
import GivenTools.TorrentInfo;

public class Destination {
	
	private TorrentInfo tinfo;
	private RandomAccessFile save;
	private String file1;
	private byte[] bitf;
	private int[] piece;
	private boolean startcheck;
	private RUBTClient client;
	public RarePiece RP;
	public Piece[] pieces;
	public int neededB;
	int sTotal, moreC;
	
	public Destination(TorrentInfo torinfo, String file1){
		int mod1;
		this.startcheck = false;
		this.createT(torinfo);
		this.moreC = torinfo.file_length;
		this.sTotal = torinfo.file_length;
		this.file1 = file1;
		this.RP = new RarePiece(torinfo.piece_hashes.length, this);
		piece = new int[torinfo.piece_hashes.length];
		pieces = new Piece[torinfo.piece_hashes.length];
		
		if( (mod1 = torinfo.piece_hashes.length%8) != 0) {
			bitf = new byte[((torinfo.piece_hashes.length - mod1) / 8) + 1];
			neededB = ((torinfo.piece_hashes.length-mod1)/8) + 1;
		}else{
			bitf = new byte[torinfo.piece_hashes.length/8];
			neededB = torinfo.piece_hashes.length/8;
		}
		this.bitstart();
		for(int i = 0; i<piece.length - 1; i++){
			pieces[i] = new Piece(torinfo.piece_length);
		}
		int lost = torinfo.file_length%torinfo.piece_length;
		if(lost != 0){
			pieces[torinfo.piece_hashes.length-1]=new Piece(lost);
		}else{
			pieces[torinfo.piece_hashes.length-1] = new Piece(torinfo.piece_length);
		}
	}
	
	private void print(){
		System.out.print("Bitfield progress:");
		for(int i = 0; i<neededB; i++){
			System.out.print(" " + String.format("%8s", Integer.toBinaryString(bitf[i] & 0xFF)).replace(' ', '0'));
		}
		System.out.print("\n");
	}
	
	public void fileS(){
		try {
			save=new RandomAccessFile(file1,"rw");
			save.setLength(tinfo.file_length);
			startcheck=true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void close(){
		try { this.save.close();
		}catch (IOException e)
		{
			System.err.println("Cannot close RandomAccessFile");
		}
	}
	
	public int verifyH(byte[] piece){ //verifies hash of torrent for integrity
		MessageDigest checkV = null;
		try { checkV = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			System.err.println("Error intitializing hash check");
		}
		byte[] hash1 = checkV.digest(piece);
		for(int i=0; i<this.tinfo().piece_hashes.length; i++){
			if(Arrays.equals(hash1,this.tinfo().piece_hashes[i].array())){
				System.out.println("Piece verified and retrieved: " + i);
				return i;
			}
		}
		System.out.println("FAILED piece");
		return -1;
	}
	
	public int verify(Piece piece){
		return verifyH(piece.get_data());
	}
	
	public synchronized boolean addbit(int pid){
		if(verifyH(this.pieces[pid].get_data())==pid){
			try {
				long lng = pid*tinfo().piece_length;
				save.seek(lng);
				save.write(this.pieces[pid].get_data());
				this.piece[pid]=2;
				this.bitupdate();
				this.moreC -= (this.pieces[pid].get_data().length);
				
				if(this.moreC<=0){
					this.client.contactTracker("completed");
					this.client.setSeeding();
					this.moreC=0;
				}
				return true;
			} catch (IOException e) {
				System.err.println("error writing to file");
			}
		}
		return false;
	}
	
	public String MapCreate(Integer input){
		String bitfield = Integer.toBinaryString(input);//conversion
		if(bitfield.length()>8){
			return bitfield.substring(bitfield.length()-8);
		}else{
			return bitfield;
		}
	}

	public boolean checkF(){
		if(!startcheck){
			this.fileS();
		}
		boolean check2=true;
		int i; 
		
		for(i=0; i<tinfo.piece_hashes.length; i++){
			byte tt[]=null;
			if(i != tinfo.piece_hashes.length-1){
				tt = new byte[tinfo.piece_length];
			}else if (i == tinfo.piece_hashes.length-1){
				if(tinfo.file_length%tinfo.piece_length != 0)
					tt = new byte[tinfo.file_length % tinfo.piece_length];
			}
			try {
				this.save.seek(i*tinfo.piece_length);
				this.save.read(tt);
				if(this.verifyH(tt)==i){
					piece[i]=2;
					this.moreC-=(this.pieces[i].get_data().length);
				}else{
					check2=false;
					System.out.println("Inavlid piece: " + i);
					piece[i]=0;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return check2;
	}

	public void bitupdate(){
		int i, div, curr;
		for(i = 0; i<this.piece.length; i++){
			div = i%8;
			curr = (i-div)/8;
			
			if(this.piece[i]==2){
				this.bitf[curr] |= (1<<7-div);
			}else{
				this.bitf[curr] &= ~(1<<7-div);
			}		
		}
		RP.masterF(this.piece);
		print();
	}

	public void bitstart(){
		int i, div, curr;
		for(i=0; i< piece.length; i++){
			div = i%8;
			curr = (i-div)/8;
			bitf[curr] &= ~(1<<7-div);
			piece[i]=0;
		}
	}
	
	public byte[] getMybitfield(){
		return bitf;
	}
	
	public void manualMod(int i, boolean flag){
		int div=i%8;
		int curr=(i-(div)) / 8;
		if(flag){
			bitf[curr]|=(1<<7-div);
		}else{
			bitf[curr]&=~(1<<7-div);
		}
	}
	public String fname() {
		return file1;
	}

	public void createname(String file1) {
		this.file1 = file1;
	}

	public TorrentInfo tinfo() {
		return tinfo;
	}

	public void createT(TorrentInfo tor) {
		this.tinfo = tor;
	}

	public void initclient(RUBTClient c1) {
		this.client = c1;
	}
	public byte[] divC(byte[] mod1, int i, boolean flag){
		int div = i%8;
		int curr = (i-(div))/8;
		if(flag){
			mod1[curr] |= (1<<7-div);
		}else{
			mod1[curr] &= ~(1<<7-div);
		}
		return mod1;
	}
	public synchronized int pieceAdd(byte[] rec){
		for(int i = 0; i < piece.length; i++){		
			int div = i%8;
			int curr = (i-(div)) / 8;
			
			if((bitf[curr] >> (7-div) & 1) != 1){
				if((rec[curr] >> (7-div) & 1) == 1){
					if(piece[i]==0){
						return i; }
				}
			}
		}
		return -1;
	}
	public byte[] downB(int bit1, int res, int quan){
		byte[] getB  = new byte[quan];
		try {
			save.seek(bit1*tinfo.piece_length + res);
			save.read(getB);
			return getB;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public synchronized void progtake(int itr){
		piece[itr] = 1;
	}
	public synchronized void eraser(int itr){
		if(piece[itr] != 2)
			piece[itr]= 0;
	}

}