public class Peer {
    private String teamName;
    private String address;
    private int port;
//    Peer[] peersSent = null;

    public Peer(String teamName, String address, int port) {
        this.teamName = teamName;
        this.address = address;
        this.port = port;
    }

    public String getTeamName() { return teamName; }
    public String getAddress() { return address; }
    public int getPort() { return port; }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }
    public void setAddress(String address) {
        this.address = address;
    }
    public void setPort(int port) {
        this.port = port;
    }
}