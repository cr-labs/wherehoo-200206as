package com.wherehoo;

import java.net.*;
import java.io.*;
import java.util.*;
import java.sql.*;
 
/**
 * WHOperation supplies methods for deleting and entry from Wherehoo database.
 */

public class WHDeleteOperation extends WHOperation {
    
    private String uid;
   
    /**
     * Initializes new <tt>WHDeleteOperation</tt>
     *@param _uid unique ID of wherehoo entry that will be removed once <tt>executeAndOutputToClient</tt> is called.
     */
    public WHDeleteOperation(String _uid){
	uid=_uid;
	
    }
   
    /**
     * Attempts to remove the wherehoo entry that has <tt>uid</tt> with which this <tt>WHDeleteOperation</tt> 
     * was initialized. If the attempt to remove was successful, <tt>executeAndOutputToClient</tt> sends a confirmation 
     * to the client via <tt>client_socket</tt>. In case failure, NAK is sent. 
     * @param client_socket the socket of connecting client.
     */
    public synchronized void executeAndOutputToClient(Socket client_socket) throws IOException {
	
	Connection C;
	Statement s;
	PrintWriter out;
	String queryString;
	String result="NAK";

	out = new PrintWriter(client_socket.getOutputStream(),true);
	try {
	    C = DriverManager.getConnection("jdbc:"+WHServer.DB,"postgres","");
	    C.setAutoCommit(false);
	    
	    // delete an existing record given a valid uniqueidSHA and companion idt (its creator)
	    s = C.createStatement();
	    queryString  = "delete from wherehoo_polygons where ";
	    queryString += "uniqueidSHA ='"+uid+"'";
	    // System.out.println(queryString);
	    if (s.executeUpdate(queryString) == 1)
		result="ACK";
	    C.commit();
	}
	catch (SQLException sqle) {
	    System.out.println("SQLException: " + sqle.getMessage());
	    System.out.println("SQLState:     " + sqle.getSQLState());
	    System.out.println("VendorError:  " + sqle.getErrorCode());
	}
	out.println(result);
	out.close();
    }
    /**
     * Returns the <tt>String</tt> representation of <tt>WHDeleteOperation</tt>
     */
    public String toString(){
	String s="wherehoo.WHCountOperation\n";
	s+="UID: "+uid;
	return s;
    }   
}

