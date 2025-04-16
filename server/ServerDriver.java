package server;

import java.io.IOException;

public class ServerDriver {
	private Server thisUDPServer;

	public ServerDriver(int serverPort) { 
		try { 	thisUDPServer = new Server(serverPort); }
		catch (IOException e) { e.printStackTrace(); } 
	}
	
	public static void main(String[] args) { 
		if(args.length >= 1) {
			System.out.println("Server Starting...");
			ServerDriver app = new ServerDriver(Integer.parseInt(args[0]));
		} 
	} 
}