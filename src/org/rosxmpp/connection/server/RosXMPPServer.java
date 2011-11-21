package org.rosxmpp.connection.server;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcServer;
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl;
import org.apache.xmlrpc.webserver.WebServer;
import org.jivesoftware.smack.XMPPException;
import org.rosxmpp.connection.RosXMPPConnectionManager;

/**
 * This class spawns a webserver
 * 
 * @author Pierre-Luc Bacon
 * 
 */
public class RosXMPPServer {
	private static final int port = 8080;
	
	public RosXMPPServer(String server, String user, String pass,
			String ressource) {
		// Attempt to connect
		try {
			RosXMPPConnectionManager.getInstance().connect(server, user, pass, ressource);
		} catch(XMPPException e) {
			System.out.println("Failed to connect : ");
			e.printStackTrace();
		}
		
		// Expose the connection manager over XML-RPC
		WebServer webServer = new WebServer(port);

		XmlRpcServer xmlRpcServer = webServer.getXmlRpcServer();
		PropertyHandlerMapping phm = new PropertyHandlerMapping();
		try {
			phm.load(Thread.currentThread().getContextClassLoader(),
					"XmlRpcServlet.properties");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (XmlRpcException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		xmlRpcServer.setHandlerMapping(phm);
		XmlRpcServerConfigImpl serverConfig = (XmlRpcServerConfigImpl) xmlRpcServer
				.getConfig();
		serverConfig.setEnabledForExtensions(true);
		serverConfig.setContentLengthOptional(false);
		
		try {
			webServer.start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		System.out.println("Server started");
	}
	
	public static void main(String[] args) throws Exception {
		if (args.length < 4) {
			System.out.println("Missing arguments : rosxmppserver [server] [user] [pass] [ressource]");
		}
		
		RosXMPPServer server = new RosXMPPServer(args[0], args[1], args[2], args[3]);
	}
}