// teamP: Parsa Honarmand, Prit Patel
// File: Peer.java
import java.time.LocalDateTime;

public class Peer {
    private String teamName;
    private String address;
    private int port;
    private LocalDateTime lastNoticed;  
//    Peer[] peersSent = null;

    public Peer(String teamName, String address, int port, LocalDateTime lastNoticed) {
        this.teamName = teamName;
        this.address = address;
        this.port = port;
        this.lastNoticed = lastNoticed;

    }

    public String getTeamName() { return teamName; }
    public String getAddress() { return address; }
    public int getPort() { return port; }
    public LocalDateTime getLastNoticed() { return lastNoticed; }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }
    public void setAddress(String address) {
        this.address = address;
    }
    public void setPort(int port) {
        this.port = port;
    }
    public void setLastNoticed(LocalDateTime lastNoticed){
        this.lastNoticed = lastNoticed;
    }

}