import java.awt.event.*;
import java.util.Scanner;

public class Tester {
	public static void main(String[] args) {
		int numPlayers = 3;
		boolean instantBid = false;
		String hostname = "localhost";
		int port = 6066;
		
		GameClient[] gcs = new GameClient[numPlayers];
		String s = "";
		
		for (int i = 0; i < numPlayers; i++) {
			gcs[i] = new GameClient();
			gcs[i].execute();
			gcs[i].connect();
			sleep(100);
		}
		
		sleep(500);
		gcs[0].readyPressed();
		sleep(100);
		
		System.out.println("READY");
		Scanner sc = new Scanner(System.in);
		
		if (instantBid) s="bid";
		
		while(true) {
			if (s.equals("")) s = sc.nextLine();
			if (s.equals("close")) {
				sc.close();
				for(int i=1;i<numPlayers;i++) {
					gcs[i].close();
					sleep(100);
				}
				gcs[0].dispatchEvent(new WindowEvent(gcs[0], WindowEvent.WINDOW_CLOSING));
			} else if(s.equals("bid")) {
				for(int i=0;i<numPlayers;i++) {
					int j = (i+1)%numPlayers;
					
					int x = 380;//800/2-(Math.min(52/numPlayers, 10)-1)*40/2;
					int y = 353;//600-210;
					
					//gcs[j].getCanvas().dispatchEvent(new MouseEvent(gcs[j],MouseEvent.MOUSE_PRESSED,System.currentTimeMillis(),0, x,y ,1,false));
					//sleep(10);
					//gcs[j].getCanvas().dispatchEvent(new MouseEvent(gcs[j],MouseEvent.MOUSE_RELEASED,System.currentTimeMillis(),0, x,y ,1,false));
					//sleep(100);
				}
			}
			s = "";
		}
	}
	
	public static void bid() {
		
	}
	
	public static void sleep(int t) {
		try {
			Thread.sleep(t);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
