package org.rosxmpp.connection.server;

public class RosXMPPBridgeConnectionImpl implements RosXMPPBridgeConnection {
	@Override
	public int connect(String server, String user, String pass, String ressource) {
        return RosXMPPBridgeConnectionManager.getInstance().connect(server, user, pass, ressource);
	}

	@Override
	public int disconnect() {
		return RosXMPPBridgeConnectionManager.getInstance().disconnect();
	}

	@Override
	public boolean isConnected() {
		return RosXMPPBridgeConnectionManager.getInstance().isConnected();
	}

	@Override
	public Object[] getAvailableNodes() {
		return RosXMPPBridgeConnectionManager.getInstance().getAvailableNodes();
	}

	@Override
	public Object[] getPublishedTopics(String remoteNode) {
		return RosXMPPBridgeConnectionManager.getInstance().getPublishedTopics(remoteNode);
	}

	@Override
	public int exposeRosMaster(String uri) {
		return RosXMPPBridgeConnectionManager.getInstance().exposeRosMaster(uri);
	}

	@Override
	public int proxyRemoteTopics(String remoteNode) {
		return RosXMPPBridgeConnectionManager.getInstance().proxyRemoteTopics(remoteNode);		
	}
}
