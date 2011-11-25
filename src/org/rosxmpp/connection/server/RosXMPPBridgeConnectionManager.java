package org.rosxmpp.connection.server;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

import org.activequant.xmpprpc.JabberRpcClient;
import org.activequant.xmpprpc.JabberRpcServer;
import org.apache.xmlrpc.XmlRpcException;
import org.jivesoftware.smack.Connection;
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
	private static final String ROSXMPP_RPC_RESOURCE = "rosxmpp";
	private static final String MASTER_NS = "master";
	private static RosXMPPBridgeConnectionManager instance;
	private static Logger logger = Logger.getLogger("org.rosxmpp.cli");

	private int xmlRpcPort = 8080;

	private File pidFile;
	private MasterImpl masterApi;
	private XMPPConnection connection;

	// Can map multiple ROS cores using this rosxmppbridge
	private HashMap<String, JabberRpcServer> jabberRpcServers = new HashMap<String, JabberRpcServer>();

	public static final String ROSXMPP_CALLERID = ROSXMPP_RPC_RESOURCE;

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

	/**
	 * This class is implemented as a singleton.
	 */
	private RosXMPPBridgeConnectionManager() {
	}

	/**
	 * Get the single instance of this class.
	 * 
	 * @return single instance of class RosXMPPBridgeConnectionManager.
	 */
	public static synchronized RosXMPPBridgeConnectionManager getInstance() {
		if (null == instance) {
			instance = new RosXMPPBridgeConnectionManager();
		}
		return instance;
	}

	public int getXmlRpcPort() {
		return xmlRpcPort;
	}

	public void setXmlRpcPort(int xmlRpcPort) {
		this.xmlRpcPort = xmlRpcPort;
	}
	
	@Override
	public int connect(String server, String user, String pass, String ressource) {
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
			logger.severe("Failed to login to XMPP server " + server + " as "
					+ user);
			return -1;
		}
		logger.info("Logged in to XMPP server " + server + " as " + user);

		// Write a dummy file under /var/run to keep track of the active
		// connection
		pidFile = new File("/var/run/rosxmpp/" + user + "@" + server);
		try {
			FileWriter fstream = new FileWriter(pidFile);
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(String.valueOf(xmlRpcPort)); 
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
		return 1;
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
		for (RosterEntry entry : roster.getEntries()) {
			availableNodes.add(entry.getUser());
		}

		return availableNodes.toArray(new String[availableNodes.size()]);
	}

	@Override
	public Object[] getPublishedTopics(String remoteUser) {
		logger.info("Handling getPublishedTopics request to node " + remoteUser);

		// Create a Jabber-RPC client
		JabberRpcClient xmppRpcClient = null;
		try {
			xmppRpcClient = new JabberRpcClient(connection, remoteUser + "/" + ROSXMPP_RPC_RESOURCE);
		} catch (Exception e) {
			logger.severe("Failed to create a jabber-rcp client.");
			logger.throwing(this.getClass().getName(), "getPublishedTopics", e);
		}

		xmppRpcClient.start();
		try {
			// FIXME ! Super-huge ugly hack
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		Object[] topics = null;
		ArrayList<Object> params = new ArrayList<Object>();
		params.add(ROSXMPP_CALLERID);
		params.add("");
		try {
			logger.info("Calling master.getPublishedTopics over Jabber-RPC ...");
			topics = (Object[]) xmppRpcClient.execute("master.getPublishedTopics", params);
		} catch (XmlRpcException e) {
			logger.severe("Failed to call remote method getPublishedTopics on "
					+ remoteUser + " over Jabber-RPC.");
			logger.throwing(this.getClass().getName(), "getPublishedTopics", e);
		}
		xmppRpcClient.stop();

		return topics;
	}

	@Override
	public int exposeRosMaster(String uri) {
		logger.info("Handling exposeRosMaster request");

		if (jabberRpcServers.get(uri) != null) {
			logger.warning("Already exposing ROS master at " + uri);
			return -1;
		}
	
		// Create a JabberRpcServer to answer queries over XMPP
		JabberRpcServer jabberRpcServer;
		try {
			jabberRpcServer = new JabberRpcServer(connection);
			jabberRpcServers.put(uri, jabberRpcServer);
		} catch (Exception e1) {
			logger.throwing(this.getClass().getName(), "exposeRosMaster", e1);
			return -1;
		}

		// Expose the Master API over Jabber-RPC
		masterApi = new MasterImpl(uri);
		try {
			jabberRpcServer.exposeObject(MASTER_NS, masterApi);
		} catch (XmlRpcException e1) {
			logger.throwing(this.getClass().getName(), "exposeRosMaster", e1);
			return -1;
		}

		// Start the thread to handle RPC requests
		jabberRpcServer.start();

		logger.info("Master API exposed over Jabber-RPC at " + connection.getUser());

		return 1;
	}

	@Override
	public int proxyRemoteTopics(String remoteNode) {
		// Retrieve the list of remote topics
		Object[] response = getPublishedTopics(remoteNode);

		int status = ((Integer) response[0]).intValue();
		if (status <= 0) {
			logger.severe("Failed to get remote topic lists for " + remoteNode);
			return -1;
		}

		Object[] result = (Object[]) response[2];
		for (int i = 0; i < result.length; i++) {
			Object[] topicTypePair = (Object[]) result[i];
			String topic = (String) topicTypePair[0];
			String type = (String) topicTypePair[1];
			
			// Start a XML-RPC slave handler 
			// SlaveApiHandler slave = new SlaveApiHandler(callerid, topic, type)
			// URL slaveUri = slave.getUri();
			
			// Register the remote topics name and types locally
			//masterApi.registerPublisher(ROSXMPP_CALLERID, topic, type,
			//		slaveUri);
		}

		return status;
	}

}
