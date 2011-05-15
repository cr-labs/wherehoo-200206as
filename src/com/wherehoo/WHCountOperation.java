package com.wherehoo;

import java.net.*;
import java.io.*;
import java.util.*;
import java.sql.*;
import java.awt.geom.Point2D;

/**
 * <tt>WHCountOperation</tt> supplies methods for counting Wherehoo entries that satisfy set criteria.
 */

public class WHCountOperation extends WHOperation {
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
     * Constructs new COUNT operation.
     * @param _search_area representation of the search area
     */
    public WHCountOperation(WHSearchArea _search_area){
	
	search_area=_search_area;
	search_poly=_search_area.getPoly();
	height=_search_area.getHeight();
	meta_is_set=false;
	limit_is_set=false;
	begin_is_set=false;
	end_is_set=false;
	mimetype_is_set=false;
	protocol_is_set=false;

	client_location=_search_area.getClientLocation();

	mimetype="";
	protocol="";
	meta="";
	
    }
     /**
     * Sets the matadata count criterion to <tt>_meta</tt>.
     * @param _meta the objects returned by this search should contain <tt>_meta</tt> in their metadata field.
     */ 
    public void setMeta(String _meta){
	meta=_meta;
	meta_is_set=true;
    }
    /**
     * Sets the limit of the number of wherehoo entries returned.
     * @param _limit the maximum of wherehoo entries to be returned
     */
    public void setLimit(int _limit){
	limit = _limit;
	limit_is_set=true;
    }
    /**
     * Sets the start of the time interval to be searched.
     * @param _begin the start of the time interval to be searched.
     */
    public void setBegin(Timestamp _begin){
	begin = _begin;
	begin_is_set=true;
    }
    /** 
     *Sets the end of the time interval to be searched.
     * @param _end the end of the time interval to be searched.
     */
    public void setEnd(Timestamp _end){
	end=_end;
	end_is_set=true;
    }
    /**
     * Sets the mimetype criterion for the search.  All results of the search will be of this <tt>_mimetype</tt>.
     * @param _mimetype the mimetype criterion for the search
     */
    public void setMimetype(String _mimetype){
	mimetype=_mimetype;
	mimetype_is_set=true;
    }
    /**
     * Sets the protocol criterion for the search. All results of the search will be of this <tt>_protocol</tt>.
     * @param _protocol the protocol criterion for the search
     */
    public void setProtocol(String _protocol){
	protocol=_protocol;
	protocol_is_set=true;
    } 
 
 /**
     * Returns an SQL query string.
     * @return SQL query string of this <tt>WHSearchOperation</tt>
     */ 
    public String getQueryString(){
	String queryString;
	//must check for zero-crossing, in which case we will have two adjacent search polies
	if (this.getVeryVerbose()) System.out.println("Composing SQL query string");
	
	queryString  = "select count(distinct uniqueidsha) ";
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
     *and satisfying other optional criteria, such as mimetype, protocol or metadata. <tt>executeAndOutputToClient</tt> 
     * then returns
     * the count of all found results to the client via <tt>client_socket</tt>
     * @param client_socket a socket of the connecting client.  After executing a search operation, this method sends
     * the results to the client via <tt>client_socket</tt>
     */ 
    public synchronized void executeAndOutputToClient(Socket client_socket) throws IOException{
	
	ResultSet rs=null;
	Connection C;
	Statement s;
	PrintWriter out;
	String queryString;
	int count;
	
	out = new PrintWriter(client_socket.getOutputStream(),true);
	try{
            C = DriverManager.getConnection("jdbc:"+WHServer.DB,"postgres","");
	    C.setAutoCommit(false);
	    
	    queryString=this.getQueryString();
	    
	    s = C.createStatement();
	    rs = s.executeQuery(queryString);
	    C.commit();
	    if(rs.next()){
		count=rs.getInt("count");
	    } else {
		count=0;
	    }
	    out.println(count);
	    //out.close();
	}
	catch (SQLException sqle) {
	    System.out.println("SQLException: " + sqle.getMessage());
	    System.out.println("SQLState:     " + sqle.getSQLState());
	    System.out.println("VendorError:  " + sqle.getErrorCode());
	}
    }
     
    /**
     * Returns the <tt>String</tt> representation of this <tt>WHCountOperation</tt>.
     */
      
    public String toString(){
	String s="wherehoo.WHCountOperation/n"; 
	    s+="Poly: "+search_poly.toString()+"\n";
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
