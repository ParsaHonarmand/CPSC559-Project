import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class SendMessages implements Runnable {
    DatagramSocket udpSocket;
    String peersSendingLog = ""; 
    ConcurrentHashMap<String, Peer> peers;
    ConcurrentHashMap<String, Peer> peersSent;
    ConcurrentHashMap<String, Peer> peersSending;

    int timeStamp;

    public SendMessages(ConcurrentHashMap<String, Peer> peersMap, DatagramSocket udpServer) {
        this.peers = peersMap;
        this.timeStamp = 0;
        this.udpSocket = udpServer;
    }

    public ConcurrentHashMap<String, Peer> getPeersSent(){ return peersSent; }

    public ConcurrentHashMap<String, Peer> getPeersSending(){ return peersSending; }

    public String getPeersSendingLog(){ return peersSendingLog; }

    private void sendSnippet() throws java.io.IOException {
        Scanner keyboard = new Scanner(System.in);
        String userSnip = keyboard.nextLine();
        String message = "snip" + timeStamp + " " + userSnip;

        byte[] packet = message.getBytes();
        for (Map.Entry<String, Peer> entry : peers.entrySet()) {
            System.out.println(entry.getValue().getAddress());
            System.out.println(entry.getValue().getPort());
            DatagramPacket dp = new DatagramPacket(packet, packet.length, InetAddress.getByName(entry.getValue().getAddress()), entry.getValue().getPort());
            udpSocket.send(dp);
        }
    }

    private void addPeerToSendLog(Peer randomSendingPeer, Peer receivingPeer, LocalDateTime timeSent){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = timeSent.format(formatter);
        if(peersSendingLog == ""){
            peersSendingLog = Integer.toString(peersSent.size()) + "\n" + 
            randomSendingPeer.getAddress() + ":" + randomSendingPeer.getPort() + " " + 
            receivingPeer.getAddress() + ":" + receivingPeer.getPort() + 
            formattedDateTime + "\n";
        }
        else{
            peersSendingLog = peersSendingLog +
            Integer.toString(peersSent.size()) + "\n" + 
            randomSendingPeer.getAddress() + ":" + randomSendingPeer.getPort() + " " + 
            receivingPeer.getAddress() + ":" + receivingPeer.getPort() + 
            formattedDateTime + "\n";
        }
    }

    private void sendPeer(int counter) throws java.io.IOException, InterruptedException{ 
        TimeUnit.SECONDS.sleep(7);

        Random r = new Random();
        int numOfPeers = peers.size();
        int random = r.nextInt(numOfPeers);
        int randomReceiver = r.nextInt(numOfPeers);
        LocalDateTime myDateObj = LocalDateTime.now();
        
        Peer receivingPeer = new Peer("", "", 0, myDateObj);
        Peer randomSendingPeer = new Peer("", "", 0, myDateObj);

        int i = 0;
        for (Entry<String, Peer> entry : peers.entrySet()) {
            if (i == random) {
                randomSendingPeer = entry.getValue();
            }
            if (i == randomReceiver) {
                    receivingPeer = entry.getValue();
                }
                i++;
        }

        DatagramSocket udpPeerSend = new DatagramSocket();

        InetAddress addressReceiverUDP = InetAddress.getByName(receivingPeer.getAddress());
        String str = "peer" + randomSendingPeer.getAddress() + ":" + randomSendingPeer.getPort();
        byte[] buf = str.getBytes();
        DatagramPacket packetSend = new DatagramPacket(buf, buf.length, addressReceiverUDP, receivingPeer.getPort());

        try{
            udpPeerSend.send(packetSend);
            addPeerToSendLog(randomSendingPeer, receivingPeer, myDateObj);
            /*
            Peer peer = new Peer("someTeamName" + Integer.toString(counter), randomSendingPeer.getAddress(), randomSendingPeer.getPort(), myDateObj);
            peersSent.put(peer.getTeamName(), peer);
            */
            udpPeerSend.close();
        }
        catch(Exception e){
            System.out.println("Peer " + randomSendingPeer.getAddress() + ":" + randomSendingPeer.getPort() + " was not sent");
        }
    }

    @Override
    public void run() {
        while (Client.isRunning) {
            int counter = 0;
            try {
                sendSnippet();
                sendPeer(counter);
                Thread.sleep(3000);
                timeStamp += 1;
                counter++;
            } catch (Exception e) {
                System.err.println("failed to send snip message ");
                e.printStackTrace();
            }
        }
    }
}
