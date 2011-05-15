package com.wherehoo;

import java.util.Vector;
import java.util.StringTokenizer;
import java.awt.geom.Point2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.math.BigDecimal;


/**
 * <tt>WHPolygon</tt> represents a geographical area.  <tt>WHPolygon</tt> supplies methods for manipulating such area, 
 * as well as for navigating it. 
 */
public class WHPolygon{
    
    private Point2D.Double[] coordinates;
    private GeneralPath path;
    private boolean zero_crossing;
    //for zero crossing case
    private WHPolygon leftPoly;
    private WHPolygon rightPoly;

    /**
     *Constructs a new <tt>WHPolygon</tt> object.
     *@param poly_descriptor a string that contains the description of all the vertices of <tt>WHPolygon</tt> 
     *in the form <i>(lat1,lon1),(lat2,lon2)....(latN,lonN)</i>.  This constructor is meant to be used to reconstruct
     * <tt>WHPolygon</tt> object when it is retrieved from Postgres. This method does not verify that
     * <tt>poly_descriptor</tt> is in a above described form.  It merely parses the <tt>poly_descriptor</tt> 
     * string looking for '(' characters as markers of vertices. If the string is not in a proper form, 
     *the constructor will in most cases produce zero-vertex <tt>WHPolygon</tt> object.  
     *@return <tt>WHPolygon</tt>, which vertices were listed by <tt>poly_descriptor</tt>.
     */
    public WHPolygon(String poly_descriptor){

	zero_crossing = false;
	leftPoly=null;
	rightPoly=null;

	StringTokenizer vertex_tokens= new StringTokenizer(poly_descriptor,"(",false);
	String vertex;
	coordinates=new Point2D.Double[vertex_tokens.countTokens()];
	
	for(int i=0;vertex_tokens.hasMoreTokens();i++){
	    vertex=vertex_tokens.nextToken();
	    //read in the next vertex
	    coordinates[i]= new Point2D.Double(
					       Double.parseDouble(vertex.substring(0,vertex.indexOf(','))),
					       Double.parseDouble(vertex.substring(vertex.indexOf(',')+1,vertex.indexOf(')'))));
	}

	zero_crossing = this.checkForZeroCrossing();
	this.constructPath();
    }
      
    /**
     * Constructs a new <tt>WHPolygon</tt> object. 
     *@param vertices an array of Point2D.Double objects describing the geographical locations that constitute vertices 
     * of this <tt>WHPolygon</tt>
     *@return <tt>WHPolygon</tt>, described by <tt>vertices</tt>
     */ 
    public WHPolygon(Point2D.Double[] vertices){
	coordinates=new Point2D.Double[vertices.length];
	for (int i=0;i<vertices.length;i++){
	    coordinates[i]= new Point2D.Double(vertices[i].getX(),vertices[i].getY());
	}
	zero_crossing=this.checkForZeroCrossing();
	this.constructPath();
    }    
   
    /**
     *Constructs a new square <tt>WHPolygon</tt> object. Its sides are 2*radius and 
     *its center is described by <tt>client_location</tt>.
     *This constructor is designed for radial searches.
     *@param client_location a geographical point which will also mark the center of constructed <tt>WHPolygon</tt>.
     *@param radius a radius of the largest circle that could be inscribed into constructed <tt>WHPolygon</tt> - 
     * effectively, it is half of the side length
     *@return Square <tt>WHPolygon</tt> object, 2*radius x 2*radius, centered at <tt>client_location</tt> 
     */ 
    public WHPolygon(Point2D.Double client_location, double radius){
	
	coordinates = new Point2D.Double[4];
	//calculate the coordinates of four vertices
	double half_diagonal=radius*Math.sqrt(2);
	Point2D.Double poly_vertex=new Point2D.Double(client_location.getX(),client_location.getY());
	//calculate a first vertex
	WHGeo.project(poly_vertex,45,half_diagonal);
	coordinates[0]=new Point2D.Double(poly_vertex.getX(),poly_vertex.getY());
	//calculate a second vertex
	WHGeo.project(poly_vertex,180,2*radius);
	coordinates[1]=new Point2D.Double(poly_vertex.getX(),poly_vertex.getY());
	//calculate a third vertex
	WHGeo.project(poly_vertex,270,2*radius);
	coordinates[2]=new Point2D.Double(poly_vertex.getX(),poly_vertex.getY());
	//calculate a fourth vertex
	WHGeo.project(poly_vertex,0,2*radius);
	coordinates[3]=new Point2D.Double(poly_vertex.getX(),poly_vertex.getY());
	
	zero_crossing=this.checkForZeroCrossing();
	this.constructPath();
    }
    /**
     *Constructs a new rectangular <tt>WHPolygon</tt> object.  
     * Its dimensions are <tt>width</tt> x <tt>length</tt>, and 
     *it is tilted by the angle that equals to <tt>heading</tt> parameter.
     *This constructor is designed for the directional searches.
     *@param client_location a geographical location of the user
     *@param heading a tilt of the search area
     *@param width width of the search area
     *@param length length of the search area
     *@return <tt>WHPolygon</tt>, rectangular shape, <tt>width</tt> x <tt>length</tt>,
     * tilted by an agle given by <tt>heading</tt>.
     * The midpoint of one of the width sides corresponds to <tt>client_location</tt> parameter.
     */  
    public WHPolygon(Point2D.Double client_location, double heading, double width, double length){
	
	//create coordinates[]
	coordinates= new Point2D.Double[4];
	double half_width= (new BigDecimal(width).divide(new BigDecimal(2),20,BigDecimal.ROUND_HALF_EVEN)).doubleValue();
	//calculate the coordinates of four vertices of the search area
	Point2D.Double poly_vertex=new Point2D.Double(client_location.getX(),client_location.getY());
	double poly_side_angle;
	//calculate a first vertex
	poly_side_angle=((heading+270)%360);
	WHGeo.project(poly_vertex,poly_side_angle,half_width);
	coordinates[0]=new Point2D.Double(poly_vertex.getX(),poly_vertex.getY());
	//calculate a second vertex
	poly_side_angle=heading;
	WHGeo.project(poly_vertex,poly_side_angle,length);
	coordinates[1]=new Point2D.Double(poly_vertex.getX(),poly_vertex.getY());
	//calculate a third vertex
	poly_side_angle=((heading+90)%360);
	WHGeo.project(poly_vertex,poly_side_angle,width);
	coordinates[2]=new Point2D.Double(poly_vertex.getX(),poly_vertex.getY());
	//calculate a fourth vertex
	poly_side_angle=((heading+180)%360);
	WHGeo.project(poly_vertex,poly_side_angle,length);
	coordinates[3]=new Point2D.Double(poly_vertex.getX(),poly_vertex.getY());
	zero_crossing=this.checkForZeroCrossing();
	this.constructPath();
	
    }
    /**
     * Projects this <tt>WHPolygon</tt> by <tt>project_heading</tt> and <tt>project_range</tt>.
     * @param project_heading the angle of projection, measured from a meridian line.
     * @param project_range the distance of projection
     */
    public void project(double project_heading, double project_range){
	//project all the vertices
	Point2D.Double temp_loc;
	for(int i=0;i<coordinates.length;i++){
	    WHGeo.project(coordinates[i],project_heading,project_range);
	}
    }
    /**
     * Calculates the geographical distance between this <tt>WHPolygon</tt> and a location described by <tt>origin</tt>.
     * @param origin some point on Earth
     * @return the geographical distance from this <tt>WHPolygon</tt> to <tt>origin</tt>, in meters.  
     *If <tt>origin</tt> lies within this <tt>WHPolygon</tt>, return 0.
     */
    public double distance(Point2D.Double origin){
	//if this <tt>WHPolygon</tt> has one vertex => return the distance to that vertex
	if (coordinates.length == 1){
	    return WHGeo.distance(origin, coordinates[0]);
	} else {
	    //if origin inside this <tt>WHPolygon</tt> => return 0
	    if (this.contains(origin)){
		//System.out.println("contained");
		return 0;
		//find the distance by calculating the distance to all the sides and taking the minimum
	    }
	    else {
		double d=WHGeo.distance(origin, new Line2D.Double(coordinates[coordinates.length-1],coordinates[0]));
		//System.out.println("temp_d: "+d);
		double temp_d;
		for (int i=1;i<coordinates.length;i++){
		    temp_d=WHGeo.distance(origin, new Line2D.Double(coordinates[i-1],coordinates[i]));
		    //System.out.println("temp_d: "+temp_d);
		    if (temp_d<d)
			d=temp_d;
		}
		return d;
	    }
	}
    }
    /**
     * Calculate the heading from a location described by <tt>origin</tt> to this <tt>WHPolygon</tt>.  
     * @param origin some point on the Earth
     * @return Heading, an angle between a meridian passing through <tt>origin</tt> 
     * and a line from <tt>origin</tt> to this <tt>WHPolygon</tt>.
     */
    public double heading(Point2D.Double origin){
	//if this WHPolygon has one vertex => return the heading to that vertex
	if (coordinates.length == 1){
	    return WHGeo.heading(origin, coordinates[0]);
	} else {
	    //if origin inside this WHPolygon => return 0
	    if (this.contains(origin)){
		return 0;
	    } else {
		//find the closest side
		double d=WHGeo.distance(origin, new Line2D.Double(coordinates[coordinates.length-1],coordinates[0]));
		Line2D.Double closest_side = new Line2D.Double(coordinates[coordinates.length-1],coordinates[0]);
		double temp_d;
		for (int i=1;i<coordinates.length;i++){
		    temp_d=WHGeo.distance(origin, new Line2D.Double(coordinates[i-1],coordinates[i]));
		    if (temp_d<d){
			d=temp_d;
			closest_side=new Line2D.Double(coordinates[i-1],coordinates[i]);
		    }
		    //System.out.println("Closest side: "+closest_side.getX1()+","+closest_side.getY1()+","+closest_side.getX2()+","+closest_side.getY2());
		}
		return WHGeo.heading(origin, closest_side);
	    }
	}
    }
    /**
     * Checks if <tt>point</tt> lies inside of this <tt>WHPolygon</tt>.
     * @param point some location on Earth
     * @return boolean value indicating whether <tt>point</tt> lies within the boundaries of this <tt>WHPolygon</tt>
     */
    public boolean contains(Point2D.Double point){
	if (zero_crossing){
	    //System.out.println("left:"+leftPoly.contains(point));
	    //System.out.println("right:"+rightPoly.contains(point));
	    return (leftPoly.contains(point) || rightPoly.contains(point));
	}
	else 
	    return path.contains(point);
    }
    /**
     * Checks if the area of this <tt>WHPolygon</tt> overlaps a 0<sup>o</sup> meridian.
     * @return a boolean value indicating whether this <tt>WHPolygon</tt> overlaps 0<sup>o</sup> meridian.
     */
    public boolean zeroCrossing(){
	return zero_crossing;
    }
    /**
     * Returns new <tt>WHPolygon</tt> objects, produced by cutting this <tt>WHPolygon</tt> 
     * in two along 0<sup>o</sup> meridian.
     * @return An array of new <tt>WHPolygon</tt> objects created by splitting 
     * this <tt>WHPolygon</tt> in two along 0<sup>0</sup> meridian.  
     * If this <tt>WHPolygon</tt> does not overlap 0<sup>o</sup> meridian, a clone of this <tt>WHPolygon</tt> is returned.
     */ 
    public WHPolygon[] splitAlongGreatMeridian(){
	WHPolygon[] half_polies;
	if (zero_crossing){
	    half_polies= new WHPolygon[2];
	    half_polies[0]=(WHPolygon)leftPoly.clone();
	    half_polies[1]=(WHPolygon)rightPoly.clone();
	} else {
	    half_polies= new WHPolygon[1];
	    half_polies[0]=(WHPolygon)this.clone();
	}
	return half_polies;
    }
    /**
     * Gives the number of vertices of this <tt>WHPolygon</tt>.
     * @return the number of vertices in this <tt>WHPolygon</tt>.
     */
    public int vertexCount(){
	return coordinates.length;
    }
    /**
     * Creates an array of lengths of sides of this <tt>WHPolygon</tt>
     * @return an array of lengths of sides of this <tt>WHPolygon</tt>.
     */
    public int[] getSides(){
	int[] sides=new int[coordinates.length];
	for (int i=0;i<coordinates.length-1;i++){
	    sides[i]=(int)WHGeo.distance(coordinates[i],coordinates[i+1]);
	}
	sides[coordinates.length-1]=(int)WHGeo.distance(coordinates[coordinates.length - 1],coordinates[0]);
	return sides;
    }
    /** Creates an array of locations of vertices of this <tt>WHPolygon</tt>.
     * @return an array of Point2D.Double objects representing the locations of vertices of this <tt>WHPolygon</tt>.
     */
    public Point2D.Double[] getVertices(){
	Point2D.Double[] copy_of_coordinates = new Point2D.Double[coordinates.length];
	for (int i=0;i<coordinates.length;i++){
	    copy_of_coordinates[i] = new Point2D.Double(coordinates[i].getX(),coordinates[i].getY());
	}
	return copy_of_coordinates;
    }
    /**
     * Returns a String representation of this <tt>WHPolygon</tt>.
     * @return a String representation of this <tt>WHPolygon</tt> in a form
     * <br><tt>((lat1,lon1),(lat2,lon2)....(latN,lonN))</tt>.
     */
    public String toString(){
	String poly_description="(";
	for (int i=0;i<coordinates.length;i++){
	    poly_description+="("+coordinates[i].getX()+","+coordinates[i].getY()+")";
	    if (i<coordinates.length-1)
		poly_description+=",";
	}
	poly_description+=")";
	return poly_description;
    }
    /**
     * Returns a <tt>String</tt> representation of this <tt>WHPolygon</tt>, but with longitudes translated to 
     * fall within the range (<tt>central_meridian</tt> - 180<sup>o</sup>, 
     * <tt>central_meridian</tt> + 180<sup>o</sup>).  
     * This method is helpful when <tt>SearchOperation</tt>
     * or <tt>CountOperation</tt> is performed on the area overlapping the 0<sup>o</sup> meridian.
     * @param central_meridian a meridian that lies halfway in the desired range for longitudes.  
     * In default Wherehoo settings, <tt>central_meridian</tt> is 180<sup>o</sup>.
     * @return <tt>String</tt> representation of this <tt>WHPolygon</tt> in a format identical to standard toString() method.
     * The range of longitude coordinates is translated to fall in 
     * (<tt>central_meridian</tt> - 180<sup>o</sup>, <tt>central_meridian</tt> + 180<sup>o</sup>).   
     * Example:<br>
     * <tt>central_meridian</tt> = 360<sup>o</sup>.  The range of longitude values is (180<sup>o</sup>,540<sup>o</sup>)
     */
    public String toString(int central_meridian){
	//this works for central meridians and initial coordinates that are in between 0 and 360
	String poly_description="(";
	double lon;
	for (int i=0;i<coordinates.length;i++){
	    if (i>0) poly_description+=",";
	    //append the latitude
	    poly_description+="("+coordinates[i].getX();
	    //append the transformed longitude
	    lon=coordinates[i].getY();
	    if (!(lon>=central_meridian-180 && lon<=central_meridian+180)){
		//put lon in the (0,360)range
		if (lon>central_meridian+180)
		    lon=lon-360;
		else 
		    //lon<central_meridian-180
		    lon=lon+360;
	    }
	    poly_description+=","+lon+")";
	}
	poly_description+=")";
	return poly_description;
    }
    /**
     * Overrides standard clone method.  It creates a deep copy, therefore it also clones all the fields.
     * @return <tt>Object</tt> that is an exact copy of this <tt>WHPolygon</tt>.
     */
    public Object clone(){
	return new WHPolygon(coordinates);
    }

    //
    //PRIVATE METHODS
    //

    private void constructPath(){
	path = new GeneralPath();
	path.moveTo((float)coordinates[0].getX(),(float)coordinates[0].getY());
	for (int i=1;i<coordinates.length;i++){
	    path.lineTo((float)coordinates[i].getX(),(float)coordinates[i].getY());
	}
	path.closePath();
    }

    private boolean checkForZeroCrossing(){
	
	boolean result = false;
	if (coordinates.length>1){
	    double lon1;
	    double lon2;
	    //check for the zero crossing 
	    for (int i=1;i<coordinates.length;i++){
		//if the difference between longitudes of two points is more than 180 at least once
		//the result will be true
		lon1=coordinates[i-1].getY();
		lon2=coordinates[i].getY();
		result=(result || ((Math.abs(lon1-lon2)>180)&&(lon1!=0)&&(lon2!=0)&&(lon1!=360)&&(lon2!=360)));
		//System.out.println("Result: "+result);
	    }
	    lon1=coordinates[coordinates.length-1].getY();
	    lon2=coordinates[0].getY();
	    result=(result || ((Math.abs(lon1-lon2)>180)&&(lon1!=0)&&(lon2!=0)&&(lon1!=360)&&(lon2!=360)));
	    //System.out.println("Result: "+result);
	    if (result){
		//set up half-polies
		Vector coordinatesL=new Vector();
		Vector coordinatesR=new Vector();
		boolean left = (coordinates[0].getY()>180);  
		
		Point2D.Double p1;
		Point2D.Double p2;
		double lat1;
		double lat2;
		
		for (int i=1;i<=coordinates.length;i++){
		    p1=coordinates[(i-1)%coordinates.length];
		    p2=coordinates[(i)%coordinates.length];
		    
		    lon1=p1.getY();
		    lon2=p2.getY();
		    lat1=p1.getX();
		    lat2=p2.getX();

		    if (Math.abs(lon1-lon2)>180){
			//zero crossing

			//find intersection of the side and Great Meridian
			double distanceP1GM=WHGeo.distance(p1,new Point2D.Double(lat1,0));
			double distanceGMP2=WHGeo.distance(new Point2D.Double(lat2,0),p2);
			double lat=((distanceP1GM/(distanceP1GM+distanceGMP2))*(lat2-lat1))+lat1;
			//System.out.println("Lat :"+lat);
			Point2D.Double cross_point = new Point2D.Double(lat,0);
			WHGeo.toWHFormat(cross_point);
			//add that point to both polygons, but change longitude to 360 on left
			coordinatesL.addElement(new Point2D.Double(cross_point.getX(),cross_point.getY()+360));
			coordinatesR.addElement(cross_point);
			//change left variable
			left = ! left;
		    }
		    if (left)
			coordinatesL.addElement(p2);
		    else
			coordinatesR.addElement(p2);
		}
		
		//change the vectors into arrays and create polies
		Point2D.Double[] left_poly_array= new Point2D.Double[coordinatesL.size()];
		coordinatesL.copyInto(left_poly_array);
		Point2D.Double[] right_poly_array= new Point2D.Double[coordinatesL.size()];
		coordinatesR.copyInto(right_poly_array);
		leftPoly = new WHPolygon(left_poly_array);
		rightPoly= new WHPolygon(right_poly_array);
	    }
	}
	return result;
    }
    private static double half_diagonal(double wid, double len){
	double angle = WHPolygon.half_diagonal_angle(wid,len);
	double c = ((new BigDecimal(wid)).divide(new BigDecimal(Math.sin(Math.toRadians(angle))),20,BigDecimal.ROUND_HALF_EVEN)).doubleValue();
	return c;
    }
    
    private static double half_diagonal_angle(double wid, double len){
	double tangent = ((new BigDecimal(wid)).divide(new BigDecimal(len),20,BigDecimal.ROUND_HALF_EVEN)).doubleValue();
	return Math.toDegrees(Math.atan(tangent));
    }
}










