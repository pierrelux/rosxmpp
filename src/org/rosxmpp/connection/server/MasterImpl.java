package org.rosxmpp.connection.server;

import java.net.MalformedURLException;
import java.util.List;

import org.apache.xmlrpc.XmlRpcException;
import org.rosxmpp.connection.client.XmlRpcServerProxy;

public class MasterImpl implements Master {

	private XmlRpcServerProxy rosServerProxy;
	static final String callerId = "rosxmpp";

	public MasterImpl(String rosMasterUri) {
		try {
			rosServerProxy = new XmlRpcServerProxy("http://localhost:11311");
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			System.out.println("Failed to contact master");
			e.printStackTrace();
		}
	}

	@Override
	public Object[] getPublishedTopics(String callerId, String subgraph) {
		Object[] params = new Object[]{callerId, subgraph};
		Object[] response = null;
		try {
			response = (Object[]) rosServerProxy.getRpcClient().execute("getPublishedTopics", params);
		} catch (XmlRpcException e) {
			System.out.println("Failed to contact master");
		}
		return response;
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
	public List<Object> registerPublisher(String callerId, String topicName,
			String topicType, String callerApi) {
		// TODO Auto-generated method stub
		return null;
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
