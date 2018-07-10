package ComparingGenomes;

import methods.ArrayMethods;
import methods.GeneticMethods;

public class Annotation {

	int[] coordinates;
	String type;
	String sequence;
//	String reverseCompliment;
	boolean compliment = false;
	
	String[] files = new String[10];
	int[] sets = new int[10];
	int fileIndex = -1;
	
	public Annotation(String seq, String annotationType){
		
		this.sequence = seq;
//		this.reverseCompliment = ArrayMethods.toString(GeneticMethods.getReverseCompliment(sequence.toCharArray(), verbose));
		this.type = annotationType;		
	}
	
	// General methods
//	public void parseCoords(String coords){
//		
//		// Remove ">" or "<" if present
//		coords = coords.replace(">", "");
//		coords = coords.replace("<", "");
//		
//		// Check if compliment required
//		if(coords.matches("complement(.*)") == true){			
//			coords = coords.substring(11, coords.length() - 1);
//		}
//		
//		// Check if join present - currently a bit of a FUDGE!!! Just taking min and max of range - but join can be used to span circle start and end!
//		if(coords.matches("join(.*)") == true){
//			
//			coords = coords.substring(5, coords.length() - 1);
//			this.coordinates = ArrayMethods.range(ArrayMethods.convertToInteger(coords.split("\\.\\.|,")));			
//		}else{
//			this.coordinates = ArrayMethods.range(ArrayMethods.convertToInteger(coords.split("\\.\\.")));
//		}		
//	}

//	public void removeSequences(){
//		this.sequence = null;
//		this.reverseCompliment = null;
//	}
	
	public static Annotation[] append(Annotation[] array, Annotation value){
		
		Annotation[] newArray = new Annotation[array.length + 1];
		
		for(int index = 0; index < array.length; index++){
			newArray[index] = array[index];
		}
		newArray[newArray.length - 1] = value;
		
		return newArray;
	}
	
	public void addFileAndSet(String fileName, int setIndex) {
		
		// Check if space for info
		this.fileIndex++;
		if(this.fileIndex < this.files.length) {
			this.files[this.fileIndex] = fileName;
			this.sets[this.fileIndex] = setIndex;
		
		// If not then make bigger arrays and add in info
		}else {
			
			String[] filesCopy = new String[this.files.length + 10];
			int[] setsCopy = new int[this.sets.length + 10];
			for(int i = 0; i < this.files.length; i++) {
				filesCopy[i] = this.files[i];
				setsCopy[i] = this.sets[i];
			}
			filesCopy[this.fileIndex] = fileName;
			setsCopy[this.fileIndex] = setIndex;
			
			this.files = filesCopy;
			this.sets = setsCopy;
		}
	}
	
	// Getting methods
	public int getStart(){
		return this.coordinates[0];
	}
	public int getEnd(){
		return this.coordinates[1];
	}
	public String getType(){
		return this.type;
	}
	public String getSequence(){
		return this.sequence;
	}
//	public String getReverseCompliment(){
//		return this.reverseCompliment;
//	}
	
	// Setting methods
//	public void setSequence(String sequence, boolean verbose){
//		this.sequence = sequence;
//		this.reverseCompliment = ArrayMethods.toString(GeneticMethods.getReverseCompliment(sequence.toCharArray(), verbose));
//	}
}
