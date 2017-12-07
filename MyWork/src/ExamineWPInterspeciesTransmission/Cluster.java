package ExamineWPInterspeciesTransmission;

public class Cluster {

	public int id;
	public String[] isolateIds;
	public int[] distancesToRef;
	public double[] distancesToMRCA;
	public double[] sequencingQualityForIsolates;
	
	public Cluster(int index, String[] names){
		this.id = index;
		this.isolateIds = names;
	}
	
	// Setting methods
	public void setDistancesToRef(int[] distances){
		this.distancesToRef = distances;
	}
	public void setDistancesToMRCA(double[] distances){
		this.distancesToMRCA = distances;
	}
	public void setSequencingQualityForIsolates(double[] qualityValues){
		this.sequencingQualityForIsolates = qualityValues;
	}
	
	
	// Getting methods
	public int getId(){
		return this.id;
	}
	public String[] getIsolateIds(){
		return this.isolateIds;
	}
	public int[] getDistancesToRef(){
		return this.distancesToRef;
	}
	public double[] getDistancesToMRCA(){
		return this.distancesToMRCA;
	}
	public double[] getSequencingQualityForIsolates(){
		return this.sequencingQualityForIsolates;
	}
	
	// General Methods
	public static Cluster[] append(Cluster[] array, Cluster value){
		Cluster[] newArray = new Cluster[array.length + 1];
		
		for(int index = 0; index < array.length; index++){
			newArray[index] = array[index];
		}
		newArray[newArray.length - 1] = value;
		
		return newArray;
	}

	public static Cluster[] subset(Cluster[] array, int start, int end){
		Cluster[] part = new Cluster[end - start + 1];
		
		int pos = -1;
		for(int index = 0; index < array.length; index++){
			
			if(index >= start && index <= end){
				pos++;
				part[pos] = array[index];
			}
		}
		
		return part;
	}
}
