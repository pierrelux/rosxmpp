package org.rosxmpp.connection.server;

import org.jivesoftware.smack.XMPPException;
import org.rosxmpp.connection.RosXMPPConnectionManager;

public class XmppServerConnectionImpl implements XmppServerConnection {
	@Override
	public int connect(String server, String user, String pass, String ressource) {
		try {
			RosXMPPConnectionManager.getInstance().connect(server, user, pass, ressource);
		} catch (XMPPException e) {
			System.out.println("Failed to connect.");
			return -1;
		}
		System.out.println("Connected.");

		return 0;
	}

	@Override
	public int disconnect() {
		System.out.println("Disconnecting");
		RosXMPPConnectionManager.getInstance().disconnect();
		return 0;
	}

	@Override
	public boolean isConnected() {
		return RosXMPPConnectionManager.getInstance().isConnected();
	}

	@Override
	public String[] getAvailableNodes() {
		return RosXMPPConnectionManager.getInstance().getAvailableNodes();
	}
}
