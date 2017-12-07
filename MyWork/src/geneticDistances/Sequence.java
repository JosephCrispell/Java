package geneticDistances;

import java.util.Hashtable;

public class Sequence {

	public String name;
	public char[] sequence;
	public char species;
	
	public Sequence(String sequenceName, char[] nucleotides){
		
		this.name = sequenceName;
		this.sequence = nucleotides;		
	}
	
	// Setting methods
	public void setName(String sequenceName){
		this.name = sequenceName;
	}
	public void setSequence(char[] nucleotides){
		this.sequence = nucleotides;
	}
	public void setSpecies(char letter){
		this.species = letter;
	}

	// Getting methods
	public String getName(){
		return this.name;
	}
	public char[] getSequence(){
		return this.sequence;
	}
	public char getSpecies(){
		return this.species;
	}
	
	// General methods
	public static Sequence[] remove(Sequence[] array, Hashtable indicesToRemove){
		
		// Create a new array to store the sequences
		Sequence[] newArray = new Sequence[array.length - indicesToRemove.size()];
		
		// Remove the unwanted elements
		int pos = -1;
		for(int i = 0; i < array.length; i++){
			
			// Skip those we want to ignore
			if(indicesToRemove.get(i) != null){
				continue;
			}
			
			// Keep those we want
			pos++;
			newArray[pos] = array[i];
		}
		
		return newArray;
	}
	
	public static Sequence[] subset(Sequence[] array, int start, int end){
		Sequence[] part = new Sequence[end - start + 1];
		
		for(int index = start; index <= end; index++){
			
			part[index - start] = array[index];
		}
		
		return part;
	}
}
