package org.rosxmpp.cli;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.rosxmpp.connection.client.XmlRpcServerProxy;

/**
 * This class implements the main interaction logic with the user from the
 * command line.
 * 
 * @author Pierre-Luc Bacon
 * 
 */
public class RosXMPP {
    HashMap<String, CommandHandler> commands = new HashMap<String, CommandHandler>();

    public RosXMPP(String[] args) {
	commands.put(connectCommand.getCommandName(), connectCommand);
	commands.put(disconnectCommand.getCommandName(), disconnectCommand);
	commands.put(nodeCommand.getCommandName(), nodeCommand);
	commands.put(exposeCommand.getCommandName(), exposeCommand);
	commands.put(topicCommand.getCommandName(), topicCommand);
	commands.put(statusCommand.getCommandName(), statusCommand);

	CommandHandler handler = commands.get(args[0]);
	if (handler == null) {
	    System.out.println(args[0] + " : No such command.");
	    printUsage();
	    System.exit(-1);
	}

	handler.handleCommand(args);
    }

    /**
     * Prints general usage help.
     */
    private void printUsage() {
	System.out.println("rosxmpp [action] [action option]");
	for (Entry<String, CommandHandler> entry : commands.entrySet()) {
	    System.out.println("        " + entry.getValue().getUsage());
	}
    }

    /**
     * Get a server connection from a user and host information. This relies on
     * /var/run/rosxmpp/user@host to record port assignment.
     * 
     * @return a XmppServerConnection proxy object
     * @throws MalformedURLException
     */
    private XmlRpcServerProxy getServerConnection(String user, String server)
	    throws XmlRpcException {
	File pidFile = new File("/var/run/rosxmpp/" + user + "@" + server);

	// Read port on which the server should run
	FileInputStream fstream = null;
	try {
	    fstream = new FileInputStream(pidFile);
	} catch (FileNotFoundException e) {
	    throw new XmlRpcException("Failed to open file "
		    + pidFile.getPath());
	}

	DataInputStream in = new DataInputStream(fstream);
	BufferedReader br = new BufferedReader(new InputStreamReader(in));
	String port = null;
	try {
	    port = br.readLine();
	    in.close();
	} catch (IOException e1) {
	    throw new XmlRpcException("Failed to read port number from " + pidFile.getPath());
	}

	XmlRpcServerProxy clientConnection = null;
	String uri = "http://localhost:" + port + "/xmlrpc";
	try {
	    clientConnection = new XmlRpcServerProxy(uri);
	} catch (MalformedURLException e) {
	    throw new XmlRpcException("Bad URI to ROS master at " + uri);
	}

	return clientConnection;
    }

    /**
     * Return a XmlRpcClient element for accessing the rosxmppbridge server for
     * user@server
     * 
     * @param user
     * @param server
     * @return Return a XmlRpcClient element for accessing the rosxmppbridge
     *         server for user@server
     * @throws XmlRpcException
     */
    private XmlRpcClient getRpcClient(String user, String server)
	    throws XmlRpcException {
	XmlRpcServerProxy client = getServerConnection(user, server);
	return client.getRpcClient();
    }

    /**
     * Check if a pid file exists under /var/run/rosxmpp
     * 
     * @param user
     * @param server
     * @return true if a pid file exists for the pair <user, server>
     */
    private boolean isPidFileExists(String user, String server) {
	File pidFile = new File("/var/run/rosxmpp/" + user + "@" + server);
	return pidFile.exists();
    }

    /**
     * Execute a method on the XML-RPC rosxmpp bridge server
     * 
     * @param method
     * @param params
     * @return The return value resulting from the remote execution.
     * @throws XmlRpcException
     */
    private Object executeBridgeMethod(String user, String server,
	    String method, Object[] params) throws XmlRpcException {
	if (!isPidFileExists(user, server)) {
	    throw new XmlRpcException(
		    "Failed to call remote method on rosxmppbridge for " + user
			    + "@" + server);
	}

	XmlRpcClient rpcClient = getRpcClient(user, server);

	return rpcClient.execute(
		"org.rosxmpp.connection.server.RosXMPPBridgeConnection."
			+ method, params);
    }

    /**
     * Start a new (connected) instance of a rosxmpp bridge server.
     * 
     * @param server
     * @param user
     * @param passwd
     */
    private void startRosXmppServer(String server, String user, String passwd) {
	System.out.println("Starting process ...");
	try {
	    Runtime.getRuntime().exec(
	    // FIXME Remove absolute path
		    "/home/pierre-luc/workspace/rosxmpp/rosxmppbridge "
			    + server + " " + user + " " + passwd + " rosxmpp");
	} catch (IOException e) {
	    System.out.println("Failed to start rosxmppbridge server.");
	}
    }

    CommandHandler connectCommand = new CommandHandler() {
	@Override
	public void handleCommand(String[] args) {
	    if (args.length < 3) {
		System.out.println("rosxmpp connect user@server.com passwd");
		printUsage();
		System.exit(-1);
	    }

	    String[] userServer = args[1].split("@");
	    String user = userServer[0];
	    String server = userServer[1];
	    String passwd = args[2];

	    startRosXmppServer(server, user, passwd);
	}

	@Override
	public String getUsage() {
	    return getCommandName() + " user@server.com passwd";
	}

	@Override
	public String getCommandName() {
	    return "connect";
	}
    };

    CommandHandler disconnectCommand = new CommandHandler() {
	@Override
	public void handleCommand(String[] args) {
	    if (args.length < 2) {
		System.out.println("rosxmpp disconnect user@server.com");
		printUsage();
		System.exit(-1);
	    }

	    String[] userServer = args[1].split("@");
	    String user = userServer[0];
	    String server = userServer[1];

	    try {
		executeBridgeMethod(user, server, "disconnect", new Object[] {});
	    } catch (XmlRpcException e) {
		System.out.println("Disconnection failed for " + user + "@"
			+ server);
		System.exit(-1);
	    }
	}

	@Override
	public String getUsage() {
	    return getCommandName() + " user@server.com";
	}

	@Override
	public String getCommandName() {
	    return "disconnect";
	}
    };

    CommandHandler statusCommand = new CommandHandler() {
	@Override
	public void handleCommand(String[] args) {
	    if (args.length < 2) {
		System.out.println("rosxmpp status user@server.com");
		printUsage();
		System.exit(-1);
	    }

	    String[] userServer = args[1].split("@");
	    String user = userServer[0];
	    String server = userServer[1];

	    boolean ret = false;
	    try {
		ret = (Boolean) executeBridgeMethod(user, server,
			"isConnected", new Object[] {});
	    } catch (XmlRpcException e) {
		System.out.println("Method call failed for " + user + "@"
			+ server);
		System.exit(-1);
	    }

	    if (ret) {
		System.out.println(args[1] + " : connected.");
	    } else {
		System.out.println(args[1] + " : not connected.");
	    }
	}

	@Override
	public String getUsage() {
	    return getCommandName() + " user@server.com";
	}

	@Override
	public String getCommandName() {
	    return "status";
	}
    };

    CommandHandler nodeCommand = new CommandHandler() {
	@Override
	public void handleCommand(String[] args) {
	    if (args.length < 3) {
		System.out.println("rosxmpp node list user@server.com");
		printUsage();
		System.exit(-1);
	    }

	    String[] userServer = args[2].split("@");
	    String user = userServer[0];
	    String server = userServer[1];

	    // Call method
	    Object[] params = new Object[] {};
	    Object[] nodesArray = null;

	    try {
		nodesArray = (Object[]) executeBridgeMethod(user, server,
			"getAvailableNodes", params);
	    } catch (XmlRpcException e) {
		System.out
			.println("Failed to retreive the list of available master nodes "
				+ user + "@" + server);
		System.exit(-1);
	    }

	    String[] nodes = Arrays.copyOf(nodesArray, nodesArray.length,
		    String[].class);

	    // Print node list
	    for (String node : nodes) {
		System.out.println(node);
	    }
	}

	@Override
	public String getUsage() {
	    return getCommandName() + " list user@server.com";
	}

	@Override
	public String getCommandName() {
	    return "node";
	}
    };

    CommandHandler exposeCommand = new CommandHandler() {
	@Override
	public void handleCommand(String[] args) {
	    if (args.length < 3) {
		System.out
			.println("rosxmpp expose http://localhost:11311 user@server.com");
		printUsage();
		System.exit(-1);
	    }

	    String[] userServer = args[2].split("@");
	    String user = userServer[0];
	    String server = userServer[1];
	    String rosMasterUri = args[1];

	    Object[] params = new Object[] { rosMasterUri };
	    Integer ret = null;
	    try {
		ret = (Integer) executeBridgeMethod(user, server,
			"exposeRosMaster", params);
	    } catch (XmlRpcException e) {
		System.out.println("Failed to expose ros master at  "
			+ rosMasterUri + " for " + user + "@" + server);
		System.exit(-1);
	    }

	    if (ret <= 0) {
		System.out.println("Failed to expose ros master "
			+ rosMasterUri + " over XMPP. " + ret);
	    } else {
		System.out.println("Ros master " + rosMasterUri
			+ " is now reachable over XMPP.");
	    }
	}

	@Override
	public String getUsage() {
	    return getCommandName() + " http://localhost:11311 user@server.com";
	}

	@Override
	public String getCommandName() {
	    return "expose";
	}
    };

    CommandHandler topicCommand = new CommandHandler() {

	@Override
	public void handleCommand(String[] args) {
	    if (args.length < 3) {
		System.out
			.println("rosxmpp topic [list, proxy] remote@server.com local@server.com");
		printUsage();
		System.exit(-1);
	    }
	    
	    if (args[1].equals("list")) {
	        handleList(args);
	    } else if (args[1].equals("proxy")) {
		handleProxy(args);
	    }
	}

	public void handleList(String[] args) {
	    String remoteUserServer = args[2];
	    String[] localUserServer = args[3].split("@");
	    String localUser = localUserServer[0];
	    String localServer = localUserServer[1];

	    Object[] params = new Object[] { remoteUserServer };
	    Object[] response = null;

	    try {
		response = (Object[]) executeBridgeMethod(localUser,
			localServer, "getPublishedTopics", params);
	    } catch (XmlRpcException e) {
		System.out.println("Failed to retreive topic list at "
			+ remoteUserServer + " using account " + args[3]);
		System.exit(-1);
	    }

	    int status = ((Integer) response[0]).intValue();
	    String statusMessage = ((String) response[1]);
	    System.out.println("Status " + status + " \"" + statusMessage
		    + "\"");

	    Object[] result = (Object[]) response[2];
	    for (int i = 0; i < result.length; i++) {
		Object[] topicTypePair = (Object[]) result[i];
		System.out.println("Topic " + (String) topicTypePair[0]
			+ " type " + (String) topicTypePair[1]);
	    }
	}
	
	public void handleProxy(String[] args) {    
	    String remoteUserServer = args[2];
	    String[] localUserServer = args[3].split("@");
	    String localUser = localUserServer[0];
	    String localServer = localUserServer[1];
	    
	    Object response = null;
	    try {
		response = executeBridgeMethod(localUser,
			localServer, "proxyRemoteTopics",  new Object[] { remoteUserServer });
	    } catch (XmlRpcException e) {
		System.out.println("Failed to proxy remote topics at "
			+ remoteUserServer + " using account " + args[3]);
		System.exit(-1);
	    }
	    
	    if ((Integer) response <= 0) {
		System.out.println("Failed to proxy remote topics at "
			+ remoteUserServer + " using account " + args[3]);
	    } else {
		System.out.println("Remote topics for " + remoteUserServer + " are now available locally." );
	    }
	}
	
	@Override
	public String getUsage() {
	    return getCommandName()
		    + " [list, proxy] remote@server.com local@server.com";
	}

	@Override
	public String getCommandName() {
	    return "topic";
	}
    };

    /**
     * @param args
     */
    public static void main(String[] args) {
	new RosXMPP(args);
    }

}
