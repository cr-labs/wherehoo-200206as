package com.wherehoo;

import java.io.*;
import java.net.*;
import java.util.*;
import java.security.*;
import java.sql.*;
import java.awt.geom.Point2D;
/**
 * <tt>WHDataCollector</tt> is responsible for gathering data from the client and checking its validity.  
 */

public class WHDataCollector{

    private boolean verbose;
    private boolean very_verbose;
    
    private Socket client_socket;
   
    private String act;
    private String idt;
    private String sha;
    private String uniqueidSHA;
    private String mimetype;
    private String protocol;
    private String shape;
    private String meta;
    
    private Point2D.Double[] coordinates;
    private double height;
    private WHPolygon ll_poly;
    private double width;
    private double length;
    private double heading;
    private double radius;
    private double project_heading;
    private double project_range;
    private int limit;      
    private byte[] data;
    private byte[] dataSHA;
    
    private WHTimeInterval time_interval;
   
    private WHFields errors;
    private WHFields received;
    private PrintWriter out;
    private BufferedReader in;
    
    /**
     * Initializes the new <tt>WHDataCollector</tt> object. It
     */
    
    protected  WHDataCollector(Socket _clientSocket){
	
	client_socket=_clientSocket;

	coordinates=new Point2D.Double[0];	
	time_interval = new WHTimeInterval();

	errors = new WHFields();
	received = new WHFields();
    }

    protected void setVerbose(boolean verbosity){
	verbose=verbosity;
	if (verbose || very_verbose) 
	    System.out.println("Verbosity set to true");
    }
    protected void setVeryVerbose(boolean verbosity){
	very_verbose=verbosity;
	if (verbose||very_verbose)
	    System.out.println("Heavy verbosity set to true");
    }
    protected boolean getVerbose(){
	return verbose;
    }
    protected boolean getVeryVerbose(){
	return very_verbose;
    }
	
    /**
     * Collects data from the client.  <tt>readDataFromClient() listens on the <tt>client_socket</tt> and records
     * the commands and the values as they come in. It does not check these commands nor data for correctness.
     * @return boolean value that indicates whether commands were read succesfully and the client wants to proceed.  
     * It does <i>not</i> indicate whether there were any errors in commands or their parameters. <tt>readDataFromClient()
     * will return <i>false</i> if the client issued BYE command and wishes to disconnect, or some fatal error occured 
     * in data collecting proccess. 
     */ 

    public boolean readDataFromClient(){

	boolean proceed = false;
	try {
	    PrintWriter out = new PrintWriter(client_socket.getOutputStream(),true);
	    BufferedReader in = new BufferedReader(new InputStreamReader(client_socket.getInputStream()));
	    
	    String line;
	    String cmd;
 
	    if (verbose || very_verbose)
		if (very_verbose) System.out.println("READING DATA FROM CLIENT:");
	    do {
		//read one line sent by the client
		line = in.readLine();
		//interpret the command
		cmd= this.interpretLine(line,in,out);
		if (very_verbose) System.out.println("returned command: "+cmd);
	    } while (! cmd.equals(".") && ! cmd.equals("bye"));
	    //return false if the client wishes to disconnect or there was a fatal error
	    if (very_verbose) System.out.println("got out of the loop that read single lines and interpreted them");
	    if (cmd.equals(".")){
		proceed = true;
	    }
	    if (very_verbose) System.out.println("result "+proceed);
	} catch (IOException ioe){}
	return proceed;
    }
    
    /**
     *Creates some <tt>WHOperation</tt>, based on the field settings of this <tt>WHDataCollector</tt>.  
     * @exception WHClientCommandException is thrown if field settings of this <tt>WHDataCollector</tt> are not 
     * consistent with what's needed to instantiate one of <tt>WHOperation</tt> objects.
     */
    public WHOperation createWHOperation() throws WHClientCommandException {
	
	//verify that we have consistent data
	if (very_verbose) 
	    System.out.println("Started to create WHOperation, about to check data");
	this.checkAndProcessData();
	if (verbose || very_verbose) 
	    System.out.println("Data check OK");
	//create an appropriate WHOperation object and return
	if (act.equals("insert")){
	    WHInsertOperation whi= new WHInsertOperation(idt,ll_poly,height,data,time_interval,mimetype,protocol);
	    if (received.get(WHServer.MET))
		whi.setMeta(meta);
	    return whi;
	}
	else {
	    if (act.equals("query")){
		WHSearchArea search_area = new WHSearchArea(ll_poly,coordinates[0],height,heading);
		WHSearchOperation whq= new WHSearchOperation(search_area);
		if (received.get(WHServer.BEG))
		    whq.setBegin(time_interval.getBegin());
		if (received.get(WHServer.END))
		    whq.setEnd(time_interval.getEnd());
		if (received.get(WHServer.LIM))
		    whq.setLimit(limit);
		if (received.get(WHServer.MET))
		    whq.setMeta(meta);
		if (received.get(WHServer.MIM))
		    whq.setMimetype(mimetype);
		if (received.get(WHServer.PRO))
		    whq.setProtocol(protocol);
		return whq;
	    } 
	    else { 
		if (act.equals("count")){
		    WHSearchArea search_area = new WHSearchArea(ll_poly,coordinates[0],height,heading);
		    WHCountOperation whc= new WHCountOperation(search_area);
		    if (received.get(WHServer.BEG))
			whc.setBegin(time_interval.getBegin());
		    if (received.get(WHServer.END))
			whc.setEnd(time_interval.getEnd());
		    if (received.get(WHServer.LIM))
			whc.setLimit(limit);
		    if (received.get(WHServer.MET))
			whc.setMeta(meta);
		    if (received.get(WHServer.MIM))
			whc.setMimetype(mimetype);
		    if (received.get(WHServer.PRO))
			whc.setProtocol(protocol);
		    return whc;
		}    
		else 
		    return new WHDeleteOperation(uniqueidSHA);
	    }
	}
    }
    
    private void checkAndProcessData() throws WHClientCommandException {
	
	String bad_variables=new String();
	
	if (very_verbose){
	    System.out.println("CHECKING THE COLLECTED DATA");
	    System.out.println("ERROR FIELDS NOW");
	    System.out.println(errors.toString());
	    System.out.println("WHDATACOLLECTOR FIELDS:");
	    System.out.println(this.toString());
	}
	
	if ((!received.get(WHServer.ACT))||
	    errors.get(WHServer.ACT)){
	    throw new WHClientCommandException("ACT");
	} else {
	    //
	    //INSERT
	    //
	    if (act.equals("insert")){
		//check idt
		if (! received.get(WHServer.IDT)
		    || (! WHDatabaseCheck.checkUser(idt)))
		    errors.set(WHServer.IDT,true);
		if (errors.get(WHServer.IDT)) bad_variables+="IDT ";
		if (very_verbose) 
		    System.out.println("checked idt");
		//check mimetype here
		if (errors.get(WHServer.MIM)) 
		    bad_variables+="MIM ";
		//check protocol
		if (! WHDatabaseCheck.checkProtocol(protocol))
		    errors.set(WHServer.PRO,true);
		if (errors.get(WHServer.PRO)) 
		    bad_variables+="PRO ";
		if (very_verbose) 
		    System.out.println("checked pro");
		//check pjt here
		if (errors.get(WHServer.PJT)) 
		    bad_variables+="PJT ";
		//check llh
		if (! received.get(WHServer.LLH))
		    errors.set(WHServer.LLH,true);
		if (errors.get(WHServer.LLH)) 
		    bad_variables+="LLH ";
		//check beg and end
		if (! time_interval.checkInsertCompliance()){
		    if (received.get(WHServer.BEG)) 
			errors.set(WHServer.BEG,true);
		    if (received.get(WHServer.END)) 
			errors.set(WHServer.END,true);
		}
		if (errors.get(WHServer.BEG)) 
		    bad_variables+="BEG ";
		if (errors.get(WHServer.END)) 
		    bad_variables+="END ";
		if (! received.get(WHServer.DAT)) 
		    errors.set(WHServer.DAT,true);
		if (errors.get(WHServer.DAT)) 
		    bad_variables+="DAT ";
		//check sha
		if (! received.get(WHServer.SHA) 
		    || (!WHDatabaseCheck.checkSignature(data,dataSHA,idt)))  
		    errors.set(WHServer.SHA,true); 
		if (errors.get(WHServer.SHA)) 
		    bad_variables+="SHA ";
		if (bad_variables.length()!=0)
		    throw new WHClientCommandException(bad_variables.trim());
		else {
		    //create a polygon object representing the inserted area
		    ll_poly=new WHPolygon(coordinates);
		    //if project set, project
		    if (received.get(WHServer.PJT)){
			ll_poly.project(project_heading,project_range);
		    }
		}
	    }
	    
	    //
	    //QUERY AND COUNT
	    //
	    if (act.equals("query")||act.equals("count")){
		//check limit
		if(errors.get(WHServer.LIM))  
		    bad_variables+="LIM ";
		if (very_verbose) System.out.println("limit checked");
		//check search area
		if (errors.get(WHServer.SHP) || (! received.get(WHServer.SHP))){
		    bad_variables+="SHP ";
		    if (very_verbose) System.out.println("shape checked, errors");
		} else {
		    if (very_verbose) System.out.println("shape checked, no errors");
		    if (shape.equals("rect_ctr")){
			if (very_verbose) System.out.println("shape rectangle-center");
			//check radius
			if (errors.get(WHServer.RAD)||(! received.get(WHServer.RAD)))
			    bad_variables+="RAD ";
			if (very_verbose) System.out.println("radius checked");
		    } else {
			if (very_verbose) System.out.println("shape rectangle-forward");
			//check heading
			if (errors.get(WHServer.HDG)||(!received.get(WHServer.HDG)))
			    bad_variables+="HDG ";
			if (very_verbose) System.out.println("heading checked");
			//check width
			if (errors.get(WHServer.WID)||(!received.get(WHServer.WID)))
			    bad_variables+="WID ";
			if (very_verbose) System.out.println("width checked");
			//check length
			if (errors.get(WHServer.LEN)||(!received.get(WHServer.LEN)))
			    bad_variables+="LEN ";
			if (very_verbose) System.out.println("length checked");
		    }
		}
		//check mimetype
		if (errors.get(WHServer.MIM))
		    bad_variables+="MIM "; 
		if (very_verbose) System.out.println("mimetype checked");
		//check protocol
		if (errors.get(WHServer.PRO)
		    ||((!WHDatabaseCheck.checkProtocol(protocol))&&(received.get(WHServer.PRO))))
		    bad_variables+="PRO ";
		if (very_verbose) System.out.println("protocol checked");
		//check beg and end
		if (errors.get(WHServer.BEG)) bad_variables+="BEG ";
		if (errors.get(WHServer.END)) bad_variables+="END ";
		//check pjt 
		if (errors.get(WHServer.PJT))
		    bad_variables+="PJT ";
		
		//check llh, should be set
		//then set search area description string
		if (errors.get(WHServer.LLH)||(!(received.get(WHServer.LLH))))
		    bad_variables+="LLH ";
		//throw exception if there were any errors
		if (bad_variables.length()!=0)
		    throw new WHClientCommandException(bad_variables.trim());
		else {
		    //create a search area polygon
		    if (shape.equals("rect_fwd"))
			ll_poly=new WHPolygon(coordinates[0],heading,width,length);
		    else
			ll_poly=new WHPolygon(coordinates[0],radius);
		}
		//if project is set, project it
		if (received.get(WHServer.PJT))
		    ll_poly.project(project_heading,project_range);
		if (verbose || very_verbose)
		    System.out.println("Search polygon: "+ll_poly.toString());
	    }
	    
	    //DELETE
	    if (act.equals("delete")){
		//check idt
		if ((! received.get(WHServer.IDT)) 
		    || errors.get(WHServer.IDT)
		    ||(! WHDatabaseCheck.checkUser(idt)))
		    bad_variables += "IDT ";
		//check uid
		if ((! received.get(WHServer.UID))
		    || errors.get(WHServer.UID)
		    ||(! WHDatabaseCheck.checkUID(uniqueidSHA)))
		    bad_variables += "UID ";
		//throw exception if there were any errors
		if (bad_variables.length()!=0)
		    throw new WHClientCommandException(bad_variables.trim());
	    }
	}
    }
    
	
    private String interpretLine(String line,BufferedReader in, PrintWriter out){
	
	StringTokenizer line_tokens;
	StringTokenizer param_tokens;
	
	// get the command portion
	String cmd;
	String param;

	if (very_verbose)
	    System.out.println("INTERPRETING LINE: "+line);


	line_tokens = new StringTokenizer(line," ",false);

	if (line_tokens.hasMoreTokens()) {
	    cmd=((String)line_tokens.nextToken()).toLowerCase(); 
	} else { 
	    cmd="";
	}
	if (very_verbose) System.out.println("COMMAND: "+cmd);
	// get the parameters
	if (line_tokens.hasMoreTokens()) {
	    param = (String) line.substring(cmd.length()).trim();
	} else {
	    param = "";
	}
	if (very_verbose) System.out.println("PARAMS: "+param);

	//set appropriate variables
	switch(WHServer.commandIndex(cmd)){
	    
	    //IDT
	case WHServer.IDT:
	    received.set(WHServer.IDT, true);
	    idt=param.toLowerCase();
	    errors.set(WHServer.IDT,(idt.equals("") 
					  || (idt.length() > WHServer.MAXIDT)));
	    out.println(WHServer.serverHeader());	    
	    if (very_verbose) {
		System.out.println("Interpreting IDT ="+ idt);
		System.out.println("Sent: "+WHServer.serverHeader());
	    }
	    return cmd;
	    
	    //SHA
	case WHServer.SHA:
	    received.set(WHServer.SHA, true);
	    sha=param;
	    if (very_verbose) 
		System.out.println("Interpreting SHA ="+sha);
	    return cmd;
	    
	    //ACT
	case WHServer.ACT: 
	    received.set(WHServer.ACT ,true);
	    act = param.toLowerCase();
	    errors.set(WHServer.ACT,(!(act.equals("insert")
				       ||act.equals("count")
				       ||act.equals("delete")
				       ||act.equals("query"))));
	    if (verbose || very_verbose) 
		System.out.println("Interpreting ACT ="+act);
	    return cmd;
	    
	    //LLH
	case WHServer.LLH: 
	    received.set(WHServer.LLH,true);
	    //read the values for lat, lon, h into respective vectors
	    param_tokens= new StringTokenizer(param);
	    height=-1;
	    if (verbose || very_verbose) 
		System.out.println("Interpreting LLH: "+param);
	    try {
		Point2D.Double[] temp_array=new Point2D.Double[0];
		coordinates= new Point2D.Double[0];
		double temp_height;
		int counter=0;
		do {
		    temp_array= new Point2D.Double[counter+1];
		    //copy coordinates to temp_array
		    System.arraycopy(coordinates,0,temp_array,0,counter);				     
		    //read latitudude and longitude
		    temp_array[counter]=new Point2D.Double(
							   Double.parseDouble(param_tokens.nextToken()),
							   Double.parseDouble(param_tokens.nextToken()));
		    //convert to proper wherehoo ranges
		    WHGeo.toWHFormat(temp_array[counter]);
		    //read height
		    temp_height=(Double.parseDouble(param_tokens.nextToken()));
		    if (counter==0)
			height=temp_height;
		    //check if the height is the same as in previous point that was read
		    if (height!=temp_height)
			throw new WHClientCommandException();
		    //create new coordinates array, one element larger than before
		    coordinates= new Point2D.Double[counter+1];
		    //copy temp_array into coordinates
		    System.arraycopy(temp_array,0,coordinates,0,counter+1);
		    //increase counter
		    counter=counter+1;
		    if (very_verbose) {
			System.out.println("COORDINATES: "+coordinates.toString());
			System.out.println("HEIGHT: "+height);}
		} while (param_tokens.hasMoreElements());
		errors.set(WHServer.LLH,false);
	    } catch (Exception e){
		errors.set(WHServer.LLH,true);
	    }
	    return cmd;
	    
	    //BEG
	case WHServer.BEG: 
	    received.set(WHServer.BEG,true);
	    if (very_verbose) 
		System.out.println("Interpreting BEG ="+param);
	    try {
		time_interval.setBegin(param);
		errors.set(WHServer.BEG,false);
	    } catch (WHTimeException e){
		errors.set(WHServer.BEG,true);
	    }	    
	    return cmd;
	    
	    //END
	case WHServer.END: 
	    received.set(WHServer.END ,true);
	    if (very_verbose) 
		System.out.println("Interpreting END ="+param);
	    try{
		time_interval.setBegin(param);
		errors.set(WHServer.END,false);
	    } catch (WHTimeException e){
		errors.set(WHServer.END,true);
	    }
	    return cmd;
	    
	    //HDG 
	case WHServer.HDG: 
	    received.set(WHServer.HDG,true);
	    if (very_verbose) 
		System.out.println("Interpreting HDG ="+param);
	    try{
		heading = (((Double.parseDouble(param))%360)+360)%360;
		errors.set(WHServer.HDG,false);
	    } catch(NumberFormatException nfe) {
		errors.set(WHServer.HDG,true);
	    }
	    return cmd;
	    
	    //LEN
	case WHServer.LEN: 
	    received.set(WHServer.LEN ,true);
	    if (very_verbose) 
		System.out.println("Interpreting LEN ="+param);
	    try{
		length = Double.parseDouble(param);
		errors.set(WHServer.LEN,(length<1));
	    } catch (NumberFormatException nfe){
		errors.set(WHServer.LEN,true);
	    }
	    return cmd;
	    
	    //LIM
	case WHServer.LIM: 
	    received.set(WHServer.LIM ,true);
	    if (very_verbose) 
		System.out.println("Interpreting LIM ="+param);
	    try{
		limit = Integer.parseInt(param);
		errors.set(WHServer.LIM,(limit<0));
	    } catch (NumberFormatException nfe){
		errors.set(WHServer.LIM, true);
	    }
	    return cmd;
	    
	    //MET
	case WHServer.MET: 
	    received.set(WHServer.MET ,true);
	    if (very_verbose) 
		System.out.println("Interpreting MET ="+param);
	    meta = param.substring(0, Math.min(param.length(),WHServer.MAXMETA)).trim();
	    return cmd;
	    
	    //MIM
	case WHServer.MIM: 
	    received.set(WHServer.MIM,true);
	    if (very_verbose) 
		System.out.println("Interpreting MIM ="+param);
	    mimetype=param.toLowerCase().trim();
	    errors.set(WHServer.MIM,(mimetype.equals("")));
	    return cmd;
	    
	    //PJT
	case WHServer.PJT: 
	    received.set(WHServer.PJT,true);
	    if (very_verbose) 
		System.out.println("Interpreting PJT ="+param);
	    try {
		param_tokens = new StringTokenizer(param); 
		//normalize the project angle
		project_heading = (((Float.parseFloat(param_tokens.nextToken()))%360)+360)%360; 
		project_range = Float.parseFloat(param_tokens.nextToken());
		errors.set(WHServer.PJT,(project_range<0));
	    } catch(Exception e) {
		errors.set(WHServer.PJT,true);
	    }
	    return cmd;
	    
	    //PRO
	case WHServer.PRO: 
	    received.set(WHServer.PRO,true);
	    if (very_verbose) 
		System.out.println("Interpreting PRO ="+param);
	    protocol=param.toUpperCase();
	    errors.set(WHServer.PRO,false);
	    return cmd;
	    
	    //RAD
	case WHServer.RAD: 
	    received.set(WHServer.RAD ,true);
	    if (very_verbose) 
		System.out.println("Interpreting RAD ="+param);
	    try{
		radius=Float.parseFloat(param);
		if (radius<1)
		    errors.set(WHServer.RAD,true);
		else {
		    received.set(WHServer.WID,true);
		    width = radius;
		    received.set(WHServer.LEN,true);
		    length = radius;
		}
	    } catch (NumberFormatException nfe){
		errors.set(WHServer.RAD,true);
	    }
	    return cmd;
	    
	    //SHP
	case WHServer.SHP: 
	    received.set(WHServer.SHP ,true);
	    if (very_verbose) 
		System.out.println("Interpreting SHP ="+param);
	    shape=param.toLowerCase();
	    errors.set(WHServer.SHP,(!(shape.equals("rect_fwd")||shape.equals("rect_ctr"))));
	    return cmd;
	    
	    //WID
	case WHServer.WID: 
	    received.set(WHServer.WID,true);
	    if (very_verbose) 
		System.out.println("Interpreting WID ="+param);
	    try{
		width=Float.parseFloat(param);
		//set the error field true if width is less than 1 
		errors.set(WHServer.WID,(width<1));
	    } catch (NumberFormatException nfe){
		errors.set(WHServer.WID,true);
	    }
	    return cmd;
	    
	    //DAT
	case WHServer.DAT: 
	    received.set(WHServer.DAT ,true);
	    if (very_verbose) 
		System.out.println("Interpreting DAT ");
	    int datalen;
	    try { 
		datalen = Integer.parseInt(param); 
	    } catch(NumberFormatException nfe) {
		errors.set(WHServer.DAT,true);
		return cmd;
	    }
	    if (very_verbose) 
		System.out.println("got datalen");
	    if ((datalen < 1) || (datalen > WHServer.MAXDATA)){
		errors.set(WHServer.DAT,true);
		return cmd; 
	    } else {
		try{
		    data = new byte[datalen];
		    dataSHA = new byte[WHServer.SIGNATUREBYTECOUNT]; 
		    //read the data
		    for (int _j=0; _j < datalen; _j++) {
			int _timer = WHServer.RXTIMEOUT;
			while (! in.ready()) { 
			    if (very_verbose) System.out.println("not ready");
			    try { Thread.sleep(WHServer.RXLOOPTIMEOUT);
			    } catch (Exception e) { }
			    _timer -= WHServer.RXLOOPTIMEOUT;
			    if (_timer < 0) {
				errors.set(WHServer.DAT,true);
				throw new java.io.InterruptedIOException(); 
			    }
			}
			data[_j] = (byte) in.read();
		    }
		    if (very_verbose) 
			System.out.println("read data");
		    //read the signature	
		    for (int _j=0; _j < WHServer.SIGNATUREBYTECOUNT; _j++) {
			int _timer = WHServer.RXTIMEOUT;
			while (! in.ready()) { 
			    try { Thread.sleep(WHServer.RXLOOPTIMEOUT);
			    } catch (Exception e) { } 
			    _timer -= WHServer.RXLOOPTIMEOUT;
			    if (_timer < 0) {
				errors.set(WHServer.DAT, true);
				throw new java.io.InterruptedIOException(); 
			    }
			}
			dataSHA[_j] = (byte) in.read();
		    }
		    if (very_verbose) 
			System.out.println("read signature");
		    out.println("ACK");
		    if (very_verbose) 
			System.out.println("printed ack");
		    errors.set(WHServer.DAT,false);
		} catch (Exception e){
		    errors.set(WHServer.DAT,true);
		}
		if (very_verbose) {
		    System.out.println("Returning cmd");
		    System.out.println("Data :"+data.length);
		    System.out.println("SHA :"+dataSHA.length);
		}
		return cmd;
	    }
	    //UID
	case WHServer.UID: 
	    received.set(WHServer.UID,true);
	    if (very_verbose) 
		System.out.println("Interpreting UID ="+param);
	    uniqueidSHA=param;
	    errors.set(WHServer.UID,false);
	    return cmd;
	    
	    //DBG
	case WHServer.DBG: 
	    received.set(WHServer.DBG,true);
	    if (very_verbose) 
		System.out.println("Interpreting DBG");
	    this.setVeryVerbose(true);
	    return "dbg";
	    
	    //NOP
	case WHServer.NOP:   
	    received.set(WHServer.NOP,true);
	    if (very_verbose) 
		System.out.println("Interpreting NOP");
	    out.println("ACK");
	    return cmd;
	    
	    //BYE
	case WHServer.BYE:   
	    received.set(WHServer.BYE,true);
	    if (very_verbose) 
		System.out.println("Interpreting BYE");
	    return cmd;
	    //.
	case WHServer.DOT:  
	    received.set(WHServer.DOT,true);
	    if (very_verbose) System.out.println("Interpreting  . ");
	    return cmd;

	    //COMMAND NOT RECOGNIZED
	default:
	    return "bye";
	}
    }
    /** Returns the <tt>String</tt> representation of this <tt>WHDataCollector.</tt> 
     * @return <tt>String</tt> object representing this <tt>WHDataCollector</tt>. It lists the fields that correspond to
     * parameters of defined Wherehoo commands.
     */
    public String toString(){
	String s=new String();
	s+="ACT: "+act;
	s+=",  IDT: "+idt;
	s+=",  SHA: "+sha;
	s+=",  UNIQUEIDSHA: "+uniqueidSHA;
	s+=",  MIMETYPE: "+mimetype;
	s+=",  PROTOCOL: "+protocol;
	s+=",  SHAPE: "+shape;
	s+=",  META: "+meta;
	
	s+=",  COORDINATES: "+coordinates.toString();
	s+=",  HEIGHT: "+height;
	s+=",  WIDTH: "+width; //width of the search field.
	s+=",  LENGTH: "+length; //length of the search field.
	s+=",  HEADING: "+heading;
	s+=",  RADIUS: "+radius;
	s+=",  PROJECT: "+project_heading;
	s+=",  PROJECT RANGE: "+project_range;
	s+=",  LIMIT: "+limit;
	return s;
    }

}
   
   
    



