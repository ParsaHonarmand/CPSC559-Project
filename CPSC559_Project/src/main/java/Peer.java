import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Scanner;

public class Peer {
    private Scanner keyboard = new Scanner(System.in);
    HashMap<String, Integer> peersMap = new HashMap<>();
    String address;
    int port;
    String teamName;

    Peer[] peersSent = null;

    String key() {
        return teamName;
    }
    public String toString() {
        return key() + " " + address + ":" + port;
    }

    private String getID() {
        System.out.print("Enter ID: ");
        return keyboard.nextLine();
    }

    private String getTeamName() {
        System.out.print("Enter team name: ");
        return keyboard.nextLine();
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
        String numOfPeers = reader.readLine();
        System.out.println(numOfPeers);
        while (!reader.readLine().equals("\n") && !reader.readLine().equals("get report")) {
            String peer = reader.readLine();
            System.out.println("Peer " + peer);
            String[] peerProperties = peer.split(":");
            int port = Integer.parseInt(peerProperties[1]);
            peersMap.put(peerProperties[0], port);
        }
        System.out.println(peersMap);
    }

    public void start() throws IOException {
        Scanner keyboard = new Scanner(System.in);
        int reqCounter = 0;
        String teamName = "";
        String id = "";
        Socket sock = new Socket("localhost", 55921);

        while (sock.isConnected()) {
            InputStream input = sock.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            String serverReqMsg = reader.readLine();
            System.out.println("Received from server: " + serverReqMsg);
            if (serverReqMsg.equals("get team name")) {
                teamName += getTeamName() + "\n";
                sock.getOutputStream().write(teamName.getBytes());
            } else if (serverReqMsg.equals("get code")) {
                System.out.println("Requesting code");
                String file = readFile() + "\n";
                sock.getOutputStream().write(file.getBytes());
            } else if (serverReqMsg.equals("receive peers")) {
                System.out.println("Getting peers info");
                getPeers(reader);
            } else if (serverReqMsg.equals("get report")) {
                System.out.println("Report request");
            } else if (serverReqMsg.equals("close")) {
                sock.close();
            }
            reqCounter++;
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