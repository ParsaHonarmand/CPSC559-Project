// File: SendMessages.java
// File: Peer.java
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Thread.interrupted;

public class SendMessages implements Runnable {
    DatagramSocket udpSocket;
    String peersSendingLog = ""; 
    ConcurrentHashMap<String, Peer> peers = new ConcurrentHashMap<String, Peer>();
    private List<String> peersSent;
    ConcurrentHashMap<String, Peer> peersSending = new ConcurrentHashMap<String, Peer>();

    public AtomicInteger timeStamp;
    public volatile boolean isRunning = true;
    private int counter = 0;

    public SendMessages(ConcurrentHashMap<String, Peer> peersMap, DatagramSocket udpServer, AtomicInteger timeStamp) {
        this.peers = peersMap;
        this.udpSocket = udpServer;
        this.timeStamp = timeStamp;
        this.peersSent = Collections.synchronizedList(new ArrayList<String>());
    }

    public List<String> getPeersSent(){ return peersSent; }

    public ConcurrentHashMap<String, Peer> getPeersSending(){ return peersSending; }
    public int getPeersSentSize() { return peersSent.size(); }
    public int getCounter() { return counter;}
    public String getPeersSendingLog(){ return peersSendingLog; }

    private void sendSnippet() throws java.io.IOException {
        Scanner keyboard = new Scanner(System.in);
        String userSnip = keyboard.nextLine();
        timeStamp.getAndIncrement();
        String message = "snip" + timeStamp.get() + " " + userSnip;

        byte[] packet = message.getBytes();
        for (Map.Entry<String, Peer> entry : peers.entrySet()) {
            System.out.println("Sending snippet to: " +
                    entry.getValue().getAddress() +
                    ":" +
                    entry.getValue().getPort()
            );
            try {
                DatagramPacket dp = new DatagramPacket(packet, packet.length, InetAddress.getByName(entry.getValue().getAddress()), entry.getValue().getPort());
                udpSocket.send(dp);
            } catch (Exception e) {
                System.out.println("Peer is not active");
            }
        }
    }

    private void addPeerToSendLog(Peer randomSendingPeer, Peer receivingPeer, LocalDateTime timeSent){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = timeSent.format(formatter);

        peersSendingLog +=
                randomSendingPeer.getAddress() + ":" + randomSendingPeer.getPort() + " " +
                receivingPeer.getAddress() + ":" + receivingPeer.getPort() + " " +
                formattedDateTime + "\n";
    }

    private void sendPeer() throws java.io.IOException, InterruptedException{
//        TimeUnit.SECONDS.sleep(7);
        Random r = new Random();
        int numOfPeers = peers.size();
        int random = r.nextInt(numOfPeers);
        int randomReceiver = r.nextInt(numOfPeers);
        LocalDateTime myDateObj = LocalDateTime.now();

        Peer receivingPeer = new Peer("", "", 0, myDateObj);
        Peer randomSendingPeer = new Peer("", "", 0, myDateObj);

        int i = 0;
        System.out.println("Size: " + peers.size());
        for (Entry<String, Peer> entry : peers.entrySet()) {
            if (i == random) {
                randomSendingPeer = entry.getValue();
            }
            if (i == randomReceiver) {
                    receivingPeer = entry.getValue();
                }
            i++;
        }

//        DatagramSocket udpPeerSend = new DatagramSocket();

        InetAddress addressReceiverUDP = InetAddress.getByName(receivingPeer.getAddress());
        String str = "peer" + randomSendingPeer.getAddress() + ":" + randomSendingPeer.getPort();
        System.out.println("Sending " + str + " to " + receivingPeer.getPort());
        byte[] buf = str.getBytes();
        DatagramPacket packetSend = new DatagramPacket(buf, buf.length, addressReceiverUDP, receivingPeer.getPort());

        try{
            udpSocket.send(packetSend);
            addPeerToSendLog(randomSendingPeer, receivingPeer, myDateObj);
            counter++;
            Peer peer = new Peer(
                    randomSendingPeer.getAddress()+":"+randomSendingPeer.getPort(),
                    randomSendingPeer.getAddress(),
                    randomSendingPeer.getPort(),
                    myDateObj);

            peersSent.add(peer.getTeamName());

//            udpSocket.close();
        }
        catch(Exception e){
            System.out.println("Peer " + randomSendingPeer.getAddress() + ":" + randomSendingPeer.getPort() + " was not sent");
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        Runnable sendPeer = () -> {
            while (!interrupted()) {
                try {
                    sendPeer();
                    Thread.sleep(30000);
                } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                }
            }
        };

        Thread peerThread = new Thread(sendPeer);
        peerThread.start();

        while (isRunning) {
            try {
                sendSnippet();
            } catch (Exception e) {
                System.err.println("Failed to send snippet");
                e.printStackTrace();
            }
        }
        System.out.println("Closing sendMessages Thread");
        peerThread.interrupt();
    }
}
