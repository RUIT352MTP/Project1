//Timothy Choi
//Michael Shafran
//Prasant Sinha

package RUBTClient;

public class PeerMsg {
	
	private final Peer peer;
	public Peer getPeer(){
		return this.peer;
	}

	private final byte[] msg;
	public byte[] getMsg() {
		return this.msg;
	}

	public PeerMsg(final Peer peer, final byte[] msg){
		this.peer = peer;
		this.msg = msg;
	}
}
