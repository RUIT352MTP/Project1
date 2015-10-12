package BT;

public class Peer {
    String ip;
    int port;
    byte[] peer_id;

    public Peer(String ip, int port, byte[] peer_id){
        this.ip = ip;
        this.port = port;
        this.peer_id = peer_id;
    }

}