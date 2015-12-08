package RUBTClient;

import java.util.Hashtable;
import java.util.Arrays;
import java.util.Iterator;

public class RarePiece {
	private Hashtable<byte[], boolean[]> bit;
	private final int counter, needed;
	private Destination outP;
	private int[] bitF;

	private boolean[] checker(byte[] bitfield) {
		boolean[] map = new boolean[counter];
		if(bitfield.length != needed){
			System.err.println("bitfield invalid");
			return null;
		}else{
			for(int i=0; i<counter;i++){
				int d = i%8;
				int segment = (i-(d))/8;
				
				if(((bitfield[segment]>>(7-d)&1)==1)){
					map[i] = true;
				}else{
					map[i] = false;
				}
			}
		}
		return map;
	}
	
	private Counter[] iterate(){	
		Iterator<boolean[]> values = bit.values().iterator();

		Counter[] count = new Counter[counter];
		for(int i = 0; i < counter; i++){
			count[i] = new Counter(i);
		}
		
		while(values.hasNext()){
			boolean[] temp = values.next();
			for(int i = 0; i < counter; i++){
				if(temp[i]){
					count[i].increment();
				}
			}
		}
		return count;
	}
	
	private int reqB(int rec){
		int div = rec%8;
		if(div != 0){
			return (rec/8)+1;
		}else{
			return (rec/8);
		}
	}
	
	private Counter finder(Counter[] val, byte[] bitfield) {
		boolean[] bools = checker(bitfield);
		
		for(int i = 0; i<counter; i++){
			if(bitF[val[i].getIdentifier()] == 0 && bools[val[i].getIdentifier()]){
				outP.progtake(i);
				return val[i];
			}
		}
		return new Counter(-1);
	}
	
	public void masterF(int[] bitF){
		this.bitF = bitF;
	}
	
	public RarePiece(int limit, Destination outP){
		this.bit=new Hashtable<byte[], boolean[]>(limit);
		this.outP=outP;
		this.counter=limit;
		this.needed=reqB(limit);
	}
	
	public void delete(byte[] pID){	
		if(bit.containsKey(pID)){
			bit.remove(pID);
		}
	}
	public void update(byte[] peer, int piece){
		if(bit.contains(peer)){
			bit.get(peer)[piece]=true;
		}
	}
	public synchronized void add(byte[] pID, byte[] bf){
		bit.put(pID, checker(bf));
	}
	public synchronized int rPiece(byte[] bitfield){
		Counter[] val=iterate();
		Arrays.sort(val);
		Counter rare = finder(val, bitfield);
		return rare.getIdentifier();
	}

}
