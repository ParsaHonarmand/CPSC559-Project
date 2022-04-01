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
    private ConcurrentHashMap<String, Peer> peersRecv = new ConcurrentHashMap<String, Peer>();

    private String peersRecvLog = "";

    private ArrayList<String> snippets = new ArrayList<String>();
    private int arbitraryNum = 123;


    public PeerCom(ConcurrentHashMap<String, Peer> peersMap, DatagramSocket udpServer) {
        this.peersMap = peersMap;
        this.allPeers = peersMap;
        this.udpServer = udpServer;
    }

    public String getPeersRecvLog(){ return peersRecvLog; }

    public ConcurrentHashMap<String, Peer> getAllPeers(){ return allPeers; }

    public ArrayList<String> getSnippets() {
        return snippets;
    }

    private DatagramPacket getMessage() throws IOException {
        byte[] buffer = new byte[256];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        udpServer.receive(packet);
        return packet;
    }

    private void handleStopMessage() {
        System.out.println("Stop request received");
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
        String newSnippet = msgTimeStamp + snipContent + " " + msgSenderAddress.substring(1);
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

        if(!isDuplicate){
            peersMap.put(newPeer.getTeamName(), newPeer);
        }
        if(!isDuplicateAllPeers){
            allPeers.put(arbitraryTeamName, newPeer);
        }
        if(!isDuplicatePeersRecv){
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
        System.out.println("New Peer Message: " + senderPeerAddr);
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
                System.out.println(packet.getData());
                String message = new String(packet.getData(), 0, packet.getLength());
                try {
                    LocalDateTime timeRecv = LocalDateTime.now();
                    String[] messages = message.split(" ");
                    String request = messages[0].substring(0, 4);
                    System.out.println("Request RECEIVED: " + request);
                    switch (request) {
                        case "peer":
                            handlePeerMessage(message, packet, timeRecv);
                            refreshPeerList();
                            break;
                        case "snip":
                            handleSnipMessage(messages, packet, timeRecv);
                            break;
                        case "stop":
                            handleStopMessage();
                            break;
                    }
                } catch (Exception e) {
                    System.out.println("Could not process udp message");
                    e.printStackTrace();
                }
                Thread.sleep(1000);
            } catch (Exception e) {
                System.out.println("Could not receive udp request");
                e.printStackTrace();
            }
        }
    }
}