package homoplasyFinderV2;

import java.util.ArrayList;

public class ConsistencyIndexThread extends Thread{

	// Tree
	private Tree tree;
	
	// States table
	private States statesTable;
	
	// Record number of sites examined by current thread
	private int nSitesExamined;
	
	// Consistency index
	private int[][] stateCountsAtEachPosition; // Concatenated counts for each state at each position
	private String[][] statesAtEachPosition;
	private int[] minNumberChangesOnTreeAtEachPosition;
	private int[][] internalNodeIndicesOfChanges;
	private ArrayList<Integer> inconsistentPositions;
	private double[] consistencyIndices;
	
	// Store the start and end of section current thread will work on
	private int start;
	private int end;
	
	public ConsistencyIndexThread(String name, Tree tree, States tipStates, int start, int end) {
		
		// Assign name to the current thread - using the position
		super(name);
				
		// Store the tree and tip states at each position/trait
		this.tree = tree;
		this.statesTable = tipStates;
		
		// Store the start and end
		this.start = start;
		this.end = end;
		
		// Calculate the number of sites examined by current thread
		this.nSitesExamined = (end - start) + 1;
	}
	
	// Define a run method - this will execute when thread started
	public void run(){

		// Initialise all the necessary variables for storing the information associated with calculating the consistency indices
		this.stateCountsAtEachPosition = new int[this.nSitesExamined][0]; // Concatenated counts for each possible state at each position
		this.statesAtEachPosition = new String[this.nSitesExamined][0]; // Ordered array of states observed at each position
		this.minNumberChangesOnTreeAtEachPosition = new int[this.nSitesExamined]; // The minimum number of changes for each position
		this.internalNodeIndicesOfChanges = new int[this.nSitesExamined][this.tree.getInternalNodes().size()]; // The internal node indices where those changes occur
		this.inconsistentPositions = new ArrayList<Integer>(); // Array store the inconsistent sites (consistency < 1)
		this.consistencyIndices = new double[this.nSitesExamined]; // Consistency index of each site
		
		// Calculate the consistency index of each position
		ConsistencyIndex.calculateConsistencyIndices(this.statesTable, this.tree, this.stateCountsAtEachPosition,
				this.statesAtEachPosition, this.internalNodeIndicesOfChanges, this.minNumberChangesOnTreeAtEachPosition,
				this.inconsistentPositions, this.consistencyIndices, this.start, this.end);
	}
	
	// Methods to monitor progress
	public static boolean finished(ConsistencyIndexThread[] threads){

		// Initialise a variable to record whether all finished
		boolean finished = true;

		// Examine each of the threads to see if any aren't finished
		for(ConsistencyIndexThread thread : threads){

			// Check if current thread finished
			if(thread.isAlive() == true){

				finished = false;
				break;
			}
		}

		return finished;
	}

	public static void waitUntilAllFinished(ConsistencyIndexThread[] threads){

		// Initialise a variable to record whether all threads finished
		boolean allFinished = false;

		// Keep checking the threads until they're all finished
		while(allFinished == false){

			// Check whether all threads finished
			allFinished = finished(threads);
		}
	}

	// Getting methods
	public States getStatesTable() {
		return statesTable;
	}
	public int[][] getStateCountsAtEachPosition() {
		return stateCountsAtEachPosition;
	}
	public String[][] getStatesAtEachPosition() {
		return statesAtEachPosition;
	}
	public int[] getMinNumberChangesOnTreeAtEachPosition() {
		return minNumberChangesOnTreeAtEachPosition;
	}
	public int[][] getInternalNodeIndicesOfChanges() {
		return internalNodeIndicesOfChanges;
	}
	public ArrayList<Integer> getInconsistentPositions() {
		return inconsistentPositions;
	}
	public double[] getConsistencyIndices() {
		return consistencyIndices;
	}
	public int getStart() {
		return start;
	}
	public int getEnd() {
		return end;
	}
	public int getNSitesExamined() {
		return this.nSitesExamined;
	}
	
	// Methods run multithreading
	public static void calculateConsistencyIndicesOnMultipleThreads(States states, Tree tree, int[][] stateCountsAtEachPosition,
			String[][] statesAtEachPosition, int[][] internalNodeIndicesOfChanges, int[] minNumberChangesOnTreeAtEachPosition,
			ArrayList<Integer> inconsistentPositions, double[] consistencyIndices) {
		
		// Find out the number of threads available
		int nThreads = Runtime.getRuntime().availableProcessors();

		// Initialise an array to store the thread objects
		ConsistencyIndexThread[] threads = new ConsistencyIndexThread[nThreads];
		
		// Calculate the number of sites to assign to each thread
		int nSitesPerThread = states.getNSites() / nThreads;
		
		// Start the threads
		for(int i = 0; i < nThreads; i++) {
			
			// Calculate the start and end of the current subset of positions to assign to the current thread
			int start = (i * nSitesPerThread);
			int end = start + (nSitesPerThread - 1);
			if(end > states.getNSites() - 1 || i == nThreads - 1) {
				end = states.getNSites() - 1;
			}
			
			// Create the current thread with the necessary data
			threads[i] = new ConsistencyIndexThread("thread-" + i, tree, states, start, end);
			
			// Start the current thread
			threads[i].start();
		}
		
		// Check the threads are finished
		waitUntilAllFinished(threads);
		
		// Collect the data calculated on each thread
		collect(threads, stateCountsAtEachPosition, statesAtEachPosition, minNumberChangesOnTreeAtEachPosition, internalNodeIndicesOfChanges,
				inconsistentPositions, consistencyIndices, nSitesPerThread);		
	}
	
	public static void collect(ConsistencyIndexThread[] threads, int[][] stateCountsAtEachPosition, String[][] statesAtEachPosition, int[] minNumberChangesOnTreeAtEachPosition,
			int[][] internalNodeIndicesOfChanges, ArrayList<Integer> inconsistentPositions, double[] consistencyIndices, int nSitesPerThread) {
		
		// Input the data collected from each thread
		for(int i = 0; i < threads.length; i++) {
			
			// Retrieve the data for the positions analysed by the current thread
			for(int position = 0; position < threads[i].getNSitesExamined(); position++) {
				
				// Calculate the index of the current position from the current thread in the overall data
				int positionIndex = position + (i * nSitesPerThread);
				
				// Store the information calculated for the current position
				stateCountsAtEachPosition[positionIndex] = threads[i].getStateCountsAtEachPosition()[position];
				statesAtEachPosition[positionIndex] = threads[i].getStatesAtEachPosition()[position];
				minNumberChangesOnTreeAtEachPosition[positionIndex] = threads[i].getMinNumberChangesOnTreeAtEachPosition()[position];
				internalNodeIndicesOfChanges[positionIndex] = threads[i].getInternalNodeIndicesOfChanges()[position];
				consistencyIndices[positionIndex] = threads[i].getConsistencyIndices()[position]; // Consistency index of each site
			}
			
			// Store the inconsistent positions found by the current thread
			addInconsistentPositionsFromThread(inconsistentPositions, threads[i]);
		}
	}
	
	public static void addInconsistentPositionsFromThread(ArrayList<Integer> inconsistentPositions, ConsistencyIndexThread thread) {
		
		for(int position : thread.getInconsistentPositions()) {
			inconsistentPositions.add(position);
		}
	}
}
