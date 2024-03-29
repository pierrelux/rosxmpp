package org.rosxmpp.connection.server;

import java.util.List;
import java.util.Map;
import java.util.Vector;

public interface Slave {

  /**
   * Retrieve transport/topic statistics.
   * 
   * @param callerId
   *          ROS caller ID.
   * @return stats in the form of <br>
   *         [publishStats, subscribeStats, serviceStats]
   *         <p>
   *         where <br>
   *         publishStats: [[topicName, messageDataSent, pubConnectionData]...]
   *         <br>
   *         subscribeStats: [[topicName, subConnectionData]...] <br>
   *         serviceStats: (proposed) [numRequests, bytesReceived, bytesSent] <br>
   * 
   *         pubConnectionData: [connectionId, bytesSent, numSent, connected] <br>
   *         subConnectionData: [connectionId, bytesReceived, dropEstimate,
   *         connected] <br>
   *         dropEstimate: -1 if no estimate.
   */
  public List<Object> getBusStats(String callerId);

  /**
   * Retrieve transport/topic connection information.
   * 
   * @param callerId
   *          ROS caller ID.
   * @return busInfo in the form of:<br>
   *         [[connectionId1, destinationId1, direction1, transport1, topic1,
   *         connected1]... ]
   *         <p>
   *         connectionId is defined by the node and is opaque. destinationId is
   *         the XMLRPC URI of the destination.
   *         <p>
   *         direction is one of 'i', 'o', or 'b' (in, out, both).
   *         <p>
   *         transport is the transport type (e.g. 'TCPROS'). topic is the topic
   *         name.
   *         <p>
   *         connected1 indicates connection status. Note that this field is
   *         only provided by slaves written in Python at the moment (cf.
   *         rospy/masterslave.py in _TopicImpl.get_stats_info() vs.
   *         roscpp/publication.cpp in Publication::getInfo()).
   */
  public List<Object> getBusInfo(String callerId);

  public List<Object> getMasterUri(String callerId);

  public List<Object> shutdown(String callerId, String message);

  public List<Object> getPid(String callerId);

  public List<Object> getSubscriptions(String callerId);

  /**
   * Retrieve a list of topics that this node publishes.
   * 
   * @param callerId
   *          ROS caller ID.
   * @return topicList is a list of topics published by this node and is of the
   *         form [ [topic1, topicType1]...[topicN, topicTypeN]]]
   */
  public List<Object> getPublications(String callerId);

  /**
   * Callback from master with updated value of subscribed parameter.
   * 
   * @param callerId
   *          ROS caller ID.
   * @param parameterKey
   *          Parameter name, globally resolved.
   * @param parameterValue
   *          New parameter value.
   * @return ignore
   */
  public List<Object> paramUpdate(String callerId, String key, boolean value);

  public List<Object> paramUpdate(String callerId, String key, char value);

  public List<Object> paramUpdate(String callerId, String key, byte value);

  public List<Object> paramUpdate(String callerId, String key, short value);

  public List<Object> paramUpdate(String callerId, String key, int value);

  public List<Object> paramUpdate(String callerId, String key, double value);

  public List<Object> paramUpdate(String callerId, String key, String value);

  public List<Object> paramUpdate(String callerId, String key, List<?> value);

  public List<Object> paramUpdate(String callerId, String key, Vector<?> value);

  public List<Object> paramUpdate(String callerId, String key, Map<?, ?> value);

  public List<Object> publisherUpdate(String callerId, String topic, Object[] publishers);

  /**
   * Publisher node API method called by a subscriber node. This requests that
   * source allocate a channel for communication. Subscriber provides a list of
   * desired protocols for communication. Publisher returns the selected
   * protocol along with any additional params required for establishing
   * connection. For example, for a TCP/IP-based connection, the source node may
   * return a port number of TCP/IP server.
   * 
   * @param callerId
   *          ROS caller ID
   * @param topic
   *          topic name
   * @param protocols
   *          list of desired protocols for communication in order of preference
   * @return protocolParams or empty list if there are no compatible protocols
   */
  public Object[] requestTopic(String callerId, String topic, Object[] protocols);

}