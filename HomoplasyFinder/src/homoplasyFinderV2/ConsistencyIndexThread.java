package homoplasyFinderV2;

import java.util.ArrayList;

public class ConsistencyIndexThread extends Thread{

	// Start and end of positions assigned to current thread
	public int startPosition;
	public int endPosition;
	
	// Tree
	private ArrayList<Node> internalNodes;
	private int nTerminalNodes;
	
	// Sequences
	private ArrayList<Sequence> sequences;
	public int nSites;
	private int nStatesPerSite;
	private int[] terminalNodeIndexForEachSequence;
	
	// Consistency index
	public int[][] stateCountsPerPosition;
	public int[] minNumberChangesOnTreeAtEachPosition;
	public int[][] internalNodeIndicesOfChanges;
	public ArrayList<Integer> inconsistentPositions;
	public double[] consistencyIndices;
	
	public ConsistencyIndexThread(String name, int start, int end, int nTerminalNodes, ArrayList<Sequence> sequences,
			int[] terminalNodeIndexForEachSequence, ArrayList<Node> internalNodes, int nStatesPerSite) {
		
		// Assign name to the current thread - using the position
		super(name);
		
		// Store the input information
		this.startPosition = start;
		this.endPosition = end;
		this.nSites = (this.endPosition - this.startPosition) + 1;
		this.nTerminalNodes = nTerminalNodes;
		this.sequences = sequences;
		this.terminalNodeIndexForEachSequence = terminalNodeIndexForEachSequence;
		this.internalNodes = internalNodes;
		this.nStatesPerSite = nStatesPerSite;
	}
	
	// Define a run method - this will execute when thread started
	public void run(){

		// Initialise the necessary variables
		this.stateCountsPerPosition = new int[this.nSites][this.nStatesPerSite]; // Counts for each possible (A, C, G, T) at each position
		this.minNumberChangesOnTreeAtEachPosition = new int[this.nSites]; // The minimum number of changes for each position
		this.internalNodeIndicesOfChanges = new int[this.nSites][this.internalNodes.size()]; // The internal node indices where those changes occur
		this.inconsistentPositions = new ArrayList<Integer>(); // Array store the inconsistent sites (consistency < 1)
		this.consistencyIndices = new double[this.nSites]; // Consistency index of each site
		
		// Calculate the consistency index of each position
		ConsistencyIndex.calculateConsistencyIndexForEachSiteOnPhylogeny(this.nSites, this.nTerminalNodes, this.nStatesPerSite,
						this.sequences, this.terminalNodeIndexForEachSequence, this.stateCountsPerPosition, this.internalNodes, this.internalNodeIndicesOfChanges,
						this.minNumberChangesOnTreeAtEachPosition, this.inconsistentPositions,
						this.consistencyIndices, this.startPosition, this.endPosition);
	}
	
	// Getting methods
	public int getNSites() {
		return nSites;
	}
	public int[][] getStateCountsPerPosition() {
		return stateCountsPerPosition;
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

	
	// General methods
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

	// Define a method to wait until threads finished
	public static void waitUntilAllFinished(ConsistencyIndexThread[] threads){

		// Initialise a variable to record whether all threads finished
		boolean allFinished = false;

		// Keep checking the threads until they're all finished
		while(allFinished == false){

			// Check whether all threads finished
			allFinished = finished(threads);
		}
	}
}
