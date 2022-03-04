//Need to still see if a peer has timed out therefore remove from currPeers but keep in allPeers for report
//This program should send and receive peers from/to other peers and add to list of known peers (allPeers and currPeers)

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Arrays;
import java.util.Random;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class PeerCom implements Runnable{
    ConcurrentHashMap<String, Peer> peersMap;
	Peer[] allPeers;
    Peer[] currPeers;
    Peer peerToAdd;
    String[] peerFormatter;
    boolean duplicateDetect = false;
    String teamNameGeneral;
    String address;
    String newPeerAddress;
    int port;
    int newPeerPort;


    public PeerCom(ConcurrentHashMap<String, Peer> peersMap, String address, int port) {
        this.peersMap = peersMap;
        this.address = address;
        this.port = port;
    }

    public PeerCom(String address, int port) {
        this.newPeerAddress = address;
        this.newPeerPort = port;
    }

    public void updatePeerList(String newPeerAddress, int newPeerPort){
        for(Peer p : allPeers){
            if(!(p.getAddress() == newPeerAddress && p.getPort() == newPeerPort)){
                duplicateDetect = true;
                teamNameGeneral = p.getTeamName();
            }
        }
        if(duplicateDetect == false){
            peersMap.put(teamNameGeneral, new Peer(teamNameGeneral, newPeerAddress, newPeerPort));
            allPeers = Arrays.copyOf(allPeers, allPeers.length+1);
            allPeers[allPeers.length-1] = new Peer(teamNameGeneral, newPeerAddress, newPeerPort);
        }

        currPeers = Arrays.copyOf(allPeers, allPeers.length);
    }
    
    @Override
    public void run() {
        int i = 0;
        for (Entry<String, Peer> entry : peersMap.entrySet()) {
            String teamName = entry.getKey();
            Peer currPeer = entry.getValue();
            peerFormatter[i] = teamName + ":" + currPeer.getAddress() + ":" + Integer.toString(currPeer.getPort());
            i++;  
            for(Peer p : allPeers){
                if(!(p.getAddress() == currPeer.getAddress() && p.getPort() == currPeer.getPort() && p.getTeamName() == currPeer.getTeamName())){
                    duplicateDetect = true;
                }
            }
            if(duplicateDetect == false){
                peersMap.put(teamName,currPeer);
                allPeers = Arrays.copyOf(allPeers, allPeers.length+1);
                allPeers[allPeers.length-1] = new Peer(teamName, currPeer.getAddress(), currPeer.getPort());
            }  
        }

        currPeers = Arrays.copyOf(allPeers, allPeers.length);

        

        Random r = new Random();
        while(true){
            int random = r.nextInt(peerFormatter.length);
            int randomReciever = r.nextInt(peerFormatter.length);
            String pickedPeer = peerFormatter[random];
            String pickerPeerReciever = peerFormatter[randomReciever];
            String[] peerProperties = pickedPeer.split(":");
            String[] peerRecieverProperties = pickerPeerReciever.split(":");
            
            try{
            DatagramSocket PeerSendUDP = new DatagramSocket();

            InetAddress addressRecieverUDP = InetAddress.getByName(peerRecieverProperties[1]);
            String str = "peer" + peerProperties[1] + ":" + peerProperties[2];
            byte[] buf = str.getBytes();
            DatagramPacket packetSend = new DatagramPacket(buf, buf.length, addressRecieverUDP, Integer.parseInt(peerRecieverProperties[2]));
            PeerSendUDP.send(packetSend);

            /*
            DatagramSocket PeerRecieveUDP = new DatagramSocket(port);

            byte[] buff = new byte[256];
            DatagramPacket packetReceive = new DatagramPacket(buff, buff.length);
            PeerRecieveUDP.receive(packetReceive);
            String response = new String(packetReceive.getData());
            if(response != null && response.substring(0,4) == "peer"){
                System.out.println(response+" recieved by "+address+":"+port);
                String responseR = response.substring(4);
                String[] responseSplit = responseR.split(":");

                for(Peer p : allPeers){
                    if(!(p.getAddress() == responseSplit[0] && p.getPort() == Integer.parseInt(responseSplit[1]))){
                        duplicateDetect = true;
                        teamNameGeneral = p.getTeamName();
                    }
                }
                if(duplicateDetect == false){
                    peersMap.put(teamNameGeneral, new Peer(teamNameGeneral, responseSplit[0], Integer.parseInt(responseSplit[1])));
                    allPeers = Arrays.copyOf(allPeers, allPeers.length+1);
                    allPeers[allPeers.length-1] = new Peer(teamNameGeneral, responseSplit[0], Integer.parseInt(responseSplit[1]));
                }  
            }

            currPeers = Arrays.copyOf(allPeers, allPeers.length);
            */
            
            PeerSendUDP.close();
            //PeerRecieveUDP.close();

            TimeUnit.SECONDS.sleep(10);
            }
            catch(Exception e){
                System.out.println("Host isn't valid in PeerCom thread for "+address+":"+port);
                e.printStackTrace();
            }
                       
        }

    }
}

/*
            DatagramSocket clientUDP = new DatagramSocket();

            InetAddress addressUDP = InetAddress.getByName(address);
            String str = "stop";
            byte[] buf = str.getBytes();
            DatagramPacket packetSend = new DatagramPacket(buf, buf.length, addressUDP, 4160);
            clientUDP.send(packetSend);
            clientUDP.close();
                

            DatagramSocket PeerSendUDP = new DatagramSocket(Integer.parseInt(peerProperties[2]));
            byte[] buff = new byte[256];
            DatagramPacket packetReceive = new DatagramPacket(buff, buff.length);
            PeerSendUDP.recieve(packetReceive);
            String response = new String(packetRecieve.getData());
            System.out.println(response);
            PeerSendUDP.close();
*/
