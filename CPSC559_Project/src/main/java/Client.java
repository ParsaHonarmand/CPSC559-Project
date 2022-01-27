// File: Client.java
import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class Client {
    Scanner keyboard = new Scanner(System.in);
    private ConcurrentHashMap<String, Peer> peersMap = new ConcurrentHashMap<>();
    private String serverAddress = "";
    private int serverPort;
//    Peer[] peersSent = null;

    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }
    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    private String selectTeamName() {
        System.out.print("Enter team name: ");
        return keyboard.nextLine();
    }

    public void sendSourceCode(File[] files, Socket sock) throws IOException {
//        String filesText = "java\n";
//        sock.getOutputStream().write(filesText.getBytes());
        for (File file : files) {
            if (file.isDirectory()) {
                sendSourceCode(file.listFiles(), sock);
            } else {
                Optional<String> ext = getFileExtension(file.getName());
                if (ext.isPresent() && ext.get().equals("java")) {
                    String fileText = readFile(file);
                    sock.getOutputStream().write(fileText.getBytes());
                    sock.getOutputStream().flush();
//                    filesText += fileText;
                }
            }
        }
    }

    public String readFile(File file) {
        String data = "";
        try {
            Scanner myReader = new Scanner(file);
            while (myReader.hasNextLine()) {
                String line = myReader.nextLine();
                data += line;
            }
            myReader.close();
            System.out.println("Read source code for " + file.getName());
            return data + "\n";
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
            return null;
        }
    }

    // from https://www.baeldung.com/java-file-extension
    public Optional<String> getFileExtension(String filename) {
        return Optional.ofNullable(filename)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(filename.lastIndexOf(".") + 1));
    }

    public void getPeers(BufferedReader reader) throws IOException {
        System.out.println("Getting peers info");
        int numOfPeers = Integer.parseInt(reader.readLine());
        System.out.println(numOfPeers);

        for (int i = 0; i < numOfPeers; i++) {
            String line = reader.readLine();
            System.out.println("Peer " + line);
            String[] peerProperties = line.split(":");

            Peer peer = new Peer(
                    "placeHolder_teamName",
                    peerProperties[0],
                    Integer.parseInt(peerProperties[1])
            );
            peersMap.put(peer.getTeamName(), peer);
        }
        System.out.println(peersMap);
    }

    public String report(){
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();

        String report = Integer.toString(peersMap.size()) + "\n";

        for(int i = 0; i < peersMap.size(); i++){
            report = report + peersMap.get("placeHolder_teamName").getAddress() + ":" + peersMap.get("placeHolder_teamName").getPort() + "\n";
        }
        
        report = report + "1" + "\n" + serverAddress + ":" + serverPort + "\n" + formatter.format(date) + "\n" + peersMap.size();
        
        for(int i = 0; i < peersMap.size(); i++){
            report = report + "\n" + peersMap.get("placeHolder_teamName").getAddress() + ":" + peersMap.get("placeHolder_teamName").getPort() + "\n";
        }

        return report; 
    }

    public void start(String address, int port) throws IOException {
        setServerAddress(address);
        setServerPort(port);

        Socket sock = new Socket(address, port);

        while (!sock.isClosed() && sock.isConnected()) {
            InputStream input = sock.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            String serverReqMsg = reader.readLine();
            System.out.println("Received from server: " + serverReqMsg);
            if (serverReqMsg.equals("get team name")) {
                String teamName = selectTeamName() + "\n";
                sock.getOutputStream().write(teamName.getBytes());
                sock.getOutputStream().flush();
            } else if (serverReqMsg.equals("get code")) {
                System.out.println("Requesting code");

                String filesText = "java\n";
                sock.getOutputStream().write(filesText.getBytes());
                sock.getOutputStream().flush();

                sendSourceCode(new File(".").listFiles(), sock);
                String endCode = "...\n";
                sock.getOutputStream().write(endCode.getBytes());
                sock.getOutputStream().flush();
            } else if (serverReqMsg.equals("receive peers")) {
                getPeers(reader);
            } else if (serverReqMsg.equals("get report")) {
                System.out.println("Report request");
                String report = report();
                sock.getOutputStream().write(report.getBytes());
                sock.getOutputStream().flush();
                System.out.println("Report sent to host.");
            } else if (serverReqMsg.equals("close")) {
                sock.close();
            }
        }
        System.out.println("Goodbye...");
    }

    public static void main(String[] args) {
        if (args.length == 2) {
            try {
                String address = args[0];
                int port = Integer.parseInt(args[1]);
                Client client = new Client();
                try {
                    client.start(address, port);
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            } catch (NumberFormatException e) {
                System.err.println("Argument" + args[0] + " must be an integer.");
                System.exit(1);
            }
        } else {
            System.out.println("Usage: java Client <ip_address> <port>");
        }
    }
}