package com.wherehoo;

import java.net.*;
import java.io.*;
import java.util.*;
import java.sql.*;
import java.awt.geom.Point2D;

/**
 * <tt>WHSearchOperation</tt> supplies methods for querying Wherehoo database for entries that satisfy set criteria.
 */


public class WHSearchOperation extends WHOperation {
    private WHSearchArea search_area;
    private WHPolygon search_poly;
    private double height;
    private byte[] data;
    private Timestamp begin;
    private boolean begin_is_set;
    private Timestamp end;
    private boolean end_is_set;
    private String mimetype;
    private boolean mimetype_is_set;
    private String protocol;
    private boolean protocol_is_set;
    private String meta;
    private boolean meta_is_set;
    private String metastatus;
    private int limit;
    private boolean limit_is_set;
    private Point2D.Double client_location;
   
    /**
     * Constructs new SEARCH operation.
     * @param _search_area representation of the search area
     *
     */
    public WHSearchOperation(WHSearchArea _search_area){
	search_area = _search_area;
	search_poly = _search_area.getPoly();
	height = _search_area.getHeight();
	client_location = _search_area.getClientLocation();
	meta_is_set=false;
	limit_is_set=false;
	begin_is_set=false;
	end_is_set=false;
	mimetype_is_set=false;
	protocol_is_set=false;

	mimetype="";
	protocol="";
	meta="";

    }
    /**
     * Sets the matadata search criterion to <tt>_meta</tt>.
     * @param _meta the objects returned by this search should contain <tt>_meta</tt> in their metadata field.
     */ 
    public void setMeta(String _meta){
	if (this.getVeryVerbose()) System.out.println("Meta set to: "+_meta);
	meta=_meta;
	meta_is_set=true;
    }
    /**
     * Sets the limit of the number of wherehoo entries returned.
     * @param _limit the maximum of wherehoo entries to be returned
     */
    public void setLimit(int _limit){
	if (this.getVeryVerbose()) System.out.println("Limit set to: "+_limit);
	limit = _limit;
	limit_is_set=true;
    }
    /**
     * Sets the start of the time interval to be searched.
     * @param _begin the start of the time interval to be searched.
     */
    public void setBegin(Timestamp _begin){
	if (this.getVeryVerbose()) System.out.println("Begin set");
	begin = _begin;
	begin_is_set=true;
    }
    /** 
     *Sets the end of the time interval to be searched.
     * @param _end the end of the time interval to be searched.
     */
    public void setEnd(Timestamp _end){
	if (this.getVeryVerbose()) System.out.println("End set");
	end=_end;
	end_is_set=true;
    }
    /**
     * Sets the mimetype criterion for the search.  All results of the search will be of this <tt>_mimetype</tt>.
     * @param _mimetype the mimetype criterion for the search
     */
    public void setMimetype(String _mimetype){
	if (this.getVeryVerbose()) System.out.println("Mimetype set to :"+_mimetype);
	mimetype=_mimetype;
	mimetype_is_set=true;
    }
    /**
     * Sets the protocol criterion for the search. The results of the search will all be of this <tt>_protocol</tt>.
     * @param _protocol the protocol criterion for the search
     */
    public void setProtocol(String _protocol){
	if (this.getVeryVerbose()) System.out.println("Protocol set to: "+_protocol);
	protocol=_protocol;
	protocol_is_set=true;
    }
   
  /**
     * Returns an SQL query string.
     * @return SQL query string of this <tt>WHSearchOperation</tt>
     */ 
    public String getQueryString(){
	String queryString;
	//wherehoo=# select poly from mypolytable where '((0,0),(4,4))' ?# poly or '((0,0),(4,4))' ~ poly;
	//must check for zero-crossing, in which case we will have two adjacent search polies
	if (this.getVeryVerbose()) System.out.println("Composing SQL query string");
	queryString  = "select data,meta,mimetype,protocol,area,height,end_time,uniqueidsha ";
	queryString += "from wherehoo_polygons where "; 

	if (! search_poly.zeroCrossing()){
	    if (this.getVeryVerbose()) System.out.println("No zero crossing");
	    //it either intersects
	    queryString += "poly_overlap(polygon(pclose(path'"+search_poly.toString()+"')),area) ";
	   
	}	
	else {
	    if (this.getVeryVerbose()) System.out.println("Zero crossing");
	    WHPolygon[] search_polies = search_poly.splitAlongGreatMeridian();
	    queryString += "(poly_overlap(polygon(pclose(path'"+search_polies[0].toString()+"')),area) ";
	    queryString += " OR poly_overlap(polygon(pclose(path'"+search_polies[1].toString()+"')),area)) ";
	}
	queryString +=  (begin_is_set) ? " AND begin_time >= '"+begin+"' " 
	    : "AND begin_time <= now() ";
	queryString += (end_is_set) ? "AND end_time <= '"+end+"' " 
	    : "AND end_time >= now() ";	
	queryString += (meta_is_set) ? "AND meta like '%"+meta+"%' " : "";
	queryString += (protocol_is_set) ? "AND protocol like '"+protocol+"' " : "";
	queryString += (mimetype_is_set) ? "AND mimetype like '"+mimetype+"' " : "";
	queryString += (limit==0) ? " " : "limit "+limit+" ";
	
	if (this.getVerbose()||this.getVeryVerbose()) System.out.println("The query:");
	if (this.getVerbose()||this.getVeryVerbose()) System.out.println(queryString);
	return queryString;
    }
    
     /**
     * Searches the database for entries that are within set search area, in the set time interval, 
     * and satisfying other optional criteria, such as mimetype, protocol or metadata. <tt>executeAndOutputToClient</tt> 
     * then sends the results to the client via <tt>client_socket</tt>.
     * @param client_socket a socket of the connecting client.  After executing a search operation, this method sends
     * the results to the client via <tt>client_socket</tt>
     */ 
    public synchronized void executeAndOutputToClient(Socket client_socket) throws IOException {
	
	ResultSet rs=null;
	Connection C;
	Statement s;
	String queryString;
	
	if (this.getVerbose()||this.getVeryVerbose()) System.out.println("Executing and outputing to client");
	try{
	    if (this.getVeryVerbose()) System.out.println("Connecting to database");
	    C = DriverManager.getConnection("jdbc:"+WHServer.DB,"postgres","");
	    C.setAutoCommit(false);
	    queryString=this.getQueryString();
	    s = C.createStatement();
	    if (this.getVeryVerbose()) System.out.println("About to execute query");
	    rs = s.executeQuery(queryString);
	    if (this.getVeryVerbose()) System.out.println("Commiting the query");
	    C.commit();
	    if (this.getVeryVerbose()) System.out.println("About to output to client");
	    this.outputToClient(rs,client_socket);
	    C.close();
	}
	catch (SQLException sqle) {
	    System.out.println("SQLException: " + sqle.getMessage());
	    System.out.println("SQLState:     " + sqle.getSQLState());
	    System.out.println("VendorError:  " + sqle.getErrorCode());
	} 
    }
	
    
    private synchronized void outputToClient(ResultSet rs, Socket client_socket) throws IOException {
	WHPolygon poly;
	String mimetype;
	String protocol;
	String meta;
	String meta_status;
	byte[] data;
	
	String header;
	
	String client_data_command;
	
	PrintWriter out;
	BufferedOutputStream outstream;
	ByteArrayOutputStream outbytes;
	BufferedReader in;
	
	java.util.Date endtime;
	
	//set up the communication channels with client;
	if (this.getVeryVerbose()) 
	    System.out.println("Opening the PrintWriter");
	out = new PrintWriter(client_socket.getOutputStream(),true);
	if (this.getVeryVerbose()) 
	    System.out.println("Opening the BufferedOutputStream");
	outstream = new BufferedOutputStream(client_socket.getOutputStream());
	if (this.getVeryVerbose()) 
	    System.out.println("Opening the ByteArrayOutputStream");
	outbytes = new ByteArrayOutputStream();
	if (this.getVeryVerbose()) 
	    System.out.println("Opening the BufferedReader");
	in = new BufferedReader(new InputStreamReader(client_socket.getInputStream()));
	try {
	    if (this.getVeryVerbose()) 
		System.out.println("Outputing the results to the client");
	    client_data_command = new String("next");
	    while (rs.next() 
		   && ( client_data_command.equals("next")
			|| client_data_command.equals("skip")))
		{
		    if (this.getVeryVerbose()) 
			System.out.println("Next row");
		    poly = new WHPolygon(rs.getString("area"));
		    mimetype = rs.getString("mimetype");
		    protocol = rs.getString("protocol");
		    meta = rs.getString("meta");
		    data = rs.getBytes("data");
		    if (meta.equals("") || (meta==null)) {
			meta_status = "NONE"; meta="";
		    }
		    else {
			meta_status = "META";
		    }
		    //this is a big, big problem. Does it always work?
		    endtime = rs.getDate("end_time");
		    header = this.recordHeader(poly,data.length,mimetype,protocol,meta_status,endtime);
		    out.println(header);
		    if (this.getVeryVerbose()) System.out.println("sent the header to client:");
		    if (this.getVerbose()||this.getVeryVerbose()) System.out.println(header);
		    do {   
			client_data_command = in.readLine().trim().toLowerCase();
			if (this.getVeryVerbose()) 
			    System.out.println("Client sent: "+client_data_command);
			if (client_data_command.equals("meta")) 
			    out.println(meta); 
			if (client_data_command.equals("data")) {
			    outbytes.write(data);
			    outbytes.writeTo(outstream);
			    outstream.flush();
			    outbytes.reset();
			}
		    } while (client_data_command.equals("meta")|| client_data_command.equals("data"));
		}
	} catch(SQLException sqle) {
	    System.out.println(sqle.toString()); 
	}
    }
    

    private String recordHeader(WHPolygon point_of_interest,int dlength,String _mimetype,
				String _protocol,String _metastatus,java.util.Date _endtime){
	//Record descriptive data appears on one line, space-delimited, in this format:
	//bearing compassdirection distance ttl bytes protocol mimetype meta
	
	java.util.Date nowtime = new java.util.Date();
	long ttl = Math.abs(_endtime.getTime() - nowtime.getTime()) / 1000L;
	ttl = Math.min(ttl, WHServer.MAXTTL);
	double r_heading=search_area.heading(point_of_interest);
	double r_distance=search_area.distance(point_of_interest);
	double bearing=search_area.bearing(point_of_interest);
	String r_quadrant = WHGeo.quadrant(r_heading);
	String result = new String();	
	result += Math.round(bearing)
	    +" "+r_quadrant
	    +" "+Math.round(r_distance)
	    +" "+ttl
	    +" "+dlength
	    +" "+_protocol
	    +" "+_mimetype
	    +" "+_metastatus;
	return result;
    }
    
    /**
     * Returns the <tt>String</tt> representation of this <tt>WHSearchOperation</tt>.
     */
    public String toString(){
	String s="Poly: "+search_poly.toString()+"\n";
	s+="Height "+height+"\n";
	if (begin_is_set)
	    s+="Begin "+begin.toString()+"\n";
	else
	    s+="Begin not set\n";
	if(end_is_set)
	    s+="End "+end.toString()+"\n";
	else
	    s+="End not set\n";
	if (mimetype_is_set)
	    s+="Mimetype "+mimetype+"\n";
	else
	    s+="Mimetype not set\n";
	if(protocol_is_set)
	    s+="Protocol "+protocol+"\n";
	else
	    s+="Protocol not set\n";
	if (meta_is_set)
	    s+="Meta "+meta;
	else
	    s+="Meta not set";
	return s;
    } 
}
