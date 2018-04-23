package ComparingGenomes;

import methods.ArrayMethods;
import methods.GeneticMethods;

public class Annotation {

	int[] coordinates;
	String type;
	String sequence;
	String reverseCompliment;
	boolean compliment = false;
	
	public Annotation(String coords, String annotationType){
		
		this.type = annotationType;
		
		// Parse the coordinate information
		parseCoords(coords);
		
	}
	
	// General methods
	public void parseCoords(String coords){
		
		// Remove ">" or "<" if present
		coords = coords.replace(">", "");
		coords = coords.replace("<", "");
		
		// Check if compliment required
		if(coords.matches("complement(.*)") == true){			
			coords = coords.substring(11, coords.length() - 1);
		}
		
		// Check if join present
		if(coords.matches("join(.*)") == true){
			
			coords = coords.substring(5, coords.length() - 1);
			this.coordinates = ArrayMethods.range(ArrayMethods.convertToInteger(coords.split("\\.\\.|,")));			
		}else{
			this.coordinates = ArrayMethods.range(ArrayMethods.convertToInteger(coords.split("\\.\\.")));
		}		
	}

	public void removeSequences(){
		this.sequence = null;
		this.reverseCompliment = null;
	}
	
	public static Annotation[] append(Annotation[] array, Annotation value){
		
		Annotation[] newArray = new Annotation[array.length + 1];
		
		for(int index = 0; index < array.length; index++){
			newArray[index] = array[index];
		}
		newArray[newArray.length - 1] = value;
		
		return newArray;
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
	public String getReverseCompliment(){
		return this.reverseCompliment;
	}
	
	// Setting methods
	public void setSequence(String sequence, boolean verbose){
		this.sequence = sequence;
		this.reverseCompliment = ArrayMethods.toString(GeneticMethods.getReverseCompliment(sequence.toCharArray(), verbose));
	}
}