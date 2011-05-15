package com.wherehoo;

import java.awt.geom.Point2D;

public class WHSearchArea{


    private WHPolygon poly;
    private double h;
    private Point2D.Double cl;
    private double hdg;

    protected WHSearchArea(WHPolygon search_poly, Point2D.Double client_location, double height, double heading){
	poly = search_poly;
	h = height;
	cl = client_location;
	hdg = heading;
    }

    protected WHPolygon getPoly(){
	return poly;
    }
    protected double getHeight(){
	return h;
    }
    protected Point2D.Double getClientLocation(){
	return cl;
    }
    protected double getHeading(){
	return hdg;
    }
    protected double distance(WHPolygon point_of_interest){
	return WHGeo.distance(cl,point_of_interest);
    }
    
    protected double heading(WHPolygon point_of_interest){
	return WHGeo.heading(cl,point_of_interest);
    }

    protected double bearing(WHPolygon point_of_interest){
	double temp_heading = this.heading(point_of_interest);
	return (((temp_heading - hdg)+360)%360);
    }
}
