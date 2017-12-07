package identifyingHomoplasies;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Scanner;

import javax.swing.plaf.synth.SynthSeparatorUI;

import geneticDistances.Sequence;
import methods.ArrayMethods;
import methods.CalendarMethods;
import methods.GeneticMethods;
import methods.HashtableMethods;
import phylogeneticTree.BeastNewickTreeMethods;
import phylogeneticTree.CalculateDistancesToMRCAs;
import phylogeneticTree.Node;
import phylogeneticTree.NodeMethods;

public class HomoplasyFinder {
	
	public static void main(String[] args) throws IOException{
		
		/**
		 * Setting up
		 */
		
		// Set the path
		//String path = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Homoplasmy/";
		String path = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester_CattleAndBadgers/NewAnalyses_13-07-17/vcfFiles/";
				
		// Get the current date
		//String date = CalendarMethods.getCurrentDate("dd-MM-yy");
		
		/**
		 * Indexing SNPs in FASTA file
		 */
		
		// Read in the FASTA file
		//String fasta = path + "example_" + date + ".fasta";
		String fasta = path + "sequences_Prox-10_29-09-2017.fasta";
		Sequence[] sequences = GeneticMethods.readFastaFile(fasta);
				
		// Parse file names
		for(Sequence sequence : sequences){
			if(sequence.getName().matches("Ref-1997") == true){
				continue;
			}
			sequence.setName(sequence.getName().split("_")[0]);
		}
		
		// Index the isolates
		Hashtable<String, Integer> isolateIndices =  getIndicesOfIsolatesSequences(sequences);
		
		// Build a genetic distance matrix
		int[][] geneticDistances = GeneticMethods.createGeneticDistanceMatrix(sequences);
		
		// Examine Variant Postions in FASTA
		Hashtable<Integer, VariantPosition> variantPositionInfo = 
				countNoIsolatesWithEachAlleleAtEachVariantPositionInFasta(sequences);
		/**
		 * Identify the alleles associated with the reference - if available
		 * 	Assumes no homoplasies back to reference <- impossible to find any?
		 */
		 
		// Identify the reference sequence
		String reference = getReferenceID(); // If not available leave as NONE
				
		// Index the reference allele
		Hashtable<Integer, Character> refAlleles = indexReferenceAlleles(reference, sequences, isolateIndices);
		
		/**
		 * Note SNPs present in each isolate
		 */
		
		// Build a hashtable listing the SNPs present in each isolate
		Hashtable<String, String[]> variantPositionAlleleForEachIsolate = 
				noteAllelePresentAtEachVariantPositionInEachIsolate(sequences, variantPositionInfo, refAlleles);
	
		/**
		 * Tying SNPs to nodes within phylogeny
		 */
		
		// Read in the newick tree
		//String treeFile = path + "example_" + date + ".tree";
		String treeFile = path + "mlTree_29-09-2017.tree";
		Node tree = readNewickTree(treeFile);
		
		// Note the paths to root of each node and the terminal nodes
		notePathToRootForAllNodesAndNoteTerminalNodes(tree, new Node[0]);
		Node[] terminalNodes = Global.terminalNodes;
		
		// Note the terminal node associated with each isolate
		Hashtable<String, Node> isolateNodes = noteTerminalNodeAssiociatedWithEachIsolate(terminalNodes);
		
		// Note the variant position alleles common to all daughter isolates of node
		// This code also assigns an ID (IDs of terminal nodes concatenated) to each internal node
		noteCommonVariantPositionAllelesBetweenSubNodesOfEachNode(tree, variantPositionAlleleForEachIsolate);
		
		/**
		 * Identifying potential homoplasmy events
		 */
		
		// Define threshold proportion of isolates to be considered ancestral
		double thresholdAlleleSupportInPop = 0.5;
		
		// Examine each of the nodes to check for evidence of homoplasmies
		identifyPotentialHomoplasiesInAllNodes(tree, variantPositionInfo, thresholdAlleleSupportInPop,
				variantPositionAlleleForEachIsolate, geneticDistances, sequences, isolateIndices,
				isolateNodes, reference);
	}

	public static void identifyPotentialHomoplasiesInAllNodes(Node node, 
			Hashtable<Integer, VariantPosition> variantPositionInfo, double thresholdAlleleSupportInPop,
			Hashtable<String, String[]> variantPositionAlleleForEachIsolate,
			int[][] geneticDistances, Sequence[] sequences, Hashtable<String, Integer> isolateIndices,
			Hashtable<String, Node> isolateNodes, String reference){
		
		// Get the isolates associated with the current node
		String[] isolatesAssociatedWithCurrentNode = node.getNodeInfo().getNodeId().split("-");
		
		// Check that current node isn't terminal
		if(node.getSubNodes() != null){
			
			// Check the current node isn't the root
			if(node.getParentNode().getNodeInfo() != null){
				
				// Initialise a vector to store the isolates each potential homoplasy was found in
				String[] isolatesFoundIn;
				
				// Identify the variant position alleles only present in current isolate/terminalNode/subNode and 
				// not in isolates of other sub nodes
				String[] uniqueVariantPositionAlleles = 
						identifyVariantPositionAllelesFoundInNodeAndNotInOthersOfParentNode(node);
				
				// Are any of these variant position alleles present in other isolates in the population?
				for(String vpAllele : uniqueVariantPositionAlleles){
					
					// Get position of current variant position
					String[] parts = vpAllele.split(":");
					int position = Integer.parseInt(parts[0]);
					char allele = parts[1].toCharArray()[0];
					
					// Note which isolates allele found in
					isolatesFoundIn = reportWhichIsolatesAlleleIn(variantPositionAlleleForEachIsolate,
							isolatesAssociatedWithCurrentNode, vpAllele);
					
					if(isolatesFoundIn.length != 0 &&
							variantPositionInfo.get(position).getAlleleSupport(allele) < thresholdAlleleSupportInPop){
						
						// Print information about allele
						System.out.println("################################################################");
						System.out.println("Potential introduced homoplasy found in node associated with isolate(s): " + 
								node.getNodeInfo().getNodeId() +
								"\tPosition in FASTA sequence: " + (position + 1) + "\tAllele: " + allele);
						
						// Search for allele on path to root
						searchForAlleleOnPathToRoot(node, vpAllele);
						
						// Note the other isolates found in
						System.out.print("\nAlelle also found in: " + ArrayMethods.toString(isolatesFoundIn, ", "));
						System.out.println("\nFraction = " + isolatesFoundIn.length + "/" + 
								(variantPositionAlleleForEachIsolate.size() - isolatesAssociatedWithCurrentNode.length));
						
						// Are the other isolates the SNP was found in monophyletic?
						checkIfIsolatesAreMonophyletic(isolatesFoundIn, isolateNodes);
						
						// Look at the genetic distances associated with the isolates with an allele at this site
						examineInterIsolateGeneticDistancesAssociatedWithVariantPosition(geneticDistances, sequences, 
								variantPositionAlleleForEachIsolate, vpAllele,
								HashtableMethods.getValuesInt(isolateIndices, isolatesAssociatedWithCurrentNode));
					}
				}
			}
			
			// Check if the current node has sub nodes - if so examine them
			if(node.getSubNodes().length != 0){
					
				// Examine the subnodes of the current node
				for(Node subNode : node.getSubNodes()){
					identifyPotentialHomoplasiesInAllNodes(subNode, variantPositionInfo, thresholdAlleleSupportInPop,
							variantPositionAlleleForEachIsolate, geneticDistances, sequences, isolateIndices,
							isolateNodes, reference);
				}
			}
		}
	}

	public static String getReferenceID(){
		
		// Initialise a variable to store the reference ID
		String reference = "NONE";
		
		// Initialise a scanner to get user input
		Scanner reader = new Scanner(System.in);
		
		// Print user information
		System.out.println("Enter the name of the reference sequence, if available (otherwise press enter): ");
		
		// Get the user input
		String input = reader.nextLine(); // Scans the next token of the input as an int.
		if(input.matches("") == false){
			reference = input;
			
			System.out.println("Reference assigned to: " + reference + "\n");
		}else{
			System.out.println("No reference sequence assigned\n");
		}
		
		// Close the scanner
		reader.close(); 
		
		return(reference);
	}
	
 	public static Hashtable<Integer, Character> indexReferenceAlleles(String reference, Sequence[] sequences,
			Hashtable<String, Integer> isolateIndices){
		
		// Initialise a hashtable to store the indexed reference alleles
		Hashtable<Integer, Character> indexedReferenceAlleles = new Hashtable<Integer, Character>();
		
		// Check if reference present
		if(isolateIndices.get(reference) != null){
			
			// Get the reference sequence
			char[] sequence = sequences[isolateIndices.get(reference)].getSequence();
			
			// Index each position
			for(int pos = 0; pos < sequence.length; pos++){
				indexedReferenceAlleles.put(pos, sequence[pos]);
			}
			
		}else if(reference.matches("NONE") == false){
			System.out.println("ERROR: Reference sequence not found under: " + reference);
		}
		
		return indexedReferenceAlleles;
	}
	
	public static Hashtable<String, Node> noteTerminalNodeAssiociatedWithEachIsolate(Node[] terminalNodes){
		
		// Initialise a hashtable
		Hashtable<String, Node> isolateNodes = new Hashtable<String, Node>();
		
		// Examine each terminal node
		for(Node terminalNode : terminalNodes){
			isolateNodes.put(terminalNode.getNodeInfo().getNodeId(), terminalNode);
		}
		
		return isolateNodes;
	}
	
	public static Hashtable<String, Integer> getIndicesOfIsolatesSequences(Sequence[] sequences){
		
		Hashtable<String, Integer> output = new Hashtable<String, Integer>();
		
		for(int i = 0; i < sequences.length; i++){
			output.put(sequences[i].getName(), i);
		}
		
		return output;
	}
	
	public static void identifyPotentialHomoplasiesInTerminalNodes(Node[] terminalNodes, 
			Hashtable<Integer, VariantPosition> variantPositionInfo, double thresholdAlleleSupportInPop,
			Hashtable<String, String[]> variantPositionAlleleForEachIsolate,
			int[][] geneticDistances, Sequence[] sequences, Hashtable<String, Integer> isolateIndices,
			Hashtable<String, Node> isolateNodes, String reference){
		
		// Initialise an array to store the isolate associated with a given terminal node 
		// (needs to be in array to work with a method)
		String[] terminalNodeIsolate;
		
		// Initialise a vector to store the isolates each potential homoplasy was found in
		String[] isolatesFoundIn;
		
		// Examine each terminal node
		for(Node terminalNode : terminalNodes){
			
			// Get the isolate associated with the current node and store in array
			terminalNodeIsolate = terminalNode.getNodeInfo().getNodeId().split("-");
			
			// Identify the variant position alleles only present in current isolate/terminalNode/subNode and 
			// not in isolates of other sub nodes
			String[] uniqueVariantPositionAlleles = 
					identifyVariantPositionAllelesFoundInTerminalNodeAndNotInOthersOfParentNode(terminalNode);
			
			// Are any of these variant position alleles present in other isolates in the population?
			for(String vpAllele : uniqueVariantPositionAlleles){
				
				// Get position of current variant position
				String[] parts = vpAllele.split(":");
				int position = Integer.parseInt(parts[0]);
				char allele = parts[1].toCharArray()[0];
				
				if(variantPositionInfo.get(position).getAlleleCount(allele) > 1 &&
						variantPositionInfo.get(position).getAlleleSupport(allele) < thresholdAlleleSupportInPop){
					
					// Print information about allele
					System.out.println("################################################################");
					System.out.println("Potential introduced homoplasy found in isolate: " + 
							terminalNode.getNodeInfo().getNodeId() +
							"\tPosition in FASTA sequence: " + (position + 1) + "\tAllele: " + allele);
					
					// Search for allele on path to root
					searchForAlleleOnPathToRoot(terminalNode, vpAllele);
					
					// Note which isolates allele found in
					isolatesFoundIn = reportWhichIsolatesAlleleIn(variantPositionAlleleForEachIsolate,
							terminalNodeIsolate, vpAllele);
					
					// Are the other isolates the SNP was found in monophyletic?
					checkIfIsolatesAreMonophyletic(isolatesFoundIn, isolateNodes);
					
					// Look at the genetic distances associated with the isolates with an allele at this site
					examineInterIsolateGeneticDistancesAssociatedWithVariantPosition(geneticDistances, sequences, 
							variantPositionAlleleForEachIsolate, vpAllele,
							HashtableMethods.getValuesInt(isolateIndices, terminalNodeIsolate));
				}
			}			
		}
	}
	
	public static boolean checkIfIsolatesAreMonophyletic(String[] isolates, Hashtable<String, Node> isolateNodes){
				
		// Initialise a variable to return
		boolean monophyletic = false;
		
		// Check that there is more than one isolate
		if(isolates.length != 1){
			// Initialise a variable to store the parent node
			Node parent = isolateNodes.get(isolates[0]).getParentNode();
			
			// Initialise an array to store the isolates associated with termainl nodes
			String[] terminalNodeIsolates;
			Hashtable<String, Integer> terminalNodeIsolatesIndexed;
			
			// Initialise the necessary variables
			int nFound;
			
			// Select first isolate - keep examining parents until found all other isolates
			while(parent.getParentNode() != null){
				
				// Reset the terminal nodes array
				Global.terminalNodes = new Node[0];
							
				// Get all the terminal nodes of the current parent node
				noteTerminalNodes(parent);
				
				// Get the isolates associated with the terminal nodes
				terminalNodeIsolates = getIsolateIDsFromTerminalNodes(Global.terminalNodes);
				
				// Index the terminal node isolates
				terminalNodeIsolatesIndexed = ArrayMethods.indexArray(terminalNodeIsolates);
				
				// Check if all of the isolates with VP allele are found in isolates of terminal nodes
				nFound = 0;
				for(String isolateWithVPAllele : isolates){
					if(terminalNodeIsolatesIndexed.get(isolateWithVPAllele) != null){
						nFound++;
					}
				}
				
				// If found all the isolates finish search
				if(nFound == isolates.length){
					
					// Check only the isolates of interest were found in the terminal node isolates
					if(isolates.length == terminalNodeIsolates.length){
						monophyletic = true;
						System.out.println("These " + isolates.length +	" isolates are monophyletic" + 
								" (present single clade containing no other isolates)");
					}else{
						System.out.println("These " + isolates.length +	" isolates are NOT monophyletic" + 
								" (present in multiple clades)");
					}
					
					// FINISH
					break;
					
				// Else check the next parent
				}else{
					parent = parent.getParentNode();
				}
			}
		}		
		
		return monophyletic;		
	}
	
	public static void examineInterIsolateGeneticDistancesAssociatedWithVariantPosition(
			int[][] geneticDistances, Sequence[] sequences, 
			Hashtable<String, String[]> variantPositionAlleleForEachIsolate,
			String vpAllele, int[] isolateIndices){
		
		// Get the position for the input variant position allele
		String[] parts = vpAllele.split(":");
		String position = parts[0];
		String allele = parts[1];
		
		// Initialise an array to record the distances to other isolates with variant position allele
		int[] distancesToOtherIsolatesThatHaveIt = new int[0];
		
		// Note distances to isolates with variant position allele
		for(int i = 0; i < sequences.length; i++){
			
			// Skip the isolates of interest
			if(ArrayMethods.in(isolateIndices, i) == true){
				continue;
			}
			
			// Check if current isolate has same variant position allele
			if(allele.equals(getAlleleOfVariantPositionInIsolate(
					variantPositionAlleleForEachIsolate.get(sequences[i].getName()), position))){
				
				for(int isolateIndex : isolateIndices){
					if(allele.equals(getAlleleOfVariantPositionInIsolate(
							variantPositionAlleleForEachIsolate.get(sequences[isolateIndex].getName()), position))){
						
						distancesToOtherIsolatesThatHaveIt = ArrayMethods.append(distancesToOtherIsolatesThatHaveIt, 
								geneticDistances[isolateIndex][i]);
					}
				}				
			}	
		}
		
		// Initialise an array to record the distances between other isolates with allele 
		int[] distancesBetweenOtherIsolatesThatHaveIt = new int[0];
		
		// Compare each of the isolates to one another
		String alleleI;
		String alleleJ;
		
		for(int i = 0; i < sequences.length; i++){
			
			// Skip the isolates of interest
			if(ArrayMethods.in(isolateIndices, i) == true){
				continue;
			}
			
			// Get the allele of the variant position in the current isolate
			alleleI = getAlleleOfVariantPositionInIsolate(
					variantPositionAlleleForEachIsolate.get(sequences[i].getName()), position);
			
			// Skip isolates without variant position allele
			if(allele.equals(alleleI) == false){
				continue;
			}
			
			for(int j = 0; j < sequences.length; j++){
				
				// Skip comparisons that aren't of interest
				if(i >= j || ArrayMethods.in(isolateIndices, j) == true){
					continue;
				}
				
				// Get the allele of the variant position in the current isolate
				alleleJ = getAlleleOfVariantPositionInIsolate(
						variantPositionAlleleForEachIsolate.get(sequences[j].getName()), position);
				
				// Skip isolates without variant position allele
				if(allele.equals(alleleJ) == false){
					continue;
				}
				
				distancesBetweenOtherIsolatesThatHaveIt = ArrayMethods.append(distancesBetweenOtherIsolatesThatHaveIt, 
						geneticDistances[i][j]);
			}
		}
		
		// Report findings
		System.out.println("\nmean distance to other isolates with variant position allele = " +
				ArrayMethods.mean(distancesToOtherIsolatesThatHaveIt));
		System.out.println("mean distance between other isolates with variant position allele = " +
				ArrayMethods.mean(distancesBetweenOtherIsolatesThatHaveIt));
		System.out.println("ratio = " + ArrayMethods.mean(distancesToOtherIsolatesThatHaveIt) / 
				ArrayMethods.mean(distancesBetweenOtherIsolatesThatHaveIt));
	}
	
	public static String getAlleleOfVariantPositionInIsolate(String[] isolateVariantPositionAlleles, String position){
		
		String allele = "NA";
		for(String variantPostionAllele : isolateVariantPositionAlleles){
			
			// Get the information for the current variant position allele
			String[] parts = variantPostionAllele.split(":");
			
			// Check if at correct position
			if(parts[0].matches(position) == true){
				allele = parts[1];
				break;
			}
		}
		
		return allele;
	}

	public static String[] identifyVariantPositionAllelesFoundInNodeAndNotInOthersOfParentNode(
			Node node){
		
		// Get sub-nodes of the ancestral nodes
		Node[] subNodesOfAncestor = node.getParentNode().getSubNodes();
		
		// Note the variant position alleles present at terminal node
		String[] nodeVariantPositionAlleles = node.getCommonVariantPositionAlleles();
		
		// Initialise an array of falses relating to these alleles
		boolean[] found = ArrayMethods.repeat(false, nodeVariantPositionAlleles.length);
		
		// Check to see is any of these are present in any of the other subnodes (of the parent node
		for(Node subNode : subNodesOfAncestor){
			
			// Skip the current terminal node
			if(subNode == node){
				continue;
			}
			
			for(int i = 0; i < nodeVariantPositionAlleles.length; i++){
				if(found[i] == false){
					found[i] = ArrayMethods.in(subNode.getCommonVariantPositionAlleles(), 
							nodeVariantPositionAlleles[i]);
				}
			}
		}
		
		// Note the alleles not found on other sub nodes
		String[] uniqueVariantPositionAlleles = new String[nodeVariantPositionAlleles.length];
		int pos = -1;
		for(int i = 0; i < nodeVariantPositionAlleles.length; i++){
			if(found[i] == false){
				pos++;
				uniqueVariantPositionAlleles[pos] = nodeVariantPositionAlleles[i];
			}			
		}		
		
		return ArrayMethods.subset(uniqueVariantPositionAlleles, 0, pos);
	}
	
	public static String[] identifyVariantPositionAllelesFoundInTerminalNodeAndNotInOthersOfParentNode(
			Node terminalNode){
		
		// Get sub-nodes of the ancestral nodes
		Node[] subNodesOfAncestor = terminalNode.getParentNode().getSubNodes();
		
		// Note the variant position alleles present at terminal node
		String[] terminalNodeVariantPositionAlleles = terminalNode.getCommonVariantPositionAlleles();
		
		// Initialise an array of falses relating to these alleles
		boolean[] found = ArrayMethods.repeat(false, terminalNodeVariantPositionAlleles.length);
		
		// Check to see is any of these are present in any of the other subnodes (of the parent node
		for(Node subNode : subNodesOfAncestor){
			
			// Skip the current terminal node
			if(subNode == terminalNode){
				continue;
			}
			
			for(int i = 0; i < terminalNodeVariantPositionAlleles.length; i++){
				if(found[i] == false){
					found[i] = ArrayMethods.in(subNode.getCommonVariantPositionAlleles(), 
							terminalNodeVariantPositionAlleles[i]);
				}
			}
		}
		
		// Note the alleles not found on other sub nodes
		String[] uniqueVariantPositionAlleles = new String[terminalNodeVariantPositionAlleles.length];
		int pos = -1;
		for(int i = 0; i < terminalNodeVariantPositionAlleles.length; i++){
			if(found[i] == false){
				pos++;
				uniqueVariantPositionAlleles[pos] = terminalNodeVariantPositionAlleles[i];
			}			
		}		
		
		return ArrayMethods.subset(uniqueVariantPositionAlleles, 0, pos);
	}
	
	public static String[] reportWhichIsolatesAlleleIn(Hashtable<String, String[]> variantPositionAlleleForEachIsolate, 
			String[] nodeIsolates, String vpAllele){
		
		// Index the isolate IDs associated with the current node
		Hashtable<String, Integer> nodeIsolatesIndexed = HashtableMethods.indexArray(nodeIsolates);
		
		String[] isolates = new String[0];
		
		for(String isolate : HashtableMethods.getKeysString(variantPositionAlleleForEachIsolate)){
			if(nodeIsolatesIndexed.get(isolate) == null && 
					ArrayMethods.in(variantPositionAlleleForEachIsolate.get(isolate), vpAllele) == true){
				isolates = ArrayMethods.append(isolates, isolate);
			}
		}

		return isolates;
	}
	
	public static void searchForAlleleOnPathToRoot(Node node, String vpAllele){
		
		// Does the SNP ever appear on nodes on path to root?
		int found = -1;
		for(int i = node.getPathToRoot().length - 1; i >= 0; i--){
			
			if(ArrayMethods.in(node.getPathToRoot()[i].getCommonVariantPositionAlleles(), vpAllele) == true){
				found = node.getPathToRoot().length - i;
				break;
			}
		}				
		
		if(found != -1){
			System.out.println("\nFound " + found + " node(s) back on path towards root (length = " + 
					node.getPathToRoot().length + ")");
		}else{
			System.out.println("\nAllele not found to be common on any nodes on path towards root");
		}
	}
	
	public static void identifySNPsInIsolatesThatArentOnPathToRootButArePresentInOtherIsolates(
			Node[] terminalNodes, Hashtable<String, String[]> isolateSNPs, 
			Hashtable<String, Integer> nIsolatesEachSNPFoundIn){
		
		// Initialise an array to store the snps found on the path to the root
		String[] snpsOnPathToRoot;
		
		// Initialise an array to store the snps present in each isolate
		String[] snpsInIsolate;
		
		// Initialise an array to store the snps not present on path to root
		String[] vpAllelesNotInPathToRoot;
		
		// Examine each terminal node
		for(Node terminalNode : terminalNodes){
			
			System.out.println("-------------------------------------------------------------------------");
			System.out.println("Isolate: " + terminalNode.getNodeInfo().getNodeId());
			
			// Get the common SNPs of the nodes on the path to the root
			snpsOnPathToRoot = getCommonSNPsOfNodesOnPathToRoot(terminalNode);
			
			System.out.println("Number of snps present on nodes on path to root: " + snpsOnPathToRoot.length);
			
			// Get the SNPs present in the isolate associated with the terminal node
			snpsInIsolate = isolateSNPs.get(terminalNode.getNodeInfo().getNodeId());
			
			System.out.println("Isolate has " + snpsInIsolate.length + " snps");
			
			// Are the any SNPs present in the current isolate that aren't present in the nodes on path to root?
			vpAllelesNotInPathToRoot = ArrayMethods.returnElementsOnlyPresentInA(snpsInIsolate, snpsOnPathToRoot);
			
			System.out.println(vpAllelesNotInPathToRoot.length + " SNPs not found on path but present in isolate");
			System.out.println("SNPs in isolate:\t" + ArrayMethods.toString(snpsInIsolate, ", "));
			if(snpsOnPathToRoot.length > 0){
				System.out.println("SNPs on path:\t\t" + ArrayMethods.toString(snpsOnPathToRoot, ", "));
			}
			System.out.println("Not common snps: ");
			System.out.println(ArrayMethods.toString(vpAllelesNotInPathToRoot, ", "));
			
			
			// If there are any snps no present on path to root, are any of them found in other isolates?
			if(vpAllelesNotInPathToRoot.length != 0){
				
				for(String alleleOfVP : vpAllelesNotInPathToRoot){
					
					if(nIsolatesEachSNPFoundIn.get(alleleOfVP) > 1){
						//System.out.println(snp);
					}
				}
			}
		}
	}
	
	public static String[] getCommonSNPsOfNodesOnPathToRoot(Node node){
		
		// Initialise a hashtable to store the snps found
		Hashtable<String, Integer> commonSNPsOnPathToRoot = new Hashtable<String, Integer>();
		
		// Get the nodes on the path to root
		Node[] nodesOnPathToRoot = node.getPathToRoot();
		
		// Examine each node on the path to the root - note last node on path will be current node
		for(int i = 0; i < nodesOnPathToRoot.length - 1; i++){
			
			for(String vpAllele : nodesOnPathToRoot[i].getCommonVariantPositionAlleles()){
				
				if(commonSNPsOnPathToRoot.get(vpAllele) == null){
					commonSNPsOnPathToRoot.put(vpAllele, 1);
				}
			}
		}
		
		return HashtableMethods.getKeysString(commonSNPsOnPathToRoot);
	}
	
	public static void getTerminalNodes(Node node, Node[] terminalNodes){
		
		// Is the current node a terminal node?
		if(node.getSubNodes().length == 0){
			
			System.out.println("here!");
			
			terminalNodes = NodeMethods.append(terminalNodes, node);
		}else{
			// Examine the subnodes of the current node
			for(Node subNode : node.getSubNodes()){
						
				getTerminalNodes(subNode, terminalNodes);			
			}
		}
	}
	
	public static void noteCommonVariantPositionAllelesBetweenSubNodesOfEachNode(Node node, 
			Hashtable<String, String[]> variantPositionAllelesOfEachIsolate){
		
		// Check if we have reached a terminal node
		if(node.getSubNodes().length != 0){
		
			// Find and store the common SNPs for the current node
			node.setCommonVariantPositionAlleles(findCommonVariantPositionAllelesBetweenSubNodes(node,
					variantPositionAllelesOfEachIsolate));
			
			
			// Examine the subnodes of the current node
			for(Node subNode : node.getSubNodes()){
			
				noteCommonVariantPositionAllelesBetweenSubNodesOfEachNode(subNode, variantPositionAllelesOfEachIsolate);			
			}
		}else{
			node.setCommonVariantPositionAlleles(variantPositionAllelesOfEachIsolate.get(node.getNodeInfo().getNodeId()));
		}
	}
	
	public static String[] findCommonVariantPositionAllelesBetweenSubNodes(Node node, 
			Hashtable<String, String[]> variantPositionAllelesOfEachIsolate){

		// Initialise an array to store the common variant position alleles
		Hashtable<String, Integer> commonVariantPositionAlleles = new Hashtable<String, Integer>();
		
		// Initialise an array to store the isolates of the terminal nodes under current node
		String[] isolates = new String[0];
		
		// Examine the variant position alleles at each sub node
		for(int i = 0; i < node.getSubNodes().length; i++){
			
			// Reset the terminal nodes array
			Global.terminalNodes = new Node[0];
			
			// Note the variant position alleles present in isolates present in the current sub node
			noteTerminalNodes(node.getSubNodes()[i]);
			String[] isolatesInSubNode = getIsolateIDsFromTerminalNodes(Global.terminalNodes);
			Hashtable<String, Integer> variantPositionAllelesAtSubNode = getAllelesOfVariantPositionsFoundInIsolates(
					isolatesInSubNode, variantPositionAllelesOfEachIsolate);
			
			// Combine these terminal isolates to the growing array
			isolates = ArrayMethods.combine(isolates, isolatesInSubNode);
			
			// Check at first sub node
			if(i == 0){
				commonVariantPositionAlleles = variantPositionAllelesAtSubNode;
			}else{
				// Get the alleles common amongst previous sub nodes
				String[] previouslyCommon = HashtableMethods.getKeysString(commonVariantPositionAlleles);
				commonVariantPositionAlleles = new Hashtable<String, Integer>();
				
				// Examine each of these alleles and check if present at current sub node
				for(String variantPositionAllele : previouslyCommon){
					if(variantPositionAllelesAtSubNode.get(variantPositionAllele) != null){
						commonVariantPositionAlleles.put(variantPositionAllele, 1);
					}
				}
			}
		}
		
		// Create an ID for the current node - IsolateA-IsolateB-IsolateC - IDs of terminal nodes below
		node.getNodeInfo().setNodeId(ArrayMethods.toString(isolates, "-"));
		
		return HashtableMethods.getKeysString(commonVariantPositionAlleles);
	}

	public static Hashtable<String, Integer> getAllelesOfVariantPositionsFoundInIsolates(String[] isolates, 
			Hashtable<String, String[]> variantPositionAllelesOfEachIsolate){
		
		// Initialise a hashtable to store the alleles of variant positions
		Hashtable<String, Integer> allelesOfVPs = new Hashtable<String, Integer>();
		
		// Examine the SNPs present in each isolate
		for(String isolate : isolates){
			
			// Examine each SNP
			for(String vpAllele : variantPositionAllelesOfEachIsolate.get(isolate)){
				
				if(allelesOfVPs.get(vpAllele) == null){
					allelesOfVPs.put(vpAllele, 1);
				}
			}
		}
		
		return allelesOfVPs;
	}
	
	public static String[] getIsolateIDsFromTerminalNodes(Node[] terminalNodes){
		
		// Initialise an array to store the isolate IDs
		String[] isolates = new String[terminalNodes.length];
		
		// Examine each node
		for(int i = 0; i < terminalNodes.length; i++){
			isolates[i] = terminalNodes[i].getNodeInfo().getNodeId();
		}
		
		return isolates;
	}
	
 	public static void noteTerminalNodes(Node node){
		
		// Check if we have reached a terminal node
		if(node.getSubNodes().length == 0){
		
			Global.terminalNodes = NodeMethods.append(Global.terminalNodes, node);
		}else{
			
			// Examine the subnodes of the current node
			for(Node subNode : node.getSubNodes()){
						
				noteTerminalNodes(subNode);			
			}
		}
	}
	
	public static void notePathToRootForAllNodesAndNoteTerminalNodes(Node node, Node[] pathToRoot){
		
		// Set the Path to the root for the current node
		node.setPathToRoot(pathToRoot);
		
		// Add the current node to the pathToRoot
		pathToRoot = NodeMethods.append(pathToRoot, node);
		
		// Check if we have reached a terminal node
		if(node.getSubNodes().length != 0){
		
			// Examine the subnodes of the current node
			for(Node subNode : node.getSubNodes()){
			
				notePathToRootForAllNodesAndNoteTerminalNodes(subNode, pathToRoot);			
			}
		}else{
			Global.terminalNodes = NodeMethods.append(Global.terminalNodes, node);
		}
	}
		
	public static Node readNewickTree(String pathToFile) throws IOException{
		
		// Get the Newick tree string from file
		String newickTree = CalculateDistancesToMRCAs.readNewickFile(pathToFile); 
		
		// Store the tree as a series of traversable nodes
		Node tree = BeastNewickTreeMethods.readNewickNode(newickTree, new Node(null, null, null));
		
		return tree;
	}
	
	public static Hashtable<String, String[]> noteAllelePresentAtEachVariantPositionInEachIsolate(Sequence[] sequences,
			Hashtable<Integer, VariantPosition> variantPositionInfo, Hashtable<Integer, Character> refAlleles){
		
		// Initialise a Hashtable to store the SNPs each isolate has
		Hashtable<String, String[]> isolateVariantPositionAlleles = new Hashtable<String, String[]>();
				
		// Examine each isolate and store their SNPs
		for(int i = 0; i < sequences.length; i++){
			isolateVariantPositionAlleles.put(sequences[i].getName(),
					noteAllelesPresentAtEachVariantPositionInSequence(sequences[i].getSequence(),
							variantPositionInfo, refAlleles));
		}
		
		return isolateVariantPositionAlleles;
	}
	
	public static String[] noteAllelesPresentAtEachVariantPositionInSequence(char[] sequence,
			Hashtable<Integer, VariantPosition> variantPositionInfo,
			Hashtable<Integer, Character> refAlleles){
		
		// Initialise an array to store the SNPs present in the current isolate
		String[] snpsPresent = new String[variantPositionInfo.size()];
		int snpPresentPos = -1;
		
		// Examine each position of the isolates sequence
		for(int pos = 0; pos < sequence.length; pos++){
			
			// Check position is a variant position and that allele isn't the reference
			if(variantPositionInfo.get(pos) != null && sequence[pos] != 'N' &&
					(refAlleles.get(pos) == null || refAlleles.get(pos) != sequence[pos])){
				snpPresentPos++;
				snpsPresent[snpPresentPos] = pos + ":" + sequence[pos];
			}
		}
		
		return ArrayMethods.subset(snpsPresent, 0, snpPresentPos);
	}
	
	public static Hashtable<Integer, VariantPosition> countNoIsolatesWithEachAlleleAtEachVariantPositionInFasta(
			Sequence[] sequences){
		
		// Initialise an array to store the SNP IDs = pos:allele
		Hashtable<Integer, VariantPosition> snps = new Hashtable<Integer, VariantPosition>();
		
		// Initialise a hashtable to store the alleles present at each position
		Hashtable<Character, Integer> alleles;
		
		// Examine each of the sites within the FASTA file
		for(int pos = 0; pos < sequences[0].getSequence().length; pos++){
			
			// Create a hashtable to store the alleles at the current position
			alleles = new Hashtable<Character, Integer>();
			
			// Examine the alleles present across the isolates at the current position
			for(int i = 0; i < sequences.length; i++){
				
				if(sequences[i].getSequence()[pos] != 'N' && alleles.get(sequences[i].getSequence()[pos]) == null){
					alleles.put(sequences[i].getSequence()[pos], 1);
				}else if(alleles.get(sequences[i].getSequence()[pos]) != null){
					alleles.put(sequences[i].getSequence()[pos], alleles.get(sequences[i].getSequence()[pos]) + 1);
				}
			}
			
			// Check whether we found multiple alleles at the current position
			if(alleles.size() > 1){
				
				// Create a SNP ID for each allele present
				for(char allele : HashtableMethods.getKeysChar(alleles)){
					snps.put(pos, new VariantPosition(pos, alleles));
				}
			}			
		}
		
		return snps;
	}
}
