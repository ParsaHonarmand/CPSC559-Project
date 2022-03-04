import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

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

    @Override
    public void run() {
        while (Client.isRunning) {
            try {
                sendSnippet();
                Thread.sleep(3000);
                timeStamp += 1;
            } catch (Exception e) {
                System.err.println("failed to send snip message ");
                e.printStackTrace();
            }
        }
    }
}
