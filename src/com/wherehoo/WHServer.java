package com.wherehoo;
/* whserver.java - wherehoo server for unix port access */

// WHEREHOO SERVER 
// COPYRIGHT (c) 2000, Jim Youll and Massachusetts Institute of Technology, Cambridge, MA
// all rights reserved. Thou shalt not steal, and all that.

import java.io.*;
import java.net.*;
import java.sql.*;


public class WHServer {
	
    /** standard copyright notice */
    protected static final String copyrightNotice=
	"COPYRIGHT (c) 2000, Jim Youll and The Media Laboratory, Cambridge, MA. all rights reserved";    
    /** URL of the database behind Wherehoo */
    protected static final String DB = "postgresql://localhost:9999/wherehoo";
    /** version of server code */
    public static final double 	VERSION = (double) 0.850;
    /** max number of connections this server will handle at a time */
    public static final int Q_LEN = 50;
    /** standard port of Wherehoo server, 5650 */
    public static final int PORT  = 5650; 
    /** msec of silence permitted from client before disconnect  */
    protected static final int RXTIMEOUT = 200000; 
    

    public static void main(String[] args) throws IOException {	
	
	Socket client_socket;
	String client_address;

	// load the class for db server access
	try {
	    //System.out.println("load driver");
	    Driver driver = (Driver)Class.forName("org.postgresql.Driver").newInstance();
	    DriverManager.registerDriver(driver);
	    //System.out.println("got the driver");
	}
	catch (Exception e) {    
	    System.err.println("Unable to load SQL driver.");
	    e.printStackTrace();
	}
	
	// setup for incoming socket connections
	ServerSocket server_socket = new ServerSocket(WHServer.PORT,WHServer.Q_LEN);
	System.out.println("Wherehoo socket server v"+WHServer.VERSION+" on port "+WHServer.PORT);
	String server_address = InetAddress.getLocalHost().getHostAddress();
	
	// wait for a client connection, then start a new thread to handle it
	while (true) {
	    System.out.println("Host "+server_address+" blocking on accept()");
	    client_socket = server_socket.accept(); // block until next client connection
	    client_address = client_socket.getInetAddress().getHostAddress();
	    System.out.println("connection accepted from "+client_address+" Launching thread.");
	    client_socket.setSoTimeout(WHServer.RXTIMEOUT);
	    new WHClientProcess(client_socket).start();
	}
    }
    //#############################################################################################
    //#                  utility variables and classes
    //#############################################################################################
    //
    //
        
    /**     max size of binary data stored in a wherehoo record     */
    protected static final int	MAXDATA = 65535; 
    /**     max size of metadata text field describing the wherehoo data field contents */    
    protected static final int	MAXMETA = 1024;
    /**     longest TTL that will be reported       */    
    protected static final int	MAXTTL = 99999999; 
    /**     largest value for RAD, WID or LEN     */    
    protected static final int 	MAXWIDLEN = 999999; 
    /**     seconds ahead of "now" for min. expiration timestamp on a record being inserted     */    
    protected static final int 	RECORD_MIN_LIFE = 12;
    /**     max length of IDENT field     */    
    protected static final int	MAXIDT = 10;
    /**      number of bytes to read after DATA - containing the signature     */    
    protected static final int	SIGNATUREBYTECOUNT = 20; 

    /** loop timeout */
    public static final int RXLOOPTIMEOUT = 1000;

    /** Index of IDT command     */
    protected static final int IDT = 1313;
    /** Index of SHA command     */
    protected static final int SHA = 1613;
    /** Index of ACT command     */
    protected static final int ACT = 1913;
    /** Index of LLH command     */
    protected static final int LLH = 2313;
    /** Index of BEG command     */
    protected static final int BEG = 2613;
    /** Index of END command     */
    protected static final int END = 2913;
    /** Index of HDG command     */
    protected static final int HDG = 3313;
    /** Index of LEN command     */
    protected static final int LEN = 3613;
    /** Index of LIM command     */
    protected static final int LIM = 3913;
    /** Index of MET command     */
    protected static final int MET = 4313;
    /** Index of MIM command     */
    protected static final int MIM = 4613; 
    /** Index of PJT command     */
    protected static final int PJT = 4913; 
    /** Index of PRO command     */
    protected static final int PRO = 5313;
    /** Index of RAD command     */
    protected static final int RAD = 5613;
    /** Index of SHP command     */
    protected static final int SHP = 5913;
    /** Index of WID command     */
    protected static final int WID = 6313;
    /** Index of DAT command     */
    protected static final int DAT = 6613;
    /** Index of UID command     */
    protected static final int UID = 6913;
    /** Index of DBG command     */
    protected static final int DBG = 7313;
    /** Index of NOP command     */
    protected static final int NOP = 7613;
    /** Index of BYE command     */
    protected static final int BYE = 7913;
    /** Index of "." command     */
    protected static final int DOT = 8313;

    private static final Object[][] command_pairs = {{"idt", new Integer(WHServer.IDT )},
						     {"act", new Integer(WHServer.ACT )},
						     {"sha", new Integer(WHServer.SHA )},
						     {"llh", new Integer(WHServer.LLH )},
						     {"beg", new Integer(WHServer.BEG )},
						     {"end", new Integer(WHServer.END )},
						     {"hdg", new Integer(WHServer.HDG )},
						     {"len", new Integer(WHServer.LEN )},
						     {"lim", new Integer(WHServer.LIM )},
						     {"met", new Integer(WHServer.MET )},
						     {"mim", new Integer(WHServer.MIM )},
						     {"pjt", new Integer(WHServer.PJT )},
						     {"pro", new Integer(WHServer.PRO )},
						     {"rad", new Integer(WHServer.RAD )},
						     {"shp", new Integer(WHServer.SHP )},
						     {"wid", new Integer(WHServer.WID )},
						     {"dat", new Integer(WHServer.DAT )},
						     {"uid", new Integer(WHServer.UID )},
						     {"dbg", new Integer(WHServer.DBG )},
						     {"nop", new Integer(WHServer.NOP )},
						     {"bye", new Integer(WHServer.BYE )},
						     {"dot", new Integer(WHServer.DOT )}};
   

    protected static int commandIndex(String command){
	for (int i=0;i<command_pairs.length;i++){
	    if (((String)command_pairs[i][0]).equals(command))
		return ((Integer)command_pairs[i][1]).intValue();
	}
	return -1;
    }

    protected static String commandName(int command_index){
	for (int i=0;i<command_pairs.length;i++){
	    if (((Integer) command_pairs[i][1]).intValue() == command_index){
		return ((String)command_pairs[i][0]);
	    }
	}
	return "";
    }
    protected static String serverHeader(){
	return ("wherehoo_server "+WHServer.VERSION+" "
		+WHServer.RXTIMEOUT/1000+" "
		+WHServer.RECORD_MIN_LIFE+" "
		+WHServer.MAXMETA+" "+WHServer.MAXDATA);
    }
}







