import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class Peer {
    private ConcurrentHashMap<String, Peer> peersMap = new ConcurrentHashMap<>();
    private String teamName;
    private String address;
    private int port;
//    Peer[] peersSent = null;

    private String getTeamName() { return teamName; }
    private String getAddress() { return address; }
    private int getPort() { return port; }

    private void setTeamName(String teamName) {
        this.teamName = teamName;
    }
    private void setAddress(String address) {
        this.address = address;
    }
    private void setPort(int port) {
        this.port = port;
    }

    private String selectTeamName() {
        Scanner keyboard = new Scanner(System.in);
        System.out.print("Enter team name: ");
        setTeamName(keyboard.nextLine());
        return getTeamName();
    }

    public String readFile() {
        try {
            String data = "java\n";
            File myObj = new File("Peer.java");
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                String line = myReader.nextLine();
                System.out.println(line);
                data += line;
            }
            myReader.close();
            return data + "\n" + "...";
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
            return null;
        }
    }

    public void getPeers(BufferedReader reader) throws IOException {
        System.out.println("Getting peers info");
        int numOfPeers = Integer.parseInt(reader.readLine());
        System.out.println(numOfPeers);

        for (int i = 0; i < numOfPeers; i++) {
            String line = reader.readLine();
            System.out.println("Peer " + line);
            String[] peerProperties = line.split(":");

            Peer peer = new Peer();
            peer.setTeamName(getTeamName());
            peer.setAddress(peerProperties[0]);
            peer.setPort(Integer.parseInt(peerProperties[1]));
            peersMap.put(teamName, peer);
        }
        System.out.println(peersMap);
    }

    public void start() throws IOException {
        Socket sock = new Socket("localhost", 55921);

        while (sock.isConnected()) {
            InputStream input = sock.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            String serverReqMsg = reader.readLine();
            System.out.println("Received from server: " + serverReqMsg);
            if (serverReqMsg.equals("get team name")) {
                String teamName = selectTeamName() + "\n";
                sock.getOutputStream().write(teamName.getBytes());
            } else if (serverReqMsg.equals("get code")) {
                System.out.println("Requesting code");
                String file = readFile() + "\n";
                sock.getOutputStream().write(file.getBytes());
            } else if (serverReqMsg.equals("receive peers")) {
                getPeers(reader);
            } else if (serverReqMsg.equals("get report")) {
                System.out.println("Report request");
            } else if (serverReqMsg.equals("close")) {
                sock.close();
            }
        }
        System.out.println("Goodbye...");
    }

    public static void main(String[] args) {
        Peer client = new Peer();
        try {
            client.start();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}