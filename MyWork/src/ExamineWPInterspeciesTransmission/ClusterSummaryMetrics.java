package ExamineWPInterspeciesTransmission;

import java.util.Calendar;

import methods.ArrayMethods;

public class ClusterSummaryMetrics {

	int id;
	
	double[] meanDistanceToRef;
	double[] meanSequenceQuality;
	
	int[][] nSampled;
	int[][] nUnSampledDetected;
	int[] nUnSampledInconclusive;
	int[][] nNegative;
	
	Calendar[][] earliestDetectionDateOfSampled;
	Calendar[][] earliestDetectionDateOfUnSampled;
	
	double[] meanSpatialDistance;
	
	double[][] meanShortestPathLength;
	double[][] proportionShortestPathsThatExist;
	
	int[][] nSampledLocations;
	
	int index = -1;
	
	public ClusterSummaryMetrics(ClusterSummary cluster, int nRepeats){
		
		this.index++;

		this.id = cluster.getId();
		
		this.meanDistanceToRef = new double[2];
		this.meanDistanceToRef[0] = ArrayMethods.mean(cluster.getDistancesToRefBadgers());
		this.meanDistanceToRef[1] = ArrayMethods.mean(cluster.getDistancesToRefCattle());
		this.meanSequenceQuality = new double[2];
		this.meanSequenceQuality[0] = ArrayMethods.mean(cluster.getSequencingQualityForIsolatesBadgers());
		this.meanSequenceQuality[1] = ArrayMethods.mean(cluster.getSequencingQualityForIsolatesCattle());
		
		this.nSampled = new int[nRepeats + 1][2];
		this.nSampled[this.index][0] = cluster.getNSampledBadgers();
		this.nSampled[this.index][1] = cluster.getNSampledCattle();
		this.nUnSampledDetected = new int[nRepeats + 1][2];
		this.nUnSampledDetected[this.index][0] = cluster.getNUnSampledDetectedBadgers();
		this.nUnSampledDetected[this.index][1] = cluster.getNUnSampledDetectedCattle();
		this.nUnSampledInconclusive = new int[nRepeats + 1];
		this.nUnSampledInconclusive[this.index] = cluster.getNUnSampledInconclusiveCattle();
		this.nNegative = new int[nRepeats + 1][2];
		this.nNegative[this.index][0] = cluster.getNNegativeBadgers();
		this.nNegative[this.index][1] = cluster.getNNegativeCattle();
		
		this.earliestDetectionDateOfSampled = new Calendar[nRepeats + 1][2];
		this.earliestDetectionDateOfSampled[this.index][0] = cluster.getEarliestDateSampledBadgerTestedPositive();
		this.earliestDetectionDateOfSampled[this.index][1] = cluster.getEarliestDateSampledCowTestedPositive();
		this.earliestDetectionDateOfUnSampled = new Calendar[nRepeats + 1][2];
		this.earliestDetectionDateOfUnSampled[this.index][0] = cluster.getEarliestDateUnSampledBadgerTestedPositive();
		this.earliestDetectionDateOfUnSampled[this.index][1] = cluster.getEarliestDateUnSampledCowTestedPositive();
		
		this.meanSpatialDistance= new double[nRepeats + 1];
		this.meanSpatialDistance[this.index] = cluster.getMeanSpatialDistanceOfSampledHerdsToWP();
		
		this.meanShortestPathLength = new double[nRepeats + 1][2];
		this.meanShortestPathLength[this.index][0] = cluster.getMeanShortestPathLengthBetweenSampledGroups();
		this.meanShortestPathLength[this.index][1] = cluster.getMeanShortestPathLengthBetweenSampledHerds();
		this.proportionShortestPathsThatExist = new double[nRepeats + 1][2];
		this.proportionShortestPathsThatExist[this.index][0] = cluster.getProportionShortestPathsBetweenSampledGroupsPresent();
		this.proportionShortestPathsThatExist[this.index][1] = cluster.getProportionShortestPathsBetweenSampledHerdsPresent();
		
		this.nSampledLocations = new int[nRepeats + 1][2];
		this.nSampledLocations[this.index][0] = cluster.getNSampledGroups();
		this.nSampledLocations[this.index][1] = cluster.getNSampledHerds();
	}

	// Setting methods
	public void addSummaryMetricsForCluster(ClusterSummary cluster){
		
		this.index++;

		this.nSampled[this.index][0] = cluster.getNSampledBadgers();
		this.nSampled[this.index][1] = cluster.getNSampledCattle();
		this.nUnSampledDetected[this.index][0] = cluster.getNUnSampledDetectedBadgers();
		this.nUnSampledDetected[this.index][1] = cluster.getNUnSampledDetectedCattle();
		this.nUnSampledInconclusive[this.index] = cluster.getNUnSampledInconclusiveCattle();
		this.nNegative[this.index][0] = cluster.getNNegativeBadgers();
		this.nNegative[this.index][1] = cluster.getNNegativeCattle();
		
		this.earliestDetectionDateOfSampled[this.index][0] = cluster.getEarliestDateSampledBadgerTestedPositive();
		this.earliestDetectionDateOfSampled[this.index][1] = cluster.getEarliestDateSampledCowTestedPositive();
		this.earliestDetectionDateOfUnSampled[this.index][0] = cluster.getEarliestDateUnSampledBadgerTestedPositive();
		this.earliestDetectionDateOfUnSampled[this.index][1] = cluster.getEarliestDateUnSampledCowTestedPositive();
		
		this.meanSpatialDistance[this.index] = cluster.getMeanSpatialDistanceOfSampledHerdsToWP();

		this.meanShortestPathLength[this.index][0] = cluster.getMeanShortestPathLengthBetweenSampledGroups();
		this.meanShortestPathLength[this.index][1] = cluster.getMeanShortestPathLengthBetweenSampledHerds();
		this.proportionShortestPathsThatExist[this.index][0] = cluster.getProportionShortestPathsBetweenSampledGroupsPresent();
		this.proportionShortestPathsThatExist[this.index][1] = cluster.getProportionShortestPathsBetweenSampledHerdsPresent();
		
		this.nSampledLocations[this.index][0] = cluster.getNSampledGroups();
		this.nSampledLocations[this.index][1] = cluster.getNSampledHerds();
	}

	// Getting methods
	public int getOriginalClusterId(){
		return this.id;
	}
	
	public double getMeanDistanceOfIsolatesToRef(int index){
		
		return this.meanDistanceToRef[index];
	}
	public double getMeanSequenceQualityOfIsolates(int index){

		return this.meanSequenceQuality[index];
	}

	public int[] getNSampled(int index){
		
		int[] output = new int[this.nSampled.length];
		for(int i = 0; i <this.nSampled.length; i++){
			output[i] = this.nSampled[i][index];
		}
		
		return output;
	}
	public int[] getNUnSampledDetected(int index){
		
		int[] output = new int[this.nUnSampledDetected.length];
		for(int i = 0; i <this.nUnSampledDetected.length; i++){
			output[i] = this.nUnSampledDetected[i][index];
		}
		
		return output;
	}
	public int[] getNUnSampledInconclusive(){
		
		return this.nUnSampledInconclusive;
	}
	public int[] getNNegative(int index){
		
		int[] output = new int[this.nNegative.length];
		for(int i = 0; i <this.nNegative.length; i++){
			output[i] = this.nNegative[i][index];
		}
		
		return output;
	}
	
	public Calendar[] getEarliestDetectionDateOfSampled(int index){
		
		Calendar[] output = new Calendar[this.earliestDetectionDateOfSampled.length];
		for(int i = 0; i <this.earliestDetectionDateOfSampled.length; i++){
			output[i] = this.earliestDetectionDateOfSampled[i][index];
		}
		
		return output;
	}
	public Calendar[] getEarliestDetectionDateOfUnSampled(int index){
		
		Calendar[] output = new Calendar[this.earliestDetectionDateOfUnSampled.length];
		for(int i = 0; i <this.earliestDetectionDateOfUnSampled.length; i++){
			output[i] = this.earliestDetectionDateOfUnSampled[i][index];
		}
		
		return output;
	}
	
	public double[] getMeanSpatialDistance(){
		
		return this.meanSpatialDistance;
	}

	public double[] getMeanShortestPathLength(int index){
		
		double[] output = new double[this.meanShortestPathLength.length];
		for(int i = 0; i < this.meanShortestPathLength.length; i++){
			output[i] = this.meanShortestPathLength[i][index];
		}
		
		return output;
	}
	public double[] getProportionShortestPathsThatExist(int index){
		
		double[] output = new double[this.proportionShortestPathsThatExist.length];
		for(int i = 0; i < this.proportionShortestPathsThatExist.length; i++){
			output[i] = this.proportionShortestPathsThatExist[i][index];
		}
		
		return output;
	}

	public int[] getNSampledLocations(int index){
		
		int[] output = new int[this.nSampledLocations.length];
		for(int i = 0; i <this.nSampledLocations.length; i++){
			output[i] = this.nSampledLocations[i][index];
		}
		
		return output;
	}
}
