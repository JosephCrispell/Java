package VNTR10;

import java.util.Calendar;

public class Movement {

	public String offId;
	public String onId;
	public Calendar[] startEnd;
	public String animalId;
	public int pathLength;
	
	public Movement(String off, String on, Calendar[] startAndEndDateOfMovement, String id, int length){
		
		this.offId = off;
		this.onId = on;
		this.animalId = id;
		this.pathLength = length;
		
		this.startEnd = new Calendar[2];
		this.startEnd[0] = startAndEndDateOfMovement[0];
		if(startAndEndDateOfMovement[1] != null){
			this.startEnd[1] = startAndEndDateOfMovement[1];
		}
	}
	
	// Getting methods
	public String getOffId(){
		return this.offId;
	}
	public String getOnId(){
		return this.onId;
	}
	public Calendar[] getStartEnd(){
		return this.startEnd;
	}
	public String getAnimalId(){
		return this.animalId;
	}
	public int getPathLength(){
		return this.pathLength;
	}
}
