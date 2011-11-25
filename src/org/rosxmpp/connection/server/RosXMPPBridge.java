package org.rosxmpp.connection.server;

import java.io.IOException;
import java.util.logging.Logger;

import org.apache.mina.util.AvailablePortFinder;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcServer;
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl;
import org.apache.xmlrpc.webserver.WebServer;

/**
 * This class spawns a webserver to handle commands from rosxmpp cli
 * 
 * @author Pierre-Luc Bacon
 * 
 */
public class RosXMPPBridge {
	private static final int port = AvailablePortFinder.getNextAvailable(8080);
    private static Logger logger = Logger.getLogger("org.rosxmpp.cli");

	public RosXMPPBridge(String server, String user, String pass,
			String resource) {	
		// Connect to XMPP server
		RosXMPPBridgeConnectionManager.getInstance().connect(server, user, pass, resource);
		
		// Instantiate XML-RPC interface for the cli program rosxmpp 
		WebServer webServer = new WebServer(port);

		XmlRpcServer xmlRpcServer = webServer.getXmlRpcServer();
		PropertyHandlerMapping phm = new PropertyHandlerMapping();
		try {
			phm.load(Thread.currentThread().getContextClassLoader(),
					"XmlRpcServlet.properties");
		} catch (IOException e1) {
			logger.throwing(this.getClass().getName(), "RosXMPPBridge", e1);
			System.exit(-1);
		} catch (XmlRpcException e1) {
			logger.throwing(this.getClass().getName(), "RosXMPPBridge", e1);
			System.exit(-1);
		}
		xmlRpcServer.setHandlerMapping(phm);
		XmlRpcServerConfigImpl serverConfig = (XmlRpcServerConfigImpl) xmlRpcServer
				.getConfig();
		serverConfig.setEnabledForExtensions(true);
		serverConfig.setContentLengthOptional(false);
		
		// Start XML-RPC server
		try {
			webServer.start();
		} catch (IOException e) {
			logger.severe("Failed to start rosxmppbridge XML-RPC interface.");
			logger.throwing(this.getClass().getName(), "RosXMPPBridge", e);
			System.exit(-1);
		}
		
		RosXMPPBridgeConnectionManager.getInstance().setXmlRpcPort(port);
		
		logger.info("rosxmppbridge XML-RPC interface listening on port " + port);
	}
	
	public static void main(String[] args) throws Exception {
		if (args.length < 4) {
			System.out.println("rosxmppbridge [server] [user] [passwd] [resource]");
			System.exit(-1);
		}
		new RosXMPPBridge(args[0], args[1], args[2], args[3]);
	}
}