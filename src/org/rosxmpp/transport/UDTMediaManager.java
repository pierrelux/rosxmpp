package org.rosxmpp.transport;

import org.jivesoftware.smackx.jingle.JingleSession;
import org.jivesoftware.smackx.jingle.media.JingleMediaManager;
import org.jivesoftware.smackx.jingle.media.JingleMediaSession;
import org.jivesoftware.smackx.jingle.media.PayloadType;
import org.jivesoftware.smackx.jingle.nat.JingleTransportManager;
import org.jivesoftware.smackx.jingle.nat.TransportCandidate;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements a JingleMediaManager for ScreenSharing.
 * It currently uses an Audio payload Type. Which needs to be fixed in the next version.
 *
 * @author Thiago Camargo
 */

public class UDTMediaManager extends JingleMediaManager {

    public static final String MEDIA_NAME = "UDT";

    private List<PayloadType> payloads = new ArrayList<PayloadType>();

    public UDTMediaManager(JingleTransportManager transportManager) {
        super(transportManager);
        setupPayloads();
    }

    /**
     * Setup API supported Payloads
     */
    private void setupPayloads() {
        payloads.add(new PayloadType.Audio(30, "UDT"));
    }

    /**
     * Return all supported Payloads for this Manager.
     *
     * @return The Payload List
     */
    public List<PayloadType> getPayloads() {
        return payloads;
    }

    /**
     * Returns a new JingleMediaSession
     *
     * @param payloadType payloadType
     * @param remote      remote Candidate
     * @param local       local Candidate
     * @return JingleMediaSession JingleMediaSession
     */
    public JingleMediaSession createMediaSession(PayloadType payloadType, final TransportCandidate remote, final TransportCandidate local, final JingleSession jingleSession) {
        UDTMediaSession session = null;
        session = new UDTMediaSession(payloadType, remote, local, "UDT", jingleSession);

        return session;
    }

    public PayloadType getPreferredPayloadType() {
        return super.getPreferredPayloadType();
    }
    
    public  String getName() {
        return MEDIA_NAME;
    }
}