package org.rosxmpp.connection.server;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Logger;

import org.activequant.xmpprpc.InstanceBasedHandler;
import org.apache.mina.util.AvailablePortFinder;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.server.XmlRpcServer;
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl;
import org.apache.xmlrpc.webserver.WebServer;

public class SlaveApiHandler implements Slave {
    private static final int MIN_PORT_NUMBER = 40000;
    private WebServer webServer;
    private static Logger logger = Logger.getLogger("org.rosxmpp.cli");

    /**
     * This class is in charge of managing a remote 
     * (over XMPP) topic for local subscribers.  
     * 
     * @author Pierre-Luc Bacon
     *
     */
    class TopicHandler {
	private static final String TCPROS = "TCPROS";
	// Map callerid to server sockets.
	HashMap<String, TcpRosServer> channels = new HashMap<String, TcpRosServer>();
	
	public TopicHandler(String topic, String type) {
	    // TODO Auto-generated constructor stub
	}
	
	/**
	 * Create a communication channel for the caller
	 * @param callerId The caller requesting this channel
	 * @param protocols The protocols that the caller wants to use.
	 * @return (int, str, [str, !XMLRPCLegalValue*] )
	 */
	public Object[] requestTopic(String callerId, Object[] protocols)
	{
	    // Iterate over all protocols to find TCP ROS
	    int code = -1;
	    String statusMessage = "Request failed";
	    Object[] chosenProtocol = new Object[3];
	    
	    for (Object prot : protocols) {
		Object[] protocol = (Object[]) prot;
		String[] candidateProtocol = Arrays.copyOf(protocol, protocol.length, String[].class);
		
		if (candidateProtocol[0].equals(TCPROS)) {
		    logger.info("Creating TCPROS channel for callerid " + callerId);
		    TcpRosServer tcpRosServer = null;
		    try {
			tcpRosServer = new TcpRosServer(AvailablePortFinder.getNextAvailable(MIN_PORT_NUMBER));
			tcpRosServer.start();	
			logger.info("TcpRosServer started");
		    } catch (TcpRosServerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			break;
		    }
		    	    
		    // TODO Start server thread
		    channels.put(callerId, tcpRosServer);

		    chosenProtocol[0] = (String) "TCPROS";
		    try {
			chosenProtocol[1] = (String) tcpRosServer.getHostname();
		    } catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		    }
		    chosenProtocol[2] = (int) tcpRosServer.getLocalPort();
		    
		    statusMessage = "ready on " + (String) chosenProtocol[1] + ":" + tcpRosServer.getLocalPort();
		    
		    logger.info(statusMessage);
		    
		    code = 1;
		    return new Object[]{code, statusMessage, chosenProtocol};
		}
	    }
	    
            return new Object[]{code, statusMessage, chosenProtocol};
	}
    };
    
    HashMap<String, TopicHandler> topicHandlers = new HashMap<String, TopicHandler>();
    
    public SlaveApiHandler() {

	startWebserver();

	logger.info("rosxmppbridge XML-RPC interface started on port " + webServer.getPort());
    }

    public void startWebserver() {
	// Instantiate a ROS slave XML-RPC interface
	webServer = new WebServer(AvailablePortFinder
		.getNextAvailable(MIN_PORT_NUMBER));

	// Map this instance
	XmlRpcServer xmlRpcServer = webServer.getXmlRpcServer();
	InstanceBasedHandler mapper = new InstanceBasedHandler();
	xmlRpcServer.setHandlerMapping(mapper);
	try {
	    mapper.addHandler("", this);
	} catch (XmlRpcException e1) {
	    logger.severe("Failed to map slave API over XMl-RPC");
	    e1.printStackTrace();
	}

	// Web server configs
	XmlRpcServerConfigImpl serverConfig = (XmlRpcServerConfigImpl) xmlRpcServer
		.getConfig();
	serverConfig.setEnabledForExtensions(true);
	serverConfig.setContentLengthOptional(false);

	// Start XML-RPC server
	try {
	    webServer.start();
	} catch (IOException e) {
	    logger.severe("Failed to start rosxmppbridge XML-RPC interface.");
	    e.printStackTrace();
	}
    }

    /**
     * @return the XML-RPC slave URI.
     */
    public String getUri() {
	return "http://merlin:" + webServer.getPort() + "/";
    }

    @Override
    public List<Object> getBusInfo(String callerId) {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public List<Object> getBusStats(String callerId) {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public List<Object> getMasterUri(String callerId) {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public List<Object> getPid(String callerId) {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public List<Object> getPublications(String callerId) {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public List<Object> getSubscriptions(String callerId) {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public List<Object> paramUpdate(String callerId, String key, boolean value) {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public List<Object> paramUpdate(String callerId, String key, char value) {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public List<Object> paramUpdate(String callerId, String key, byte value) {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public List<Object> paramUpdate(String callerId, String key, short value) {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public List<Object> paramUpdate(String callerId, String key, int value) {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public List<Object> paramUpdate(String callerId, String key, double value) {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public List<Object> paramUpdate(String callerId, String key, String value) {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public List<Object> paramUpdate(String callerId, String key, List<?> value) {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public List<Object> paramUpdate(String callerId, String key, Vector<?> value) {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public List<Object> paramUpdate(String callerId, String key, Map<?, ?> value) {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public List<Object> publisherUpdate(String callerId, String topic,
	    Object[] publishers) {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public Object[] requestTopic(String callerId, String topic,
	    Object[] protocols) {
	logger.info("Requesting topic " + topic);
	TopicHandler handler = topicHandlers.get(topic);
	if (handler == null) {
	    int code = -1;
	    String statusMessage = "Request failed";
	    Object[] chosenProtocol = new Object[] {"TCPROS", "", 0};
	    
	    logger.warning("No topic handler found for " + topic);
	    return new Object[]{code, statusMessage, chosenProtocol};
	}

	logger.info("Calling requestTopic on handler");
	return handler.requestTopic(callerId, protocols);
    }

    @Override
    public List<Object> shutdown(String callerId, String message) {
	// TODO Auto-generated method stub
	return null;
    }

    public void manageTopic(String topic, String type) {
	topicHandlers.put(topic, new TopicHandler(topic, type));	
    }
}
