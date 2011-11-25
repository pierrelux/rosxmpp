package org.rosxmpp.connection.server;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Logger;

import org.activequant.xmpprpc.JabberRpcClient;
import org.activequant.xmpprpc.JabberRpcServer;
import org.apache.xmlrpc.XmlRpcException;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;

/**
 * This class manages interaction to a given XMPP server. The CLI rosxmpp
 * program communicates with this class via XML-RPC.
 * 
 * @author Pierre-Luc Bacon
 * 
 */
public class RosXMPPBridgeConnectionManager implements RosXMPPBridgeConnection {
    private static final String PATH_PIDFILES = "/var/run/rosxmpp/";

    private static Logger logger = Logger.getLogger("org.rosxmpp.cli");

    // Resource name for the Jabber-RPC interface
    private static final String ROSXMPP_RPC_RESOURCE = "rosxmpp";

    // Caller id used for XML-RPC over ROS
    public static final String ROSXMPP_CALLERID = ROSXMPP_RPC_RESOURCE;

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

    // XMPP connection to the XMPP server
    private XMPPConnection connection;

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
            logger.info("Node " + presence.getFrom() + " is now " + presence.getType());
            if (presence.isAvailable() == false) {
        	if (jabberRpcClients.get(presence.getFrom()) != null) {
            	    logger.info("Getting rid of Jabber-RPC client connection for " 
        		    + presence.getFrom() + " which is now offline.");
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

    /**
     * Get, or create, a JabberRpcClient for the remote user
     * @param remoteUser The user running a Jabber-RPC server
     * @return a JabberRpcClient for the remote user
     */
    private JabberRpcClient getJabberRpcClient(String remoteUser)
    {
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
	    logger.throwing(this.getClass().getName(), "getPublishedTopics", e);
	    return null;
	}

	// Start the client thread
	xmppRpcClient.start();
	try {
	    // FIXME ! Super-huge ugly hack
	    Thread.sleep(1000);
	} catch (InterruptedException e1) {
	    // TODO Auto-generated catch block
	    e1.printStackTrace();
	}
	
	return xmppRpcClient;
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
	    topics = (Object[]) xmppRpcClient.execute(
		    "master.getPublishedTopics", params);
	} catch (XmlRpcException e) {
	    logger.severe("Failed to call remote method getPublishedTopics on "
		    + remoteUser + " over Jabber-RPC.");
	    logger.throwing(this.getClass().getName(), "getPublishedTopics", e);
	    
	    xmppRpcClient.stop();
	    return new Object[] { (int) -1,
		    "Failed to call getPublishedTopics on Jabber-RPC server." };
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
	MasterImpl masterApi = new MasterImpl(uri);
	try {
	    jabberRpcServer.exposeObject(MASTER_NAMESPACE, masterApi);
	} catch (XmlRpcException e1) {
	    logger.throwing(this.getClass().getName(), "exposeRosMaster", e1);
	    return -1;
	}

	// Start the thread to handle RPC requests
	jabberRpcServer.start();

	logger.info("Master API exposed over Jabber-RPC at "
		+ connection.getUser());

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
	    // SlaveApiHandler slave = new SlaveApiHandler(callerid, topic,
	    // type)
	    // URL slaveUri = slave.getUri();

	    // Register the remote topics name and types locally
	    // masterApi.registerPublisher(ROSXMPP_CALLERID, topic, type,
	    // slaveUri);
	}

	return status;
    }

}
