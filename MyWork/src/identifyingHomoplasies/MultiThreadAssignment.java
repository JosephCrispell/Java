package identifyingHomoplasies;

import java.util.ArrayList;
import java.util.Hashtable;

import geneticDistances.Sequence;
import methods.ArrayListMethods;
import methods.ArrayMethods;
import phylogeneticTree.Node;

public class MultiThreadAssignment extends Thread {
	public String position;
	public Hashtable<String, ArrayList<String>> alleles;
	public ArrayList<Node> nodes;
	public ArrayList<String> unassigned;
	public ArrayList<String> ids;
	
	public MultiThreadAssignment(String position, Hashtable<String, ArrayList<String>> alleles, ArrayList<Node> nodes, ArrayList<String> ids){
		super(position);
		this.position = position;
		this.alleles = alleles;
		this.nodes = nodes;
		this.ids = ids;
	}
	
	public void run(){
		
		// Initialise an array to note the unassianged
		this.unassigned = new ArrayList<String>();
		
		// Get the alleles associated with the current position
		ArrayList<String> allelesAtPosition = HomoplasyFinder6.getAllelesAtPosition(position, this.alleles);
				
		// Check position isn't constant - has more than one allele
		if(allelesAtPosition.size() > 1){
						
			// Get an array of the isolates with an N at the current position
			ArrayList<String> isolatesWithN = this.alleles.get(position + ":N");
			
			// Initialise which alleles are assigned to nodes
			boolean[] assigned = ArrayMethods.initialise(allelesAtPosition.size(), false);
			
			// Examine each node
			for(Node node : this.nodes){
				
				// Check if all alleles assigned
				if(HomoplasyFinder6.checkIfAllAssigned(assigned)){
					break;
				}
				
				// Get all the isolates below the current node and note their common alleles
				ArrayList<String> idsBelow = ArrayListMethods.copy(node.getTips());
							
				// Get all the isolates above the current node and note their common alleles
				ArrayList<String> idsAbove = ArrayListMethods.getUncommon(this.ids, idsBelow);
				
				// Remove the isolates with Ns from those above and below the current node
				if(isolatesWithN != null){
					ArrayListMethods.remove(idsBelow, isolatesWithN);
					ArrayListMethods.remove(idsAbove, isolatesWithN);
				}
				
				// Examine each allele
				for(int i = 0; i < allelesAtPosition.size(); i++){
									
					// Check if either the isolates above or below match those associated with the current allele
					if(ArrayListMethods.compare(alleles.get(allelesAtPosition.get(i)), idsAbove) == true || 
							ArrayListMethods.compare(alleles.get(allelesAtPosition.get(i)), idsBelow) == true){
						assigned[i] = true;
					}
				}
			}
			
			// Note the unassigned alleles if any present
			for(int i = 0; i < allelesAtPosition.size(); i++){
				if(assigned[i] == false){
					this.unassigned.add(allelesAtPosition.get(i));
				}
			}
		}
	}

	public ArrayList<String> getUnassigned(){
		return this.unassigned;
	}
	
	public static ArrayList<String> collect(MultiThreadAssignment[] threads){
		
		// Intialise an ArrayList to store the unassigned alleles found
		ArrayList<String> notAssigned = new ArrayList<String>();
		
		// Examine each thread
		for(MultiThreadAssignment thread : threads){
		
			// Check whether current thread had any unassigned alleles
			if(thread.getUnassigned() != null){
				
				// Add the unassigned alleles from the current thread
				notAssigned.addAll(thread.getUnassigned());
			}			
		}
		
		return notAssigned;
	}

	public static boolean finished(MultiThreadAssignment[] threads){
		
		boolean finished = true;
		for(MultiThreadAssignment thread : threads){
			if(thread.isAlive() == true){
				finished = false;
				break;
			}
		}
		
		return finished;
	}
	
	public static void waitUntilAllFinished(MultiThreadAssignment[] threads){
		
		boolean finished = false;
		while(finished == false){
			finished = finished(threads);
		}
	}
}
