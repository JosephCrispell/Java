package woodchesterCattle;

import java.io.IOException;
import java.util.Calendar;
import java.util.Hashtable;

import methods.ArrayMethods;
import methods.CalendarMethods;
import methods.GeneralMethods;
import methods.HashtableMethods;
import methods.MatrixMethods;

public class MakeEpidemiologicalComparisons {

	public static void main(String[] args) throws IOException {
		
		// Get the date
		String date = "14-04-2016";
		
		// Note the path
		String path = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester_CattleAndBadgers/";
		
		// Read the Collated isolate data
		String consolidatedDataFile = path + "IsolateData/ConsolidatedCattleIsolateData_" + date + ".txt";
		//Hashtable<String, IsolateData> isolateData = BuildCattleLifeHistories.readSampledAnimalLifeHistories(consolidatedDataFile);
		
		// Read the location data
		String locationInfoCollated = path + "IsolateData/CollatedCattleLocationInfo_" + date + ".txt";
		//Hashtable<String, Location> locations = BuildCattleLifeHistories.readLocationInfo(locationInfoCollated);
		
		// Read the location adjacency matrix
		String adjacencyMatrix = path + "IsolateData/CattleAdjacencyMatrix_" + date + ".txt";
		//int[][] adjacency = MatrixMethods.readInt(adjacencyMatrix, "\t", locations.size());
		
		// Read the spatial distance matrix for the locations
		String spatialDistanceMatrix = path + "IsolateData/CattleSpatialDistanceMatrix_" + date + ".txt";
		//double[][] spatialDistances = MatrixMethods.readDouble(spatialDistanceMatrix, "\t", locations.size());
		
		// Read the nShared cattle between locations matrix for locations
		String nSharedMatrix = path + "IsolateData/NumberOfAnialsSharedBetweenLocations_" + date + ".txt";
		
		// Note the file with the Cattel TEsting History information
		String testHistoryFile = path + "CattleTestData/tblccdAnimal_13-09-16.txt";
		
		// Store the adjacency and spatial matrices with the location info
		//MovementNetwork network = new MovementNetwork(locations, adjacency);
		//network.setSpatialDistanceMatrix(spatialDistances);
		
		// Examine the movement history of each sampled animal
		//examineEachSampledAnimalsMovementHistory(isolateData);
		
		// Testing some epidemiological comparison methods
		collateCattleIsolateData(consolidatedDataFile, locationInfoCollated, adjacencyMatrix,
				spatialDistanceMatrix, nSharedMatrix, testHistoryFile);
		
	}
	
	public static CattleIsolateLifeHistoryData collateCattleIsolateData(String consolidatedSampledAnimalInfo,
			String consolidatedLocationsOfInterestFile, String locationAdjacencyMatrixFile,
			String locationSpatialDistanceMatrixFile, String nSharedMatrix, String cattleTestDataFile) throws IOException{
		
		System.out.println("Reading herd Information...");
		
		// Read the location data
		Hashtable<String, Location> locations = BuildCattleLifeHistories.readLocationInfo(consolidatedLocationsOfInterestFile);
		
		System.out.println("Reading Animal Life Histories...");
		
		// Read the Collated isolate data
		Hashtable<String, IsolateData> isolateData = BuildCattleLifeHistories.readSampledAnimalLifeHistories(consolidatedSampledAnimalInfo);
		
		System.out.println("Reading Cattle Testing Data...");
		
		BuildConsolidatedCattleData.examineCattleTestHistory(cattleTestDataFile, isolateData);
		
		System.out.println("Reading Network Data...");
		
		// Count the number of sampled herds - non-negative AdjacencyPos value
		Hashtable<String, Location> locationForSampledAnimals = getLocationsUsedInNetworks(locations);
		
		// Read the location adjacency matrix
		int[][] adjacency = MatrixMethods.readInt(locationAdjacencyMatrixFile, "\t", locationForSampledAnimals.size());
			
		// Read the spatial distance matrix for the locations
		double[][] spatialDistances = MatrixMethods.readDouble(locationSpatialDistanceMatrixFile, "\t",
				locationForSampledAnimals.size());
		
		// Read the number of shared animals between locations matrix
		int[][] nShared = MatrixMethods.readInt(nSharedMatrix, "\t", locationForSampledAnimals.size());
		
		// Store the adjacency and spatial matrices with the location info
		MovementNetwork network = new MovementNetwork(locationForSampledAnimals, adjacency);
		network.setSpatialDistanceMatrix(spatialDistances);
		network.setNSharedBetweenLocationsMatrix(nShared);
		
		System.out.println("Examining Movement History of Sampled Animals...");
		
		// Examine the movement history of each sampled animal
		Hashtable<String, String> eartagsForStrainIds = examineEachSampledAnimalsMovementHistory(isolateData, locations);
		
		// Store all the above data
		return new CattleIsolateLifeHistoryData(isolateData, locations, network, eartagsForStrainIds);
	}
	
	public static Hashtable<String, Location> getLocationsUsedInNetworks(Hashtable<String, Location> locations){
		
		// Intialise a new hashtable to store only the locations that the sampled animals passed through
		Hashtable<String, Location> sampledLocations = new Hashtable<String, Location>();
		
		for(String id : HashtableMethods.getKeysString(locations)){
			
			if(locations.get(id).getPosInAdjacencyMatrix() != -1){
				sampledLocations.put(id, locations.get(id));
			}
		}
		
		return sampledLocations;
	}
	
	public static double calculatePeriodSpentInSameHerd(Hashtable<String, Location> infoForHerdsAInhabited,
			Hashtable<String, Location> infoForHerdsBInhabited){
		
		// Initialise a variable to store the number of days the individuals spent on the same herd
		double nDays = 0;
		
		// Get a list of all the herds that A inhabited
		String[] herdsAInhabited = HashtableMethods.getKeysString(infoForHerdsAInhabited);
		
		// Initialise variables to store the starts and ends of each period that the individuals spent on a herd
		long[] aStarts;
		long[] aEnds;
		long[] bStarts;
		long[] bEnds;
		
		// Did individual B spend any time in the herds that individual A inhabited?
		for(String herd : herdsAInhabited){
			
			// Skip this herd if B never inhabited it
			if(infoForHerdsBInhabited.get(herd) == null){
				continue;
			}
			
			// Get the starts and ends for each period that the individuals spent on the current herd
			aStarts = infoForHerdsAInhabited.get(herd).getStarts();
			aEnds = infoForHerdsAInhabited.get(herd).getEnds();
			bStarts = infoForHerdsBInhabited.get(herd).getStarts();
			bEnds = infoForHerdsBInhabited.get(herd).getEnds();
			
			// Do any of the periods that A inhabited the current herd overlap with the periods that B inhabited it?
			for(int a = 0; a < aStarts.length; a++){
				
				for(int b = 0; b < bStarts.length; b++){
					
					nDays += calculateNDaysOverlapped(aStarts[a], aEnds[a], bStarts[b], bEnds[b]);
				}
			}			
		}		
		
		return nDays;
	}
	
	public static double calculateNDaysOverlapped(long aStart, long aEnd, long bStart, long bEnd){
		
		// Initialise a variable to store the number of days that the two individuals were alive together
		double nDays = -1;
		
		// Check that the start and end times are available for both isolates
		if(aStart != -1 && aEnd != -1 && bStart != -1 && bEnd != -1){
			
			nDays = 0;
			
			// Check that neither start after the other has finished - i.e. check if intervals overlap
			if(aStart < bEnd && bStart < aEnd){
				
				nDays = (returnSmallest(aEnd, bEnd) - returnLargest(aStart, bStart))  / (24 * 60 * 60 * 1000);
			}
		}
		
		return nDays;
	}
	
	public static long returnLargest(long a, long b){
		
		long value = a;
		
		if(b > value){
			value = b;
		}
		
		return value;
	}
	
	public static long returnSmallest(long a, long b){
		long value = a;
		
		if(b < value){
			value = b;
		}
		
		return value;
	}
	
	// Methods to examine isolate data
	
	public static void setLocationIdForCPH(IsolateData a, Hashtable<String, Location> locations){
		
		// Examine each of the locations to find a CPH that matches the current isolates
		for(String key : HashtableMethods.getKeysString(locations)){
			
			if(a.getCph().matches(locations.get(key).getCph())){
				a.setLocationId(locations.get(key).getLocationId());
			}
		}
	}
	
 	public static void setStartAndEndOfCowsLifespan(IsolateData a){
		
		// The first and last times the animal were recorded
		
		// Initialise a variable to store the movement records
		Movement[] movements;
		
		// Check if a birth and death date exist
		if(a.getBirth() != null && a.getDeath() != null){
			a.setStart(a.getBirth().getTimeInMillis());
			a.setEnd(a.getDeath().getTimeInMillis());
		}else{
			
			// Get the date of the first and last movements
			if(a.getNMovements() > 1){
				
				movements = a.getMovementRecords();
				
				a.setStart(movements[0].getDate().getTimeInMillis()); // Date animal first recorded
				a.setEnd(movements[movements.length - 1].getDate().getTimeInMillis()); // Date animal last recorded
			}
		}
	}
	
	public static String findMainHerd(IsolateData info){
		
		// Get a list of the herds inhabited
		Hashtable<String, Location> herdsInhabited = info.getInfoForHerdsInhabited();
		
		// Get a list of the herds inhabited
		String[] herds = HashtableMethods.getKeysString(herdsInhabited);
		
		// Initialise variables to store the id of the herd that the current sampled animal spent the most time on
		String mainHerd = "";
		double nDays = 0;
		double maxNDays = 0;
		
		// Examine each herd
		for(String herd : herds){
			
			// Calculate the number of days spent on the current herd
			nDays = ArrayMethods.sum(herdsInhabited.get(herd).getPeriodsSpentOnHerd());
			
			// Check if we have found a new maximum
			if(nDays > maxNDays){
				mainHerd = herd;
				maxNDays = nDays;
			}
		}
		
		return mainHerd;
	}
	
	public static Hashtable<String, String> examineEachSampledAnimalsMovementHistory(Hashtable<String, IsolateData> isolateData,
			Hashtable<String, Location> locations){
		
		// Initialise a Hashtable to store the eartag for each strain id
		Hashtable<String, String> eartagsForStrainIds = new Hashtable<String, String>();
		
		// Get an array of the keys
		String[] keys = HashtableMethods.getKeysString(isolateData);
		String id;
		
		for(int i = 0; i < keys.length; i++){
			
			id = keys[i];
			
			// Store the eartag
			eartagsForStrainIds.put(isolateData.get(id).getStrainId(), isolateData.get(id).getEartag());

			// Check that there are movements to examine
			if(isolateData.get(id).getNMovements() != 0){
			
				// Record the locations that each sampled animal visited
				isolateData.get(id).setInfoForHerdsInhabited(noteHerdsInhabited(isolateData.get(id).getMovementRecords()));
			
				// Find the herd that each sampled animal spent the most amount of time in
				isolateData.get(id).setMainherd(findMainHerd(isolateData.get(id)));
			}
			
			// Record the first and last time each isolate was observed
			setStartAndEndOfCowsLifespan(isolateData.get(id));
		
			// Note the location id for the sampled CPH of the current isolate
			setLocationIdForCPH(isolateData.get(id), locations);
			
			if((i+1) % 10000 == 0){
				System.out.print(".");
			}
		}
		System.out.println();
		
		return eartagsForStrainIds;
	}
	
	public static Hashtable<String, Location> noteHerdsInhabited(Movement[] movements){
		
		// Initialise a hashtable to store the details of the herds inhabited
		Hashtable<String, Location> herdsInhabited = new Hashtable<String, Location>();
		
		// Initialise a variable to store the Location information
		Location location;
		
		// Examine each of the movement events
		for(int i = 1; i < movements.length; i++){
			
			// Check that the OFF location for the current movement matches the ON location for the previous movement
			if(movements[i].getOffLocation().matches(movements[i-1].getOnLocation()) == true){
				
				// Check if information exists for the current herd
				if(herdsInhabited.get(movements[i].getOffLocation()) != null){
					
					// Add the information for the visit to the current herd
					herdsInhabited.get(movements[i].getOffLocation()).appendStayInfo(movements[i-1].getDate(), movements[i].getDate());
				}else{
					
					// Create a new entry for the current herd
					location = new Location(movements[i].getOffLocation());
					
					// Add the information for the visit to the current herd
					location.appendStayInfo(movements[i-1].getDate(), movements[i].getDate());
					
					// Store the location period info
					herdsInhabited.put(movements[i].getOffLocation(), location);
				}				
			}else{
				
				System.err.println("Error: The OFF location for the current movement doen's match the ON location of the previous movement");
			}
		}
		
		return herdsInhabited;		
	}
	
	
}
