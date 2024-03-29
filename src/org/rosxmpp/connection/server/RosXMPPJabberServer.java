package org.rosxmpp.connection.server;

import java.net.MalformedURLException;
import java.util.List;
import java.util.logging.Logger;

import org.apache.xmlrpc.XmlRpcException;
import org.rosxmpp.connection.client.XmlRpcServerProxy;
import org.rosxmpp.transport.UDTMediaSession;

/**
 * This class is exposed over Jabber-RPC. This is not the XML-RPC master
 * interface for ROS.
 * 
 * @author pierre-luc
 * 
 */
public class RosXMPPJabberServer implements Master {

    private XmlRpcServerProxy rosServerProxy;
    static final String callerId = "rosxmpp";

    private static Logger logger = Logger.getLogger("org.rosxmpp.cli");

    public RosXMPPJabberServer(String rosMasterUri) {
	try {
	    rosServerProxy = new XmlRpcServerProxy("http://localhost:11311");
	} catch (MalformedURLException e) {
	    logger.throwing(this.getClass().getName(), "MasterImpl", e);
	    // TODO Do something. Throw again ?
	}
    }

    @Override
    public Object[] getPublishedTopics(String callerId, String subgraph) {
	logger
		.info("Jabber-RPC Server : Requesting topic list from ROS master ...");
	Object[] params = new Object[] { callerId, subgraph };
	Object[] response = null;
	try {
	    response = (Object[]) rosServerProxy.getRpcClient().execute(
		    "getPublishedTopics", params);
	} catch (XmlRpcException e) {
	    logger.throwing(this.getClass().getName(), "MasterImpl", e);
	}
	return response;
    }

    /**
     * This method will be called once a jingle session has been established.
     * 
     * @param jingleSessionId
     *            Jingle session id previsouly established
     * @param topicName
     *            The topic name to serve over the jingle ICE UDP transport
     * @return -1 on error, 1 on success (ROS convention)
     */
    public Object[] requestTopic(String jingleSessionId, String topicName) {
	logger.info("Handling requestTopic for jingle session id "
		+ jingleSessionId + " for topic " + topicName);
	Object[] ret = new Object[] { 1, "ready", 1 };

	// Check if there is an existing jingle session
	UDTMediaSession mediaSession = (UDTMediaSession) RosXMPPBridgeConnectionManager.getInstance().getIncomingMediaSession(jingleSessionId);
	if (mediaSession == null) {
	    ret[0] = -1;
	    ret[1] = "No such jingle sid";
	    return ret; 
	}
	
	// Start a XML-RPC handler for publisher notifications
	// get slave api server address
	
	// Register a new subscriber to the topic
	logger.info("Jabber-RPC Server : Registering subscriber on behalf of remote peer");
	Object[] params = new Object[] { callerId, topicName, new Object[] {"TCPROS"} };
	Object[] response = null;
	try {
	    response = (Object[]) rosServerProxy.getRpcClient().execute(
		    "registerSubscriber", params);
	} catch (XmlRpcException e) {
	    logger.severe("Failed to request topic");
	    e.printStackTrace();
	}
	
	// Request topic on publisher
	logger.info("Jabber-RPC Server : Requesting topic for remote peer");
	params = new Object[] { callerId, topicName, new Object[] {"TCPROS"} };
	response = null;
	try {
	    response = (Object[]) rosServerProxy.getRpcClient().execute(
		    "requestTopic", params);
	} catch (XmlRpcException e) {
	    logger.severe("Failed to request topic");
	    e.printStackTrace();
	}
	
	// Open TCP connection to publisher
	mediaSession.setLocalTopicPublisherAddress((String) response[1], (Integer) response[2]);
	mediaSession.setTrasmit(true);
	mediaSession.startTrasmit();
	
	return ret;
    }

    @Override
    public List<Object> getSystemState(String callerId) {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public List<Object> getUri(String callerId) {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public List<Object> lookupNode(String callerId, String nodeName) {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public List<Object> lookupService(String callerId, String service) {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public Object[] registerPublisher(String callerId, String topicName,
	    String topicType, String callerApi) {
	Object[] params = new Object[] { callerId, topicName };
	Object[] response = null;
	try {
	    response = (Object[]) rosServerProxy.getRpcClient().execute(
		    "registerPublisher", params);
	} catch (XmlRpcException e) {
	    logger.throwing(this.getClass().getName(), "MasterImpl", e);
	}
	return response;
    }

    @Override
    public List<Object> registerService(String callerId, String service,
	    String serviceApi, String callerApi) {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public List<Object> registerSubscriber(String callerId, String topicName,
	    String topicType, String callerApi) {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public List<Object> unregisterPublisher(String callerId, String topicName,
	    String callerApi) {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public List<Object> unregisterService(String callerId, String service,
	    String serviceApi) {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public List<Object> unregisterSubscriber(String callerId, String topicName,
	    String callerApi) {
	// TODO Auto-generated method stub
	return null;
    }

}
