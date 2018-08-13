package newickTree;

import java.io.IOException;

import geneticDistances.Sequence;
import methods.GeneticMethods;

public class methods {
	
	public static void main(String[] args) throws IOException {
		
		// Get current time (in milliseconds)
		long start = System.nanoTime();
		
		// Set the path
		String path = "/home/josephcrispell/Desktop/Research/Homoplasy/";
		
		// Read the NEWICK tree and store as a traversable node set
		Tree tree = new Tree(path + "example-AFTER_10-08-18.tree");
		
		// Read in the sequences
		Sequence[] sequences = GeneticMethods.readFastaFile(path + "example_10-08-18.fasta", false);
		
		// Calculate the consistency index of each position in the alignment on the phylogeny
		ConsistencyIndex consistency = new ConsistencyIndex(tree, sequences, true);
		
//		// Create a FASTA file without inconsistent sites
//		consistency.printSequencesWithoutInConsistentSites(path + "example_withoutInconsistentSites_10-08-18.fasta");
//		
//		// Create an annotated NEWICK tree file
//		consistency.printAnnotatedTree(path + "example-AFTER_withInconsistentSitesAnnotated_10-08-18.tree");
//		
//		// Create a report file
//		consistency.printSummary(path + "consistencyIndexReport_10-08-18.txt");
		
		// Get the current time (in milliseconds)
		long end = System.nanoTime();

		System.out.println("Time taken: " + (end - start) / 1000000000.0 + " seconds");
	}
	
}
