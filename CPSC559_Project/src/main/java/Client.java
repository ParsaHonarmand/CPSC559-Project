
// File: Client.java
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Client {
    Scanner keyboard = new Scanner(System.in);
    private ConcurrentHashMap<String, Peer> peersMap = new ConcurrentHashMap<>();
    Date date;
    int arbtiraryNum;
    Socket sock;
    InputStream input;
    BufferedReader reader;
    DatagramSocket udpServer;
    private PeerCom peerCom;
    private SendMessages msgSender;
    public static volatile boolean isRunning = true;
    private ArrayList<String> snippets;

    // Prompt for team name
    private String selectTeamName() {
        System.out.print("Enter team name: ");
        return keyboard.nextLine();
    }

    /**
     * Recursively loops through a directory and calls readFile() on java source
     * code files
     * 
     * @param files a list of files in the src directory
     * @param sock  socket connection object
     * @throws IOException
     */
    public void sendSourceCode(File[] files, Socket sock) throws IOException {
        for (File file : files) {
            if (file.isDirectory()) { // If a folder is detected then extract nested files
                sendSourceCode(file.listFiles(), sock);
            } else { // If a java file is detected then extract source code
                Optional<String> ext = getFileExtension(file.getName());
                if (ext.isPresent() && ext.get().equals("java")) {
                    String fileText = readFile(file);
                    sock.getOutputStream().write(fileText.getBytes()); // Send to registry
                    sock.getOutputStream().flush();
                }
            }
        }
    }

    /**
     * Takes a java file as input and reads it line by line and returns it as a
     * string
     * 
     * @param file
     * @return the string of the source code that's been read
     */
    public String readFile(File file) { // Used in sendSourceCode processes to read files
        String data = "";
        try {
            Scanner myReader = new Scanner(file);
            while (myReader.hasNextLine()) { // Extract code line by line given a java file input
                String line = myReader.nextLine();
                data += line + "\n";
            }
            myReader.close();
            System.out.println("Read source code for " + file.getName());
            return data + "\n"; // Return entire file code to calling method
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * from https://www.baeldung.com/java-file-extension gets the file extension
     * (java, txt, etc) of the inputted file
     * 
     * @param filename the file to extract the extension from
     * @return an Optional object that if present, contains the extension as a
     *         string
     */
    public Optional<String> getFileExtension(String filename) {
        return Optional.ofNullable(filename).filter(f -> f.contains("."))
                .map(f -> f.substring(filename.lastIndexOf(".") + 1));
    }

    /**
     * Extracts the peers from the socket reader and stores them in the peersMap
     * field for this class that keeps track of these peers
     * 
     * @param reader
     * @throws IOException
     */
    public void getPeers(BufferedReader reader) throws IOException {
        int numOfPeers = Integer.parseInt(reader.readLine());
        date = new Date();
        for (int i = 0; i < numOfPeers; i++) {
            String line = reader.readLine();
            System.out.println("Peer " + line);
            String[] peerProperties = line.split(":"); // Split address:port
            boolean repeated = false;

            // check to see if this peer already exists or not to handle duplication
            for (Peer existingPeer : peersMap.values()) {
                String peerLocation = existingPeer.getAddress() + ":" + existingPeer.getPort();
                if (peerLocation.equals(line)) {
                    repeated = true;
                    break;
                }
            }

            // if peer is not duplicate, add to map of peers
            if (!repeated) {
                LocalDateTime myDateObj = LocalDateTime.now();

                Peer peer = new Peer(peerProperties[0]+":"+Integer.parseInt(peerProperties[1]),
                        peerProperties[0], Integer.parseInt(peerProperties[1]), myDateObj);
                peersMap.put(peer.getTeamName(), peer); // Populate hashmap using team name as key
                arbtiraryNum++;
            }
        }
        System.out.println(peersMap);
    }

    /**
     * Creates a report from the peersMap and the date that this map's data was
     * retrieved
     * @param udpServer
     * 
     * @return the report as a string
     */
    public String createReport(Socket sock, DatagramSocket udpServer) {
        // Used to record report date
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        PeerCom peerCom = new PeerCom(peersMap, udpServer);
//        SendMessages sendMessages = new SendMessages(peersMap, udpServer);
//        ConcurrentHashMap<String, Peer> allPeers = peerCom.getAllPeers();
//        ConcurrentHashMap<String, Peer> peersSent = sendMessages.getPeersSent();

        String report = peersMap.size() + "\n"; // Initial state of report composition in string form

        // If peers are previously received
        if (peersMap.size() > 0) {
            // Add all peer details
            for (Map.Entry<String, Peer> entry : peersMap.entrySet()) {
                String teamName = entry.getKey();
                Peer peer = entry.getValue();
                report += peer.getAddress() + ":" + peer.getPort() + "\n";
            }

            // Add number of sources, source address, source port, date,
            // number of peers for that source
            // In this case we will only have 1 source so this
            // implementation will change to accommodation more sources
            report += "1\n" + sock.getInetAddress().getHostAddress() + ":" + sock.getPort() + "\n"
                    + formatter.format(date) + "\n" + peersMap.size() + "\n";

            for (Map.Entry<String, Peer> entry : peersMap.entrySet()) {
                String teamName = entry.getKey();
                Peer peer = entry.getValue();
                report += peer.getAddress() + ":" + peer.getPort() + "\n";
            }

            System.out.println(peerCom.getReceivedPeers());
            peerCom.getReceivedPeers().forEach((key, value) -> System.out.println(key + " " + value.getPort()));
            System.out.println("HELLO");

            String peersReceivedLog = "";
            peersReceivedLog += peerCom.getPeersRecvSize() + "\n";
            peersReceivedLog += peerCom.getPeersRecvLog();
            report += peersReceivedLog;

            String peersSentLog = "";
            peersSentLog += msgSender.getPeersSentSize() + "\n";
            peersSentLog += msgSender.getPeersSendingLog();
            report += peersSentLog;

            //report += snippet messages with timestamps

        } else { // If no peers are detected
            /*
             * report += "1\n" + serverAddress + ":" + serverPort + "\n" +
             * formatter.format(date) + "\n" + peersMap.size() + "\n";
             */

            report += "0\n";
        }

        report += snippets.size() + "\n";
        for (int i = 0; i < snippets.size(); i++) {
            report += snippets.get(i) + "\n";
        }

        return report;
    }

    public void connectToRegistryAgain(String address, int port, String teamName) throws IOException {
        sock = new Socket(address, port);
        input = sock.getInputStream();
        reader = new BufferedReader(new InputStreamReader(input));
        String serverReqMsg = "";
        while (!sock.isClosed() && sock.isConnected() && (serverReqMsg = reader.readLine()) != null) {
            System.out.println("Received from server: " + serverReqMsg);
            if (serverReqMsg.equals("get team name")) {
                teamName += "\n";
                sock.getOutputStream().write(teamName.getBytes());
                sock.getOutputStream().flush();
            } else if (serverReqMsg.equals("get code")) {
                String filesText = ".java\n";
                sock.getOutputStream().write(filesText.getBytes());
                sock.getOutputStream().flush();

                sendSourceCode(new File(".").listFiles(), sock);
                String endCode = "...\n";
                sock.getOutputStream().write(endCode.getBytes());
                sock.getOutputStream().flush();
            } else if (serverReqMsg.equals("receive peers")) {
                getPeers(reader);
            } else if (serverReqMsg.equals("get report")) {
                String report = createReport(sock, udpServer);
                System.out.println("Report:\n" + report);
                sock.getOutputStream().write(report.getBytes());
                sock.getOutputStream().flush();
                System.out.println("Report sent to host.");
            } else if (serverReqMsg.equals("close")) {
                sock.close();
            } else if (serverReqMsg.equals("get location")){
                String clientAddress = sock.getLocalAddress().toString().substring(1);
                int udpPort = udpServer.getLocalPort();

                String location = clientAddress + ":" + udpPort;
                System.out.println("Sending peer " + location + " location to registry");
                location += "\n";
                sock.getOutputStream().write(location.getBytes());
                sock.getOutputStream().flush();
            }
        }
    }

    /**
     * Starts the socket and handles all the incoming requests from the registry
     * 
     * @param address the address of the registry
     * @param port    the port in which the registry listens on
     * @throws IOException
     */
    public void start(String address, int port, String teamName) throws IOException {
        udpServer = new DatagramSocket();
        System.out.println("UDP socket setup at " + udpServer.getLocalPort());

        sock = new Socket(address, port);
        InputStream input = sock.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        String serverReqMsg = "";

        //PeerCom thread initialize
        peerCom = new PeerCom(peersMap, udpServer);
        msgSender = new SendMessages(peersMap, udpServer);
        Thread peerComThread = new Thread(peerCom);
        Thread msgSenderThread = new Thread(msgSender);

        while (!sock.isClosed() && sock.isConnected() && (serverReqMsg = reader.readLine()) != null) {
            System.out.println("Received from server: " + serverReqMsg);
            if (serverReqMsg.equals("get team name")) {
                teamName += "\n";
                sock.getOutputStream().write(teamName.getBytes());
                sock.getOutputStream().flush();
            } else if (serverReqMsg.equals("get code")) {
                String filesText = ".java\n";
                sock.getOutputStream().write(filesText.getBytes());
                sock.getOutputStream().flush();

                sendSourceCode(new File(".").listFiles(), sock);
                String endCode = "...\n";
                sock.getOutputStream().write(endCode.getBytes());
                sock.getOutputStream().flush();
            } else if (serverReqMsg.equals("receive peers")) {
                getPeers(reader);
                //Start sending other peers that this peer exists
                peerComThread.start();
                msgSenderThread.start();
                while (peerComThread.isAlive()) {
                    snippets = peerCom.getSnippets();
                }
                connectToRegistryAgain(address, port, teamName);
            } else if (serverReqMsg.equals("get report")) {
                String report = createReport(sock, udpServer);
                System.out.println("Report:\n" + report);
                sock.getOutputStream().write(report.getBytes());
                sock.getOutputStream().flush();
                System.out.println("Report sent to host.");
            } else if (serverReqMsg.equals("close")) {
                sock.close();
            } else if (serverReqMsg.equals("get location")){
                String clientAddress = sock.getLocalAddress().toString().substring(1);
                int udpPort = udpServer.getLocalPort();
                String location = clientAddress + ":" + udpPort;
                System.out.println("Sending peer " + location + " location to registry");
                location += "\n";
                sock.getOutputStream().write(location.getBytes());
                sock.getOutputStream().flush();
            }
        }
        System.out.println("Goodbye...");
    }

    /**
     * Checks for registry address and port number and if available, starts the
     * client workflow
     * 
     * @param args address and port
     */
    public static void main(String[] args) throws IOException {
        if (args.length == 3) {
            try {
                String address = args[0];
                int port = Integer.parseInt(args[1]);
                String teamName = args[2];
                Client client = new Client();
                try {
                    client.start(address, port, teamName);
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            } catch (NumberFormatException e) {
                System.err.println("Argument" + args[0] + " must be an integer.");
                System.exit(1);
            }
        } else {
            System.out.println("Usage: java Client <ip_address> <port> <team_name>");
        }
    }
}