package org.rosxmpp.transport;
import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.JingleSession;
import org.jivesoftware.smackx.jingle.JingleSessionRequest;
import org.jivesoftware.smackx.jingle.listeners.JingleSessionRequestListener;
import org.jivesoftware.smackx.jingle.media.JingleMediaManager;
import org.jivesoftware.smackx.jingle.nat.BasicTransportManager;

public class JingleCommunication {
    private Connection xmppConnection = null;
    private JingleManager jm = null;
    private JingleSession incoming = null;
    private JingleSession outgoing = null;

    public JingleCommunication(String server, String user, String pass, String ressource) {
	JingleManager.setJingleServiceEnabled();

	connect(server, user, pass, ressource);
	initializeJingle();
    }

    /**
     * Login to the XMPP server
     * @param server
     * @param user
     * @param pass
     */
    private void connect(String server, String user, String pass, String ressource)
    {
	xmppConnection = new XMPPConnection(server);
	try {
	    xmppConnection.connect();
	    xmppConnection.login(user, pass, ressource);
	}
	catch (XMPPException e) {
	    e.printStackTrace();
	}

	System.out.println("Connected");
    }

    private void initializeJingle()
    {
	BasicTransportManager transportManager = new BasicTransportManager();
	List<JingleMediaManager> mediaManagers = new ArrayList<JingleMediaManager>();
	mediaManagers.add(new UDTMediaManager(transportManager));

	jm = new JingleManager(xmppConnection, mediaManagers);
	//jm.addCreationListener(transportManager);

	jm.addJingleSessionRequestListener(new JingleSessionRequestListener() {
	    public void sessionRequested(JingleSessionRequest request) {
		try {
		    // Accept the call
		    incoming = request.accept();

		    System.out.println("Channel establishment accepted");

		    // Start the call
		    incoming.startIncoming();
		}
		catch (XMPPException e) {
		    e.printStackTrace();
		}
	    }
	});
    }

    public void startOutgoingChannel(String node)
    {
	try {
	    outgoing = jm.createOutgoingJingleSession(node);
	} catch (XMPPException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	outgoing.startOutgoing();	
    }

    public static void main(String args[]) {
	Connection.DEBUG_ENABLED = true;

	JingleCommunication jingle1 = new JingleCommunication("merlin", "pierre-luc", "test", "ros/topic1");
	new JingleCommunication("merlin", "rodney", "brooks", "ros/topic2");
	jingle1.startOutgoingChannel("rodney@merlin/ros/topic2");

	while(true) {

	}
    }
}
