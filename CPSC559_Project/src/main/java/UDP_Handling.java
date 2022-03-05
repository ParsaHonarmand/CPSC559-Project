import java.io.IOException;
import java.net.DatagramPacket;
import java.net.Socket;

public class UDP_Handling implements Runnable{

    String response = "";
    Socket sock;

    public UDP_Handling(String response, Socket sock) {
        this.response = response;
        this.sock = sock;
    }

    public boolean reportSend(){
        return true;
    }

    @Override
    public void run() {    
            Client c = new Client();
            if(response.substring(0,4) == "peer"){
                String responseR = response.substring(4);
                String[] peerToAddPrperties = responseR.split(":");
                PeerCom peerAdd = new PeerCom(peerToAddPrperties[0], Integer.parseInt(peerToAddPrperties[1]));
//                peerAdd.updatePeerList(peerToAddPrperties[0], Integer.parseInt(peerToAddPrperties[1]));
            }
            else if(response.substring(0,4) == "snip"){
                //Handle snips coming in from other peers 
            }
        
    }
    
}
