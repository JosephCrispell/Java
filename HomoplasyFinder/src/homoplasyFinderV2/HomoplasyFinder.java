package homoplasyFinderV2;

import java.io.IOException;
import java.util.ArrayList;

import homoplasyFinder.Methods;
import homoplasyFinder.Sequence;

public class HomoplasyFinder {

	public static void main(String[] args) throws IOException {
		
		// Get the command line arguments
//		Arguments arguments = new Arguments(args);
		String path = "/home/josephcrispell/Desktop/WorkingOnHomoplasyFinder/";
		String[] testArgs = {
				"--tree", path + "example_29-11-19.tree",
				"--fasta", path + "example_29-11-19.fasta"};
		Arguments arguments = new Arguments(testArgs);
		
		// Get the current date
		String date = Methods.getCurrentDate("dd-MM-yy");
		
		// Set the path
//		String path = "";

		// Read the NEWICK tree and store as a traversable node set
		Tree tree = new Tree(arguments.getTreeFile());
		
		// Initialise a matrix to store the states of each tip at each site/trait
		int[][] tipStates;
		
		// Check if FASTA file provided
		if(arguments.getFastaFile() != null) {
			
			// Read in the sequences
			ArrayList<Sequence> sequences = Methods.readFastaFile(arguments.getFastaFile(), arguments.isVerbose());
			
			// Build tip states matrix (
			
		// Otherwise read in the traits file
		}else {
			
		}
		
	}
	
	
}
