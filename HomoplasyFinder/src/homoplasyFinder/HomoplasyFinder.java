package homoplasyFinder;

import java.io.IOException;

public class HomoplasyFinder {

	public static void main(String[] args) throws IOException {
		
		// Check if help requested
		if(args.length == 0 || args[0].equals("-help") || args[0].equals("") || args[0].equals("-h") || args[0].equals("help") || 
				args[0].equals("--help")){
			System.out.println("HomoplasyFinder: a tool to identify homoplasies within a phylogenetic tree and alignment");
			System.out.println("\nCommand Line Structure:");
			System.out.println("\tjava -jar homoplasyFinder_DATE.jar verbose sequences.fasta newick.tree\n");
			System.out.println("\t\tverbose\tDetailed output [0] or none [1]");
			System.out.println("\t\tsequences.fasta\tFASTA file containing alignment");
			System.out.println("\t\tnewick.tree\tNewick formatted tree file");
			System.out.println("\nNotes:");
			System.out.println("First line of input FASTA file contains the number of isolates and sites in the file");

			System.exit(0);
		}

		// Get the command line arguments
		boolean verbose = args[0].matches("1");
		String path = "";
//		boolean verbose = true;
		
		// Get the current date
		String date = Methods.getCurrentDate("dd-MM-yy");
		
		// Set the path
//		String path = "/home/josephcrispell/Desktop/Research/Homoplasy/";
		
		// Read in the sequences
		Sequence[] sequences = Methods.readFastaFile(args[1], false);
//		Sequence[] sequences = Methods.readFastaFile(path + "example_10-08-18.fasta", false);
		
		// Read the NEWICK tree and store as a traversable node set
		Tree tree = new Tree(args[2]);
//		Tree tree = new Tree(path + "example-AFTER_10-08-18.tree");
		
		// Calculate the consistency index of each position in the alignment on the phylogeny
		ConsistencyIndex consistency = new ConsistencyIndex(tree, sequences, verbose);
		
		// Create a FASTA file without inconsistent sites
		consistency.printSequencesWithoutInConsistentSites(path + "nInconsistentSites_" + date + ".fasta");
		
		// Create an annotated NEWICK tree file
		consistency.printAnnotatedTree(path + "annotatedNewickTree_" + date + ".tree");
		
		// Create a report file
		consistency.printSummary(path + "consistencyIndexReport_" + date + ".txt");
	}
	
	public static int[] runHomoplasyFinderFromR(String treeFile, String fastaFile, String pathForOutput,
			boolean createFasta, boolean createReport, boolean createTree, boolean verbose) throws IOException {
		
		// Get the current date
		String date = Methods.getCurrentDate("dd-MM-yy");
		
		// Read in the sequences
		Sequence[] sequences = Methods.readFastaFile(fastaFile, false);
		
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
			consistency.printSummary(pathForOutput + "consistencyIndexReport_" + date + ".txt");
			if(verbose) {
				System.out.println("Created report detailing the inconsistent sites identified:\n\t" + 
						pathForOutput + "consistencyIndexReport_" + date + ".txt");
			}
		}	
		
		return consistency.getPositions();
	}
	
}
