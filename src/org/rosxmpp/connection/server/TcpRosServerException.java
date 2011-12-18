package org.rosxmpp.connection.server;

import java.io.IOException;

public class TcpRosServerException extends IOException {
    public TcpRosServerException(String message) {
	super(message);
    }

    private static final long serialVersionUID = -1157172208631788483L;
}
