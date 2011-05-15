package com.wherehoo;

import java.sql.*;
import java.util.*;
import java.security.*;

/**
 * <tt>WHDatabaseCheck</tt> supplies methods to verify incoming client data against data already stored in Wherehoo.
 * Specifically, it provides methods for testing usernames, protocols, uids of data,  
 * and a signatures of data to be inserted.
 */

public abstract class WHDatabaseCheck {

    private static synchronized boolean checkTable(String tablename, String fieldname, String fieldvalue) throws SQLException{
	String tn="";
	String fn="";
	ResultSet rs=null;
	boolean result=false;
	String queryString;
       
	Connection C = DriverManager.getConnection("jdbc:"+WHServer.DB,"postgres","");
	Statement stmt = C.createStatement();
	// construct and run a SELECT query
	queryString  = "select * from "+tablename+" ";
	queryString += "where "+fieldname+"='";
	queryString += fieldvalue;
	queryString += "' ";
	rs = stmt.executeQuery(queryString);
	result = rs.next();
	C.close();
	return (result);
    }
    
    
    private static synchronized String fetchFromTable(String tablename, String keyfieldname, String keyfieldvalue, String targetfieldname) throws SQLException{
	ResultSet rs=null;
	String result="";
	String queryString;
	Connection C;
	
	C = DriverManager.getConnection("jdbc:"+WHServer.DB,"postgres","");
	Statement stmt = C.createStatement();
	queryString  = "select "+targetfieldname+" from "+tablename+" ";
	queryString += "where "+keyfieldname+"='";
	queryString += keyfieldvalue;
	queryString += "' ";
	rs = stmt.executeQuery(queryString);
	//System.out.println("fetchFromTable query string: "+queryString); 
	if (rs.next()) { 
	    result = rs.getString(targetfieldname); 
	}
	C.close();
	return (result) ;
    }

    /**
     * Verifies that user with <tt>idt</tt> username is registered.
     * @param idt username to be checked
     * @return boolean value indicating whether <tt>idt</tt> is a registered username.
     */
    protected static synchronized boolean checkUser(String idt){
	try {
	    return checkTable("users","userid",idt);
	}
	catch (SQLException sqle){
	    return false;
	}
    }

    /**
     * Verifies that <tt>pro</tt> is a supported by Wherehoo.
     * @param pro protocol name to be verified
     * @return boolean value indicating whether <tt>pro</tt> is a supported protocol.
     */
    protected static synchronized boolean checkProtocol(String pro){
	try{
	    return checkTable("protocol","protocol",pro);
	}
	catch (SQLException sqle){
	    return false;
	}
    }
    /**
     * Verifies that <tt>datSHA</tt> signature sent by the client is the same 
     * as the signature of <tt>data</tt> calculated using secret fetched from the table in Wherehoo.
     * @param data data that was signed and which signature needs to be verified
     * @param datSHA a signature of the data. <tt>idt</tt>'s password is necessary to calculate this value.   
     * @param idt client's username
     * @return boolean value indicating whether <tt>datSHA</tt> is a valid signature
     */
    protected static synchronized boolean checkSignature(byte[] data, byte[] datSHA, String idt){  
	try{
	    byte[] databaseSHA;
	    databaseSHA = WHDatabaseCheck.SHAhash(data,fetchFromTable("users","userid",idt,"secret"));
	    return (java.util.Arrays.equals(databaseSHA,datSHA));
	}
	catch (SQLException sqle){
	    System.out.println("SQLException : "+sqle.getMessage());
	    return false;
	}
    }
    /**
     * Checks whether there exists Wherehoo entry that has unique ID of <tt>uid</tt>.
     * @param uid unique ID of some Wherehoo entry.
     * @return boolean value indicating whether there exists Wherehoo entry that has <tt>uid</tt> parameter value.
     */
    protected static synchronized boolean checkUID(String uid){
	try {
	    return checkTable("wherehoo_polygons","uniqueidsha",uid);
	}
	catch (SQLException sqle){
	    return false;
	}
    }

    private static byte[] SHAhash(byte[] data, String mysecret) {
	byte[] mdfinal;
	try {
	    MessageDigest md = MessageDigest.getInstance("SHA-1");
	    md.update(data); 				// the data  and...
	    md.update(mysecret.getBytes()); // my secret
	    mdfinal = md.digest(); 			// yield the secure hash
	}
	catch (NoSuchAlgorithmException nsae) { return null; }	
	return mdfinal;
    }
}



