//Need to still see if a peer has timed out therefore remove from currPeers but keep in allPeers for report
//This program should send and receive peers from/to other peers and add to list of known peers (allPeers and currPeers)

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.io.IOException;
import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class PeerCom implements Runnable{
    private DatagramSocket udpServer;

    private ConcurrentHashMap<String, Peer> peersMap;
    private ConcurrentHashMap<String, Peer> currentPeers;

	private ArrayList<Peer> allPeers = new ArrayList<Peer>();
    private ArrayList<Peer> currPeers = new ArrayList<Peer>();

    private boolean duplicateDetect = false;

    private String teamNameGeneral;
    private String newPeerAddress;
    private int newPeerPort;

    public ArrayList<String> snippets = new ArrayList<String>();
    private int arbitraryNum = 123;


    public PeerCom(ConcurrentHashMap<String, Peer> peersMap, DatagramSocket udpServer) {
        this.peersMap = peersMap;
        this.currentPeers = peersMap;
        this.udpServer = udpServer;
    }

    public PeerCom(String address, int port) {
        this.newPeerAddress = address;
        this.newPeerPort = port;
    }

//    public void updatePeerList(String newPeerAddress, int newPeerPort){
//        for(Peer p : allPeers){
//            if(!(p.getAddress() == newPeerAddress && p.getPort() == newPeerPort)){
//                duplicateDetect = true;
//                teamNameGeneral = p.getTeamName();
//            }
//        }
//        if(duplicateDetect == false){
//            peersMap.put(teamNameGeneral, new Peer(teamNameGeneral, newPeerAddress, newPeerPort));
//            allPeers = Arrays.copyOf(allPeers, allPeers.length+1);
//            allPeers[allPeers.length-1] = new Peer(teamNameGeneral, newPeerAddress, newPeerPort);
//        }
//
//        currPeers = Arrays.copyOf(allPeers, allPeers.length);
//    }

    private DatagramPacket getMessage() throws IOException {
        DatagramPacket packet = new DatagramPacket(new byte[256], new byte[256].length);
        udpServer.receive(packet);
        return packet;
    }

    private void handleStopMessage() {
        Client.isRunning = false;
    }

    private void handleSnipMessage(String[] messages, DatagramPacket packet) {
        String[] msg = messages[0].split("snip");
        int msgTimeStamp = Integer.parseInt(msg[1]);
        String snipContent = "";
        for (int i = 1; i < messages.length; i++) {
            snipContent += " " + messages[i];
        }

        String msgSenderAddress = packet.getSocketAddress().toString();
        String newSnippet = msgTimeStamp + snipContent + " " + msgSenderAddress;
        System.out.println("Snippet Received: " + newSnippet);
        snippets.add(newSnippet);
    }

    private void addPeer(String address) {
        boolean isDuplicate = false;
        for (Entry<String, Peer> entry : peersMap.entrySet()) {
            Peer existingPeer = entry.getValue();
            if (address.equals(existingPeer.getAddress() + ":" + existingPeer.getPort())) {
                isDuplicate = true;
                break;
            }
        }

        if(isDuplicate == false){
            String arbitraryTeamName = "someTeamName" + arbitraryNum++;
            String[] addressArr = address.split(":");

            String socketAddr = addressArr[0];
            int port = Integer.parseInt(addressArr[1]);
            Peer newPeer = new Peer(arbitraryTeamName, socketAddr, port);

            peersMap.put(arbitraryTeamName, newPeer);
        }
    }

    private void handlePeerMessage(String message, DatagramPacket packet) {
        String receivedPeerAddr = message.split("peer")[1];
        String senderPeerAddr = packet.getSocketAddress().toString();

        addPeer(receivedPeerAddr);
        addPeer(senderPeerAddr);
    }

    @Override
    public void run() {
        while (Client.isRunning) {
            try {
                DatagramPacket packet = getMessage();
                System.out.println(packet);
                String message = new String(packet.getData(), 0, packet.getLength());
                try {
                    String[] messages = message.split(" ");
                    String request = messages[0].substring(0, 4);
                    if (request.equals("peer")) {
                        handlePeerMessage(message, packet);
                    }
                    else if (request.equals("snip")) {
                        handleSnipMessage(messages, packet);
                    }
                    else if (request.equals("stop")) {
                        handleStopMessage();
                    }
                } catch (Exception e) {
                    System.err.println(e);
                }
                Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
//        int i = 0;
//        for (Entry<String, Peer> entry : peersMap.entrySet()) {
//            String teamName = entry.getKey();
//            Peer currPeer = entry.getValue();
//            peerFormatter[i] = teamName + ":" + currPeer.getAddress() + ":" + currPeer.getPort();
//            i++;
//            for(Peer p : allPeers){
//                if(!(p.getAddress() == currPeer.getAddress() && p.getPort() == currPeer.getPort() && p.getTeamName() == currPeer.getTeamName())){
//                    duplicateDetect = true;
//                }
//            }
//            if(duplicateDetect == false){
//                peersMap.put(teamName,currPeer);
//                allPeers = Arrays.copyOf(allPeers, allPeers.length+1);
//                allPeers[allPeers.length-1] = new Peer(teamName, currPeer.getAddress(), currPeer.getPort());
//            }
//        }

//        currPeers = Arrays.copyOf(allPeers, allPeers.length);


//        Random r = new Random();
//        while(true){
//            int numOfPeers = peersMap.size();
//            int random = r.nextInt(numOfPeers);
//            int randomReceiver = r.nextInt(numOfPeers);
//            Peer receivingPeer = new Peer("", "", 0);
//            Peer randomPeer = new Peer("", "", 0);
//
//            int i = 0;
//            for (Entry<String, Peer> entry : peersMap.entrySet()) {
//                if (i == random) {
//                    randomPeer = entry.getValue();
////                    String[] peerProperties = pickedPeer.split(":");
//                }
//                if (i == randomReceiver) {
//                    receivingPeer = entry.getValue();
////                    String[] peerRecieverProperties = pickerPeerReciever.split(":");
//                }
//                i++;
//            }
//
//            try{
//                InetAddress addressReceiverUDP = InetAddress.getByName(receivingPeer.getAddress());
//                String str = "peer" + randomPeer.getAddress() + ":" + randomPeer.getPort();
//                byte[] buf = str.getBytes();
//                DatagramPacket packetSend = new DatagramPacket(buf, buf.length, addressReceiverUDP, receivingPeer.getPort());
//
//                udpServer.send(packetSend);
//                System.out.println("Sent peer at " + randomPeer.getAddress() + ":" +randomPeer.getPort() + " to " + addressReceiverUDP.toString() + ":"+ receivingPeer.getPort());
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
            
//                udpServer.close();
                //PeerRecieveUDP.close();

//                TimeUnit.SECONDS.sleep(10);
//            }
//            catch(Exception e){
//                System.out.println("Host isn't valid in PeerCom thread for "+address+":"+port);
//                e.printStackTrace();
//            }
//
//        }

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
