package com.wherehoo;

import java.io.IOException;
import java.net.Socket;

/**
 * All operations supported by Wherehoo extend <tt>WHOperation</tt> . which does not know which operation exactly the connecting client wishes to perform and what kind of output it expects.  This exploits the fact that all wherehoo operations, after initial data collection stage, perform some kind of SQL query, which results are output to client.
 */
public abstract class WHOperation {

    private boolean verbose;
    private boolean very_verbose;
    
    WHOperation(){
	verbose = false;
	very_verbose = false;
    }
    /**
     * Performs some SQL query and outputs the results of it to connecting client via client_socket.
     * @param client_socket the connecting client socket
     */
    abstract void executeAndOutputToClient(Socket client_socket) throws IOException;
    /**
     *turns the verbosity on and off.  The output is to System.out.
     */
    protected void setVerbose(boolean verbosity){
	verbose = verbosity;
    }
    protected void setVeryVerbose(boolean verbosity){
	very_verbose = verbosity;
    }
    protected boolean getVerbose(){
	return verbose;
    }
    protected boolean getVeryVerbose(){
	return very_verbose;
    }
}
