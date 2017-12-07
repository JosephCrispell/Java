package ExamineWPInterspeciesTransmission;

import java.util.Calendar;
import java.util.Hashtable;

import methods.ArrayMethods;
import methods.CalendarMethods;
import methods.HashtableMethods;

public class LifeHistorySummary {

	public String animalId;
	public String species;
	
	// Infection information
	public Calendar detectionDate;
	public Calendar[] testDates;
	public String[] testResults;
	
	// Un-sampled cattle
	public int[] associatedClusters = new int[99];
	public int associatedClusterIndex = -1;
	public boolean associatedClusterSubsetted = false;
	public ContactEvent[] contactInfo;
	
	// Sampling information
	public String[] isolateIds = new String[99];
	public Calendar[] samplingDates = new Calendar[99];
	public int[] clusters = new int[99];
	public int[] distancesToRef = new int[99];
	public double[] distancesToMRCA = new double[99];
	public double[] sequencingQualities = new double[99];
	public int isolateIndex = -1;
	public boolean isolateInfoSubsetted = false;
	
	// Movement information
	public double[][] coordinates = new double[999][2];
	public Calendar[] movementDates = new Calendar[999];
	public String[] premisesTypes = new String[999];
	public String[] groupIds =  new String[999];
	public int movementIndex = -1;	
	public boolean movementsSubsetted = false;
	
	// Breakdown information
	public String breakdownCph;
	public Calendar breakdownDate;
	public int breakdownX;
	public int breakdownY;
	public double[][] breakdownLandParcelCentroids;
	
	// Sample social group info
	public String[] sampledGroups;
	
	// InContactAnimals
	public String[] inContactAnimals = new String[99];
	public int inContactAnimalsIndex = -1;
	public boolean inContactAnimalsSubsetted;
	
	public LifeHistorySummary(String id, String animal){
		this.animalId = id;
		this.species = animal;
	}
	
	// Setting methods
	public void setBreakdownInfo(String cph, Calendar date, int x, int y){
		this.breakdownCph = cph;
		this.breakdownDate = date;
		this.breakdownX = x;
		this.breakdownY = y;
	}
	public void setTestInfo(Calendar[] dates, String[] results){
		this.testDates = dates;
		this.testResults = results;
	}
	public void setDetectionDate(Calendar date){
		this.detectionDate = date;
	}
	public void addMovement(double[] coordinate, Calendar date, String type, String groupId){
		this.movementIndex++;
		this.coordinates[this.movementIndex] = coordinate;
		this.movementDates[this.movementIndex] = date;
		this.premisesTypes[this.movementIndex] = type;
		this.groupIds[this.movementIndex] = groupId;
	}
	public void appendIsolate(String isolate, Calendar date, int cluster, int distanceToRef, double distanceToMRCA,
			double quality){
		this.isolateIndex++;
		
		this.isolateIds[this.isolateIndex] = isolate;
		this.samplingDates[this.isolateIndex] = date;
		this.clusters[this.isolateIndex] = cluster;
		this.distancesToRef[this.isolateIndex] = distanceToRef;
		this.distancesToMRCA[this.isolateIndex] = distanceToMRCA;
		this.sequencingQualities[this.isolateIndex] = quality;
	}
	public void addClusterAssociation(int cluster){
		this.associatedClusterIndex++;
		this.associatedClusters[this.associatedClusterIndex] = cluster;
	}
	public void setContactInfo(ContactEvent[] contacts){
		this.contactInfo = contacts;
	}
	
	public void setCoordinates(double[][] xsAndYs){
		this.coordinates = xsAndYs;
	}
	public void setMovementDates(Calendar[] dates){
		this.movementDates = dates;
	}
	public void setPremisesTypes(String[] types){
		this.premisesTypes = types;
	}
	public void setGroupIds(String[] ids){
		this.groupIds = ids;
	}
	public void setMovementsSubsetted(boolean value){
		this.movementIndex = this.movementDates.length - 1;
		this.movementsSubsetted = value;
	}
	
	public void setIsolateIds(String[] ids){
		this.isolateIds = ids;
	}
	public void setSamplingDates(Calendar[] dates){
		this.samplingDates = dates;
	}
	public void setClusters(int[] values){
		this.clusters = values;
	}
	public void setDistancesToRef(int[] distances){
		this.distancesToRef = distances;
	}
	public void setDistancesToMRCA(double[] distances){
		this.distancesToMRCA = distances;
	}
	public void setSequencingQualities(double[] values){
		this.sequencingQualities = values;
	}	
	public void setIsolateInfoSubsetted(boolean value){
		this.isolateIndex = this.isolateIds.length - 1;
		this.isolateInfoSubsetted = value;
	}
	
	public void setSampledGroups(String[] groups){
		this.sampledGroups = groups;
	}
	
	public void removeTestData(Hashtable<Integer, Integer> remove){
		
		if(this.testDates.length > remove.size()){
						
			Calendar[] newTestDates = new Calendar[this.testDates.length - remove.size()];
			String[] newTestResults = new String[this.testResults.length - remove.size()];
			int index = -1;
			
			for(int i = 0; i < this.testDates.length; i++){
				
				if(remove.get(i) == null){
					index++;
					newTestDates[index] = CalendarMethods.copy(this.testDates[i]);
					newTestResults[index] = this.testResults[i];
				}
			}
			this.testDates = newTestDates;
			this.testResults = newTestResults;
		}else{
			this.testDates = null;
			this.testResults = null;
		}		
	}
	public void removeMovementData(Hashtable<Integer, Integer> remove){
		
		if(this.movementDates.length > remove.size()){
						
			Calendar[] newMovementDates = new Calendar[this.movementDates.length - remove.size()];
			String[] newPremisesTypes = new String[this.movementDates.length - remove.size()];
			double[][] newCoordinates = new double[this.movementDates.length - remove.size()][2];
			int index = -1;
			
			for(int i = 0; i < this.movementDates.length; i++){
				
				if(remove.get(i) == null){
					index++;
					newMovementDates[index] = CalendarMethods.copy(this.movementDates[i]);
					newPremisesTypes[index] = this.premisesTypes[i];
					newCoordinates[index][0] = this.coordinates[i][0];
					newCoordinates[index][1] = this.coordinates[i][1];
				}
			}
			this.movementDates = newMovementDates;
			this.premisesTypes = newPremisesTypes;
			this.coordinates = newCoordinates;
			this.movementIndex = this.movementDates.length - 1;
		}else{
			this.movementDates = null;
			this.premisesTypes = null;
			this.coordinates = null;
			this.movementIndex = -1;
		}	
	}
	
	public void addInContactAnimal(String id){
		
		this.inContactAnimalsIndex++;
		
		if(this.inContactAnimalsIndex < this.inContactAnimals.length){
			this.inContactAnimals[this.inContactAnimalsIndex] = id;
		}else{
			String[] newArray = new String[this.inContactAnimals.length * 2];
			for(int i = 0; i < this.inContactAnimals.length; i++){
				newArray[i] = this.inContactAnimals[i];
			}
			newArray[this.inContactAnimals.length] = id;
			this.inContactAnimals = newArray;
		}
	}
	
	public void setBreakdownCphCentroids(double[][] centroids){
		this.breakdownLandParcelCentroids = centroids;
	}
	
	// Getting methods
	public String getBreakdownCph(){
		return this.breakdownCph;
	}
	public Calendar getBreakdownDate(){
		return this.breakdownDate;
	}
	public int getBreakdownX(){
		return this.breakdownX;
	}
	public int getBreakdownY(){
		return this.breakdownY;
	}
	public Calendar[] getTestDates(){
		return this.testDates;
	}
	public String[] getTestResults(){
		return this.testResults;
	}
	public int getNTests(){
		
		int nTests = 0;
		if(this.testDates != null){
			nTests = this.testDates.length;
		}
		
		return nTests;
	}
	public String getAnimalId(){
		return this.animalId;
	}
	public String getSpecies(){
		return this.species;
	}
	public Calendar getDetectionDate(){
		return this.detectionDate;
	}
	public String[] getIsolateIds(){
		
		if(this.isolateInfoSubsetted == false){
			
			this.isolateIds = ArrayMethods.subset(this.isolateIds, 0, this.isolateIndex);
			this.samplingDates = CalendarMethods.subset(this.samplingDates, 0, this.isolateIndex);
			this.clusters = ArrayMethods.subset(this.clusters, 0, this.isolateIndex);
			this.distancesToRef = ArrayMethods.subset(this.distancesToRef, 0, this.isolateIndex);
			this.distancesToMRCA = ArrayMethods.subset(this.distancesToMRCA, 0, this.isolateIndex);
			this.sequencingQualities = ArrayMethods.subset(this.sequencingQualities, 0, this.isolateIndex);
			this.isolateInfoSubsetted = true;
		}
		
		return this.isolateIds;
	}
	public Calendar[] getSamplingDates(){
		
		if(this.isolateInfoSubsetted == false){
			
			this.isolateIds = ArrayMethods.subset(this.isolateIds, 0, this.isolateIndex);
			this.samplingDates = CalendarMethods.subset(this.samplingDates, 0, this.isolateIndex);
			this.clusters = ArrayMethods.subset(this.clusters, 0, this.isolateIndex);
			this.distancesToRef = ArrayMethods.subset(this.distancesToRef, 0, this.isolateIndex);
			this.distancesToMRCA = ArrayMethods.subset(this.distancesToMRCA, 0, this.isolateIndex);
			this.sequencingQualities = ArrayMethods.subset(this.sequencingQualities, 0, this.isolateIndex);
			this.isolateInfoSubsetted = true;
		}
		
		return this.samplingDates;
	}
	public int[] getClusters(){
		
		if(this.isolateInfoSubsetted == false){
			
			this.isolateIds = ArrayMethods.subset(this.isolateIds, 0, this.isolateIndex);
			this.samplingDates = CalendarMethods.subset(this.samplingDates, 0, this.isolateIndex);
			this.clusters = ArrayMethods.subset(this.clusters, 0, this.isolateIndex);
			this.distancesToRef = ArrayMethods.subset(this.distancesToRef, 0, this.isolateIndex);
			this.distancesToMRCA = ArrayMethods.subset(this.distancesToMRCA, 0, this.isolateIndex);
			this.sequencingQualities = ArrayMethods.subset(this.sequencingQualities, 0, this.isolateIndex);
			this.isolateInfoSubsetted = true;
		}
		
		return this.clusters;
	}
	public int[] getDistancesToRef(){
		
		if(this.isolateInfoSubsetted == false){
			
			this.isolateIds = ArrayMethods.subset(this.isolateIds, 0, this.isolateIndex);
			this.samplingDates = CalendarMethods.subset(this.samplingDates, 0, this.isolateIndex);
			this.clusters = ArrayMethods.subset(this.clusters, 0, this.isolateIndex);
			this.distancesToRef = ArrayMethods.subset(this.distancesToRef, 0, this.isolateIndex);
			this.distancesToMRCA = ArrayMethods.subset(this.distancesToMRCA, 0, this.isolateIndex);
			this.sequencingQualities = ArrayMethods.subset(this.sequencingQualities, 0, this.isolateIndex);
			this.isolateInfoSubsetted = true;
		}
		
		return this.distancesToRef;
	}
	public double[] getDistancesToMRCA(){
		
		if(this.isolateInfoSubsetted == false){
			
			this.isolateIds = ArrayMethods.subset(this.isolateIds, 0, this.isolateIndex);
			this.samplingDates = CalendarMethods.subset(this.samplingDates, 0, this.isolateIndex);
			this.clusters = ArrayMethods.subset(this.clusters, 0, this.isolateIndex);
			this.distancesToRef = ArrayMethods.subset(this.distancesToRef, 0, this.isolateIndex);
			this.distancesToMRCA = ArrayMethods.subset(this.distancesToMRCA, 0, this.isolateIndex);
			this.sequencingQualities = ArrayMethods.subset(this.sequencingQualities, 0, this.isolateIndex);
			this.isolateInfoSubsetted = true;
		}
		
		return this.distancesToMRCA;
	}
	public double[] getSequencingQualities(){
		
		if(this.isolateInfoSubsetted == false){
			
			this.isolateIds = ArrayMethods.subset(this.isolateIds, 0, this.isolateIndex);
			this.samplingDates = CalendarMethods.subset(this.samplingDates, 0, this.isolateIndex);
			this.clusters = ArrayMethods.subset(this.clusters, 0, this.isolateIndex);
			this.distancesToRef = ArrayMethods.subset(this.distancesToRef, 0, this.isolateIndex);
			this.distancesToMRCA = ArrayMethods.subset(this.distancesToMRCA, 0, this.isolateIndex);
			this.sequencingQualities = ArrayMethods.subset(this.sequencingQualities, 0, this.isolateIndex);
			this.isolateInfoSubsetted = true;
		}
		
		return this.sequencingQualities;
	}
	
	public String[] getSampledGroups(){
		return this.sampledGroups;
	}
	
	public double[][] getCoordinates(){
		if(this.movementsSubsetted == false){
			this.coordinates = ArrayMethods.subset(this.coordinates, 0, this.movementIndex);
			this.movementDates = CalendarMethods.subset(this.movementDates, 0, this.movementIndex);
			this.premisesTypes = ArrayMethods.subset(this.premisesTypes, 0, this.movementIndex);
			this.groupIds = ArrayMethods.subset(this.groupIds, 0, this.movementIndex);
			this.movementsSubsetted = true;
		}
		
		return this.coordinates;
	}
	public Calendar[] getMovementDates(){
		if(this.movementsSubsetted == false){
			this.coordinates = ArrayMethods.subset(this.coordinates, 0, this.movementIndex);
			this.movementDates = CalendarMethods.subset(this.movementDates, 0, this.movementIndex);
			this.premisesTypes = ArrayMethods.subset(this.premisesTypes, 0, this.movementIndex);
			this.groupIds = ArrayMethods.subset(this.groupIds, 0, this.movementIndex);
			this.movementsSubsetted = true;
		}
		
		return this.movementDates;
	}
	public String[] getPremisesTypes(){
		if(this.movementsSubsetted == false){
			this.coordinates = ArrayMethods.subset(this.coordinates, 0, this.movementIndex);
			this.movementDates = CalendarMethods.subset(this.movementDates, 0, this.movementIndex);
			this.premisesTypes = ArrayMethods.subset(this.premisesTypes, 0, this.movementIndex);
			this.groupIds = ArrayMethods.subset(this.groupIds, 0, this.movementIndex);
			this.movementsSubsetted = true;
		}
		
		return this.premisesTypes;
	}
	public String[] getGroupIds(){
		if(this.movementsSubsetted == false){
			this.coordinates = ArrayMethods.subset(this.coordinates, 0, this.movementIndex);
			this.movementDates = CalendarMethods.subset(this.movementDates, 0, this.movementIndex);
			this.premisesTypes = ArrayMethods.subset(this.premisesTypes, 0, this.movementIndex);
			this.groupIds = ArrayMethods.subset(this.groupIds, 0, this.movementIndex);
			this.movementsSubsetted = true;
		}
		
		return this.groupIds;
	}
	
	public int getDistanceToRef(int index){
		return this.distancesToRef[index];
	}
	public double getDistanceToMRCA(int index){
		return this.distancesToMRCA[index];
	}
	public double getSequenceQuality(int index){
		return this.sequencingQualities[index];
	}
	
	public boolean movementDataAvailable(){
		boolean result = false;
		if(this.movementIndex != -1){
			result = true;
		}
		return result;
	}
	public boolean sampled(){
		boolean result = false;
		if(this.isolateIndex != -1){
			result = true;
		}
		return result;
	}
	public boolean associatedClustersAvailable(){
		boolean result = false;
		if(this.associatedClusterIndex != -1){
			result = true;
		}
		return result;
	}
	public int[] getAssociatedClusters(){
		if(this.associatedClusterSubsetted == false){
			this.associatedClusters = ArrayMethods.subset(associatedClusters, 0, this.associatedClusterIndex);
			this.associatedClusterSubsetted = false;
		}
		return this.associatedClusters;
	}
	
	public int getIsolateIndex(String isolate){
		int index = -1;
		for(int i = 0; i < this.isolateIds.length; i++){
			if(isolateIds[i].matches(isolate) == true)
				index = i;
		}
		
		return index;
	}
	
	public int[] getIndicesOfClusters(int cluster){
		
		if(this.isolateInfoSubsetted == false){
			
			this.isolateIds = ArrayMethods.subset(this.isolateIds, 0, this.isolateIndex);
			this.samplingDates = CalendarMethods.subset(this.samplingDates, 0, this.isolateIndex);
			this.clusters = ArrayMethods.subset(this.clusters, 0, this.isolateIndex);
			this.distancesToRef = ArrayMethods.subset(this.distancesToRef, 0, this.isolateIndex);
			this.distancesToMRCA = ArrayMethods.subset(this.distancesToMRCA, 0, this.isolateIndex);
			this.sequencingQualities = ArrayMethods.subset(this.sequencingQualities, 0, this.isolateIndex);
			this.isolateInfoSubsetted = true;
		}
		
		int[] indices = new int[this.clusters.length];
		int pos = -1;
		
		for(int i = 0; i < this.clusters.length; i++){
			
			if(this.clusters[i] == cluster){
				pos++;
				indices[pos] = i;
			}
		}
		
		return ArrayMethods.subset(indices, 0, pos);
	}
	public ContactEvent[] getContactInfo(){
		return this.contactInfo;
	}
	
	public boolean hasInContactAnimals(){
		boolean result = false;
		if(this.inContactAnimalsIndex != -1){
			result = true;
		}
		return result;
	}
	
	public String[] getInContactAnimals(){
		
		if(this.inContactAnimalsSubsetted == false){
			this.inContactAnimals = ArrayMethods.subset(this.inContactAnimals, 0, this.inContactAnimalsIndex);
			this.inContactAnimalsSubsetted = true;
		}
		return this.inContactAnimals;
	}
	
	public double[][] getBreakdownLandParcelCentroids(){
		return this.breakdownLandParcelCentroids;
	}
	
	// General Methods
	public static LifeHistorySummary[] append(LifeHistorySummary[] array, LifeHistorySummary value){
		LifeHistorySummary[] newArray = new LifeHistorySummary[array.length + 1];
		
		for(int index = 0; index < array.length; index++){
			newArray[index] = array[index];
		}
		newArray[newArray.length - 1] = value;
		
		return newArray;
	}
}
