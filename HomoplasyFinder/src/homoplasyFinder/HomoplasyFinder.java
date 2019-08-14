package homoplasyFinder;

import java.io.IOException;
import java.util.ArrayList;

public class HomoplasyFinder {

	public static void main(String[] args) throws IOException {
		
		// Get the command line arguments
		Arguments arguments = new Arguments(args);
//		String path = "/home/josephcrispell/Desktop/Research/Homoplasy/INDELS/";
//		String[] testArgs = {
//				"--tree", path + "neighbourJoining_13-08-19.tree",
//				"--presenceAbsence", path + "indel_sites.csv"};
//		Arguments arguments = new Arguments(testArgs);
		
		// Get the current date
		String date = Methods.getCurrentDate("dd-MM-yy");
		
		// Set the path
		String path = "";

		// Read the NEWICK tree and store as a traversable node set
		Tree tree = new Tree(arguments.getTreeFile());
		
		// Check if FASTA file provided
		if(arguments.getFastaFile() != null) {
		
			// Read in the sequences
			ArrayList<Sequence> sequences = Methods.readFastaFile(arguments.getFastaFile(), arguments.isVerbose());
			
			// Calculate the consistency index of each position in the alignment on the phylogeny
			ConsistencyIndex consistency = new ConsistencyIndex(tree, sequences, arguments.isVerbose(), arguments.isMultithread(), 4);
			
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
					
		// Read in presence absence file instead
		}else {
			
			// Read in the presence absence matrix
			PresenceAbsence presenceAbsenceMatrix = Methods.readPresenceAbsenceTable(arguments.getPresenceAbsenceFile(), 
					arguments.isVerbose());
			
			// Calculate the consistency index of each position in the alignment on the phylogeny
			ConsistencyIndex consistency = new ConsistencyIndex(tree, presenceAbsenceMatrix.getBooleanSequences(), arguments.isVerbose(),
					arguments.isMultithread(), 2);
			
			// Create an annotated NEWICK tree file
			if(arguments.isCreateAnnotatedNewickTree()) {
				consistency.printAnnotatedTree(path + "annotatedNewickTree_" + date + ".tree");
			}
			
			// Create a report file
			consistency.printSummary(path + "consistencyIndexReport_" + date + ".txt", presenceAbsenceMatrix.getRegionCoords(),
					arguments.isIncludeConsistentSitesInReport());
		}
	}	
	
	public static int[] runHomoplasyFinderFromR(String treeFile, String fastaFile, String presenceAbsenceFile, String pathForOutput,
			boolean createFasta, boolean createReport, boolean createTree, boolean includeConsistentSites, boolean verbose,
			boolean multithread) throws IOException {
		
		// Get the current date
		String date = Methods.getCurrentDate("dd-MM-yy");
		
		// Read the NEWICK tree and store as a traversable node set
		Tree tree = new Tree(treeFile);
		
		// Initialise an object to store the consistency index information for each site
		ConsistencyIndex consistency = null;
		
		// Check if FASTA file provided
		if(fastaFile.matches("Not provided") == false) {
			
			// Read in the sequences
			ArrayList<Sequence> sequences = Methods.readFastaFile(fastaFile, verbose);
			
			// Calculate the consistency index of each position in the alignment on the phylogeny
			consistency = new ConsistencyIndex(tree, sequences, verbose, multithread, 4);
			
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
		
		// Otherwise read in the presence/absence table
		}else {
			
			// Read in the presence absence matrix
			PresenceAbsence presenceAbsenceMatrix = Methods.readPresenceAbsenceTable(presenceAbsenceFile, verbose);
			
			// Calculate the consistency index of each position in the alignment on the phylogeny
			consistency = new ConsistencyIndex(tree, presenceAbsenceMatrix.getBooleanSequences(), verbose, multithread, 2);
						
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
				consistency.printSummary(pathForOutput + "consistencyIndexReport_" + date + ".txt",
						presenceAbsenceMatrix.getRegionCoords(), includeConsistentSites);
				if(verbose) {
					System.out.println("Created report detailing the inconsistent regions identified:\n\t" + 
							pathForOutput + "consistencyIndexReport_" + date + ".txt");
				}
			}
		}
		
		return consistency.getPositions();
	}
	
	public static int[] runHomoplasyFinderOnPresenceAbsenceFromR(String treeFile, String presenceAbsenceFile, String pathForOutput,
			boolean createFasta, boolean createReport, boolean createTree, boolean includeConsistentSites, boolean verbose,
			boolean multithread) throws IOException {
		
		// Get the current date
		String date = Methods.getCurrentDate("dd-MM-yy");
		
		// Read the NEWICK tree and store as a traversable node set
		Tree tree = new Tree(treeFile);
		
		// Read in the presence absence matrix
		PresenceAbsence presenceAbsenceMatrix = Methods.readPresenceAbsenceTable(presenceAbsenceFile, verbose);
				
		// Calculate the consistency index of each position in the alignment on the phylogeny
		ConsistencyIndex consistency = new ConsistencyIndex(tree, presenceAbsenceMatrix.getBooleanSequences(), verbose, multithread,
				2);
					
		// Create an annotated NEWICK tree file
		if(createTree) {
			consistency.printAnnotatedTree(pathForOutput + "annotatedNewickTree_" + date + ".tree");
		}
					
		// Create a report file
		consistency.printSummary(pathForOutput + "consistencyIndexReport_" + date + ".txt", presenceAbsenceMatrix.getRegionCoords(),
			includeConsistentSites);
		
		return consistency.getPositions();
	}	
}
