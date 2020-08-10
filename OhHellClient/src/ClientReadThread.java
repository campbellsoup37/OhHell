import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ClientReadThread extends Thread {
    private BufferedReader reader;
    private GameClient client;
    
    public ClientReadThread(Socket socket, GameClient client) {
        this.client = client;
        
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            client.getKicked();
            e.printStackTrace();
        }
    }
    
    @Override
    public void run() {
        while (true) {
            try {
                String command = reader.readLine();
                if (command == null) {
                    break;
                }
                client.receiveCommand(command);
            } catch (IOException e) {
                client.getKicked();
                break;
            }
        }
    }
}