package phylogeneticTree;

import java.util.Hashtable;

import methods.ArrayMethods;


public class NodeMethods {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	// Methods
	public static Node[] append(Node[] array, Node node){
		
		Node[] newArray = new Node[array.length + 1];
		
		for(int i = 0; i < array.length; i++){
			newArray[i] = array[i];
		}
		
		newArray[array.length] = node;
		
		return newArray;		
	}

	
//	public static Node findMRCA4TerminalNodePair(Node nodeA, Node nodeB){
//		
//		/**
//		 *  Examine the Internal Nodes for the Terminal Nodes
//		 *  
//		 *  The Internal is a list of node passed through on the path from the Terminal node back to the Root
//		 *  
//		 *  If you compare these paths from the root towards the Terminal Nodes, then the first Internal node that is not the 
//		 *  same for the Terminal nodes will immediately follow the Most Recent Common Ancestor of the pair in the Internal 
//		 *  Node lists.
//		 *  
//		 *  	1	2	3		5			Internal Nodes:			MRCAs:
//		 * 		|	|	|		|			1: d, c, b					1	2	3	4	5
//		 * 		| d	|	|	4	|			2: d, c, b				1	-	d	c	b	b
//		 * 		 ---    |   | a |			3: c, b					2	d	-	c	b	b
//		 * 		  |	 c	|	 ---			4: a, b					3	c	c	-	b	b
//		 * 		   -----	  |				5: a, b					4	b	b	b	-	a
//		 * 			 |	  b	  |										5	b	b	b	a	-
//		 * 			  --------
//		 * 				  |
//		 */
//		
//		// Retrieve the Internal Nodes for each of the Terminal Nodes
//		Node[] internalNodesA = nodeA.getInternalNodes();
//		Node[] internalNodesB = nodeB.getInternalNodes();
//		
//		// Initialise a MRCA empty Node
//		Node mrca = new Node(new Node[0], new Hashtable[0], "NULL");
//		
//		// If the Parent Node of the Terminal Nodes is the same then it is the MRCA
//		if(internalNodesA[0] == internalNodesB[0]){
//			mrca = internalNodesA[0];
//					
//		}else{
//				
//			// Which is shortest?
//			int[] lengths = {internalNodesA.length, internalNodesB.length};
//			int length = ArrayMethods.min(lengths);
//					
//			// Compare each Internal Node Working back from the Root (last added) to Parent (first)
//			for(int x = 1; x < length + 1; x++){
//				
//				// If the Internal Nodes aren't the same then the previous (back towards Root) is the MRCA
//				if(internalNodesA[internalNodesA.length - x] != internalNodesB[internalNodesB.length - x]){
//					mrca = internalNodesA[internalNodesA.length - x + 1];
//					break;
//				}
//			}
//					
//			// If the no MRCA was identified then it is the Parent of the Terminal Node with the least Internal Nodes 
//			if(mrca.getNodeId().equals("NULL")){
//				mrca = internalNodesA[internalNodesA.length - length];
//			}
//		}		
//		
//		return mrca;		
//	}
}
