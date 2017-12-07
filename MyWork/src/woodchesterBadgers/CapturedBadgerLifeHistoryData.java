package woodchesterBadgers;

import java.util.Hashtable;

import methods.HashtableMethods;

public class CapturedBadgerLifeHistoryData {
	
	public Hashtable<String, CaptureData> badgerCaptureHistories;
	public Hashtable<String, Integer> badgerGroupIndices;
	public int[][] groupAdjacencyMatrix;
	Hashtable<String, SampleInfo> sampledIsolateInfo;
	Hashtable<String, TerritoryCentroids> territoryCentroids;
	public Hashtable<Integer, int[][]> shortestPathsBetweenGroups;
	public int[][] nSharedBadgersBetweenGroups;
	public String[] capturedBadgerTattoos;
	public double[][] geneticRelatedness;
	
	public CapturedBadgerLifeHistoryData(Hashtable<String, CaptureData> captureData, Hashtable<String, Integer> indexedGroups,
			int[][] adjacency, Hashtable<String, SampleInfo> samplingInfo, Hashtable<String, TerritoryCentroids> centroids,
			int[][] nShared, double[][] relatedness){
		
		this.badgerCaptureHistories = captureData;
		this.capturedBadgerTattoos = HashtableMethods.getKeysString(captureData);
		this.badgerGroupIndices = indexedGroups;
		this.groupAdjacencyMatrix = adjacency;
		this.sampledIsolateInfo = samplingInfo;
		this.territoryCentroids = centroids;
		this.nSharedBadgersBetweenGroups = nShared;
		this.geneticRelatedness = relatedness;
	}
	
	// Setting Methods
	public void setGeneticRelatedness(double[][] geneticRelatedness) {
		this.geneticRelatedness = geneticRelatedness;
	}
	public void setBadgerCaptureHistories(Hashtable<String, CaptureData> captureData){
		this.badgerCaptureHistories = captureData;
	}
	public void setBadgerGroupIndices(Hashtable<String, Integer> indexedGroups){
		this.badgerGroupIndices = indexedGroups;
	}
	public void setGroupAdjacencyMatrix(int[][] adjacency){
		this.groupAdjacencyMatrix = adjacency;
	}
	public void setSampledIsolateInfo(Hashtable<String, SampleInfo> samplingInfo){
		this.sampledIsolateInfo = samplingInfo;
	}
	public void setTerritoryCentroids(Hashtable<String, TerritoryCentroids> centroids){
		this.territoryCentroids = centroids;
	}
	public void setShortestPathsBetweenGroups(Hashtable<Integer, int[][]> shortestPaths){
		this.shortestPathsBetweenGroups = shortestPaths;
	}
	public void setNSharedBadgersBetweenGroups(int[][] nShared){
		this.nSharedBadgersBetweenGroups = nShared;
	}
	
	// Getting Methods
	public double[][] getGeneticRelatedness() {
		return geneticRelatedness;
	}
	public Hashtable<String, CaptureData> getBadgerCaptureHistories(){
		return this.badgerCaptureHistories;
	}
	public Hashtable<String, Integer> getBadgerGroupIndices(){
		return this.badgerGroupIndices;
	}
	public int[][] getGroupAdjacencyMatrix(){
		return this.groupAdjacencyMatrix;
	}
	public Hashtable<String, SampleInfo> getSampledIsolateInfo(){
		return this.sampledIsolateInfo;
	}
	public Hashtable<String, TerritoryCentroids> getTerritoryCentroids(){
		return this.territoryCentroids;
	}
	public Hashtable<Integer, int[][]> getShortestPathsBetweenGroups(){
		return this.shortestPathsBetweenGroups;
	}
	public int[][] getNSharedBadgersBetweenGroups(){
		return this.nSharedBadgersBetweenGroups;
	}
	public String[] getCapturedBadgerTattoos(){
		return this.capturedBadgerTattoos;
	}
}
