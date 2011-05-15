package com.wherehoo;

import java.util.Calendar;
import java.util.StringTokenizer;
import java.sql.SQLException;
import java.sql.Timestamp;
/**
 * Provides the utility methods for time-related objects.
 */
public class WHTimeInterval{

    private Calendar basetime;
    private Calendar begin;
    private Calendar end;
    private boolean begin_is_set;
    private boolean end_is_set;
    private Calendar default_end;

    /**
     * Initializes <tt>WHTimeInterval</tt>.  The default <tt>WHTimeInterval</tt> 
     * begins at the time of its initialization
     * and it ends on <tt>Dec 31,9999</tt>.
     */ 
    protected WHTimeInterval(){	
	basetime = Calendar.getInstance();
	begin = (Calendar) basetime.clone();
	end = (Calendar) default_end.clone();
	default_end = Calendar.getInstance();
	default_end.set(9999,12,31,23,59,59);
    }

    /**
     * Sets the begining of this <tt>WHTimeInterval</tt> to a point in time that is
     * offset from the basetime of this <tt>WHTimeInterval</tt> by parameters passed in the offsets[]
     * array.
     */
    protected void setBegin(int[] offsets) throws WHTimeException {
	begin = WHTimeInterval.addAllDates(basetime, offsets);
    }

    protected void setBegin(String offsets) throws WHTimeException {
	StringTokenizer param_tokens = new StringTokenizer(offsets);
	int[] deltabeg = new int[5];
	try {  
	    deltabeg[0]=Integer.parseInt(param_tokens.nextToken()); 
	    deltabeg[1]=Integer.parseInt(param_tokens.nextToken()); 
	    deltabeg[2]=Integer.parseInt(param_tokens.nextToken()); 
	    deltabeg[3]=Integer.parseInt(param_tokens.nextToken()); 
	    deltabeg[4]=Integer.parseInt(param_tokens.nextToken()); 
	    deltabeg[5]=Integer.parseInt(param_tokens.nextToken());
	} catch(Exception e) { 
	    throw new WHTimeException();
	}
	this.setBegin(deltabeg);
    }
    
    protected void setEnd(int[] offsets) throws WHTimeException {
	end = WHTimeInterval.addAllDates(basetime,offsets);
    }

    protected void setEnd(String offsets) throws WHTimeException {
	StringTokenizer param_tokens = new StringTokenizer(offsets); 
	int[] deltaend = new int[5];
	try {  
	    deltaend[0]=Integer.parseInt(param_tokens.nextToken()); 
	    deltaend[1]=Integer.parseInt(param_tokens.nextToken()); 
	    deltaend[2]=Integer.parseInt(param_tokens.nextToken()); 
	    deltaend[3]=Integer.parseInt(param_tokens.nextToken()); 
	    deltaend[4]=Integer.parseInt(param_tokens.nextToken()); 
	    deltaend[5]=Integer.parseInt(param_tokens.nextToken());
	} catch(Exception e) { 
	    throw new WHTimeException();
	}
	this.setEnd(deltaend);
    }

    protected boolean checkInsertCompliance(){
	//check if begin before end
	//check if there is minimal interval
	return true;
    }
    protected Timestamp getBegin() {
	return this.calToTimeStamp(begin);
    }
    protected Timestamp getEnd(){
	return this.calToTimeStamp(end);
    }
    
    /**
     * Returns a new <tt>WHCalendar</tt> object that represents the point in time of <tt>cal</tt>, when it is
     * offset by the parameters passed in <tt>offsets[]</tt> array. 
     * <tt> offsets[]</tt> has to be at least 6 elements long:<br><br>
     *<center><tt>[year offset, month offset, day of month offset,hour of day offset, minute offset, second offset]
     *</tt></center><br>
     * The remaining elements of an array are ignored, if present.
     */
    public static Calendar addAllDates(Calendar cal, int offsets[]) throws WHTimeException {
	Calendar _cal = Calendar.getInstance();
	_cal = (Calendar) cal.clone();
	try {
	    // Add the offset to the current object
	    _cal.add(Calendar.YEAR,			offsets[0]);
	    _cal.add(Calendar.MONTH,			offsets[1]);
	    _cal.add(Calendar.DAY_OF_MONTH,	offsets[2]);
	    _cal.add(Calendar.HOUR_OF_DAY,	offsets[3]);
	    _cal.add(Calendar.MINUTE,		offsets[4]);
	    _cal.add(Calendar.SECOND,		offsets[5]);
	} catch (ArrayIndexOutOfBoundsException e){
	    throw new WHTimeException();
	}
	return _cal;
    }
    /**
     * Generates <tt>Timestamp</tt> for the point in time represented by <tt>cal</tt>.
     *@param cal <tt>Calendar</tt> object. 
     *@return <tt>Timestamp</tt> that can be used in a SQL query.
     */
    public static Timestamp calToTimeStamp(Calendar cal) {
	String s="";
	s  = cal.get(Calendar.YEAR)+"-"+(cal.get(Calendar.MONTH)+1)+"-"+cal.get(Calendar.DAY_OF_MONTH)+" ";
	s += cal.get(Calendar.HOUR_OF_DAY)+":"+cal.get(Calendar.MINUTE)+":"+cal.get(Calendar.SECOND);
	return Timestamp.valueOf(s);
    }
    /**
     *Provides a patch for an apparent bug in SQL driver. 
     *The driver throws an <tt>SQLException</tt> at the attempt to read a timestamp dated before 
     *<tt>1900-01-01 00:00:00</tt> . 
     *Fortunately, the string description of the timestamp is
     *included in error message.
     *The sample message:<br>
     *<center><tt>Bad Timestamp Format at 19 in 1888-10-09 00:02:01</tt>.</center><br> 
     *This method scrapes <tt>"1888-10-09 00:02:01"</tt>, parses the scraped string and generates a proper <tt>Timestamp</tt>.
     *@param sqle <tt>SQLException</tt> throwed by the faulty driver
     *@return <tt>Timestamp</tt> the driver should've retrieved without throwing an exception.
     */
    public static Timestamp retrieveTimestamp(SQLException sqle){
	
	int year,month,day,hour,minute,second;
	long milis;
	String message = sqle.getMessage();
	String ts=message.substring(message.indexOf("in")+2).trim();
	Timestamp t=Timestamp.valueOf(ts);
	return t;
    }
    /*
//set the basetime to "now"
		basetime=Calendar.getInstance();
		basetime.setTime(new java.util.Date());
		if (very_verbose) System.out.println("set the basetime");
		//check beg
		if (errors.get(.beg){
		    bad_variables+="BEG ";
		} else {
		    if (delta_beg_is_set) 
			begin = WHCalendar.addAllDates(basetime,deltabeg);
		    else 
			begin = basetime;
		}
		if (very_verbose) System.out.println("Checked begin");
		//check end
		if (errors.get(.end){
		    bad_variables+="END ";
		} else {
		    if(!errors.get(.beg){
			Calendar endMin = Calendar.getInstance();
			endMin = WHCalendar.addAllDates(begin,deltamin);
			
			if (! delta_end_is_set) {
			    end=Calendar.getInstance();
			    end.set(9999,11,31,23,59,59); // default END is a really long time from now
			} else { // an offset was provided, so set a real end time
			    end = WHCalendar.addAllDates(basetime,deltaend);
			}
			if(end.getTime().before(endMin.getTime()))
			    bad_variables+="END ";
			if (!(begin.getTime().before(end.getTime())))
			    bad_variables+="BEG";	
		    }
		}
		    if (very_verbose) System.out.println("Checked end");
    */
    
}


