package identifyingHomoplasies;

import java.util.ArrayList;
import java.util.Hashtable;

import geneticDistances.Sequence;
import methods.HashtableMethods;

public class MultiThreadPositions extends Thread{

	public int position;
	public Sequence[] sequences;
	public Hashtable<String, ArrayList<String>> alleles;
	
	public MultiThreadPositions(int position, Sequence[] sequences){
		super(Integer.toString(position));
		this.position = position;
		this.sequences = sequences;		
	}
	
	public void run(){
		
		// Initialise an hashtable to store the the isolates associated with each allele
		this.alleles = new Hashtable<String, ArrayList<String>>();
		
		// Examine each position in the current isolate's sequence
		for(Sequence sequence : this.sequences){
						
			// Create a key for the current allele
			String allele = this.position + ":" + sequence.getSequence()[this.position];
						
			// Check if we have encountered the current allele before - note each sequence allele found in
			if(this.alleles.containsKey(allele) == true){
				this.alleles.get(allele).add(sequence.getName());
			}else{
							
				ArrayList<String> ids = new ArrayList<String>();
				ids.add(sequence.getName());
				this.alleles.put(allele, ids);
			}
		}

	}

	public Hashtable<String, ArrayList<String>> getAlleles(){
		return this.alleles;
	}
	
	public static Hashtable<String, ArrayList<String>> collect(MultiThreadPositions[] threads){
		
		// Initialise a hashtable to store the isolates associated with each allele
		Hashtable<String, ArrayList<String>> isolatesForEachAllele = new Hashtable<String, ArrayList<String>>();	
		
		// Examine each thread
		for(MultiThreadPositions thread : threads){
			
			// Examine each of the alleles recorded by the current thread
			for(String allele : HashtableMethods.getKeysString(thread.getAlleles())){
				
				// Don't need to check if allele exists as each thread worked on position so the alleles noted should be unique
				isolatesForEachAllele.put(allele, thread.getAlleles().get(allele));
			}
		}
		
		return isolatesForEachAllele;
	}

	public static boolean finished(MultiThreadPositions[] threads){
		
		boolean finished = true;
		for(MultiThreadPositions thread : threads){
			if(thread.isAlive() == true){
				finished = false;
				break;
			}
		}
		
		return finished;
	}
	
	public static void waitUntilAllFinished(MultiThreadPositions[] threads){
		
		boolean finished = false;
		while(finished == false){
			finished = finished(threads);
		}
	}
}
