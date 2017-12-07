package ExamineWPInterspeciesTransmission;

import java.io.IOException;
import java.util.Hashtable;

import methods.ArrayMethods;
import methods.CalendarMethods;
import methods.HashtableMethods;
import woodchesterCattle.CattleIsolateLifeHistoryData;
import woodchesterCattle.IsolateData;
import woodchesterCattle.MakeEpidemiologicalComparisons;
import woodchesterCattle.Movement;
import woodchesterGeneticVsEpi.CompareIsolates;

public class ExamineCattleMovements {

	public static void main(String[] args) throws IOException{
		
		// Set the date for these files
		String date = "15-09-2016"; // Override date
		String path = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester_CattleAndBadgers/NewAnalyses_02-06-16/";
		
		// Read in the Cattle data
		String consolidatedSampledAnimalInfo = path + "IsolateData/ConsolidatedCattleIsolateData_" + date + ".txt";
		String consolidatedLocationsOfInterestFile = path + "IsolateData/CollatedCattleLocationInfo_" + date + ".txt";
		String locationAdjacencyMatrixFile = path + "IsolateData/CattleAdjacencyMatrix_" + date + ".txt";
		String locationSpatialDistanceMatrixFile = path + "IsolateData/CattleSpatialDistanceMatrix_" + date + ".txt";
		String nSharedBetweenLocationsMatrixFile = path + "IsolateData/NumberOfAnimalsSharedBetweenLocations_" + date + ".txt";
		String testHistoryFile = path + "CattleTestData/tblccdAnimal_13-09-16.txt";
		CattleIsolateLifeHistoryData cattleIsolateLifeHistoryData = MakeEpidemiologicalComparisons.collateCattleIsolateData(
				consolidatedSampledAnimalInfo, consolidatedLocationsOfInterestFile, locationAdjacencyMatrixFile,
				locationSpatialDistanceMatrixFile, nSharedBetweenLocationsMatrixFile, testHistoryFile);
		cattleIsolateLifeHistoryData.getNetworkInfo().setShortestPathsFull(CompareIsolates.findShortestPathsBetweenAllNodes(
				cattleIsolateLifeHistoryData.getNetworkInfo().getAdjacencyMatrix()));
		String[] premisesTypesToIgnore = {"SR", "CC"}; // Slaughter Houses and Collection Centres
		cattleIsolateLifeHistoryData.getNetworkInfo().setShortestPathsWithoutSelectedPremises(CompareIsolates.findShortestPathsBetweenAllNodesExcludePremiseTypes(cattleIsolateLifeHistoryData.getNetworkInfo().getAdjacencyMatrix(), cattleIsolateLifeHistoryData.getNetworkInfo().getLocations(), premisesTypesToIgnore));
		
		// Find the animals that were in the same herd at the same time as any of the sampled animals for the focus isolates
		String[] isolates = {"TB1481", "TB1782", "TB1753", "TB1805", "TB1819", "TB1473"};
		Hashtable<String, String> animalsToKeep = findAnimalIdsOfCattleThatEncounterSampledCattle(isolates,
				cattleIsolateLifeHistoryData);
		
		System.out.println("\n--------------------------------------------------------------------------------------------");
		HashtableMethods.print(animalsToKeep);
		
	}
	
	public static Hashtable<String, String> findAnimalIdsOfCattleThatEncounterSampledCattle(String[] strainIds,
			CattleIsolateLifeHistoryData cattleIsolateLifeHistoryData){
		
		// Initialise a variable to store an animal's data
		String eartag;
				
		// Initialise a hashtable to record the eartags of the animals that we are interested in
		Hashtable<String, String> animalsToKeep = new Hashtable<String, String>();
		double timeSpentTogetherInSameHerd;
		
		for(String strainId : strainIds){
			
			// Get the eartag
			eartag = cattleIsolateLifeHistoryData.getEartagsForStrainIds().get(strainId);
						
			// Compare this animal to all others, did it spend any time with them?
			for(String id : HashtableMethods.getKeysString(cattleIsolateLifeHistoryData.getIsolates())){
						
				// Skip if we are comparing the same animal
				if(id.matches(eartag) == true){
					continue;
				}
							
				// Calculate how much time the cattle being compared spent in the same herd
				timeSpentTogetherInSameHerd = MakeEpidemiologicalComparisons.calculatePeriodSpentInSameHerd(
								cattleIsolateLifeHistoryData.getIsolates().get(eartag).getInfoForHerdsInhabited(),
								cattleIsolateLifeHistoryData.getIsolates().get(id).getInfoForHerdsInhabited());
							
				// If the two animals spent time together then keep them
				if(timeSpentTogetherInSameHerd > 0){
					animalsToKeep.put(id, strainId);
				}
			}
		}
		
		return animalsToKeep;
	}
}
