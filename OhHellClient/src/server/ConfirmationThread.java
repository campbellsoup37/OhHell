package server;
public class ConfirmationThread extends Thread {
    private final int maxNumberOfTries = 10;
    
    private boolean confirmed = false;
    private boolean disconnected = false;
    private Command command;
    private PlayerThread playerThread;
    
    public ConfirmationThread(Command command, PlayerThread playerThread) {
        this.command = command;
        this.playerThread = playerThread;
    }
    
    @Override
    public void run() {
        int count = 0;
        while (!confirmed && !disconnected && playerThread.getConfThread().equals(this)) {
            try {
                if (count > 0) {
                    playerThread.write(command.toString());
                }
                sleep(1000);
                count++;
                if (count == maxNumberOfTries) {
                    playerThread.handleDisconnect();
                    break;
                }
            } catch (InterruptedException e) {
                // This is how we kill the thread
            }
        }
        if (disconnected) {
            System.out.println("Player " + playerThread.getPlayer().realName() + " failed to confirm command " + command + ".");
        }
    }
    
    public void confirm() {
        //System.out.println(command.getId()+" confirmed from thread");
        confirmed = true;
        interrupt();
    }
    
    public void disconnect() {
        //System.out.println(command.getId()+" disconnected");
        disconnected = true;
        interrupt();
    }
}