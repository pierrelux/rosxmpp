package org.rosxmpp.connection.server;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import org.activequant.xmpprpc.JabberRpcClient;
import org.activequant.xmpprpc.JabberRpcServer;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.JingleSession;
import org.jivesoftware.smackx.jingle.JingleSessionRequest;
import org.jivesoftware.smackx.jingle.listeners.JingleSessionRequestListener;
import org.jivesoftware.smackx.jingle.media.JingleMediaManager;
import org.jivesoftware.smackx.jingle.nat.BasicTransportManager;
import org.rosxmpp.connection.client.XmlRpcServerProxy;
import org.rosxmpp.transport.UDTMediaManager;

/**
 * This class manages interaction to a given XMPP server. The CLI rosxmpp
 * program communicates with this class via XML-RPC.
 * 
 * @author Pierre-Luc Bacon
 * 
 */
public class RosXMPPBridgeConnectionManager implements RosXMPPBridgeConnection {
    private static final String DEFAULT_ROS_MASTER_URI = "http://localhost:11311";

    private static final String PATH_PIDFILES = "/var/run/rosxmpp/";

    private static Logger logger = Logger.getLogger("org.rosxmpp.cli");

    // Resource name for the Jabber-RPC interface
    private static final String ROSXMPP_RPC_RESOURCE = "rosxmpp";

    // Caller id used for XML-RPC over ROS
    public static final String ROSXMPP_CALLERID = "/rosxmpp";

    // Jabber-RPC namespace used for the mapping
    private static final String MASTER_NAMESPACE = "master";

    // This class is implemented as a singleton to overcome the
    // problem of persistence across XML-RPC requests.
    private static RosXMPPBridgeConnectionManager instance;

    // Base port on which the XML-RPC CLI interface for rosxmpp
    // should attempt to run. If not available, will search starting
    // from this value.
    private int xmlRpcPort = 8080;

    // This file is user@server and contains the port for
    // the XML-RPC CLI interface
    private File pidFile;

    // A single instance of a Slave API handler deals with all topic
    // subscriptions
    SlaveApiHandler slaveHandler;

    // Instead of instantiating new proxies to ros master, we maintain them here
    HashMap<String, XmlRpcServerProxy> rosServerProxies = new HashMap<String, XmlRpcServerProxy>();

    // XMPP connection to the XMPP server
    private XMPPConnection connection;

    // Jingle manager for the Jingle ICE UDP server
    private JingleManager jm;

    // Can map multiple ROS cores using this rosxmppbridge
    private HashMap<String, JabberRpcServer> jabberRpcServers = new HashMap<String, JabberRpcServer>();

    // To avoid re-creating a new JabberRpcClient for every remote call,
    // keep them in memory for the given remote node.
    // TODO : Auto-remove when node disconnects. Implies node presence.
    private HashMap<String, JabberRpcClient> jabberRpcClients = new HashMap<String, JabberRpcClient>();

    /**
     * We use a connection listener to automatically delete the entry under
     * /var/run/rosxmpp that indicates the port of the XML-RPC webserver in
     * charge of bridging using a given identity over XMPP.
     */
    private ConnectionListener conlistener = new ConnectionListener() {
	public void connectionClosed() {
	    logger.info("XMPP connection closed for " + connection.getUser());
	    pidFile.delete();
	}

	public void connectionClosedOnError(Exception e) {
	    logger.warning("XMPP connection closed on error for "
		    + connection.getUser());
	}

	@Override
	public void reconnectingIn(int seconds) {
	    logger.info("XMPP reconnection for " + connection.getUser());
	}

	@Override
	public void reconnectionFailed(Exception e) {
	    logger.info("XMPP reconnection failed for " + connection.getUser());
	}

	@Override
	public void reconnectionSuccessful() {
	    logger.info("XMPP reconnection succeeded for "
		    + connection.getUser());
	}
    };

    /**
     * Used for housekeeping on the JabberRpcClient instances.
     */
    private RosterListener rosterListener = new RosterListener() {

	@Override
	public void presenceChanged(Presence presence) {
	    logger.info("Node " + presence.getFrom() + " is now "
		    + presence.getType());
	    if (presence.isAvailable() == false) {
		if (jabberRpcClients.get(presence.getFrom()) != null) {
		    logger
			    .info("Getting rid of Jabber-RPC client connection for "
				    + presence.getFrom()
				    + " which is now offline.");
		    jabberRpcClients.remove(presence.getFrom());
		}
	    }
	}

	@Override
	public void entriesUpdated(Collection<String> addresses) {
	    // TODO Auto-generated method stub

	}

	@Override
	public void entriesDeleted(Collection<String> addresses) {
	    // TODO Auto-generated method stub

	}

	@Override
	public void entriesAdded(Collection<String> addresses) {
	    // TODO Auto-generated method stub

	}
    };

    /**
     * This class is implemented as a singleton.
     */
    private RosXMPPBridgeConnectionManager() {
    }

    /**
     * Get, or create, a JabberRpcClient for the remote user
     * 
     * @param remoteUser
     *            The user running a Jabber-RPC server
     * @return a JabberRpcClient for the remote user
     */
    private JabberRpcClient getJabberRpcClient(String remoteUser) {
	JabberRpcClient xmppRpcClient = jabberRpcClients.get(remoteUser);
	if (xmppRpcClient != null) {
	    return xmppRpcClient;
	}

	// Create a Jabber-RPC client
	try {
	    xmppRpcClient = new JabberRpcClient(connection, remoteUser + "/"
		    + ROSXMPP_RPC_RESOURCE);
	} catch (Exception e) {
	    logger.severe("Failed to create a jabber-rcp client.");
	    logger.severe(e.getMessage());
	    return null;
	}

	// Start the client thread
	xmppRpcClient.start();

	jabberRpcClients.put(remoteUser, xmppRpcClient);
	logger.info("User " + remoteUser + "/"
		    + ROSXMPP_RPC_RESOURCE
		+ " added for instantiated Jabber-RPC client");

	return xmppRpcClient;
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

    /**
     * Get the port over which the XML-RPC interface for the CLI tool rosxmpp
     * should use.
     * 
     * @return the XML-RPC port.
     */
    public int getXmlRpcPort() {
	return xmlRpcPort;
    }

    /**
     * Set the port over which the XML-RPC interface for the CLI tool rosxmpp
     * should use. This value is set from RosXMPPBridge after it successfully
     * started a webserver instance. param xmlRpcPort the XML-RPC port.
     */
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

	// Use for PID file managemement under /var/run/rosxmpp
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

	// Subscribe to Roster events
	connection.getRoster().addRosterListener(rosterListener);

	// Write a dummy file under /var/run/rosxmpp to keep track of the active
	// connection
	pidFile = new File(PATH_PIDFILES + user + "@" + server);
	try {
	    FileWriter fstream = new FileWriter(pidFile);
	    BufferedWriter out = new BufferedWriter(fstream);
	    out.write(String.valueOf(xmlRpcPort));
	    out.close();
	} catch (Exception e) {
	    logger.severe("Failed to write PID file " + pidFile.getName());
	    return -1;
	}
	
	initializeJingleServer();

	// Start a Slave API handlers
	slaveHandler = new SlaveApiHandler();

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
	    String user = entry.getUser();
	    if (roster.getPresence(user).isAvailable()) {
		availableNodes.add(user);
	    }
	}

	return availableNodes.toArray(new String[availableNodes.size()]);
    }

    @Override
    public Object[] getPublishedTopics(String remoteUser) {
	logger
		.info("Handling getPublishedTopics request to node "
			+ remoteUser);

	JabberRpcClient xmppRpcClient = getJabberRpcClient(remoteUser);
	if (xmppRpcClient == null) {
	    return new Object[] { (int) -1,
		    "Failed to start Jabber-RPC client." };
	}

	Object[] topics = null;
	ArrayList<Object> params = new ArrayList<Object>();
	params.add(ROSXMPP_CALLERID);
	// Empty string below is to obtain all topics (root node)
	params.add("");
	try {
	    logger
		    .info("Calling master.getPublishedTopics over Jabber-RPC ...");

	    topics = (Object[]) xmppRpcClient.execute(MASTER_NAMESPACE
		    + ".getPublishedTopics", params);

	} catch (XmlRpcException e) {
	    logger.severe("Failed to call remote method getPublishedTopics on "
		    + remoteUser + " over Jabber-RPC.");
	    logger.severe(e.getMessage());

	    xmppRpcClient.stop();

	    return new Object[] { (int) -1,
		    "Failed to call getPublishedTopics on Jabber-RPC server." };
	}

	return topics;
    }

    /**
     * Initializes a Jingle listener for TCPROS tunnelling
     */
    private void initializeJingleServer()
    {
	BasicTransportManager transportManager = new BasicTransportManager();
	List<JingleMediaManager> mediaManagers = new ArrayList<JingleMediaManager>();
	mediaManagers.add(new UDTMediaManager(transportManager));

	jm = new JingleManager(connection, mediaManagers);

	jm.addJingleSessionRequestListener(new JingleSessionRequestListener() {
	    public void sessionRequested(JingleSessionRequest request) {
		try {
		    logger.info("Accepting Jingle session requests ...");
		    
		    // Accept the call
		    JingleSession incoming = request.accept();
		    logger.info("Jingle session request accepted.");

		    // Keep track of the session id somewhere 
		    
		    // Start the call
		    incoming.startIncoming();
		}
		catch (XMPPException e) {
		   logger.severe("Failed to accept jingle session requests");
		   e.printStackTrace();
		}
	    }
	});
	
	logger.info("Jingle server initialized");
    }
    
    /**
     * Initiate a jingle session to a remote ros master
     * TODO Find another way to avoid exposing this method
     * @param remoteMaster The remote ros master jid to contact
     */
    public void startOutgoingJingleChannel(String remoteMaster)
    {
	logger.info("Initiating Jingle session to " + remoteMaster);
	JingleSession outgoing = null;
	try {
	    outgoing = jm.createOutgoingJingleSession(remoteMaster);
	} catch (XMPPException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	outgoing.startOutgoing();	
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
	    logger.severe(e1.getMessage());
	    return -1;
	}

	// Expose the Master API over Jabber-RPC
	MasterImpl masterApi = new MasterImpl(uri);
	try {
	    jabberRpcServer.exposeObject(MASTER_NAMESPACE, masterApi);
	} catch (XmlRpcException e1) {
	    logger.severe(e1.getMessage());
	    return -1;
	}

	// Start the thread to handle RPC requests
	jabberRpcServer.start();

	logger.info("Master API exposed over Jabber-RPC at "
		+ connection.getUser());
	
	return 1;
    }

    /**
     * Get or create a proxy to a ROS master.
     * @param masterUri The uri to the ros master.
     * @return A instance of an XmlRpcClient to the specified server.
     */
    private XmlRpcClient getRosMasterRpcClient(String masterUri) {
	XmlRpcServerProxy proxy = rosServerProxies.get(masterUri);
	if (proxy == null) {
	    XmlRpcServerProxy rosServerProxy = null;
	    try {
		rosServerProxy = new XmlRpcServerProxy(masterUri);
	    } catch (MalformedURLException e) {
		logger.severe("Failed to create proxy to ROS master at "
			+ masterUri);
	    }
	    rosServerProxies.put(masterUri, rosServerProxy);
	    return rosServerProxy.getRpcClient();
	}

	return proxy.getRpcClient();
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

	// Iterate over all remote topics and register them locally
	Object[] result = (Object[]) response[2];
	for (int i = 0; i < result.length; i++) {
	    Object[] topicTypePair = (Object[]) result[i];
	    String topic = ((String) topicTypePair[0]) + "_xmppremote"; // FIXME Get rid of this suffix
	    String type = (String) topicTypePair[1];

	    // Register the remote topics name and types on the ROS master
	    XmlRpcClient client = getRosMasterRpcClient(DEFAULT_ROS_MASTER_URI);
	    Object[] params = new Object[] {ROSXMPP_CALLERID, topic, type, slaveHandler.getUri()};
	    try {
		response = (Object[]) client.execute("registerPublisher", params);
	    } catch (XmlRpcException e) {
		logger.severe("Failed to invoke registerPublisher.");
		return -1;
	    }
	    
	    // Check if the local registration succeeded
	    if (((Integer) response[0]).intValue() <= 0) {
		logger.severe("Failed to register remote topic " + 
			topic + " of type " + type + 
			" on ROS master at " + DEFAULT_ROS_MASTER_URI);
		return -1;
	    }
	    
	    // Tell the slave handler to now serve request for this topic
	    // TODO Check for concurrency issues (client connecting before the handler is up)
	    slaveHandler.manageTopic(topic, type, remoteNode + "/" + ROSXMPP_RPC_RESOURCE);	    
	}

	return status;
    }

}
