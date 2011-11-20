/**
 * $RCSfile: TestMediaSession.java,v $
 * $Revision: 1.1 $
 * $Date: 08/11/2006
 * <p/>
 * Copyright 2003-2006 Jive Software.
 * <p/>
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import org.jivesoftware.smackx.jingle.JingleSession;
import org.jivesoftware.smackx.jingle.media.JingleMediaSession;
import org.jivesoftware.smackx.jingle.media.PayloadType;
import org.jivesoftware.smackx.jingle.nat.TransportCandidate;

/**
 * This Class implements a complete JingleMediaSession.
 *
 * @author Thiago Camargo
 */
public class UDTMediaSession extends JingleMediaSession {

     /**
     * Creates a TestMediaSession with defined payload type, remote and local candidates
     *
     * @param payloadType Payload of the jmf
     * @param remote      the remote information. The candidate that the jmf will be sent to.
     * @param local       the local information. The candidate that will receive the jmf
     * @param locator     media locator
     */
    public UDTMediaSession(final PayloadType payloadType, final TransportCandidate remote, final TransportCandidate local,
            final String locator, JingleSession jingleSession) {
        super(payloadType, remote, local, "UDT", jingleSession);
        initialize();
    }

    /**
     * Initialize the UDT channels.
     */
    public void initialize() {
        JingleSession session = getJingleSession();
        if ((session != null) && (session.getInitiator().equals(session.getConnection().getUser()))) {
            // If the initiator of the jingle session is us then we transmit a UDT stream.
            try {
                InetAddress remote = InetAddress.getByName(getRemote().getIp());
                               
                DatagramSocket socket = new DatagramSocket(getLocal().getPort());
                
                byte buf[] = new byte[1024];
                buf = (new String("Hello world").getBytes());
                DatagramPacket packet = new DatagramPacket(buf, buf.length, remote, getRemote().getPort());
                try {
                    socket.send(packet);
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
        	System.out.println("Receiving");
            byte buf[] = new byte[1024];
            DatagramPacket p = new DatagramPacket(buf, 1024);
            
            DatagramSocket socket = null;
			try {
				socket = new DatagramSocket(getLocal().getPort());
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            try {
				socket.receive(p);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            
            String received = new String(p.getData(), 0, p.getLength());
            System.out.println("received :" + received);
        }
    }

    /**
     * Starts transmission and for NAT Traversal reasons start receiving also.
     */
    public void startTrasmit() {
        
    }

    /**
     * Set transmit activity. If the active is true, the instance should trasmit.
     * If it is set to false, the instance should pause transmit.
     *
     * @param active active state
     */
    public void setTrasmit(boolean active) {
        
    }

    /**
     * For NAT Reasons this method does nothing. Use startTransmit() to start transmit and receive jmf
     */
    public void startReceive() {
        // Do nothing
    }

    /**
     * Stops transmission and for NAT Traversal reasons stop receiving also.
     */
    public void stopTrasmit() {
       
    }

    /**
     * For NAT Reasons this method does nothing. Use startTransmit() to start transmit and receive jmf
     */
    public void stopReceive() {
       
    }
}