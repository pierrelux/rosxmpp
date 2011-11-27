package org.rosxmpp.connection.server;

import java.io.IOException;
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
    private static Logger logger = Logger.getLogger("org.rosxmpp.cli");
    private static final int MIN_PORT_NUMBER = 40000;
    private WebServer webServer;

    public SlaveApiHandler() {

	startWebserver();

	logger.info("rosxmppbridge XML-RPC interface started");
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
	return "http://localhost:" + webServer.getPort();
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
    public List<Object> requestTopic(String callerId, String topic,
	    Object[] protocols) {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public List<Object> shutdown(String callerId, String message) {
	// TODO Auto-generated method stub
	return null;
    }
}
