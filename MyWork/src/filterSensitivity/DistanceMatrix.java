package filterSensitivity;
public class DistanceMatrix {

    public String[] sampleNames;
    public double[][] distanceMatrix;
    
    public DistanceMatrix(String[] samples, double[][] inputDistanceMatrix){
    	this.sampleNames = samples;
	   	this.distanceMatrix = inputDistanceMatrix;
	}
    
    // Methods for Setting
    public void setSampleNames(String[] samples){
    	this.sampleNames = samples;
    }
    public void setDistanceMatrix(double[][] inputDistanceMatrix){
    	this.distanceMatrix = inputDistanceMatrix;
    }
     
    // Methods for Getting
    public String[] getSampleNames(){
   	 	return sampleNames;
    }
    public double[][] getDistanceMatrix(){
   	 	return distanceMatrix;
    }
}
