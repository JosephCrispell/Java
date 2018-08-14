package identifyingHomoplasies;

import java.io.IOException;

import geneticDistances.Sequence;
import methods.GeneticMethods;
import newickTree.ConsistencyIndex;
import newickTree.Tree;

public class HomoplasyFinder {

	public static void main(String[] args) throws IOException {
		
//		// Check if help requested
//		if(args[0].equals("-help") || args[0].equals("") || args[0].equals("-h") || args[0].equals("help") || args[0].equals("--help") ){
//			System.out.println("HomoplasyFinder: a tool to identify homoplasies within a phylogenetic tree and alignment");
//			System.out.println("\nCommand Line Structure:");
//			System.out.println("\tjava -jar homoplasyFinder_DATE.jar verbose sequences.fasta newick.tree\n");
//			System.out.println("\t\tverbose\tDetailed output [0] or none [1]");
//			System.out.println("\t\tsequences.fasta\tFASTA file containing alignment");
//			System.out.println("\t\tnewick.tree\tNewick formatted tree file");
//			System.out.println("\nNotes:");
//			System.out.println("First line of input FASTA file contains the number of isolates and sites in the file");
//
//			System.exit(0);
//		}
//
//		// Get the command line arguments
//		boolean verbose = args[0].matches("1");
//		String path = "";
		boolean verbose = true;
		
		// Set the path
		String path = "/home/josephcrispell/Desktop/Research/Homoplasy/";
		
		// Read in the sequences
//		Sequence[] sequences = GeneticMethods.readFastaFile(args[1], false);
		Sequence[] sequences = GeneticMethods.readFastaFile(path + "example_10-08-18.fasta", false);
		
		// Read the NEWICK tree and store as a traversable node set
//		Tree tree = new Tree(args[2]);
		Tree tree = new Tree(path + "example-AFTER_10-08-18.tree");
		
		// Calculate the consistency index of each position in the alignment on the phylogeny
		ConsistencyIndex consistency = new ConsistencyIndex(tree, sequences, verbose);
		
		// Create a FASTA file without inconsistent sites
		consistency.printSequencesWithoutInConsistentSites(path + "example_withoutInconsistentSites_10-08-18.fasta");
		
		// Create an annotated NEWICK tree file
		consistency.printAnnotatedTree(path + "example-AFTER_withInconsistentSitesAnnotated_10-08-18.tree");
		
		// Create a report file
		consistency.printSummary(path + "consistencyIndexReport_10-08-18.txt");
	}
	
	public static String test(String input) {
		return "This was input into Java: " + input;
	}
	
	public static void runHomoplasyFinderFromR(String treeFile, String fastaFile, String pathForOutput,
			boolean verbose) throws IOException {
		
		// Read in the sequences
		Sequence[] sequences = GeneticMethods.readFastaFile(fastaFile, false);
		
		// Read the NEWICK tree and store as a traversable node set
		Tree tree = new Tree(treeFile);
		
		// Calculate the consistency index of each position in the alignment on the phylogeny
		ConsistencyIndex consistency = new ConsistencyIndex(tree, sequences, verbose);
		
		// Create a FASTA file without inconsistent sites
		consistency.printSequencesWithoutInConsistentSites(pathForOutput + "example_withoutInconsistentSites_10-08-18.fasta");
		
		// Create an annotated NEWICK tree file
		consistency.printAnnotatedTree(pathForOutput + "example-AFTER_withInconsistentSitesAnnotated_10-08-18.tree");
		
		// Create a report file
		consistency.printSummary(pathForOutput + "consistencyIndexReport_10-08-18.txt");
	}
	
}
