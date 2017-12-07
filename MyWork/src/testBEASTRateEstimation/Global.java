package testBEASTRateEstimation;

import java.util.Hashtable;

public class Global {

	public static int seedIndex;
	
	public static int mutationEventNo;
	public static int[] who = new int[99999];
	public static int[] when = new int[99999];	
	
	public static int[] constantSiteCounts = new int[4];
	public static Hashtable<Integer, Integer> sitesUsed;
	public static double[] mutations = new double[0];
	public static int noMutationsFellOnUsedSite;
	
	public static int[][] mutationEventInfo;
	
	public static int geneticDistance = 0;
	public static int temporalDistance = 0;
	
	public static int[][] sampledAdjacencyMatrix;
		
	public static int arraySizeLimit;
	public static int[] reference;
	public static int[][] mutationEventInfoNew;
	public static Hashtable<Integer, Integer> informativeGenomeEventSites;
	
	public static IntArray windowSizes;
	
	public static double[][] stateTransitionTimes;
	public static int[][] stateTransitionCounts;
	public static int[] nSampledFromEachState;
}

