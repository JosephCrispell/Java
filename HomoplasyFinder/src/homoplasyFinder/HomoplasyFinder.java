package homoplasyFinder;

import java.io.IOException;
import java.util.ArrayList;

public class HomoplasyFinder {

	public static void main(String[] args) throws IOException {
		
		// Get the command line arguments
		Arguments arguments = new Arguments(args);	
		
		// Get the current date
		String date = Methods.getCurrentDate("dd-MM-yy");
		
		// Set the path
		String path = "";
		
		// Read in the sequences
		ArrayList<Sequence> sequences = Methods.readFastaFile(arguments.getFastaFile(), false);
		
		// Read the NEWICK tree and store as a traversable node set
		Tree tree = new Tree(arguments.getTreeFile());
		
		// Calculate the consistency index of each position in the alignment on the phylogeny
		ConsistencyIndex consistency = new ConsistencyIndex(tree, sequences, arguments.isVerbose(), arguments.isMultithread());
		
		// Create a FASTA file without inconsistent sites
		if(arguments.isCreateFasta()) {
			consistency.printSequencesWithoutInConsistentSites(path + "noInconsistentSites_" + date + ".fasta");
		}
			
		// Create an annotated NEWICK tree file
		if(arguments.isCreateAnnotatedNewickTree()) {
			consistency.printAnnotatedTree(path + "annotatedNewickTree_" + date + ".tree");
		}
		
		// Create a report file
		consistency.printSummary(path + "consistencyIndexReport_" + date + ".txt", arguments.isIncludeConsistentSitesInReport());
	}	
	
	public static int[] runHomoplasyFinderFromR(String treeFile, String fastaFile, String pathForOutput,
			boolean createFasta, boolean createReport, boolean createTree, boolean includeConsistentSites, boolean verbose,
			boolean multithread) throws IOException {
		
		// Get the current date
		String date = Methods.getCurrentDate("dd-MM-yy");
		
		// Read in the sequences
		ArrayList<Sequence> sequences = Methods.readFastaFile(fastaFile, false);
		
		// Read the NEWICK tree and store as a traversable node set
		Tree tree = new Tree(treeFile);
		
		// Calculate the consistency index of each position in the alignment on the phylogeny
		ConsistencyIndex consistency = new ConsistencyIndex(tree, sequences, verbose, multithread);
		
		// Create a FASTA file without inconsistent sites
		if(createFasta) {
			consistency.printSequencesWithoutInConsistentSites(pathForOutput + "nInconsistentSites_" + date + 
					".fasta");
			if(verbose) {
				System.out.println("\nCreated output FASTA file without inconsistent sites:\n\t" + 
						pathForOutput + "nInconsistentSites_" + date + ".fasta");
			}
		}
		
		// Create an annotated NEWICK tree file
		if(createTree) {
			consistency.printAnnotatedTree(pathForOutput + "annotatedNewickTree_" + date + ".tree");
			if(verbose) {
				System.out.println("Created Newick phylogenetic tree file with annotated inconsistent positions:\n\t" + 
						pathForOutput + "annotatedNewickTree_" + date + ".tree");
			}
		}		
		
		// Create a report file
		if(createReport) {
			consistency.printSummary(pathForOutput + "consistencyIndexReport_" + date + ".txt", includeConsistentSites);
			if(verbose) {
				System.out.println("Created report detailing the inconsistent sites identified:\n\t" + 
						pathForOutput + "consistencyIndexReport_" + date + ".txt");
			}
		}	
		
		return consistency.getPositions();
	}
	
}
