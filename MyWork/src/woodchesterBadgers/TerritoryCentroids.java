package woodchesterBadgers;

import java.util.Hashtable;

import methods.HashtableMethods;

public class TerritoryCentroids {

	String name;
	Hashtable<String, double[]> centroidsPerYear = new Hashtable<String, double[]>();
	double[] meanCentroid = new double[2];
	
	public TerritoryCentroids(String id, Hashtable<String, double[]> centroids){
		
		this.name = id;
		this.centroidsPerYear = centroids;
		calculateMeanCentroid();
	}

	// Setting methods
	public void calculateMeanCentroid(){
		
		// Get the array of years (keys)
		String[] keys = HashtableMethods.getKeysString(this.centroidsPerYear);
		double[] centroid;
		
		// Examine each centroid
		for(String year : keys){
			
			centroid = this.centroidsPerYear.get(year);
			
			this.meanCentroid[0] += centroid[0];
			this.meanCentroid[1] += centroid[1];
		}
		
		this.meanCentroid[0] = this.meanCentroid[0] / (double) keys.length;
		this.meanCentroid[1] = this.meanCentroid[1] / (double) keys.length;
	}
	
	// Getting methods
	public double[] getCoords(String year){
		double[] coords = this.meanCentroid;
		if(this.centroidsPerYear.get(year) != null){
			coords = this.centroidsPerYear.get(year);
		}
		
		return coords;
	}
	
	public String getName() {
		return name;
	}

	public Hashtable<String, double[]> getCentroidsPerYear() {
		return centroidsPerYear;
	}

	public double[] getMeanCentroid() {
		return meanCentroid;
	}	
}
