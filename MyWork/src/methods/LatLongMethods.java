package methods;

public class LatLongMethods {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		double lat1 = -41.254945224448406;
		double long1 = 172.1230303586782;
		
		double lat2 = -41.25384335750062;
		double long2 = 172.12546015548168;
		
		double height1 = 20.885641237336444;
		double height2 = 10.0;
		
		double length1a = 12.924939295226086;
		double length1b = 12.924939295226103;
		
		double length2a = 10.88564123733642;
		double length2b = 10.8856412373364444;
		
		double distance = distance(lat1, long1, lat2, long2, 'K');
		
		
		
		double diffusionRate = distance / length2a;
		
		System.out.println("Length = " + (height1 - height2));
		
		
		

	}
	
	public static double distance(double lat1, double lon1, double lat2, double lon2, char unit) {
		
		// Method taken directly from: http://www.geodatasource.com/developers/java
		
		/*::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
		/*::                                                                         :*/
		/*::  This routine calculates the distance between two points (given the     :*/
		/*::  latitude/longitude of those points). It is being used to calculate     :*/
		/*::  the distance between two locations using GeoDataSource (TM) prodducts  :*/
		/*::                                                                         :*/
		/*::  Definitions:                                                           :*/
		/*::    South latitudes are negative, east longitudes are positive           :*/
		/*::                                                                         :*/
		/*::  Passed to function:                                                    :*/
		/*::    lat1, lon1 = Latitude and Longitude of point 1 (in decimal degrees)  :*/
		/*::    lat2, lon2 = Latitude and Longitude of point 2 (in decimal degrees)  :*/
		/*::    unit = the unit you desire for results                               :*/
		/*::           where: 'M' is statute miles                                   :*/
		/*::                  'K' is kilometers (default)                            :*/
		/*::                  'N' is nautical miles                                  :*/
		/*::  Worldwide cities and other features databases with latitude longitude  :*/
		/*::  are available at http://www.geodatasource.com                          :*/
		/*::                                                                         :*/
		/*::  For enquiries, please contact sales@geodatasource.com                  :*/
		/*::                                                                         :*/
		/*::  Official Web site: http://www.geodatasource.com                        :*/
		/*::                                                                         :*/
		/*::           GeoDataSource.com (C) All Rights Reserved 2014                :*/
		/*::                                                                         :*/
		/*::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
		
		double theta = 0;
		double dist = 0;		
		if(lat1 != lat2 || lon1 != lon2){
			theta = lon1 - lon2;
		  	dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
		  	dist = Math.acos(dist);
		  	dist = rad2deg(dist);
		  	dist = dist * 60 * 1.1515;
		  	if (unit == 'K') {
		  		dist = dist * 1.609344;
		  	}else if(unit == 'N') {
		  		dist = dist * 0.8684;
		  	}
		}
				
	  	return (dist);
	}
	 
	public static double deg2rad(double deg) {
		
		//  This function converts decimal degrees to radians
		return (deg * Math.PI / 180.0);
	}
	 
	public static double rad2deg(double rad) {
		
		//  This function converts radians to decimal degrees
		return (rad * 180 / Math.PI);
	}

}
