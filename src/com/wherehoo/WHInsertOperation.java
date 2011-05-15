package com.wherehoo;

import java.net.*;
import java.io.*;
import java.util.*;
import java.sql.*;
import java.security.*;


public class WHInsertOperation extends WHOperation {
    private WHPolygon poly;
    private double height;
    private byte[] data;
    private WHTimeInterval time;
    private String mimetype;
    private String protocol;
    private String uniqueidSHA;
    private String idt;
    private String meta;
    private boolean meta_is_set; 
   
    /** 
     * Constructs new WHInsertOperation
     * @param _idt user id of the client who wishes to insert an entry into wherehoo  
     * @param _poly WHPolygon representation of the geographical area of wherehoo object
     * @param _height the height of locale of the wherehoo object
     * @param _data the actual data to be inserted
     * @param _time the time interval when this wherehoo object is active
     * @param _mimetype mimetype of the data to be inserted
     * @param _protocol protocol of the data to be inserted
     */

    public WHInsertOperation(String _idt,WHPolygon _poly,double _height,byte[] _data, WHTimeInterval _time, String _mimetype, String _protocol){
	poly=_poly;
	height=_height;
	data=_data;
	time=_time;
	mimetype=_mimetype;
	protocol=_protocol;
	idt=_idt;
	meta="";
	meta_is_set=false;
	
    }
    /**
     * sets meta field to _meta
     * @param _meta the metadata string of the wherehoo object
     */
    public void setMeta(String _meta){
	meta=_meta;
	meta_is_set=true;
	if (this.getVeryVerbose()) System.out.println("Meta is set to :"+ _meta);
    }
    /**
     * Returns an SQL query string.
     * @return SQL query string of this WHInsertOperation
     */ 
    public String getQueryString(){
	return this.getQueryString(0);
    }
    /**
     * Returns an SQL query string, for the case when there is a zero crossing.
     * @param type the integer representing the case
     * @return SQL query string of this WHInsertOperation
     */ 
    public String getQueryString(int type){
	   
	String queryString;
	queryString =  "insert into wherehoo_polygons (";
	queryString += "area,height,begin_time,end_time,";
	queryString += "authority,data,mimetype,protocol,uniqueidsha";
	if (meta_is_set)
	    queryString+=",meta";
	queryString += ") values (polygon(pclose(path'";
	switch (type){
	    //180<lon<540
	case -1: {
	    queryString +=poly.toString(360);
	    break;
	}
	//no zero crossing case, 0<lon<360
	case 0: {
 
	    queryString += poly.toString();
	    break;
	}
	//-180<lon<180
	case 1: {
	    queryString += poly.toString(0);
	    break;
	}
	}
	queryString +="')),"+height+",?,?,'"+idt+"',?,'"+mimetype+"','"+protocol+"','"+uniqueidSHA+"'";
	if (meta_is_set)
	    queryString+=",?";	queryString += ")";
	if (this.getVerbose()||this.getVeryVerbose()) System.out.println(queryString);
	return queryString;
    }
    /**
     * Inserts an entry into the database, and sends an uid to the client as a confirmation
     * @param client_socket a socket of the connecting client.  After executing an insert operation, this method sends
     * uid to the client via client_socket.
     */
    public synchronized void executeAndOutputToClient(Socket client_socket) throws IOException{
	
	
	String queryString;
 
	Connection C;
	PreparedStatement pps;
	
	String client_address = client_socket.getInetAddress().getHostAddress();
	int st;
	PrintWriter out;
	
	if (this.getVeryVerbose()) System.out.println("Opening PrintWriter");
	out = new PrintWriter(client_socket.getOutputStream(),true);
	try {
	    uniqueidSHA=getUniqueID(client_address);
	    if (this.getVeryVerbose()) System.out.println("Calculated uniqueID :"+uniqueidSHA);
	    C = DriverManager.getConnection("jdbc:"+WHServer.DB,"postgres","");
	    if (this.getVeryVerbose()) System.out.println("Connected to database");
	    C.setAutoCommit(false);
	
	    if ( ! poly.zeroCrossing()){	
		if (this.getVeryVerbose()) System.out.println("No zero crossing");
		queryString=this.getQueryString(0);
		pps = C.prepareStatement(queryString);
		if (this.getVeryVerbose()) System.out.println("Prepared statement");
		pps.setTimestamp(1,time.getBegin());
		pps.setTimestamp(2,time.getEnd());
		if (this.getVeryVerbose()) System.out.println("Set the timestamps in SQL query string");
		pps.setBytes(3, data);
		if (this.getVeryVerbose()) System.out.println("Set data bytes in SQL query string");
		if (meta_is_set){
		    pps.setString(4,meta);
		    if (this.getVeryVerbose()) System.out.println("Set meta in SQL query string");
		}
		st = pps.executeUpdate();
		if (this.getVeryVerbose()) System.out.println("Executed query, result : "+st);
		C.commit();
		if (this.getVeryVerbose()) System.out.println("Commited changes");
		if (st == 1){
		    out.println(uniqueidSHA);
		    if (this.getVeryVerbose())System.out.println("Sent UID to client");
		}
	    } else {
		if (this.getVeryVerbose()) System.out.println("Zero crossing");
		//insert two objects, each with different coordinates
		
		//object represented by polygon with 180<lon<540;
		queryString=this.getQueryString(-1);
		if (this.getVeryVerbose()) System.out.println("Prepared statement centered around zero meridian");
		pps = C.prepareStatement(queryString);
		pps.setTimestamp(1,time.getBegin());
		pps.setTimestamp(2,time.getEnd());
		pps.setBytes(3, data);
		if (meta_is_set)
		    pps.setString(4,meta);
		
		st = pps.executeUpdate();
		if (this.getVeryVerbose()) System.out.println("Executed statement");
		//object represented by polygon with -180<lon<180
		queryString=this.getQueryString(1);
		pps = C.prepareStatement(queryString);
		if (this.getVeryVerbose()) System.out.println("Prepared statement centered around 360 meridian");
		pps.setTimestamp(1,time.getBegin());
		pps.setTimestamp(2,time.getEnd());
		pps.setBytes(3, data);
		if (meta_is_set)
		    pps.setString(4,meta);
		
		st = st + pps.executeUpdate();
		if (this.getVeryVerbose()) System.out.println("Executed statement");
		C.commit();
		
		if (st == 2)
		    out.println(uniqueidSHA);
		if (this.getVeryVerbose()) System.out.println("Insert fully sucessfull, sent uniqueidSHA to client");
	    }
	    C.close();
	}
	catch (SQLException sqle) {
	    System.out.println("SQLException: " + sqle.getMessage());
	    System.out.println("SQLState:     " + sqle.getSQLState());
	    System.out.println("VendorError:  " + sqle.getErrorCode());
	}
	catch (NoSuchAlgorithmException nsae){
	    System.out.println("NoSuchAlgorithmException: "+nsae.getMessage());
	}
	catch (UnknownHostException uhe){
	    System.out.println("UnknownHostException: "+uhe.getMessage());
	}
	//out.close();
    }
    
    private String getUniqueID(String client_address) throws NoSuchAlgorithmException , UnknownHostException{
	
	String server_address=InetAddress.getLocalHost().getHostAddress();
	
	MessageDigest md = MessageDigest.getInstance("SHA-1");
	// the server where the record was created
	md.update(server_address.getBytes());
	// and the client that made the record
	md.update(client_address.getBytes()); 
	// and the data that's recorded in it
	md.update(data); 
	// and a random number
	md.update(Double.toString(Math.random()).getBytes());
	// and a timestamp in msec			
	md.update(Long.toString(new java.util.Date().getTime()).getBytes());	
	byte[] mdfinal = md.digest();
	
	String idSHA = new String();
	for (int i = 0; i < mdfinal.length; i++) {
	    int z;
	    z = ((int) mdfinal[i]) & (0x000000FF);      
	    // need an INT because Byte does not have "toHexString" method
	    // but AND out the leading bytes so it does not go negative
	    idSHA += (z < 16) ? "0" : "";    // pad leading zero if needed (conversion to hex strips it)
	    idSHA += Integer.toHexString(z); // and convert the value to hex string
	}
	return idSHA;
    }
    /**
     * Returns the String representation of this InsertOperation
     * @return a String object representation of this InsertOperation.  It comprises information on area of the 
     * wherehoo entry to be inserted, its height, begin time, end time, mimetype, protocol, idt and metadata.
     */
    public String toString(){
	String s="Poly: "+poly.toString()+"\n";
	s+="Height "+height+"\n";
	s+="Begin "+time.getBegin().toString()+"\n";
	s+="End "+time.getEnd().toString()+"\n";
	s+="Mimetype "+mimetype+"\n";
	s+="Protocol "+protocol+"\n";
	s+="IDT "+idt+"\n";
	if (meta_is_set)
	    s+="Meta "+meta;
	else
	    s+="Meta not set";
	return s;
    }
}







