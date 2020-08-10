import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ConnectionFinder extends Thread {
    private ServerSocket serverSocket;
    private GameServer server;
    
    public ConnectionFinder(ServerSocket serverSocket, GameServer server) {
        this.serverSocket = serverSocket;
        this.server = server;
    }
    
    @Override
    public void run() {
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                server.connectPlayer(socket);
            } catch (IOException e) {
                break;
            }
        }
    }
}