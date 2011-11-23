package org.rosxmpp.connection.server;

public interface RosXMPPBridgeConnection {
	/**
	 * Connect a ros bridge instance to a XMPP server.
	 * @param server
	 * @param user 
	 * @param pass 
	 * @param resource
	 * @return -1 on error
	 */
	public int connect(String server, String user, String pass, String resource);

	/**
	 * Disconnect a ros bridge instance from a XMPP server.
	 * @return -1 on error
	 */
	public int disconnect();

	/**
	 * Tell if the ros bridge instance is connected to a XMPP server
	 * @return true if connected, false otherwise
	 */
	public boolean isConnected();
	
	/**
	 * Expose a local ROS master node over Jabber-RPC
	 * @param uri The XML-RPC URI to reach the ros master.
	 * @return -1 on error
	 */
	public int exposeRosMaster(String uri);

	/**
	 * Get the list of all the ROS nodes exposing their interface over XMPP.
	 * These nodes are part of a XMPP "roster". 
	 * @return [string] A list of jid strings.
	 */
	public Object[] getAvailableNodes();

	  /**
	   * Get list of topics that can be subscribed to. This does not return topics
	   * that have no publishers. See getSystemState() to get more comprehensive
	   * list.
	   * 
	   * @param callerId
	   *          ROS caller ID
	   * @param subgraph
	   *          Restrict topic names to match within the specified subgraph.
	   *          Subgraph namespace is resolved relative to the caller's namespace.
	   *          Use empty string to specify all names.
	   * @return
	   */
	  public Object[] getPublishedTopics(String callerId, String subgraph);
}
