package org.rosxmpp.cli;

public interface CommandHandler {
    public String getCommandName();
    public void handleCommand(String args[]);
    public String getUsage();
}
