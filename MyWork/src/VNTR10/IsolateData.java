package VNTR10;

import java.util.Calendar;
import java.util.Hashtable;

public class IsolateData {
	
	public String isolateId;
	public String animalId;
	int yearOfIsolation;
	boolean isBadger;
	public String herdId;
	public double[] latLongs;
	
	public int index;
	
	Movement[] movements;
	int movementPos = -1;
	int limit;
	Calendar[] startEnd;
	boolean gotStartEnd = false;
	
	Episode episode;
	
	Hashtable<String, Herd> herdsInhabited = new Hashtable<String, Herd>();
	
	public IsolateData(String isolate, String animal, int year, boolean value, String herd, double[] coords){
		
		this.isolateId = isolate;
		this.animalId = animal;
		this.yearOfIsolation = year;
		this.isBadger = value;
		this.herdId = herd;
		this.latLongs = coords;
	}
	
	// Setting methods
	public void setIndex(int i){
		this.index = i;
	}
	public void appendMovement(Movement record, int size){
		
		// Check that the movement records array exists
		if(this.movements == null){
			this.limit = size;
			this.movements = new Movement[this.limit];
		}
		
		// Move to the next position
		this.movementPos++;
		
		// Check haven't reached the end of the array
		if(this.movementPos < this.movements.length){
			
			this.movements[this.movementPos] = record;
		}else{
			
			Movement[] newArray = new Movement[this.movements.length + this.limit];
			for(int i = 0; i < this.movements.length; i++){
				newArray[i] = this.movements[i];
			}
			newArray[this.movementPos] = record;
		}
	}
	public void setMovements(Movement[] movements){
		this.movements = movements;
		this.movementPos = movements.length - 1;
	}
	public void setStartEnd(Calendar[] dates){
		this.startEnd = new Calendar[2];
		this.startEnd[0] = dates[0];
		if(dates[1] != null){
			this.startEnd[1] = dates[1];
			this.gotStartEnd = true;
		}
	}
	public void setHerdsInhabited(Hashtable<String, Herd> herds){
		this.herdsInhabited = herds;
	}
	public void setEpisode(Episode episode){
		this.episode = episode;
	}
	
	// Getting methods
	public boolean getGotStartEnd(){
		return this.gotStartEnd;
	}
	public int getIndex(){
		return this.index;
	}
	public double[] getLatLongs(){
		return this.latLongs;
	}
	public String getIsolateId(){
		return this.isolateId;
	}
	public String getAnimalId(){
		return this.animalId;
	}
	public int getYearOfIsolation(){
		return this.yearOfIsolation;
	}
	public Movement[] getMovements(){
		
		// Check if we need to subset
		if(this.movements.length > 0 && this.movements[this.movements.length - 1] == null){
			this.movements = subset(this.movements, 0, this.movementPos);
		}
		
		return this.movements;
	}
	public boolean getIsBadger(){
		return this.isBadger;
	}
	public String getHerdId(){
		return this.herdId;
	}
	public int getNMovements(){
		return this.movementPos + 1;
	}
	public Calendar[] getStartEnd(){
		return this.startEnd;
	}
	public Hashtable<String, Herd> getHerdsInhabited(){
		return this.herdsInhabited;
	}
	public Episode getEpisode(){
		return this.episode;
	}
	
	// General methods
	public static Movement[] subset(Movement[] array, int start, int end){
		Movement[] part = new Movement[end - start + 1];
		
		int pos = -1;
		for(int index = 0; index < array.length; index++){
			
			if(index >= start && index <= end){
				pos++;
				part[pos] = array[index];
			}
		}
		
		return part;
	}
}
