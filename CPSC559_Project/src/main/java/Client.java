// source code is multiple files
import java.io.*;
import java.net.Socket;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class Client {
    Scanner keyboard = new Scanner(System.in);
    private ConcurrentHashMap<String, Peer> peersMap = new ConcurrentHashMap<>();
//    Peer[] peersSent = null;

    private String selectTeamName() {
        System.out.print("Enter team name: ");
        return keyboard.nextLine();
    }

    public void processFiles(File[] files, Socket sock) throws IOException {
        String filesText = "java\n";
        for (File file : files) {
            if (file.isDirectory()) {
                processFiles(file.listFiles(), sock);
            } else {
                Optional<String> ext = getFileExtension(file.getName());
                if (ext.isPresent() && ext.get().equals("java")) {
                    String fileText = readFile(file);
                    filesText += fileText;
                }
            }
        }
        filesText += "\n...\n";
        sock.getOutputStream().write(filesText.getBytes());
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
                processFiles(new File(".").listFiles(), sock);
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
        Client client = new Client();
        try {
            client.start();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}