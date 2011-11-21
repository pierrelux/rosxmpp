package org.rosxmpp.connection;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.logging.Logger;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

/**
 * This class manages interaction to a given XMPP server. The CLI rosxmpp
 * program communicates with this class via XML-RPC.
 * 
 * @author Pierre-Luc Bacon
 * 
 */
public class RosXMPPConnectionManager {
	private XMPPConnection connection;
	private String server;
	private String user;
	private String pass;
	private String ressource;
	private static RosXMPPConnectionManager instance;
	private File pidFile;

	private ConnectionListener conlistener = new ConnectionListener() {
		public void connectionClosed() {
			System.out.println("XMPP Connection Closed");
			pidFile.delete();
		}

		public void connectionClosedOnError(Exception e) {
		}

		@Override
		public void reconnectingIn(int seconds) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void reconnectionFailed(Exception e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void reconnectionSuccessful() {
			// TODO Auto-generated method stub
			
		}
	};
	
	private RosXMPPConnectionManager() {}
	public static synchronized RosXMPPConnectionManager getInstance() {
		if (null == instance) {
			instance = new RosXMPPConnectionManager();
		}
		return instance;
	}
	
	public void connect(String server, String user, String pass,
			String ressource) throws XMPPException {
		// Create XMPP connection to gmail.com server
		connection = new XMPPConnection(server);	

		connection.connect();
		connection.addConnectionListener(conlistener);
		connection.login(user, pass, ressource);

		this.server = server;
		this.user = user;
		this.pass = pass;
		this.ressource = ressource;
		
		// Write a dummy file under /var/run to keep track of the active connection
		pidFile = new File("/var/run/rosxmpp/" + user + "@" + server);
		try {
			FileWriter fstream = new FileWriter(pidFile);
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(String.valueOf(8080)); // FIXME Should take its value from RosXMPPServer
			out.close();
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
		}
		
		System.out.println("Connected");
	}

	public void disconnect() {
		connection.disconnect();
	}

	public boolean isConnected() {
		return connection.isConnected();
	}
	
	public String[] getAvailableNodes() {
        Roster roster = connection.getRoster();

		ArrayList<String> availableNodes = new ArrayList<String>();
        for (RosterEntry entry : roster.getEntries())
        {
            availableNodes.add(entry.getUser());
        }
        
        return availableNodes.toArray(new String[availableNodes.size()]);
	}

}
