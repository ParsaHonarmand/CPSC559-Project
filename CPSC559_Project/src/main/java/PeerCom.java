//Need to still see if a peer has timed out therefore remove from currPeers but keep in allPeers for report
//This program should send and receive peers from/to other peers and add to list of known peers (allPeers and currPeers)

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private ConcurrentHashMap<String, Peer> allPeers;
    private ConcurrentHashMap<String, Peer> peersRecv;
    private String thisPeerAddress;
    private int thisPeerPort; 

    private String peersRecvLog = ""; 
    private String newPeerAddress;
    private int newPeerPort;

    public ArrayList<String> snippets = new ArrayList<String>();
    private int arbitraryNum = 123;


    public PeerCom(ConcurrentHashMap<String, Peer> peersMap, DatagramSocket udpServer, String thisPeerAddress, int thisPeerPort) {
        this.peersMap = peersMap;
        this.allPeers = peersMap;
        this.udpServer = udpServer;
        this.thisPeerAddress = thisPeerAddress;
        this.thisPeerPort = thisPeerPort; 
    }

    public PeerCom(String address, int port) {
        this.newPeerAddress = address;
        this.newPeerPort = port;
    }

    public String getPeersRecvLog(){ return peersRecvLog; }

    public ConcurrentHashMap<String, Peer> getAllPeers(){ return allPeers; }
    
    public ConcurrentHashMap<String, Peer> getPeersRecv(){ return peersRecv; }

    public String getThisPeerAddress(){ return thisPeerAddress; }

    public int getThisPeerPort(){ return thisPeerPort; }

    public 

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

    private void handleSnipMessage(String[] messages, DatagramPacket packet, LocalDateTime timeRecv) {
        String[] msg = messages[0].split("snip");
        int msgTimeStamp = Integer.parseInt(msg[1]);
        String snipContent = "";
        for (int i = 1; i < messages.length; i++) {
            snipContent += " " + messages[i];
        }

        String msgSenderAddress = packet.getSocketAddress().toString();
        String[] address = msgSenderAddress.split("/");
        String newSnippet = msgTimeStamp + snipContent + " " + msgSenderAddress;
        System.out.println("Snippet Received: " + newSnippet);
        snippets.add(newSnippet);
        addPeer(address[1], timeRecv);
    }

    private void addPeer(String address, LocalDateTime timeRecv) {
        boolean isDuplicate = false;
        boolean isDuplicateAllPeers = false;
        boolean isDuplicatePeersRecv = false;

        for (Entry<String, Peer> entry : peersMap.entrySet()) {
            Peer existingPeer = entry.getValue();
            if (address.equals(existingPeer.getAddress() + ":" + existingPeer.getPort())) {
                isDuplicate = true;
                break;
            }
        }

        for (Entry<String, Peer> entry : allPeers.entrySet()) {
            Peer existingPeer = entry.getValue();
            if (address.equals(existingPeer.getAddress() + ":" + existingPeer.getPort())) {
                isDuplicateAllPeers = true;
                break;
            }
        }

        for (Entry<String, Peer> entry : peersRecv.entrySet()) {
            Peer existingPeer = entry.getValue();
            if (address.equals(existingPeer.getAddress() + ":" + existingPeer.getPort())) {
                isDuplicatePeersRecv = true;
                break;
            }
        }

        String arbitraryTeamName = "someTeamName" + arbitraryNum++;
        String[] addressArr = address.split(":");
        String socketAddr = addressArr[0];
        int port = Integer.parseInt(addressArr[1]);
        Peer newPeer = new Peer(arbitraryTeamName, socketAddr, port, timeRecv);
        

        if(isDuplicate == false){
            peersMap.put(newPeer.getTeamName(), newPeer); 
        }
        if(isDuplicateAllPeers == false){
            allPeers.put(arbitraryTeamName, newPeer);
        }
        if(isDuplicatePeersRecv == false){
            peersRecv.put(arbitraryTeamName, newPeer);
        }

    }

    private void refreshPeerList(){
        for (Entry<String, Peer> entry : peersMap.entrySet()) {
            Peer existingPeer = entry.getValue();
            LocalDateTime lastNoticed = existingPeer.getLastNoticed();
            LocalDateTime currentTime = LocalDateTime.now();
            LocalDateTime expiryTime = lastNoticed.plusHours(2);

            int nowToLastNoticed = currentTime.compareTo(lastNoticed);
            int nowToExpiry = currentTime.compareTo(expiryTime);

            
            if((nowToLastNoticed > 0 && nowToExpiry > 0) || (nowToLastNoticed < 0 && nowToExpiry > 0)){
                peersMap.remove(entry.getKey());
            }else if(nowToLastNoticed >= 0 && nowToExpiry <= 0){
                //Keep Peer
            }
            
        }
    }

    private void handlePeerMessage(String message, DatagramPacket packet, LocalDateTime timeRecv) {
        String receivedPeerAddr = message.split("peer")[1];
        String senderPeerAddr = packet.getSocketAddress().toString();

        addPeer(receivedPeerAddr, timeRecv);
        addPeer(senderPeerAddr, timeRecv);
        addPeersRecvLog(receivedPeerAddr, senderPeerAddr, timeRecv);
    }

    private void addPeersRecvLog(String receivedPeerAddr, String senderPeerAddr, LocalDateTime timeRecv){
        //String[] receivedPeerAddrProperties = receivedPeerAddr.split(":");
        //String[] senderPeerAddrProperties = senderPeerAddr.split(":");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = timeRecv.format(formatter);

        if(peersRecvLog == ""){
            peersRecvLog = Integer.toString(peersRecv.size()) + "\n" +
            senderPeerAddr + " " +
            receivedPeerAddr + " " + 
            formattedDateTime + "\n";
        }
        else{
            peersRecvLog = peersRecvLog + 
            Integer.toString(peersRecv.size()) + "\n" +
            senderPeerAddr + " " +
            receivedPeerAddr + " " + 
            formattedDateTime + "\n";
        }
    }

    @Override
    public void run() {
        while (Client.isRunning) {
            try {
                DatagramPacket packet = getMessage();
                System.out.println(packet);
                String message = new String(packet.getData(), 0, packet.getLength());
                try {
                    LocalDateTime timeRecv = LocalDateTime.now();
                    String[] messages = message.split(" ");
                    String request = messages[0].substring(0, 4);
                    if (request.equals("peer")) {
                        handlePeerMessage(message, packet, timeRecv);
                        refreshPeerList();
                    }
                    else if (request.equals("snip")) {
                        handleSnipMessage(messages, packet, timeRecv);
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

/*
    LocalDateTime myDateObj = LocalDateTime.now();
    System.out.println("Before formatting: " + myDateObj);
    DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    String formattedDate = myDateObj.format(myFormatObj);
*/

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
