package ExamineWPInterspeciesTransmission;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Hashtable;

import geneticDistances.GeneticDistances;
import geneticDistances.Sequence;
import methods.ArrayMethods;
import methods.CalendarMethods;
import methods.GeneticMethods;
import methods.HashtableMethods;
import methods.MatrixMethods;
import methods.WriteToFile;
import phylogeneticTree.BeastNewickTreeMethods;
import phylogeneticTree.CalculateDistancesToMRCAs;
import phylogeneticTree.Node;
import woodchesterBadgers.CaptureData;
import woodchesterBadgers.CapturedBadgerLifeHistoryData;
import woodchesterBadgers.CreateDescriptiveEpidemiologicalStats;
import woodchesterBadgers.SampleInfo;
import woodchesterBadgers.TerritoryCentroids;
import woodchesterCattle.CattleIsolateLifeHistoryData;
import woodchesterCattle.Location;
import woodchesterCattle.MakeEpidemiologicalComparisons;
import woodchesterCattle.Movement;
import woodchesterGeneticVsEpi.CompareIsolates;

public class ExamineCluster {

	public static void main(String[] args) throws IOException {

		/**
		 * READ IN THE CATTLE AND BADGER DATA
		 */

		// Set the date for these files
		String date = "04-04-2018"; // Override date
		String path = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester_CattleAndBadgers/NewAnalyses_22-03-18/";

		// Read in the Cattle data
		String consolidatedSampledAnimalInfo = path + "IsolateData/ConsolidatedCattleIsolateData_" + date + ".txt";
		String consolidatedLocationsOfInterestFile = path + "IsolateData/CollatedCattleLocationInfo_" + date + ".txt";
		String locationAdjacencyMatrixFile = path + "IsolateData/CattleAdjacencyMatrix_" + date + ".txt";
		String locationSpatialDistanceMatrixFile = path + "IsolateData/CattleSpatialDistanceMatrix_" + date + ".txt";
		String nSharedBetweenLocationsMatrixFile = path + "IsolateData/NumberOfAnimalsSharedBetweenLocations_" + date
				+ ".txt";
		String testHistoryFile = path + "CattleTestData/tblccdAnimal_13-09-16.txt";
		CattleIsolateLifeHistoryData cattleIsolateLifeHistoryData = 
				MakeEpidemiologicalComparisons.collateCattleIsolateData(consolidatedSampledAnimalInfo,
						consolidatedLocationsOfInterestFile,
						locationAdjacencyMatrixFile, locationSpatialDistanceMatrixFile,
						nSharedBetweenLocationsMatrixFile, testHistoryFile);
		cattleIsolateLifeHistoryData.getNetworkInfo().setShortestPathsFull(
				CompareIsolates.findShortestPathsBetweenAllNodes(cattleIsolateLifeHistoryData.getNetworkInfo().getAdjacencyMatrix()));
		String[] premisesTypesToIgnoreInNetwork = { "SR", "CC", "SW", "EX"}; // Slaughter Houses and Collection Centres

		cattleIsolateLifeHistoryData.getNetworkInfo().setShortestPathsWithoutSelectedPremises(
				CompareIsolates.findShortestPathsBetweenAllNodesExcludePremiseTypes(
						cattleIsolateLifeHistoryData.getNetworkInfo().getAdjacencyMatrix(),
						cattleIsolateLifeHistoryData.getNetworkInfo().getLocations(), premisesTypesToIgnoreInNetwork));

		// Read in the Land Parcel centroid data
		String landParcelCentroidsFile = path + "LandParcelData/RPA_CLAD_ASI_CURRENT_SP_ST-SO_NE-SE/";
		landParcelCentroidsFile += "RPA_CLAD_ASI_CURRENT_SP_ST-SO_NE-SE_Centroids-XY.csv";
		CompareIsolates.getLocationCentroidInformation(landParcelCentroidsFile,
				cattleIsolateLifeHistoryData.getLocations());

		// Read in the Badger data
		String sampledIsolateInfo = path + "IsolateData/BadgerInfo_08-04-15_LatLongs_XY_Centroids.csv";
		String consolidatedCaptureData = path + "BadgerCaptureData/WP_CaptureData_Consolidated_31-07-2017.txt";
		String territoryCentroidsFile = path + "BadgerCaptureData/BadgerTerritoryMarkingData/"
				+ "SocialGroupsCentroidsPerYear_16-05-17.txt";
		String relatednessMatrixFile = path
				+ "BadgerRelatedness/GenotypedBadgerRelatedness_ImputationOnly_12-07-17.csv";
		CapturedBadgerLifeHistoryData badgerIsolateLifeHistoryData = 
				CreateDescriptiveEpidemiologicalStats.collateCaptureBadgerInformation(sampledIsolateInfo,
						consolidatedCaptureData, territoryCentroidsFile, false, relatednessMatrixFile);
		badgerIsolateLifeHistoryData.setShortestPathsBetweenGroups(
				CompareIsolates.findShortestPathsBetweenAllNodes(
						badgerIsolateLifeHistoryData.getGroupAdjacencyMatrix()));

		/**
		 * EXAMINE THE CLUSTERS SURROUNDING INTER-SPECIES TRANSMISSION EVENTS
		 */

		date = CalendarMethods.getCurrentDate("dd-MM-yyyy");

		// Get a list of the isolates in each cluster
		String clusterFile = path + "vcfFiles/clusters_27-03-18.csv";
		Cluster[] clusters = readClustersFile(clusterFile);

		// Get the isolate sequences
		String fastaFile = path + "vcfFiles/";
		fastaFile += "sequences_withoutHomoplasies_27-03-18.fasta";
		Sequence[] sequences = GeneticDistances.readFastaFile(fastaFile);
		setIsolateNames(sequences);

		// Add a cluster that contains all the isolates that aren't associated
		// with a cluster
		clusters = addSequencedAnimalsThatDontHaveIsolatesInClusters(clusters, sequences);

		// Get the genetic distances to the Reference
		addDistanceToRefUsingInformativeSitesToCluster(clusters, sequences, sequences.length - 1, 0);

		// Get the genetic distances to the MRCAs of each cluster
		String newickFile = path + "vcfFiles/";
		newickFile += "mlTree_27-03-18.tree";
		noteDistancesToMRCAsOfClusters(clusters, newickFile);

		// Add in the sequencing quality information
		String isolateGenomeCoverageFile = path + "vcfFiles/" + 
				"IsolateVariantPositionCoverage_RESCUED_24-03-2018.txt";
		Hashtable<String, Double> isolateQuality = readIsolateQuality(isolateGenomeCoverageFile);
		addIsolateSequencingQuality(clusters, isolateQuality);

		/**
		 * Find the following information for each of the Isolates AnimalId
		 * IsolateIds Clusters SamplingDates DetectionDate Xs Ys Dates 0 1 2 3 4
		 * 5 6 7
		 */
		Hashtable<String, LifeHistorySummary> sampledAnimals = noteWhichAnimalsIsolatesAreFrom(clusters,
				badgerIsolateLifeHistoryData, cattleIsolateLifeHistoryData);

		summariseSampledAnimalsLifeHistories(sampledAnimals, badgerIsolateLifeHistoryData,
				cattleIsolateLifeHistoryData);

		// Print out the life history data
		String lifeHistoryFile = path + "InterSpeciesClusters/sampledAnimalsLifeHistories_" + date + ".txt";
		printLifeHistoriesForSampledAnimals(lifeHistoryFile, sampledAnimals, true);

		// Note the Premises Types to ignore
		Hashtable<String, Integer> premisesTypesToIgnore = HashtableMethods.indexArray(
				premisesTypesToIgnoreInNetwork);

		// Find associated cattle
		Hashtable<String, LifeHistorySummary> cattleEncountered = findAnimalsThatEncounteredSampledCattle(clusters,
				cattleIsolateLifeHistoryData, sampledAnimals, premisesTypesToIgnore);
		summariseSampledAnimalsLifeHistories(cattleEncountered, badgerIsolateLifeHistoryData,
				cattleIsolateLifeHistoryData);
		printLifeHistoriesForSampledAnimals(lifeHistoryFile, cattleEncountered, false);
		System.out.println("Found " + cattleEncountered.size() + " in-contact cattle.");
		
		// Find associated badgers
		Hashtable<String, LifeHistorySummary> badgersEncountered = findAnimalsThatEncounteredSampledBadgers(clusters,
				badgerIsolateLifeHistoryData, sampledAnimals);

		summariseSampledAnimalsLifeHistories(badgersEncountered, badgerIsolateLifeHistoryData,
				cattleIsolateLifeHistoryData);
		printLifeHistoriesForSampledAnimals(lifeHistoryFile, badgersEncountered, false);
		System.out.println("Found " + badgersEncountered.size() + " in-contact badgers.");
	}

	public static Hashtable<String, Integer> getListOfIsolatesInClusters(Cluster[] clusters) {

		Hashtable<String, Integer> isolatesInClusters = new Hashtable<String, Integer>();
		String[] isolates;
		for (Cluster cluster : clusters) {

			isolates = cluster.getIsolateIds();
			for (String id : isolates) {

				if (isolatesInClusters.get(id) == null) {
					isolatesInClusters.put(id, cluster.getId());
				}
			}
		}

		return isolatesInClusters;
	}

	public static Hashtable<String, Integer> getListOfAllSequencedIsolates(Sequence[] sequences) {
		Hashtable<String, Integer> isolates = new Hashtable<String, Integer>();

		for (Sequence sequence : sequences) {

			if (isolates.get(sequence.getName()) == null) {
				isolates.put(sequence.getName(), -1);
			}
		}

		return isolates;
	}

	public static String[] getArrayOfSequencedIsolatesThatArentInClusters(Hashtable<String, Integer> isolatesInClusters,
			Hashtable<String, Integer> sequencedIsolates) {

		String[] isolates = new String[sequencedIsolates.size() - isolatesInClusters.size()];
		int index = -1;

		for (String id : HashtableMethods.getKeysString(sequencedIsolates)) {

			if (isolatesInClusters.get(id) == null) {
				index++;
				isolates[index] = id;
			}
		}

		return isolates;
	}

	public static Cluster[] addSequencedAnimalsThatDontHaveIsolatesInClusters(Cluster[] clusters,
			Sequence[] sequences) {

		// Get a list of all isolates from the clusters
		Hashtable<String, Integer> isolatesInClusters = getListOfIsolatesInClusters(clusters);

		// Get a list of all sequenced isolates
		Hashtable<String, Integer> sequencedIsolates = getListOfAllSequencedIsolates(sequences);

		// Get a list of the isolates that aren't in the clusters
		String[] isolatesNotInClusters = getArrayOfSequencedIsolatesThatArentInClusters(isolatesInClusters,
				sequencedIsolates);

		// Create a new array to contain the clusters
		Cluster[] newClusters = new Cluster[clusters.length + 1];
		for (int i = 0; i < clusters.length; i++) {
			newClusters[i] = clusters[i];
		}

		// Create the new cluster and add to array
		newClusters[clusters.length] = new Cluster(clusters.length, isolatesNotInClusters);

		// Replace the original array
		return newClusters;
	}

	public static void addIsolateSequencingQuality(Cluster[] clusters, Hashtable<String, Double> isolateQuality) {

		// Initialise an array to temporally store the isolate quality values
		double[] qualityForIsolates;
		String[] isolates;

		// Examine the sets of isolates in each cluster
		for (Cluster cluster : clusters) {

			// Get the isolate IDs for the current cluster
			isolates = cluster.getIsolateIds();
			qualityForIsolates = new double[isolates.length];

			// Get the quality for each isolate
			for (int i = 0; i < isolates.length; i++) {

				if (isolateQuality.get(isolates[i]) != null) {
					qualityForIsolates[i] = isolateQuality.get(isolates[i]);
				} else {
					qualityForIsolates[i] = -1;
				}
			}

			// Store the quality values for the isolates
			cluster.setSequencingQualityForIsolates(qualityForIsolates);
		}
	}

	public static Hashtable<String, Double> readIsolateQuality(String isolateGenomeCoverageSummaryFile)
			throws IOException {

		/**
		 * Isolate genome coverage summary file structure: Isolate
		 * VariantPositionCoverage TB1385_S1_1.vcf.gz 0.691068814055637
		 * 
		 */

		// Initialise a hashtable to store
		Hashtable<String, Double> isolateQuality = new Hashtable<String, Double>();

		// Open the input file
		InputStream input = new FileInputStream(isolateGenomeCoverageSummaryFile);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));

		// Initialise variables necessary for parsing the file
		String line = null;
		int lineNo = 0;
		String[] cols;

		// Begin reading the file
		while ((line = reader.readLine()) != null) {
			lineNo++;

			// Skip the header and last line
			if (lineNo == 1) {
				continue;
			}

			// Split the line into its columns
			cols = line.split("\t");

			// Get the isolate Id
			cols[0] = cols[0].split("_")[0];

			// Store the isolates sequencing quality
			isolateQuality.put(cols[0], Double.valueOf(cols[1]));
		}

		// Close the current movements file
		input.close();
		reader.close();

		return (isolateQuality);
	}

	public static void setIsolateNames(Sequence[] sequences) {

		String name;
		for (int i = 0; i < sequences.length; i++) {

			name = sequences[i].getName().split("_")[0];
			sequences[i].setName(name);
		}
	}

	public static ContactEvent getInfoForPeriodsSpentInSameHerd(Hashtable<String, Location> infoForHerdsAInhabited,
			Hashtable<String, Location> infoForHerdsBInhabited, Hashtable<String, Integer> premisesTypesToIgnore,
			String eartagOfSequencedCow, String eartag, Hashtable<String, Location> locations) {

		// Initialise a variable to store the number of days the individuals
		// spent on the same herd
		double nDays;
		double nDaysTotal = 0;

		// Arrays to store the start and end dates of contact periods
		Calendar[] starts = new Calendar[0];
		Calendar[] ends = new Calendar[0];
		Calendar start;
		Calendar end;

		// Initialise an array to store the herds where contact occur
		String[] herds = new String[0];

		// Get a list of all the herds that A inhabited
		String[] herdsAInhabited = HashtableMethods.getKeysString(infoForHerdsAInhabited);

		// Initialise variables to store the starts and ends of each period that
		// the individuals spent on a herd
		long[] aStarts;
		long[] aEnds;
		long[] bStarts;
		long[] bEnds;
		Calendar[] aStartDates;
		Calendar[] aEndDates;
		Calendar[] bStartDates;
		Calendar[] bEndDates;
		
		// Initialise a variable to store the premises type
		String premisesType;

		// Did individual B spend any time in the herds that individual A
		// inhabited?
		for (String herd : herdsAInhabited) {

			premisesType = locations.get(herd).getPremisesType();
			
			// Skip herd if it is a type that we want to ignore
			if(premisesType != null && premisesTypesToIgnore.get(premisesType) != null) {
				continue;
			}else if(premisesType == null){
				System.out.println("ERROR: Premises type not found for current herd: " + herd);
			}

			// Skip this herd if B never inhabited it
			if (infoForHerdsBInhabited.get(herd) == null) {
				continue;
			}

			// Get the starts and ends for each period that the individuals
			// spent on the current herd
			aStarts = infoForHerdsAInhabited.get(herd).getStarts();
			aEnds = infoForHerdsAInhabited.get(herd).getEnds();
			bStarts = infoForHerdsBInhabited.get(herd).getStarts();
			bEnds = infoForHerdsBInhabited.get(herd).getEnds();
			aStartDates = infoForHerdsAInhabited.get(herd).getOnDates();
			aEndDates = infoForHerdsAInhabited.get(herd).getOffDates();
			bStartDates = infoForHerdsBInhabited.get(herd).getOnDates();
			bEndDates = infoForHerdsBInhabited.get(herd).getOffDates();

			// Do any of the periods that A inhabited the current herd overlap
			// with the periods that B inhabited it?
			for (int a = 0; a < aStarts.length; a++) {

				for (int b = 0; b < bStarts.length; b++) {

					nDays = MakeEpidemiologicalComparisons.calculateNDaysOverlapped(aStarts[a], aEnds[a], bStarts[b],
							bEnds[b]);
					nDaysTotal += nDays;

					// Did they spend time together in the current comparison
					if (nDays > 0) {

						// Find start
						start = aStartDates[a];
						if (start.before(bStartDates[b])) {
							start = bStartDates[b];
						}

						// Find end
						end = aEndDates[a];
						if (end.after(bEndDates[b])) {
							end = bEndDates[b];
						}

						// Add the current start and end dates
						starts = CalendarMethods.append(starts, start);
						ends = CalendarMethods.append(ends, end);
						herds = ArrayMethods.append(herds, herd);
					}
				}
			}
		}

		// Return the information about contact
		ContactEvent info = null;
		if (nDaysTotal > 0) {
			info = new ContactEvent(eartagOfSequencedCow, eartag, starts, ends, nDaysTotal, herds);
		}
		return info;
	}

	public static double calculatePeriodSpentInSameHerd(Hashtable<String, Location> infoForHerdsAInhabited,
			Hashtable<String, Location> infoForHerdsBInhabited, Hashtable<String, Integer> premisesTypesToIgnore) {

		// Initialise a variable to store the number of days the individuals
		// spent on the same herd
		double nDays;
		double nDaysTotal = 0;

		// Get a list of all the herds that A inhabited
		String[] herdsAInhabited = HashtableMethods.getKeysString(infoForHerdsAInhabited);

		// Initialise variables to store the starts and ends of each period that
		// the individuals spent on a herd
		long[] aStarts;
		long[] aEnds;
		long[] bStarts;
		long[] bEnds;

		// Did individual B spend any time in the herds that individual A
		// inhabited?
		for (String herd : herdsAInhabited) {

			// Skip herd if it is a type that we want to ignore
			if (infoForHerdsAInhabited.get(herd).getPremisesType() != null
					&& premisesTypesToIgnore.get(infoForHerdsAInhabited.get(herd).getPremisesType()) != null) {
				continue;
			}

			// Skip this herd if B never inhabited it
			if (infoForHerdsBInhabited.get(herd) == null) {
				continue;
			}

			// Get the starts and ends for each period that the individuals
			// spent on the current herd
			aStarts = infoForHerdsAInhabited.get(herd).getStarts();
			aEnds = infoForHerdsAInhabited.get(herd).getEnds();
			bStarts = infoForHerdsBInhabited.get(herd).getStarts();
			bEnds = infoForHerdsBInhabited.get(herd).getEnds();

			// Do any of the periods that A inhabited the current herd overlap
			// with the periods that B inhabited it?
			for (int a = 0; a < aStarts.length; a++) {

				for (int b = 0; b < bStarts.length; b++) {

					nDays = MakeEpidemiologicalComparisons.calculateNDaysOverlapped(aStarts[a], aEnds[a], bStarts[b],
							bEnds[b]);
					nDaysTotal += nDays;
				}
			}
		}

		return nDaysTotal;
	}

	public static Hashtable<String, LifeHistorySummary> findAnimalsThatEncounteredSampledBadgers(Cluster[] clusters,
			CapturedBadgerLifeHistoryData badgerIsolateLifeHistoryData,
			Hashtable<String, LifeHistorySummary> sampledAnimals) {

		// Initialise a hashtable to store the badgers found for each sampled
		// badger examined
		Hashtable<String, ContactEvent[]> badgersFoundForIsolate;
		Hashtable<String, LifeHistorySummary> badgersFound = new Hashtable<String, LifeHistorySummary>();
		String[] badgers;

		// Initialise a hashtable to record the badgers whose histories we have
		// searched for contacts
		Hashtable<String, Integer> badgersExamined = new Hashtable<String, Integer>();
		String tattoo;

		// Examine the cattle in each cluster
		for (Cluster cluster : clusters) {

			// Examine each of the isolates in the cluster
			for (String isolate : cluster.getIsolateIds()) {

				// Skip cattle
				if (isolate.matches("TB(.*)") == true) {
					continue;
				}

				// Skip isolates that we weren't able to identify animals for
				if (badgerIsolateLifeHistoryData.getSampledIsolateInfo().get(isolate) == null) {
					continue;
				}

				// Get the tattoo
				tattoo = badgerIsolateLifeHistoryData.getSampledIsolateInfo().get(isolate).getTattoo();
				if (badgersExamined.get(tattoo) == null) {
					badgersExamined.put(tattoo, 1);
				} else {
					continue;
				}

				// Find badgers that encountered the sampled badger
				badgersFoundForIsolate = findAnimalIdsOfBadgersThatEncounterSampledBadger(tattoo,
						badgerIsolateLifeHistoryData);

				// For any badgers found, add them to the large Hashtable for
				// all badgers across clusters
				badgers = HashtableMethods.getKeysString(badgersFoundForIsolate);
				for (String badger : badgers) {

					// Skip sampled badgers
					if (sampledAnimals.get(badger) != null) {
						continue;
					}

					// Add an unsampled badger
					if (badgersFound.get(badger) != null) {
						badgersFound.get(badger).addClusterAssociation(cluster.getId());
						badgersFound.get(badger).setContactInfo(ContactEvent.combine(
								badgersFound.get(badger).getContactInfo(), badgersFoundForIsolate.get(badger)));

					} else {
						badgersFound.put(badger, new LifeHistorySummary(badger, "BADGER"));
						badgersFound.get(badger).addClusterAssociation(cluster.getId());
						badgersFound.get(badger).setContactInfo(badgersFoundForIsolate.get(badger));
					}
				}
			}
		}

		return badgersFound;
	}

	public static Hashtable<String, ContactEvent[]> findAnimalIdsOfBadgersThatEncounterSampledBadger(String tattoo,
			CapturedBadgerLifeHistoryData badgerIsolateLifeHistoryData) {

		// Initialise a hashtable to record the tattoos of the animals that we
		// are interested in
		Hashtable<String, ContactEvent[]> animalsToKeep = new Hashtable<String, ContactEvent[]>();
		ContactEvent contactInfo;

		// Check that there is movement data available for this sampled badger
		if (badgerIsolateLifeHistoryData.getBadgerCaptureHistories().get(tattoo).getNMovements() != 0) {

			// Compare this animal to all others, did it spend any time with
			// them?
			for (String id : badgerIsolateLifeHistoryData.getCapturedBadgerTattoos()) {

				// Skip if we are comparing the same animal
				if (id.matches(tattoo) == true) {
					continue;
				}

				// Calculate how much time the cattle being compared spent in
				// the same herd
				contactInfo = getInfoForPeriodsSpentInSameGroup(
						badgerIsolateLifeHistoryData.getBadgerCaptureHistories().get(tattoo).getPeriodsInEachGroup(),
						badgerIsolateLifeHistoryData.getBadgerCaptureHistories().get(id).getPeriodsInEachGroup(),
						tattoo, id);

				// If the two animals spent time together then keep them
				if (contactInfo != null) {

					if (animalsToKeep.get(id) == null) {
						animalsToKeep.put(id, new ContactEvent[0]);
						animalsToKeep.put(id, ContactEvent.append(animalsToKeep.get(id), contactInfo));
					} else {
						animalsToKeep.put(id, ContactEvent.append(animalsToKeep.get(id), contactInfo));
					}
				}
			}
		}

		return animalsToKeep;
	}

	public static ContactEvent getInfoForPeriodsSpentInSameGroup(Hashtable<String, long[][]> infoForGroupsAInhabited,
			Hashtable<String, long[][]> infoForGroupsBInhabited, String tattooOfSequencedBadger,
			String tattooOfInContactBadger) {

		// Initialise a variable to store the number of days the individuals
		// spent on the same herd
		double nDays;
		double nDaysTotal = 0;

		// Arrays to store the start and end dates of contact periods
		Calendar[] starts = new Calendar[0];
		Calendar[] ends = new Calendar[0];

		// Initialise an array to store the badger groups where contact occur
		String[] groups = new String[0];

		// Get a list of all the groups that A inhabited
		String[] groupsAInhabited = HashtableMethods.getKeysString(infoForGroupsAInhabited);

		// Initialise variables necessary for comparing the periods spent in a
		// given group
		long[][] periodsSpentA;
		long[][] periodsSpentB;

		// Did badger B spend any time in the groups that badger A inhabited?
		for (String group : groupsAInhabited) {

			// Skip this group if B never inhabited it
			if (infoForGroupsBInhabited.get(group) == null) {
				continue;
			}

			// Do any of the periods that A spent in the current group overlap
			// with any of the periods that B spent in the current group?
			periodsSpentA = infoForGroupsAInhabited.get(group);
			periodsSpentB = infoForGroupsBInhabited.get(group);

			// Examine each of the periods that badger A spent in the current
			// group and compare those to the periods that badger B spent in the
			// group
			for (int rowA = 0; rowA < periodsSpentA.length; rowA++) {
				for (int rowB = 0; rowB < periodsSpentB.length; rowB++) {

					// Reset the calendar instances here to avoid their settings
					// being carried into the starts/ends array
					Calendar start = Calendar.getInstance();
					Calendar end = Calendar.getInstance();

					// Sum up the time spent together across groups and periods
					nDays = CreateDescriptiveEpidemiologicalStats.findTimeTogether(periodsSpentA[rowA][0],
							periodsSpentA[rowA][1], periodsSpentB[rowB][0], periodsSpentB[rowB][1]);
					nDaysTotal += nDays;

					// Did they spend time together in the current comparison
					if (nDays > 0) {

						// Find start
						start.setTimeInMillis(periodsSpentA[rowA][0]);
						if (start.before(periodsSpentB[rowB][0])) {
							start.setTimeInMillis(periodsSpentB[rowB][0]);
						}

						// Find end
						end.setTimeInMillis(periodsSpentA[rowA][1]);
						if (end.after(periodsSpentB[rowB][1])) {
							end.setTimeInMillis(periodsSpentB[rowB][1]);
						}

						// Add the current start and end dates
						starts = CalendarMethods.append(starts, start);
						ends = CalendarMethods.append(ends, end);
						groups = ArrayMethods.append(groups, group);
					}
				}
			}
		}

		// Return the information about contact
		ContactEvent info = null;
		if (nDaysTotal > 0) {
			info = new ContactEvent(tattooOfSequencedBadger, tattooOfInContactBadger, starts, ends, nDaysTotal, groups);
		}
		return info;
	}

	public static Hashtable<String, LifeHistorySummary> findAnimalsThatEncounteredSampledCattle(Cluster[] clusters,
			CattleIsolateLifeHistoryData cattleIsolateLifeHistoryData,
			Hashtable<String, LifeHistorySummary> sampledAnimals, 
			Hashtable<String, Integer> premisesTypesToIgnore) {

		// Initialise a hashtable to store the cattle found for each sampled cow
		// examined
		Hashtable<String, ContactEvent[]> cattleFoundForIsolate;
		Hashtable<String, LifeHistorySummary> cattleFound = new Hashtable<String, LifeHistorySummary>();
		String[] cattle;

		// Examine the cattle in each cluster
		for (Cluster cluster : clusters) {

			// Examine each of the isolates in the cluster
			for (String isolate : cluster.getIsolateIds()) {

				// Skip badgers
				if (isolate.matches("WB(.*)") == true) {
					continue;
				}

				// Find cattle that encountered the sampled cow
				cattleFoundForIsolate = findAnimalIdsOfCattleThatEncounterSampledCow(isolate,
						cattleIsolateLifeHistoryData, premisesTypesToIgnore);

				// For any cattle found, add them to the large Hashtable for all
				// cattle across clusters
				cattle = HashtableMethods.getKeysString(cattleFoundForIsolate);
				for (String cow : cattle) {

					// Skip sampled cattle
					if (sampledAnimals.get(cow) != null) {
						continue;
					}

					// Add an unsampled cow
					if (cattleFound.get(cow) != null) {
						cattleFound.get(cow).addClusterAssociation(cluster.getId());
						cattleFound.get(cow).setContactInfo(ContactEvent.combine(cattleFound.get(cow).getContactInfo(),
								cattleFoundForIsolate.get(cow)));

					} else {
						cattleFound.put(cow, new LifeHistorySummary(cow, "COW"));
						cattleFound.get(cow).addClusterAssociation(cluster.getId());
						cattleFound.get(cow).setContactInfo(cattleFoundForIsolate.get(cow));
					}
				}
			}
		}

		return cattleFound;
	}

	public static Hashtable<String, ContactEvent[]> findAnimalIdsOfCattleThatEncounterSampledCow(String strainId,
			CattleIsolateLifeHistoryData cattleIsolateLifeHistoryData,
			Hashtable<String, Integer> premisesTypesToIgnore) {

		// Initialise a variable to store an animal's data
		String eartag;

		// Initialise a hashtable to record the eartags of the animals that we
		// are interested in
		Hashtable<String, ContactEvent[]> animalsToKeep = new Hashtable<String, ContactEvent[]>();
		ContactEvent contactInfo;

		// Get the eartag
		eartag = cattleIsolateLifeHistoryData.getEartagsForStrainIds().get(strainId);

		// Check that there is movement data available for this sampled cow
		if (cattleIsolateLifeHistoryData.getEartagsForStrainIds().get(strainId) != null
				&& cattleIsolateLifeHistoryData.getIsolates().get(eartag).getNMovements() != 0) {

			// Compare this animal to all others, did it spend any time with
			// them?
			for (String id : HashtableMethods.getKeysString(cattleIsolateLifeHistoryData.getIsolates())) {

				// Skip if we are comparing the same animal
				if (id.matches(eartag) == true) {
					continue;
				}

				// Calculate how much time the cattle being compared spent in
				// the same herd
				contactInfo = getInfoForPeriodsSpentInSameHerd(
						cattleIsolateLifeHistoryData.getIsolates().get(eartag).getInfoForHerdsInhabited(),
						cattleIsolateLifeHistoryData.getIsolates().get(id).getInfoForHerdsInhabited(),
						premisesTypesToIgnore, eartag, id, cattleIsolateLifeHistoryData.getLocations());

				// If the two animals spent time together then keep them
				if (contactInfo != null) {
					if (animalsToKeep.get(id) == null) {
						animalsToKeep.put(id, new ContactEvent[0]);
						animalsToKeep.put(id,
								ContactEvent.append(animalsToKeep.put(id, new ContactEvent[0]), contactInfo));
					} else {
						animalsToKeep.put(id,
								ContactEvent.append(animalsToKeep.put(id, new ContactEvent[0]), contactInfo));
					}
				}
			}
		}

		return animalsToKeep;
	}

	public static void noteDistancesToMRCAsOfClusters(Cluster[] clusters, String newickFile) throws IOException {

		// Read the Newick String into a variable
		String newickTree = CalculateDistancesToMRCAs.readNewickFile(newickFile);

		// Convert the Newick Tree into a Java traversable Node
		Node tree = BeastNewickTreeMethods.readNewickNode(newickTree, new Node(null, null, null));

		// For each node in the phylogenetic tree note the path to the root
		CalculateDistancesToMRCAs.notePathToRootForAllNodes(tree, new Node[0]);

		// Get a hashtable linking the terminal node IDs to their nodes
		Hashtable<String, Node> terminalNodes = getTerminalNodes(tree, new Hashtable<String, Node>());

		// Find the MRCAs of each cluster and calculate the distances from the
		// tips to the MRCAs
		for (int i = 0; i < clusters.length; i++) {

			clusters[i].setDistancesToMRCA(calculateDistancesToMRCAForCluster(clusters[i].getIsolateIds(),
					terminalNodes));
		}
	}

	public static double[] calculateDistancesToMRCAForCluster(String[] isolates,
			Hashtable<String, Node> terminalNodes) {

		// Get the nodes of the isolates in the cluster (tips on the phylogeny)
		Node[] nodes = getClustersTerminalNodes(isolates, terminalNodes);

		// Get the paths to the MRCA of the cluster
		Node[][] pathsToRoot = getPathsToRootForEachNodeInCluster(nodes);

		// Find the MRCA of the cluster
		Node mrca = CalculateDistancesToMRCAs.findMRCA(pathsToRoot);

		// Calculate distances of each isolate to the MRCA of the cluster
		double[] distances = calculateDistancesToMRCAForNodes(mrca, nodes);

		return distances;
	}

	public static double[] calculateDistancesToMRCAForNodes(Node mrca, Node[] nodes) {

		double[] distances = new double[nodes.length];

		for (int i = 0; i < nodes.length; i++) {

			distances[i] = CalculateDistancesToMRCAs.calculateDistanceToMRCA(nodes[i], mrca);
		}

		return distances;
	}

	public static Node[][] getPathsToRootForEachNodeInCluster(Node[] nodes) {

		Node[][] pathsToRoot = new Node[nodes.length][0];

		for (int i = 0; i < nodes.length; i++) {
			pathsToRoot[i] = nodes[i].getPathToRoot();
		}

		return pathsToRoot;
	}

	public static Node[] getClustersTerminalNodes(String[] isolates, Hashtable<String, Node> terminalNodes) {

		Node[] nodes = new Node[isolates.length];

		for (int i = 0; i < isolates.length; i++) {
			nodes[i] = terminalNodes.get(isolates[i]);
		}

		return nodes;
	}

	public static Hashtable<String, Node> getTerminalNodes(Node node, Hashtable<String, Node> leaves) {

		// Get the subNodes for the current Node
		Node[] subNodes = node.getSubNodes();

		// Does the current Node have any sub nodes?
		if (subNodes.length == 0) {

			// Store the current terminal node by its ID - should be sequence ID
			leaves.put(node.getNodeInfo().getNodeId(), node);
		} else {

			for (Node subNode : subNodes) {
				leaves = getTerminalNodes(subNode, leaves);
			}
		}

		return leaves;
	}

	public static void addDistanceToRefUsingInformativeSitesToCluster(Cluster[] clusters, Sequence[] sequences,
			int refIndex, double prop) {

		for (int i = 0; i < clusters.length; i++) {

			clusters[i].setDistancesToRef(calculateDistanceToRefUsingInformativeSites(clusters[i].getIsolateIds(),
					sequences, refIndex, prop));
		}
	}

	public static int[] calculateDistanceToRefUsingInformativeSites(String[] isolates, Sequence[] sequences,
			int refIndex, double prop) {

		// Get all the sequences of the isolates of interest
		Sequence[] isolateSequences = getIsolatesSequences(isolates, sequences);

		// Note which sites are informative
		boolean[] informative = findInformativeSites(isolateSequences);
		informative = ignorePoorCoveragePositions(isolateSequences, informative, prop);

		// Using only the informative sites calculate the genetic distance to
		// ref for each isolate
		return calculateDistanceToRef(isolateSequences, sequences[refIndex], informative);
	}

	public static boolean[] ignorePoorCoveragePositions(Sequence[] isolates, boolean[] informative, double prop) {

		// Get the sequence length
		int seqLength = isolates[0].getSequence().length;
		int nIsolates = isolates.length;

		// Examine each of the sites on the genome
		double[] siteCoverage = new double[seqLength];

		// Examine each isolate
		for (int i = 0; i < nIsolates; i++) {
			siteCoverage = ArrayMethods.add(siteCoverage, sitesNotN(isolates[i].getSequence()));
		}

		siteCoverage = ArrayMethods.divide(siteCoverage, (double) nIsolates);

		for (int pos = 0; pos < seqLength; pos++) {
			if (siteCoverage[pos] < prop) {
				informative[pos] = false;
			}
		}

		return informative;
	}

	public static double[] sitesNotN(char[] sequence) {
		double[] result = new double[sequence.length];

		for (int i = 0; i < sequence.length; i++) {
			if (sequence[i] != 'N') {
				result[i] = 1;
			}
		}

		return result;
	}

	public static int[] calculateDistanceToRef(Sequence[] isolateSequences, Sequence ref, boolean[] informative) {

		int[] distances = new int[isolateSequences.length];
		for (int i = 0; i < isolateSequences.length; i++) {

			distances[i] = GeneticMethods.calculateNumberDifferencesBetweenSequences(isolateSequences[i].getSequence(),
					ref.getSequence(), informative);
		}

		return distances;
	}

	public static boolean[] findInformativeSites(Sequence[] sequences) {

		// Get the sequence length
		int seqLength = sequences[0].getSequence().length;

		// Initialise an array to note which sites are informative
		boolean[] informative = ArrayMethods.repeat(false, seqLength);

		// Intialise variables to store the isolates sequences
		char[] seqI;
		char[] seqJ;

		// Examine each position in the FASTA sequence
		for (int pos = 0; pos < seqLength; pos++) {

			for (int i = 0; i < sequences.length; i++) {

				// Get the sequence for I
				seqI = sequences[i].getSequence();

				// Find an isolate without an 'N' at the current position and
				// compare it to all others
				if (seqI[pos] != 'N') {

					for (int j = 0; j < sequences.length; j++) {

						// Get the sequence for J
						seqJ = sequences[j].getSequence();

						// Skip self comparisons or a comparison that has
						// already been done
						if (i == j) {
							continue;
						}

						// Skip isolate if has a 'N' at the current position
						if (seqJ[pos] == 'N') {
							continue;
						}

						// Are the current isolates different at the current
						// position?
						if (seqI[pos] != seqJ[pos]) {
							informative[pos] = true;

							break;
						}
					}
					break;
				}
			}
		}

		return informative;
	}

	public static Sequence[] getIsolatesSequences(String[] isolates, Sequence[] sequences) {

		// Initialise an empty array to store the sequences
		Sequence[] isolateSequences = new Sequence[isolates.length];

		// Convert the array of isolates to a hashtable
		Hashtable<String, Integer> indexedIsolates = HashtableMethods.indexArray(isolates);

		// Examine each of the sequences, and store the relevant ones
		for (int i = 0; i < sequences.length; i++) {

			// Are we interested in the current isolate?
			if (indexedIsolates.get(sequences[i].getName()) != null) {
				isolateSequences[indexedIsolates.get(sequences[i].getName())] = sequences[i];
			}
		}

		return isolateSequences;
	}

	public static void printLifeHistoriesForSampledAnimals(String fileName,
			Hashtable<String, LifeHistorySummary> animals, boolean sampled) throws IOException {

		// Open the output file
		BufferedWriter bWriter;
		if (sampled == true) {
			bWriter = WriteToFile.openFile(fileName, false);
			String header = "AnimalId\tSpecies\tIsolates\tClusters\tSamplingDates\tDistancesToRef\tDistancesToMRCA\t";
			header = header
					+ "DetectionDate\tCattleTestDates\tCattleTestResults\tXs\tYs\tMovementDates\tPremisesTypes\t";
			header = header
					+ "GroupIds\tSequencingQuality\tAnimalsEncountered\tContactStartDates\tContactEndDates\tContactHerds";
			WriteToFile.writeLn(bWriter, header);
		} else {
			bWriter = WriteToFile.openFile(fileName, true);
		}

		// Get a list of the animal IDs
		String[] animalIds = HashtableMethods.getKeysString(animals);

		// Print the life history summary of each sampled animal
		for (String id : animalIds) {
			WriteToFile.writeLn(bWriter, convertLifeHistorySummaryToString(animals.get(id), sampled));
		}

		// Close the output file
		WriteToFile.close(bWriter);
	}

	public static String convertLifeHistorySummaryToString(LifeHistorySummary animal, boolean sampled) {

		// Animal ID and species
		String output = animal.getAnimalId() + "\t" + animal.getSpecies();

		// Sampling Data - Isolates, Clusters, Dates, DistancesToRef
		if (sampled == true) {
			output = output + "\t" + ArrayMethods.toString(animal.getIsolateIds(), ",");
			output = output + "\t" + ArrayMethods.toString(animal.getClusters(), ",");
			output = output + "\t" + CalendarMethods.toString(animal.getSamplingDates(), "-", ",");
			output = output + "\t" + ArrayMethods.toString(animal.getDistancesToRef(), ",");
			output = output + "\t" + ArrayMethods.toString(animal.getDistancesToMRCA(), ",");

			// Date of detection
			output = output + "\t" + CalendarMethods.toString(animal.getDetectionDate(), "-");
		} else {

			output = output + "\t" + "NA";
			output = output + "\t" + ArrayMethods.toString(animal.getAssociatedClusters(), ",");
			output = output + "\t" + "NA";
			output = output + "\t" + "NA";
			output = output + "\t" + "NA";

			// Date of detection
			if (animal.getDetectionDate() != null) {
				output = output + "\t" + CalendarMethods.toString(animal.getDetectionDate(), "-");
			} else {
				output = output + "\t" + "NA";
			}
		}

		// Add Test date for cattle
		if (animal.getSpecies().matches("COW") == false || animal.getNTests() == 0) {
			output = output + "\t" + "NA\tNA";
		} else {
			output = output + "\t" + CalendarMethods.toString(animal.getTestDates(), "-", ",");
			output = output + "\t" + ArrayMethods.toString(animal.getTestResults(), ",");
		}

		// Movement locations and dates
		Calendar[] movementDates = animal.getMovementDates();
		double[][] coordinates = animal.getCoordinates();
		String[] premisesTypes = animal.getPremisesTypes();
		String[] groupIds = animal.getGroupIds();

		if (coordinates.length != 0) {
			String coordsX = "" + check(coordinates[0][0]);
			String coordsY = "" + check(coordinates[0][1]);
			for (int i = 1; i < movementDates.length; i++) {
				coordsX = coordsX + "," + check(coordinates[i][0]);
				coordsY = coordsY + "," + check(coordinates[i][1]);
			}

			output = output + "\t" + coordsX + "\t" + coordsY;
			output = output + "\t" + CalendarMethods.toString(movementDates, "-", ",");
			output = output + "\t" + ArrayMethods.toString(premisesTypes, ",");
			output = output + "\t" + ArrayMethods.toString(groupIds, ",");
		} else {
			output = output + "\tNA\tNA\tNA\tNA\tNA";
		}

		// Isolate Sequencing Quality
		double[] sequencingQualityOfIsolates = animal.getSequencingQualities();
		if (sequencingQualityOfIsolates.length > 0) {
			output = output + "\t" + ArrayMethods.toString(sequencingQualityOfIsolates, ",");
		} else {
			output = output + "\tNA";
		}

		// Contact Event Info
		ContactEvent[] contactInfo = animal.getContactInfo();
		if (contactInfo != null) {

			// Initialise an array to store the isolates of the animals
			// encountered
			String[] eartags = ArrayMethods.repeat(contactInfo[0].getIdOfAnimal(),
					contactInfo[0].getStartDates().length);
			Calendar[] starts = contactInfo[0].getStartDates();
			Calendar[] ends = contactInfo[0].getEndDates();
			String[] herds = contactInfo[0].getContactGroupsOrHerds();
			for (int i = 1; i < contactInfo.length; i++) {
				eartags = ArrayMethods.combine(eartags,
						ArrayMethods.repeat(contactInfo[i].getIdOfAnimal(), contactInfo[i].getStartDates().length));
				starts = CalendarMethods.combine(starts, contactInfo[i].getStartDates());
				ends = CalendarMethods.combine(ends, contactInfo[i].getEndDates());
				herds = ArrayMethods.combine(herds, contactInfo[i].getContactGroupsOrHerds());
			}

			// Convert the above information into strings
			output = output + "\t" + ArrayMethods.toString(eartags, ",");
			output = output + "\t" + CalendarMethods.toString(starts, "-", ",");
			output = output + "\t" + CalendarMethods.toString(ends, "-", ",");
			output = output + "\t" + ArrayMethods.toString(herds, ",");

		} else {
			output = output + "\tNA\tNA\tNA\tNA";
		}

		return output;
	}

	public static String check(double value) {
		String output = "NA";
		if (value != 0) {
			output = "" + value;
		}
		return output;
	}

	public static void summariseSampledAnimalsLifeHistories(Hashtable<String, LifeHistorySummary> sampledAnimals,
			CapturedBadgerLifeHistoryData badgerData, CattleIsolateLifeHistoryData cattleData) {

		// Get a list of the animalIds
		String[] animalIds = HashtableMethods.getKeysString(sampledAnimals);

		// Examine each sampled animal
		for (String id : animalIds) {

			// Is the animal a badger?
			if (sampledAnimals.get(id).getSpecies().matches("BADGER")) {
				summariseBadgersLifeHistory(sampledAnimals.get(id), badgerData);
			} else {
				summariseCowsLifeHistory(sampledAnimals.get(id), cattleData);
			}
		}
	}

	public static void summariseCowsLifeHistory(LifeHistorySummary animal, CattleIsolateLifeHistoryData cattleData) {

		/**
		 * For each movement record: - Latitude and Longitude of group captured
		 * in - Date captured
		 */

		// Get the movement dates - check that there were movements
		if (cattleData.getIsolates().get(animal.getAnimalId()).getNMovements() != 0) {
			Movement[] movements = cattleData.getIsolates().get(animal.getAnimalId()).getMovementRecords();
			Location location;
			double[] coordinates;

			for (int i = 0; i < movements.length; i++) {

				// Get the X and Y coordinates of the group captured in
				if (movements[i].getOffLocation().matches("")) {
					location = cattleData.getLocations().get(movements[i].getOnLocation());
				} else {
					location = cattleData.getLocations().get(movements[i].getOffLocation());
				}

				coordinates = new double[2];
				coordinates[0] = location.getX();
				coordinates[1] = location.getY();

				// Store the information from the current capture
				animal.addMovement(coordinates, movements[i].getDate(), location.getPremisesType(), location.getCph());
			}
		}

		// When was infection detected?
		Calendar detectionDate = cattleData.getIsolates().get(animal.getAnimalId()).getBreakdownDate();
		animal.setDetectionDate(detectionDate);

		// Was the animal ever tested?
		if (cattleData.getIsolates().get(animal.getAnimalId()).getNTests() > 0) {

			if (cattleData.getIsolates().get(animal.getAnimalId()).getNTests() != 0) {
				animal.setTestInfo(cattleData.getIsolates().get(animal.getAnimalId()).getTestDates(),
						cattleData.getIsolates().get(animal.getAnimalId()).getTestResults());
			}
		}
	}

	public static Calendar getTestDate(Calendar[] dates, String[] results) {

		// Were any of the tests positive?
		Calendar date = null;
		for (int i = 0; i < dates.length; i++) {

			if (results[i].matches("R") == true || results[i].matches("SL") == true) {
				date = dates[i];
				break;
			}
		}

		return date;
	}

	public static void summariseBadgersLifeHistory(LifeHistorySummary animal,
			CapturedBadgerLifeHistoryData badgerData) {

		/**
		 * For each movement record: - Latitude and Longitude of group captured
		 * in - Date captured
		 * 
		 */

		// Get the capture dates
		CaptureData captureData = badgerData.getBadgerCaptureHistories().get(animal.getAnimalId());
		Calendar[] captureDates = captureData.getCaptureDates();
		String[] groups = captureData.getGroupsInhabited();
		Hashtable<String, TerritoryCentroids> territoryCentroids = badgerData.getTerritoryCentroids();
		double[] coordinates;

		for (int i = 0; i < captureDates.length; i++) {

			// Get the X and Y coordinates of the group captured in
			coordinates = new double[2];
			if (territoryCentroids.get(groups[i]) != null) {
				coordinates = territoryCentroids.get(groups[i]).getCoords(
						String.valueOf(captureDates[i].get(Calendar.YEAR)));
			}

			// Store the information from the current capture
			animal.addMovement(coordinates, captureDates[i], "NA", groups[i]);
		}

		// When was infection detected?
		if (captureData.getWhenInfectionDetected() != -1) {
			Calendar detectionDate = captureData.getCaptureDates()[captureData.getWhenInfectionDetected()];
			animal.setDetectionDate(detectionDate);
		}
	}

	public static Hashtable<String, LifeHistorySummary> noteWhichAnimalsIsolatesAreFrom(Cluster[] clusters,
			CapturedBadgerLifeHistoryData badgerData, CattleIsolateLifeHistoryData cattleData) {

		// Initialise a hashtable to store the sampled animals
		Hashtable<String, LifeHistorySummary> sampledAnimals = new Hashtable<String, LifeHistorySummary>();

		// Initialise a variable to store the animal identifier - tattoo or
		// eartag
		String animalId;
		Calendar date;

		// Initialise an array to store the isolates in each cluster
		String[] isolates;
		int[] distancesToRef;
		double[] distancesToMRCA;
		double[] isolatesSequencingQuality;

		// Examine each of the isolates
		for (Cluster cluster : clusters) {

			// Get a list of the isolates
			isolates = cluster.getIsolateIds();
			distancesToRef = cluster.getDistancesToRef();
			distancesToMRCA = cluster.getDistancesToMRCA();
			isolatesSequencingQuality = cluster.getSequencingQualityForIsolates();

			for (int i = 0; i < isolates.length; i++) {

				// Is it a badger?
				if (isolates[i].matches("WB(.*)") == true) {

					// Get the animal id for the current badger
					animalId = badgerData.getSampledIsolateInfo().get(isolates[i]).getTattoo();

					// Get the sampling date for the current isolate
					date = badgerData.getSampledIsolateInfo().get(isolates[i]).getDate();

					// Has this badger already been encountered?
					if (sampledAnimals.get(animalId) != null) {
						sampledAnimals.get(animalId).appendIsolate(isolates[i], date, cluster.getId(),
								distancesToRef[i], distancesToMRCA[i], isolatesSequencingQuality[i]);
					} else {
						sampledAnimals.put(animalId, new LifeHistorySummary(animalId, "BADGER"));
						sampledAnimals.get(animalId).appendIsolate(isolates[i], date, cluster.getId(),
								distancesToRef[i], distancesToMRCA[i], isolatesSequencingQuality[i]);
					}

					// It's a cow!
				} else {

					if (cattleData.getEartagsForStrainIds().get(isolates[i]) != null) {

						// Get the animal id for the current cow
						animalId = cattleData.getEartagsForStrainIds().get(isolates[i]);

						// Get the sampling date for the current isolate
						date = cattleData.getIsolates().get(animalId).getCultureDate();

						// Has this cow already been encountered?
						if (sampledAnimals.get(animalId) != null) {
							sampledAnimals.get(animalId).appendIsolate(isolates[i], date, cluster.getId(),
									distancesToRef[i], distancesToMRCA[i], isolatesSequencingQuality[i]);
						} else {
							sampledAnimals.put(animalId, new LifeHistorySummary(animalId, "COW"));
							sampledAnimals.get(animalId).appendIsolate(isolates[i], date, cluster.getId(),
									distancesToRef[i], distancesToMRCA[i], isolatesSequencingQuality[i]);
						}
					}
				}
			}
		}

		return sampledAnimals;
	}

	public static void printLifespansOfIsolatesInEachCluster(Cluster[] clusters, String fileName,
			CapturedBadgerLifeHistoryData badgerData, CattleIsolateLifeHistoryData cattleData) throws IOException {

		// Open the output file
		BufferedWriter bWriter = WriteToFile.openFile(fileName, false);
		WriteToFile.writeLn(bWriter, "IsolateId\tAnimalId\tSpecies\tCluster\tSamplingDate\tStart\tEnd\tDetection");

		// Initialise the necessary variables to store the lifespan data
		String samplingDate;
		String[] birthDeathDetection;

		// For each isolate print its birth, death, sampling time, infection
		// detected
		for (Cluster cluster : clusters) {

			for (String id : cluster.getIsolateIds()) {

				// Get the sampling time for the isolate
				samplingDate = getSamplingTimeForIsolate(id, badgerData, cattleData);

				// Get the birth, death, detection
				birthDeathDetection = getBirthDeathAndDetectionForSampledAnimal(id, badgerData, cattleData);

				// Print the information
				WriteToFile.writeLn(bWriter, id + "\t" + getSpecies(id) + "\t" + cluster.getId() + "\t" + samplingDate
						+ "\t" + ArrayMethods.toString(birthDeathDetection, "\t"));
			}
		}

		// Close the output file
		WriteToFile.close(bWriter);
	}

	public static String getSpecies(String id) {
		String species = "BADGER";
		if (id.matches("TB(.*)") == true) {
			species = "CATTLE";
		}
		return species;
	}

	public static String[] getBirthDeathAndDetectionForSampledAnimal(String id,
			CapturedBadgerLifeHistoryData badgerData, CattleIsolateLifeHistoryData cattleData) {

		// Initialise an array to store the birth, death and detection dates
		String[] dates = ArrayMethods.repeat("NA", 3);

		if (id.matches("WB(.*)")) {

			String tattoo = badgerData.getSampledIsolateInfo().get(id).getTattoo();

			CaptureData captureData = badgerData.getBadgerCaptureHistories().get(tattoo);

			dates[0] = CalendarMethods.toString(captureData.getCaptureDates()[0], "-");
			dates[1] = CalendarMethods.toString(captureData.getCaptureDates()[captureData.getCaptureDates().length - 1],
					"-");
			dates[2] = CalendarMethods.toString(captureData.getCaptureDates()[captureData.getWhenInfectionDetected()],
					"-");

		} else {

			String eartag = cattleData.getEartagsForStrainIds().get(id);
			Movement[] movements = cattleData.getIsolates().get(eartag).getMovementRecords();

			// If there are movements available, get when they started and
			// finished
			if (movements.length != 0) {
				dates[0] = CalendarMethods.toString(movements[0].getDate(), "-");
				dates[1] = CalendarMethods.toString(movements[movements.length - 1].getDate(), "-");
			} else {
				System.out.println("No movements for: " + id);
			}
			dates[2] = CalendarMethods.toString(cattleData.getIsolates().get(eartag).getBreakdownDate(), "-");
		}

		return dates;
	}

	public static String getSamplingTimeForIsolate(String id, CapturedBadgerLifeHistoryData badgerData,
			CattleIsolateLifeHistoryData cattleData) {

		String date = "";

		// Is it a badger?
		if (id.matches("WB(.*)")) {

			date = CalendarMethods.toString(badgerData.getSampledIsolateInfo().get(id).getDate(), "-");
		} else {

			String eartag = cattleData.getEartagsForStrainIds().get(id);
			date = CalendarMethods.toString(cattleData.getIsolates().get(eartag).getCultureDate(), "-");
		}

		return date;
	}

	public static Cluster[] readClustersFile(String fileName) throws IOException {

		// Open the input File
		InputStream input = new FileInputStream(fileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));

		// Initialise an array to store the cluster information
		Cluster[] clusters = new Cluster[999];
		int clusterIndex = -1;

		// Initialise an array to store isolate Ids
		String[] ids = new String[999];
		int pos = -1;

		// Initialise variables to process the fasta file
		String line = null;
		String[] cols;
		int lineNo = 0;

		// Begin reading the file
		while ((line = reader.readLine()) != null) {
			lineNo++;

			if (lineNo == 1) {
				continue;
			}

			// Split the current line into its columns
			cols = line.split(",");

			// Have we reached a new cluster?
			if (lineNo != 2 && clusterIndex != Integer.parseInt(cols[1])) {

				// Store the information for the previous cluster
				clusters[clusterIndex] = new Cluster(clusterIndex, ArrayMethods.subset(ids, 0, pos));

				// Start the new cluster
				clusterIndex = Integer.parseInt(cols[1]);
				pos = 0;
				ids = new String[999];
				ids[pos] = cols[0];

			} else if (lineNo != 2) {
				pos++;
				ids[pos] = cols[0];
			} else {
				clusterIndex = Integer.parseInt(cols[1]);
				pos++;
				ids[pos] = cols[0];
			}
		}

		// Add the last cluster
		clusters[clusterIndex] = new Cluster(clusterIndex, ArrayMethods.subset(ids, 0, pos));

		// Close the input file
		input.close();
		reader.close();

		return Cluster.subset(clusters, 0, clusterIndex);
	}
}
