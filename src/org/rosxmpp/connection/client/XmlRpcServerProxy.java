package org.rosxmpp.connection.client;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcCommonsTransportFactory;
import org.apache.xmlrpc.client.util.ClientFactory;

public class XmlRpcServerProxy {
	private XmlRpcClient client;
	private ClientFactory factory;

	public XmlRpcServerProxy(String url) throws MalformedURLException {
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
        factory = new ClientFactory(client);
    }

	@SuppressWarnings("unchecked")
	public Object getDynamicProxy(Class remoteObjectClass) {
        return factory.newInstance(remoteObjectClass);
	}
	
	public XmlRpcClient getRpcClient() {
		return client;
	}
}
