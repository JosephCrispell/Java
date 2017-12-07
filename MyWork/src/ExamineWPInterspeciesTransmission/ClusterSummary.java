package ExamineWPInterspeciesTransmission;

import java.util.Calendar;
import java.util.Hashtable;

import methods.ArrayMethods;
import methods.CalendarMethods;
import methods.HashtableMethods;
import methods.MatrixMethods;

public class ClusterSummary {

	public int id;
	public Hashtable<String, LifeHistorySummary> animalLifeHistories;
	public String[] animalIds;
	
	// Isolate information
	public int[] distancesToRefBadgers;
	public double[] distancesToMRCABadgers;
	public double[] sequencingQualityForIsolatesBadgers;
	public int[] distancesToRefCattle;
	public double[] distancesToMRCACattle;
	public double[] sequencingQualityForIsolatesCattle;
	
	// Counts of different associated animals
	Hashtable<String, Integer> sampledBadgersIds = new Hashtable<String, Integer>();
	Hashtable<String, Integer> sampledCattleIds = new Hashtable<String, Integer>();
	Hashtable<String, Integer> unSampledDetectedBadgersIds = new Hashtable<String, Integer>();
	Hashtable<String, Integer> unSampledDetectedCattleIds = new Hashtable<String, Integer>();
	Hashtable<String, Integer> unSampledInconclusiveCattleIds = new Hashtable<String, Integer>();
	Hashtable<String, Integer> negativeBadgersIds = new Hashtable<String, Integer>();
	Hashtable<String, Integer> negativeCattleIds = new Hashtable<String, Integer>();
	
	// EarliestDates
	Calendar earliestDateSampledBadgerTestedPositive;
	Calendar earliestDateSampledCowTestedPositive;
	Calendar earliestDateUnSampledBadgerTestedPositive;
	Calendar earliestDateUnSampledCowTestedPositive;
	
	// Sampled herds
	Hashtable<String, int[]> sampledCattleHerdDegree = new Hashtable<String, int[]>();
	double meanSpatialDistanceOfSampledHerdsToWPMansion;
	double[] meanDegreesOfSampledHerds;

	// Sampled social groups
	Hashtable<String, int[]> sampledBadgerGroupDegree = new Hashtable<String, int[]>();
	double[] meanDegreesOfSampledGroups;
	
	// Movements between all herds involved
	Hashtable<String, Integer> herdsInvolved = new Hashtable<String, Integer>();
	AdjacencyMatrix movementsBetweenHerdsInvolved;
	Hashtable<String, Integer> sampledHerds = new Hashtable<String, Integer>();
	Hashtable<String, Integer> groupsInvolved = new Hashtable<String, Integer>();
	AdjacencyMatrix movementsBetweenGroupsInvolved;
	Hashtable<String, Integer> sampledGroups = new Hashtable<String, Integer>();
	boolean directional;
	boolean buildAdjacency;
	
	// Spatial locations of sampled herds
	Hashtable<String, int[]> breakdownHerdLocations = new Hashtable<String, int[]>();
	Hashtable<String, double[][]> breakdownHerdCentroids = new Hashtable<String, double[][]>();

	public ClusterSummary(int index, Hashtable<String, LifeHistorySummary> lifeHistories, boolean buildAdjacencyMatrix,
			boolean isAdjacencyMatrixToBeDirectional){
		
		this.id = index;
		this.animalLifeHistories = lifeHistories;
		this.animalIds = HashtableMethods.getKeysString(this.animalLifeHistories);
		this.directional = isAdjacencyMatrixToBeDirectional;
		this.buildAdjacency = buildAdjacencyMatrix;
		examineEachAnimal();
		examineMovementsOfAllCows();
		examineMovementsOfAllBadgers();

		if(this.buildAdjacency == true){
			this.movementsBetweenGroupsInvolved.noteShortestPathForSampledNodes();
			this.movementsBetweenHerdsInvolved.noteShortestPathForSampledNodes();
		}
	}
	
	// Setting methods
	public void setMeanSpatialDistanceOfSampledHerdsToWPMansion(double value){
		this.meanSpatialDistanceOfSampledHerdsToWPMansion = value;
	}
	public void setMeanDegreesOfSampledHerds(double[] values){
		this.meanDegreesOfSampledHerds = values;
	}
	public void setMeanDegreesOfSampledGroups(double[] values){
		this.meanDegreesOfSampledGroups = values;
	}
	
	// Getting methods
	public int getId(){
		return this.id;
	}
	public Hashtable<String, LifeHistorySummary> getAnimalLifeHistories(){
		return this.animalLifeHistories;
	}
	public int[] getDistancesToRefBadgers(){
		return this.distancesToRefBadgers;
	}
	public double[] getDistancesToMRCABadgers(){
		return this.distancesToMRCABadgers;
	}
	public double[] getSequencingQualityForIsolatesBadgers(){
		return this.sequencingQualityForIsolatesBadgers;
	}
	public int[] getDistancesToRefCattle(){
		return this.distancesToRefCattle;
	}
	public double[] getDistancesToMRCACattle(){
		return this.distancesToMRCACattle;
	}
	public double[] getSequencingQualityForIsolatesCattle(){
		return this.sequencingQualityForIsolatesCattle;
	}
	public int getNSampledBadgers(){
		return this.sampledBadgersIds.size();
	}
	public int getNSampledCattle(){
		return this.sampledCattleIds.size();
	}
	public int getNUnSampledDetectedBadgers(){
		return this.unSampledDetectedBadgersIds.size();
	}
	public int getNUnSampledDetectedCattle(){
		return this.unSampledDetectedCattleIds.size();
	}
	public int getNUnSampledInconclusiveCattle(){
		return this.unSampledInconclusiveCattleIds.size();
	}
	public int getNNegativeBadgers(){
		return this.negativeBadgersIds.size();
	}
	public int getNNegativeCattle(){
		return this.negativeCattleIds.size();
	}	
	public Calendar getEarliestDateSampledBadgerTestedPositive(){
		return this.earliestDateSampledBadgerTestedPositive;
	}
	public Calendar getEarliestDateSampledCowTestedPositive(){
		return this.earliestDateSampledCowTestedPositive;
	}
	public Calendar getEarliestDateUnSampledBadgerTestedPositive(){
		return this.earliestDateUnSampledBadgerTestedPositive;
	}
	public Calendar getEarliestDateUnSampledCowTestedPositive(){
		return this.earliestDateUnSampledCowTestedPositive;
	}
	public Hashtable<String, int[]> getSampledCattleHerdDegree(){
		return this.sampledCattleHerdDegree;
	}
	public double getMeanSpatialDistanceOfSampledHerdsToWP(){
		return this.meanSpatialDistanceOfSampledHerdsToWPMansion;
	}
	public double[] getMeanDegreesOfSampledHerds(){
		return this.meanDegreesOfSampledHerds;
	}
	public Hashtable<String, int[]> getSampledBadgerGroupDegree(){
		return this.sampledBadgerGroupDegree;
	}
	public double[] getMeanDegreesOfSampledGroups(){
		return this.meanDegreesOfSampledGroups;
	}
	public double getMeanShortestPathLengthBetweenSampledGroups(){
		return this.movementsBetweenGroupsInvolved.getMeanShortestPathLength();
	}
	public double getMeanShortestPathLengthBetweenSampledHerds(){
		return this.movementsBetweenHerdsInvolved.getMeanShortestPathLength();
	}
	public double getProportionShortestPathsBetweenSampledGroupsPresent(){
		return this.movementsBetweenGroupsInvolved.getProportionsPathsPresent();
	}
	public double getProportionShortestPathsBetweenSampledHerdsPresent(){
		return this.movementsBetweenHerdsInvolved.getProportionsPathsPresent();
	}
	public Hashtable<String, int[]> getBreakdownHerdLocations(){
		return this.breakdownHerdLocations;
	}
	public Hashtable<String, double[][]> getBreakdownHerdCentroids(){
		return this.breakdownHerdCentroids;
	}
	public int getNSampledGroups(){
		return this.sampledGroups.size();
	}
	public int getNSampledHerds(){
		return this.sampledHerds.size();
	}
	
	// General Methods
	public void examineMovementsOfIndividualCow(String[] cphs, int index){
		
		for(int i = 0; i < cphs.length; i++){
						
			if(this.buildAdjacency == false && this.sampledCattleHerdDegree.get(cphs[i]) != null){
				this.sampledCattleHerdDegree.get(cphs[i])[index]++;
			}			
			
			if(this.buildAdjacency == true && i != 0){
				this.movementsBetweenHerdsInvolved.addMovement(cphs[i-1], cphs[i], this.directional);
			}			
		}
	}
	
	public void examineMovementsOfIndividualBadger(String[] groups, int index){
		
		for(int i = 1; i < groups.length; i++){
						
			if(groups[i].matches("NA") == false && groups[i-1].matches("NA") == false && 
					groups[i].matches(groups[i-1]) == false){
				
				if(this.buildAdjacency == false && this.sampledBadgerGroupDegree.get(groups[i]) != null){
					this.sampledBadgerGroupDegree.get(groups[i])[index]++;
				}
				
				if(this.buildAdjacency == false && this.sampledBadgerGroupDegree.get(groups[i-1]) != null){
					this.sampledBadgerGroupDegree.get(groups[i-1])[index]++;
				}
				
				if(this.buildAdjacency == true){
					this.movementsBetweenGroupsInvolved.addMovement(groups[i-1], groups[i], this.directional);
				}			
			}			
		}
	}
	
	public void examineEachAnimal(){
		
		// Initialise a LifeHistory variable
		LifeHistorySummary lifeHistory;
		
		// Initialise necessary variables
		int[] clusterSpecificIsolateIndices;
		
		// Initialise a variable to note cow's test history
		boolean[] infectionDetected;
		
		// Initialise isolate summaries
		this.distancesToRefBadgers = new int[this.animalIds.length];
		this.distancesToMRCABadgers = new double[this.animalIds.length];
		this.sequencingQualityForIsolatesBadgers = new double[this.animalIds.length];
		int isolateIndexBadgers = -1;
		this.distancesToRefCattle = new int[this.animalIds.length];
		this.distancesToMRCACattle = new double[this.animalIds.length];
		this.sequencingQualityForIsolatesCattle = new double[this.animalIds.length];
		int isolateIndexCattle = -1;
		
		// Initialise earliest dates
		this.earliestDateSampledBadgerTestedPositive = null;
		this.earliestDateSampledCowTestedPositive = null;
		this.earliestDateUnSampledBadgerTestedPositive = null;
		this.earliestDateUnSampledCowTestedPositive = null;
		String result;
		String[] testResults;
		Calendar[] testDates;
		
		for(String animalId : this.animalIds){
			
			// Get the life History of the current animal
			lifeHistory = this.animalLifeHistories.get(animalId);
			
			// Sampled animal
			if(lifeHistory.sampled() == true){
				
				// Note whether isolate is from a badger or cow
				if(lifeHistory.getSpecies().matches("BADGER") == true){
					
					// Check infection detection date
					if(earliestDateSampledBadgerTestedPositive != null && CalendarMethods.before(lifeHistory.getDetectionDate(), earliestDateSampledBadgerTestedPositive) == true){
						this.earliestDateSampledBadgerTestedPositive = lifeHistory.getDetectionDate();
					}else if(earliestDateSampledBadgerTestedPositive == null){
						this.earliestDateSampledBadgerTestedPositive = lifeHistory.getDetectionDate();
					}
					
					this.sampledBadgersIds.put(animalId, 1);
					
					// Examine the sampled groups
					if(lifeHistory.getSampledGroups() != null){
						
						for(String groupName : lifeHistory.getSampledGroups()){
							if(this.buildAdjacency == false && this.sampledGroups.get(groupName) == null){
								this.sampledGroups.put(groupName, 1);
							}
							if(this.buildAdjacency == true && this.sampledGroups.get(groupName) == null){
								this.sampledGroups.put(groupName, 1);
								this.groupsInvolved.put(groupName, 1);
							}
						}						
					}
				
				// Dealing with a cow
				}else{
					
					this.sampledCattleIds.put(animalId, 1);
					
					// Check infection detection date
					if(lifeHistory.getTestDates() != null){
						testDates = lifeHistory.getTestDates();
						testResults = lifeHistory.getTestResults();
						
						for(int i = 0; i < testResults.length; i++){
							
							result = testResults[i];
							
							if(result.matches("SL") == true || result.matches("R") == true){
								
								if(earliestDateSampledCowTestedPositive != null && 
										CalendarMethods.before(testDates[i], earliestDateSampledCowTestedPositive) == true){
									this.earliestDateSampledCowTestedPositive = testDates[i];
								}else if(earliestDateSampledCowTestedPositive == null){
									this.earliestDateSampledCowTestedPositive = testDates[i];
								}
								
								break;
							}
						}
					}else if(lifeHistory.getDetectionDate() != null){
						if(earliestDateSampledCowTestedPositive != null && 
								CalendarMethods.before(lifeHistory.getDetectionDate(), earliestDateSampledCowTestedPositive) == true){
							this.earliestDateSampledCowTestedPositive = lifeHistory.getDetectionDate();
						}else if(earliestDateSampledCowTestedPositive == null){
							this.earliestDateSampledCowTestedPositive = lifeHistory.getDetectionDate();
						}
					}
					
					// Note the sampled herd
					if(lifeHistory.getBreakdownCph() != null){
						
						if(this.buildAdjacency == false && this.sampledHerds.get(lifeHistory.getBreakdownCph()) == null){
							sampledHerds.put(lifeHistory.getBreakdownCph(), 1);
						}						
						
						if(this.buildAdjacency == true && this.sampledHerds.get(lifeHistory.getBreakdownCph()) == null){
							sampledHerds.put(lifeHistory.getBreakdownCph(), 1);
							this.herdsInvolved.put(lifeHistory.getBreakdownCph(), 1);
						}
					}					
				}
				
				// Note the isolate index associated with the current cluster
				clusterSpecificIsolateIndices = lifeHistory.getIndicesOfClusters(this.id);
				
				// Examine each of the current animal's isolates that are associated with the current cluster
				for(int index : clusterSpecificIsolateIndices){
					
					if(lifeHistory.getSpecies().matches("BADGER") == true){
						
						// Increment the isolate index
						isolateIndexBadgers++;
						
						this.distancesToRefBadgers[isolateIndexBadgers] = lifeHistory.getDistanceToRef(index);
						this.distancesToMRCABadgers[isolateIndexBadgers] = lifeHistory.getDistanceToMRCA(index);
						this.sequencingQualityForIsolatesBadgers[isolateIndexBadgers] = lifeHistory.getSequenceQuality(index);
					}else{
						// Increment the isolate index
						isolateIndexCattle++;
						
						this.distancesToRefCattle[isolateIndexCattle] = lifeHistory.getDistanceToRef(index);
						this.distancesToMRCACattle[isolateIndexCattle] = lifeHistory.getDistanceToMRCA(index);
						this.sequencingQualityForIsolatesCattle[isolateIndexCattle] = lifeHistory.getSequenceQuality(index);
					}
				}
			
			// Unsampled animal
			}else{
				
				// BADGERS - Was the infection ever detected?
				if(lifeHistory.getSpecies().matches("BADGER") == true){
					
					if(lifeHistory.getDetectionDate() != null){
						
						// Check infection detection date
						if(earliestDateUnSampledBadgerTestedPositive != null && CalendarMethods.before(lifeHistory.getDetectionDate(), earliestDateUnSampledBadgerTestedPositive) == true){
							this.earliestDateUnSampledBadgerTestedPositive = lifeHistory.getDetectionDate();
						}else if(earliestDateUnSampledBadgerTestedPositive == null){
							this.earliestDateUnSampledBadgerTestedPositive = lifeHistory.getDetectionDate();
						}
						
						this.unSampledDetectedBadgersIds.put(animalId, 1);
					}else{
						this.negativeBadgersIds.put(animalId, 1);
					}
				
				// CATTLE - Was the infection ever detected?
				}else if(lifeHistory.getTestResults() != null){
					
					infectionDetected = new boolean[2];
					infectionDetected[0] = false; // Reactor
					infectionDetected[1] = false; // Inconclusive
					
					// Check infection detection date
					testDates = lifeHistory.getTestDates();
					testResults = lifeHistory.getTestResults();
					
					for(int i = 0; i < testResults.length; i++){
						
						result = testResults[i];
						if(result.matches("SL") == true || result.matches("R") == true){
							infectionDetected[0] = true;
							
							if(earliestDateUnSampledCowTestedPositive != null && CalendarMethods.before(testDates[i], earliestDateUnSampledCowTestedPositive) == true){
								this.earliestDateUnSampledCowTestedPositive = testDates[i];
							}else if(earliestDateUnSampledCowTestedPositive == null){
								this.earliestDateUnSampledCowTestedPositive = testDates[i];
							}
							
							break;
						}else if(result.matches("IR") == true){
							infectionDetected[1] = true;
						}
					}
					
					if(infectionDetected[0] == true){
						this.unSampledDetectedCattleIds.put(animalId, 1);
					
					}else if(infectionDetected[1] == true){
						this.unSampledInconclusiveCattleIds.put(animalId, 1);

					}else{
						this.negativeCattleIds.put(animalId, 1);
					}
					
				}else{
					
					this.negativeCattleIds.put(animalId, 1);
				}
			}
			
			// Examine the movements of each animal
			if(this.buildAdjacency == true && lifeHistory.getGroupIds() != null){
				
				if(lifeHistory.getSpecies().matches("BADGER") == true){
					for(String groupName : lifeHistory.getGroupIds()){
						if(this.groupsInvolved.get(groupName) == null){
							this.groupsInvolved.put(groupName, 1);
						}
					}
				}else{
					for(String herdId : lifeHistory.getGroupIds()){
						if(this.herdsInvolved.get(herdId) == null){
							this.herdsInvolved.put(herdId, 1);
						}
					}
				}
			}
		}
		
		// Subset the isolate data
		this.distancesToRefBadgers = ArrayMethods.subset(this.distancesToRefBadgers, 0, isolateIndexBadgers);
		this.distancesToMRCABadgers = ArrayMethods.subset(this.distancesToMRCABadgers, 0, isolateIndexBadgers);
		this.sequencingQualityForIsolatesBadgers = ArrayMethods.subset(this.sequencingQualityForIsolatesBadgers, 0, isolateIndexBadgers);
		this.distancesToRefCattle = ArrayMethods.subset(this.distancesToRefCattle, 0, isolateIndexCattle);
		this.distancesToMRCACattle = ArrayMethods.subset(this.distancesToMRCACattle, 0, isolateIndexCattle);
		this.sequencingQualityForIsolatesCattle = ArrayMethods.subset(this.sequencingQualityForIsolatesCattle, 0, isolateIndexCattle);

		// Initialise the movement adjacency matrices
		if(this.buildAdjacency == true){
			this.movementsBetweenHerdsInvolved = new AdjacencyMatrix(HashtableMethods.getKeysString(this.herdsInvolved));
			this.movementsBetweenHerdsInvolved.setSampledGroups(HashtableMethods.getKeysString(this.sampledHerds));
			this.movementsBetweenGroupsInvolved = new AdjacencyMatrix(HashtableMethods.getKeysString(this.groupsInvolved));
			this.movementsBetweenGroupsInvolved.setSampledGroups(HashtableMethods.getKeysString(this.sampledGroups));
		}		
	}

	public void extractMovementHistoryForIndividualCow(String id, int index){
		
		// Initialise an array to store the degrees and X and Y
		int[] info;
		
		// Get the life History of the current animal
		LifeHistorySummary lifeHistory = this.animalLifeHistories.get(id);
			
		// Store the locations of sampled herds
		if(lifeHistory.getBreakdownCph() != null){
			
			info = new int[2];
			info[0] = lifeHistory.getBreakdownX();
			info[1] = lifeHistory.getBreakdownY();
			
			if(this.breakdownHerdLocations.get(lifeHistory.getBreakdownCph()) == null){
				this.breakdownHerdLocations.put(lifeHistory.getBreakdownCph(), info);
			}
			
			if(lifeHistory.getBreakdownLandParcelCentroids() != null){
				this.breakdownHerdCentroids.put(lifeHistory.getBreakdownCph(), 
						lifeHistory.getBreakdownLandParcelCentroids());
			}			
			
			if(this.buildAdjacency == false){
				info = new int[6];
				info[4] = lifeHistory.getBreakdownX();
				info[5] = lifeHistory.getBreakdownY();
				
				if(this.sampledCattleHerdDegree.get(lifeHistory.getBreakdownCph()) == null){
					this.sampledCattleHerdDegree.put(lifeHistory.getBreakdownCph(), info);
				}
			}
		}
		
		// Examine the movements of the current cow
		if(lifeHistory.getGroupIds() != null){
			examineMovementsOfIndividualCow(lifeHistory.getGroupIds(), index);
		}
	}
	
	public void extractMovementHistoryForIndividualBadger(String id, int index){
		
		// Initialise an array to store the current badgers sampled groups
		String[] sampledGroups;
		
		// Get the life History of the current animal
		LifeHistorySummary lifeHistory = this.animalLifeHistories.get(id);
			
		// Have we encountered any of the current badgers's sampled group?
		if(this.buildAdjacency == false && index == 0 && lifeHistory.getSampledGroups() != null){
			
			sampledGroups = lifeHistory.getSampledGroups();
			
			for(String group : sampledGroups){
				if(sampledBadgerGroupDegree.get(group) == null){
					sampledBadgerGroupDegree.put(group, new int[3]);
				}
			}			
		}					
					
		// Examine the movements of the current badger
		if(lifeHistory.getGroupIds() != null){
			examineMovementsOfIndividualBadger(lifeHistory.getGroupIds(), index);
		}
	}
	
	public void examineMovementsOfAllBadgers(){
		
		// Sampled cattle
		for(String id : HashtableMethods.getKeysString(this.sampledBadgersIds)){
			extractMovementHistoryForIndividualBadger(id, 0);			
		}
		
		// Reactor cattle
		for(String id : HashtableMethods.getKeysString(this.unSampledDetectedBadgersIds)){
			extractMovementHistoryForIndividualBadger(id, 1);			
		}
		
		// Negative cattle
		for(String id : HashtableMethods.getKeysString(this.negativeBadgersIds)){
			extractMovementHistoryForIndividualBadger(id, 2);			
		}
	}
	
	public void examineMovementsOfAllCows(){
		
		// Sampled cattle
		for(String id : HashtableMethods.getKeysString(this.sampledCattleIds)){
			extractMovementHistoryForIndividualCow(id, 0);			
		}
		
		// Reactor cattle
		for(String id : HashtableMethods.getKeysString(this.unSampledDetectedCattleIds)){
			extractMovementHistoryForIndividualCow(id, 1);			
		}
		
		// Inconclusive cattle
		for(String id : HashtableMethods.getKeysString(this.unSampledInconclusiveCattleIds)){
			extractMovementHistoryForIndividualCow(id, 2);			
		}
		
		// Negative cattle
		for(String id : HashtableMethods.getKeysString(this.negativeCattleIds)){
			extractMovementHistoryForIndividualCow(id, 3);			
		}
	}
}
