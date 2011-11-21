package org.rosxmpp.connection.server;

public interface XmppServerConnection {
	public int connect(String server, String user, String pass, String ressource);
	public int disconnect();	
	public boolean isConnected();	
}
