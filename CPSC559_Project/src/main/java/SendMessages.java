import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class SendMessages implements Runnable {
    DatagramSocket udpSocket;
    ConcurrentHashMap<String, Peer> peers;
    int timeStamp;

    public SendMessages(ConcurrentHashMap<String, Peer> peersMap, DatagramSocket udpServer) {
        this.peers = peersMap;
        this.timeStamp = 0;
        this.udpSocket = udpServer;
    }

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

    private void sendPeer() throws java.io.IOException, InterruptedException{ 
        TimeUnit.SECONDS.sleep(7);

        Random r = new Random();
        int numOfPeers = peers.size();
        int random = r.nextInt(numOfPeers);
        int randomReceiver = r.nextInt(numOfPeers);
        LocalDateTime myDateObj = LocalDateTime.now();
        
        Peer receivingPeer = new Peer("", "", 0, myDateObj);
        Peer randomPeer = new Peer("", "", 0, myDateObj);

        int i = 0;
        for (Entry<String, Peer> entry : peers.entrySet()) {
            if (i == random) {
                randomPeer = entry.getValue();
            }
            if (i == randomReceiver) {
                    receivingPeer = entry.getValue();
                }
                i++;
        }

        DatagramSocket udpPeerSend = new DatagramSocket();

        InetAddress addressReceiverUDP = InetAddress.getByName(receivingPeer.getAddress());
        String str = "peer" + randomPeer.getAddress() + ":" + randomPeer.getPort();
        byte[] buf = str.getBytes();
        DatagramPacket packetSend = new DatagramPacket(buf, buf.length, addressReceiverUDP, receivingPeer.getPort());

        udpPeerSend.send(packetSend);
        System.out.println("Sent peer at " + randomPeer.getAddress() + ":" +randomPeer.getPort() + " to " + addressReceiverUDP.toString() + ":"+ receivingPeer.getPort());
        udpPeerSend.close();
    }

    @Override
    public void run() {
        while (Client.isRunning) {
            try {
                sendSnippet();
                sendPeer();
                Thread.sleep(3000);
                timeStamp += 1;
            } catch (Exception e) {
                System.err.println("failed to send snip message ");
                e.printStackTrace();
            }
        }
    }
}
