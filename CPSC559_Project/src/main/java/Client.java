// File: Client.java
import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.Scanner;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Client {
    Scanner keyboard = new Scanner(System.in);
    private ConcurrentHashMap<String, Peer> peersMap = new ConcurrentHashMap<>();
    private String serverAddress = "";
    private int serverPort;
    Date date;
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

    /**
     * Recursively loops through a directory and calls readFile() on
     * java source code files
     * @param files a list of files in the src directory
     * @param sock socket connection object
     * @throws IOException
     */
    public void sendSourceCode(File[] files, Socket sock) throws IOException {
        for (File file : files) {
            if (file.isDirectory()) {
                sendSourceCode(file.listFiles(), sock);
            } else {
                Optional<String> ext = getFileExtension(file.getName());
                if (ext.isPresent() && ext.get().equals("java")) {
                    String fileText = readFile(file);
                    sock.getOutputStream().write(fileText.getBytes());
                    sock.getOutputStream().flush();
                }
            }
        }
    }

    /**
     * Takes a java file as input and reads it line by line and returns it as a string
     * @param file
     * @return the string of the source code that's been read
     */
    public String readFile(File file) {
        String data = "";
        try {
            Scanner myReader = new Scanner(file);
            while (myReader.hasNextLine()) {
                String line = myReader.nextLine();
                data += line + "\n";
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

    /**
     * from https://www.baeldung.com/java-file-extension
     * gets the file extension (java, txt, etc) of the inputted file
     * @param filename the file to extract the extension from
     * @return an Optional object that if present, contains the extension as a string
     */
    public Optional<String> getFileExtension(String filename) {
        return Optional.ofNullable(filename)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(filename.lastIndexOf(".") + 1));
    }

    /**
     * Extracts the peers from the socket reader
     * and stores them in the peersMap field for this class that
     * keeps track of these peers
     * @param reader
     * @throws IOException
     */
    public void getPeers(BufferedReader reader) throws IOException {
        int numOfPeers = Integer.parseInt(reader.readLine());
        date = new Date();

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

    /**
     * Creates a report from the peersMap and
     * the date that this map's data was retrieved
     * @return the report as a string
     */
    public String createReport(){
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String report = peersMap.size() + "\n";

        for (Map.Entry<String, Peer> entry : peersMap.entrySet()) {
            String teamName = entry.getKey();
            Peer peer = entry.getValue();
            report += peer.getAddress() + ":" + peer.getPort() + "\n";
        }

        System.out.println("DATE: " + date);
        report += "1\n" + serverAddress + ":" + serverPort +
                (date != null ? "\n" + formatter.format(date) + "\n": "") +
                peersMap.size();

        for (Map.Entry<String, Peer> entry : peersMap.entrySet()) {
            String teamName = entry.getKey();
            Peer peer = entry.getValue();
            report += "\n" + peer.getAddress() + ":" + peer.getPort() + "\n";
        }

        return report; 
    }

    /**
     * Starts the socket and handles all the incoming requests from the registry
     * @param address the address of the registry
     * @param port the port in which the registry listens on
     * @throws IOException
     */
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
                String report = createReport();
                System.out.println("Report:\n" + report);
                sock.getOutputStream().write(report.getBytes());
                sock.getOutputStream().flush();
                System.out.println("Report sent to host.");
            } else if (serverReqMsg.equals("close")) {
                sock.close();
            }
        }
        System.out.println("Goodbye...");
    }

    /**
     * Checks for registry address and port number and if available,
     * starts the client workflow
     * @param args address and port
     */
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