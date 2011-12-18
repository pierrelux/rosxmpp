package org.rosxmpp.connection.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Logger;

public class TcpRosServer extends Thread {

    private ServerSocket serverSocket = null;
    private int maxConnections = 100;
    private String remoteMasterJid;
    private String topic;
    private static Logger logger = Logger.getLogger("org.rosxmpp.cli");

    public TcpRosServer(int port, String topic, String remoteMasterJid) throws TcpRosServerException {
	logger.info("Attempting to create a local server socket on port " + port);
	try {
	    serverSocket = new ServerSocket(port);
	} catch (IOException e) {
	    throw new TcpRosServerException("Failed to start server socket");
	}
	
	this.remoteMasterJid = remoteMasterJid;
	this.topic = topic;
    }

    public String getHostname() throws UnknownHostException
    {
	return InetAddress.getLocalHost().getHostName();
    }
    
    public int getLocalPort() 
    {
	return serverSocket.getLocalPort();
    }
    
    @Override
    public void run() {
	int i = 0;
	while((i++ < maxConnections) || (maxConnections == 0)){
	    try {
		logger.info("Accepting TCP ros client on " + getHostname() + ":" + getLocalPort());
	    } catch (UnknownHostException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	    }

	    Socket server = null;
	    try {
		 server = serverSocket.accept();
	    } catch (IOException e) {
		logger.severe("Failed to accept " + e.getMessage());
	    }
	 
	    logger.info("Client accepted " + server.getInetAddress().getCanonicalHostName());
	    
	    // FIXME This whole class might be implemented inside SlaveApiServer instead
	    RosXMPPBridgeConnectionManager.getInstance().establishJingleRosChannel(remoteMasterJid, topic);

	}
    }
}
