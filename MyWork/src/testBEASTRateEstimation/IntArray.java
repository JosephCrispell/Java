package testBEASTRateEstimation;

import java.util.Hashtable;

public class IntArray {

	// Status information
	public int[] array;
	public int pos;
	public int limit;
	
	public IntArray(int sizeLimit) {
			
		this.array = new int[sizeLimit];
		this.pos = -1;
		this.limit = sizeLimit;
	}
	
	// Getting method
	public int[] getArray(){
		return this.array;
	}
	public int[] getUsedPositions(){
		
		int[] used = new int[this.pos + 1];
		
		for(int i = 0; i < this.pos + 1; i++){
			used[i] = this.array[i];
		}
		
		return used;
	}
	public int getLastValue(){
		return this.array[this.pos];
	}
	
	// General methods
	public void append(int value){
		
		// Have we reached the end of the array?
		if(this.pos == this.array.length - 1){
			
			// Create a new larger array
			int[] newArray = new int[array.length + this.limit];
			
			// Copy the original values into the new array
			for(int i = 0; i < this.array.length; i++){
				newArray[i] = this.array[i];
			}
			
			// Add in the new value
			this.pos++;
			newArray[this.pos] = value;
			
			// Replace the old array
			this.array = newArray;
		
		// If not, then put value in next position
		}else{
			this.pos++;
			this.array[this.pos] = value;
		}
	}


}
