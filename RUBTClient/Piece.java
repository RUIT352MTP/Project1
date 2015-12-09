package RUBTClient;

public class Piece {

	public byte[] data;
	private int offset;

	//constructor
	public Piece(int size)
	{
		this.offset = -1;
		this.data = new byte[size];
	}
	public void add_data(byte[] data, int offset){
		for(int i = 0; i < data.length; i++){
			this.data[offset+i] = data[i];
		}
		this.offset = offset;
	}

	public byte[] get_data()
	{
		return data;
	}
	public int get_offset() {
		return offset;
	}

}
