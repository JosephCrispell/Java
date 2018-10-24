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
//		String path = "/home/josephcrispell/Desktop/ClonalFrameML_SaureusData/";
//		String path = "/home/josephcrispell/Desktop/Research/Homoplasy/DataForTesting/";
		
		// Read in the sequences
		ArrayList<Sequence> sequences = Methods.readFastaFile(arguments.fastaFile, false);
//		ArrayList<Sequence> sequences = Methods.readFastaFile(path + "Saureus_sequences.fasta", false);
//		ArrayList<Sequence> sequences = Methods.readFastaFile(path + "example_09-04-18.fasta", false);
		
		// Read the NEWICK tree and store as a traversable node set
		Tree tree = new Tree(arguments.treeFile);
//		Tree tree = new Tree(path + "Saureus_phyML.newick");
//		Tree tree = new Tree(path + "example-TRUE_09-04-18.tree");
		
		// Calculate the consistency index of each position in the alignment on the phylogeny
		ConsistencyIndex consistency = new ConsistencyIndex(tree, sequences, arguments.verbose);
		
		// Create a FASTA file without inconsistent sites
		if(arguments.createFasta) {
			consistency.printSequencesWithoutInConsistentSites(path + "noInconsistentSites_" + date + ".fasta");
		}
			
		// Create an annotated NEWICK tree file
		if(arguments.createAnnotatedNewickTree) {
			consistency.printAnnotatedTree(path + "annotatedNewickTree_" + date + ".tree");
		}
		
		// Create a report file
		consistency.printSummary(path + "consistencyIndexReport_" + date + ".txt", arguments.includeConsistentSitesInReport);
	}	
	
	public static int[] runHomoplasyFinderFromR(String treeFile, String fastaFile, String pathForOutput,
			boolean createFasta, boolean createReport, boolean createTree, boolean includeConsistentSites, boolean verbose) throws IOException {
		
		// Get the current date
		String date = Methods.getCurrentDate("dd-MM-yy");
		
		// Read in the sequences
		ArrayList<Sequence> sequences = Methods.readFastaFile(fastaFile, false);
		
		// Read the NEWICK tree and store as a traversable node set
		Tree tree = new Tree(treeFile);
		
		// Calculate the consistency index of each position in the alignment on the phylogeny
		ConsistencyIndex consistency = new ConsistencyIndex(tree, sequences, verbose);
		
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
