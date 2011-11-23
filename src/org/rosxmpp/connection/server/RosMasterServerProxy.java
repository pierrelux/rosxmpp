package org.rosxmpp.connection.server;

import java.net.MalformedURLException;

import org.apache.xmlrpc.XmlRpcException;
import org.rosxmpp.connection.client.XmlRpcServerProxy;

public class RosMasterServerProxy extends XmlRpcServerProxy {
	public RosMasterServerProxy() throws MalformedURLException {
		super("http://localhost:11311"); // FIXME 
	}

	public Master getMasterApiProxy() {
		return (Master) getDynamicProxy(Master.class);
	}

	public static void main(String[] args) throws MalformedURLException, XmlRpcException {
		RosMasterServerProxy proxy = new RosMasterServerProxy();

		//master.getPublishedTopics("/rosxmpp", "");
		Object[] params = new Object[]{"rosclipse", ""};
		Object[] response = (Object[]) proxy.getRpcClient().execute("getPublishedTopics", params);
		int status = ((Integer)response[0]).intValue();
		String statusMessage = ((String) response[1]);
		System.out.println("Status " + status + " \"" + statusMessage + "\"");
		
		 Object[] result = (Object[]) response[2];
		 for (int i = 0; i < result.length; i++) {
		     Object[] topicTypePair = (Object[]) result[i];
		     System.out.println("Topic " + (String)topicTypePair[0] + " type " + (String)topicTypePair[1]);
		}
	}
}
