package org.rosxmpp.connection.server;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.logging.Logger;

import org.activequant.xmpprpc.JabberRpcServer;
import org.apache.xmlrpc.XmlRpcException;
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
public class RosXMPPBridgeConnectionManager implements RosXMPPBridgeConnection {
	private XMPPConnection connection;
	private static RosXMPPBridgeConnectionManager instance;
	private File pidFile;

    private static Logger logger = Logger.getLogger("org.rosxmpp.cli");

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
	private JabberRpcServer jabberRpcServer;
	private MasterImpl masterApi;
	private Thread jabberRpcThread;
	
	private RosXMPPBridgeConnectionManager() {}
	public static synchronized RosXMPPBridgeConnectionManager getInstance() {
		if (null == instance) {
			instance = new RosXMPPBridgeConnectionManager();
		}
		return instance;
	}
	
	@Override
	public int connect(String server, String user, String pass,
			String ressource) {
		logger.fine("Handling connect() request");

		connection = new XMPPConnection(server);	
		
		// Connect to XMPP server
		try {
			connection.connect();
		} catch (XMPPException e1) {
			logger.severe("Failed to connect to XMPP server " + server);
			return -1;
		}
		logger.info("Connected to XMPP server " + server);
		
		connection.addConnectionListener(conlistener);
		
		// Login to XMPP server
		try {
			connection.login(user, pass, ressource);
		} catch (XMPPException e1) {
			logger.severe("Failed to login to XMPP server " + server + " as " + user);
			return -1;
		}
		logger.info("Logged in to XMPP server " + server + " as " + user);
			
		// Write a dummy file under /var/run to keep track of the active connection
		pidFile = new File("/var/run/rosxmpp/" + user + "@" + server);
		try {
			FileWriter fstream = new FileWriter(pidFile);
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(String.valueOf(8080)); // FIXME Should take its value from RosXMPPServer
			out.close();
		} catch (Exception e) {
			logger.severe("Failed to write PID file " + pidFile.getName());
			return -1;
		}
		
		return 0;
	}

	@Override
	public int disconnect() {
		logger.info("Handling disconnect request");
		
		connection.disconnect();
		return 0;
	}

	@Override
	public boolean isConnected() {
		logger.info("Handling isConnected request");

		return connection.isConnected();
	}
	
	@Override
	public String[] getAvailableNodes() {
		logger.info("Handling getAvailableNodes request");

        Roster roster = connection.getRoster();

		ArrayList<String> availableNodes = new ArrayList<String>();
        for (RosterEntry entry : roster.getEntries())
        {
            availableNodes.add(entry.getUser());
        }
        
        return availableNodes.toArray(new String[availableNodes.size()]);
	}
	
	@Override
	public Object[] getPublishedTopics(String callerId, String subgraph) {
		logger.fine("Handling getPublishedTopics request");

		return masterApi.getPublishedTopics(callerId, subgraph);
	}
	
	@Override
	public int exposeRosMaster(String uri) {
		// TODO Make sure we are not already exposing this ROS master 
		logger.info("Handling exposeRosMaster request");
		
		// Create a JabberRpcServer to answer queries over XMPP
		try {
			jabberRpcServer = new JabberRpcServer(connection);
		} catch (Exception e1) {
			logger.throwing(this.getClass().getName(), "exposeRosMaster", e1);
			return -1;
		}
		
		// Expose the Master API over Jabber-RPC
		masterApi = new MasterImpl(uri);
		try {
			jabberRpcServer.exposeObject("master", masterApi);
		} catch (XmlRpcException e1) {
			logger.throwing(this.getClass().getName(), "exposeRosMaster", e1);
			return -1;
		}
		
		// Spawn a new thread to handle RPC requests
		jabberRpcThread = new Thread(jabberRpcServer);
		jabberRpcThread.start();

		logger.info("Master API exposed over Jabber-RPC");

		return 0;
	}

}
