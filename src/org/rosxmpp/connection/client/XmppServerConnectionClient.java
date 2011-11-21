package org.rosxmpp.connection.client;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcCommonsTransportFactory;
import org.apache.xmlrpc.client.util.ClientFactory;
import org.rosxmpp.connection.server.XmppServerConnection;

// TODO might get rid of this class
public class XmppServerConnectionClient {
	private XmppServerConnection serverConnection;
	private XmlRpcClient client;

	public XmppServerConnection getServerConnection() {
		return serverConnection;
	}
	
	public XmlRpcClient getRpcClient() {
		return client;
	}

	public XmppServerConnectionClient(String url) throws MalformedURLException {
		  // create configuration
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setServerURL(new URL(url));
        config.setEnabledForExtensions(true);  
        config.setConnectionTimeout(60 * 1000);
        config.setReplyTimeout(60 * 1000);

        client = new XmlRpcClient();
      
        client.setTransportFactory(
            new XmlRpcCommonsTransportFactory(client));
        client.setConfig(config);
     
        // make a call using dynamic proxy
        ClientFactory factory = new ClientFactory(client);
        serverConnection = (XmppServerConnection) factory.newInstance(XmppServerConnection.class);
    }
}
