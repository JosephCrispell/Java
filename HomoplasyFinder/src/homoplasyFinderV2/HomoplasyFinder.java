package homoplasyFinderV2;

import java.io.IOException;

public class HomoplasyFinder {

	public static void main(String[] args) throws IOException {
		
		// Get the command line arguments
		Arguments arguments = new Arguments(args);
//		String path = "/home/josephcrispell/Desktop/WorkingOnHomoplasyFinder/";
//		String[] testArgs = {
//				"--tree", path + "example_29-11-19.tree",
//				"--fasta", path + "example_29-11-19.fasta",
//				"--traits", path + "example_traits_29-11-19.csv",
//				"--multithread",
//				"--verbose", "--createFasta", "--createAnnotatedTree"};
//		Arguments arguments = new Arguments(testArgs);
		
		// Get the current date
		String date = Methods.getCurrentDate("dd-MM-yy");
		
		// Set the path
		String path = "";

		// Read the NEWICK tree and store as a traversable node set
		Tree tree = new Tree(arguments.getTreeFile());
		
		// Initialise a states object to store the states of each tip at each site/trait
		States tipStates = null;
		
		// Check if FASTA file provided
		if(arguments.getFastaFile() != null) {
			
			// Read in the sequences
			tipStates = new States(tree.getTerminalNodes(), arguments.getFastaFile(), "fasta", arguments.isVerbose());
			
		// Otherwise read in the traits file
		}else if(arguments.getTraitsFile() != null){
			
			// Read in the states for each trait
			tipStates = new States(tree.getTerminalNodes(), arguments.getTraitsFile(), "traits", arguments.isVerbose());
		}
		
		// Calculate the consistency index of each position/trait in the states table on the phylogeny
		ConsistencyIndex consistency = new ConsistencyIndex(tree, tipStates, arguments.isVerbose(), arguments.isMultithread());
		
		// Create a FASTA file without inconsistent sites
		if(tipStates.getFileType().matches("fasta") && arguments.isCreateFasta()) {
			consistency.printFASTAWithoutInConsistentSites(path + "sequences_noInconsistentSites_" + date + ".fasta");
		}
					
		// Create an annotated NEWICK tree file
		if(arguments.isCreateAnnotatedNewickTree()) {
			consistency.printAnnotatedTree(path + "annotatedNewickTree_" + date + ".tree");
		}
		
		// Create a report file
		consistency.printSummary(path + "consistencyIndexReport_" + date + ".txt", arguments.isIncludeConsistentSitesInReport());
	}	
}
