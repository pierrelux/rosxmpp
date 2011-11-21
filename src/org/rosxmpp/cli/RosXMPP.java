package org.rosxmpp.cli;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.rosxmpp.connection.client.XmppServerConnectionClient;
import org.rosxmpp.connection.server.XmppServerConnection;

/**
 * This class implements the main interaction logic with the user from the
 * command line.
 * 
 * @author Pierre-Luc Bacon
 * 
 */
public class RosXMPP {
	public RosXMPP(String[] args) {
		parseArguments(args);
	}

	/**
	 * Parse the CLI arguments passed to the program
	 * 
	 * @param args
	 */
	private void parseArguments(String[] args) {
		if (args.length < 1) {
			printUsage();
			System.exit(-1);
		}
		if (args[0].equals("connect")) {
			handleConnect(args);
			return;
		}

		if (args[0].equals("disconnect")) {
			handleDisconnect(args);
			return;
		}

		if (args[0].equals("status")) {
			handleStatus(args);
			return;
		}

		if (args[0].equals("node")) {
			handleNode(args);
			return;
		}
	}

	/**
	 * Get a server connection from a user and host information. This relies on
	 * /var/run/rosxmpp/user@host to record port assignment.
	 * 
	 * @return a XmppServerConnection proxy object
	 * @throws MalformedURLException
	 */
	private XmppServerConnectionClient getServerConnection(String user,
			String server) throws FileNotFoundException, MalformedURLException {
		File pidFile = new File("/var/run/rosxmpp/" + user + "@" + server);

		// Read port on which the server should run
		FileInputStream fstream = null;
		fstream = new FileInputStream(pidFile);

		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String port = null;
		try {
			port = br.readLine();
			in.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		XmppServerConnectionClient clientConnection = null;
		clientConnection = new XmppServerConnectionClient("http://localhost:"
				+ port + "/xmlrpc");
		return clientConnection;
	}

	private XmppServerConnection getServerProxy(String user, String server)
			throws FileNotFoundException, MalformedURLException {
		XmppServerConnectionClient client = getServerConnection(user, server);
		return client.getServerConnection();
	}

	private XmlRpcClient getRpcClient(String user, String server)
			throws FileNotFoundException, MalformedURLException {
		XmppServerConnectionClient client = getServerConnection(user, server);
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
	 * Test to see if a server is alive for a given pid file.
	 * 
	 * @param user
	 * @param server
	 * @return
	 */
	private boolean isServerAlive(String user, String server) {
		if (!isPidFileExists(user, server)) {
			System.out.println("Pid file does not exists");
			return false;
		}

		XmppServerConnection serverConnection = null;
		try {
			serverConnection = getServerProxy(user, server);
		} catch (FileNotFoundException e1) {
			System.out.println("Pid file not found");
			return false;
		} catch (MalformedURLException e) {
			System.out.println("XML-RPC web server URL malformed");
			return false;
		}

		try {
			serverConnection.isConnected();
		} catch (UndeclaredThrowableException e) {
			System.out.println("Cannot call isConnected");
			return false;
		}

		return true;
	}

	/**
	 * Process the "connect" action command
	 * 
	 * @param args
	 *            The full CLI arguments.
	 */
	private void handleConnect(String[] args) {
		if (args.length < 3) {
			System.out.println("rosxmpp connect user@server.com passwd");
			printUsage();
			System.exit(-1);
		}

		String[] userServer = args[1].split("@");
		String user = userServer[0];
		String server = userServer[1];
		String passwd = args[2];

		// Make sure there is no connected server
		XmppServerConnection serverConnection = null;
		if (isPidFileExists(user, server)) {
			try {
				serverConnection = getServerProxy(user, server);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (serverConnection.isConnected()) {
				System.out.println("Already connected to " + server + " as "
						+ user);
				return;
			}

			serverConnection.connect(server, user, passwd, "rosxmpp");
		} else {
			System.out.println("Starting new server");
			startRosXmppServer(server, user, passwd);
		}
	}

	private void startRosXmppServer(String server, String user, String passwd) {
		System.out.println("Starting process ...");
		try {
			Process p = Runtime.getRuntime().exec(
					"/home/pierre-luc/workspace/rosxmpp/rosxmppserver.sh "
							+ server + " " + user + " " + passwd + " rosxmpp");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Process the "connect" action command
	 * 
	 * @param args
	 *            The full CLI arguments.
	 */
	void handleDisconnect(String[] args) {
		if (args.length < 2) {
			System.out.println("rosxmpp disconnect user@server.com");
			printUsage();
			System.exit(-1);
		}

		String[] userServer = args[1].split("@");
		String user = userServer[0];
		String server = userServer[1];

		if (isPidFileExists(user, server)) {
			XmppServerConnection serverConnection = null;
			try {
				serverConnection = getServerProxy(user, server);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			serverConnection.disconnect();
		}
	}

	/**
	 * Return the status of a connection.
	 * 
	 * @param args
	 */
	private void handleStatus(String[] args) {
		if (args.length < 2) {
			System.out.println("rosxmpp disconnect user@server.com");
			printUsage();
			System.exit(-1);
		}

		String[] userServer = args[1].split("@");
		String user = userServer[0];
		String server = userServer[1];

		if (isPidFileExists(user, server)) {
			XmppServerConnection serverConnection = null;
			try {
				serverConnection = getServerProxy(user, server);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (serverConnection.isConnected()) {
				System.out.println(args[1] + " : connected.");
			} else {
				System.out.println(args[1] + " : not connected.");
			}
		} else {
			System.out.println(args[1] + " : not connected.");
		}
	}

	/**
	 * Process the "node" action command
	 * 
	 * @param args
	 *            The full CLI arguments.
	 */
	void handleNode(String[] args) {
		if (args.length < 3) {
			System.out.println("rosxmpp node list user@server.com");
			printUsage();
			System.exit(-1);
		}

		String[] userServer = args[2].split("@");
		String user = userServer[0];
		String server = userServer[1];

		if (isPidFileExists(user, server)) {
			XmlRpcClient rpcClient = null;
			try {
				rpcClient = getRpcClient(user, server);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			Object[] params = new Object[] {};
			Object[] nodesArray = null;
			try {
				nodesArray = (Object[]) rpcClient
						.execute(
								"org.rosxmpp.connection.server.XmppServerConnection.getAvailableNodes",
								params);
			} catch (XmlRpcException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			String[] nodes = Arrays.copyOf(nodesArray, nodesArray.length,
					String[].class);
			
			for (String node : nodes) {
				System.out.println(node);
			}

		}
	}

	/**
	 * Prints general usage help.
	 */
	void printUsage() {
		System.out.println("rosxmpp [action] [action option]");
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		RosXMPP rosXmpp = new RosXMPP(args);
	}

}