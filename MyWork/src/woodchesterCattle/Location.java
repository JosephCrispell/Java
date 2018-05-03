package woodchesterCattle;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;

import methods.ArrayMethods;
import methods.CalendarMethods;
import methods.HashtableMethods;

public class Location {

	public String locationId;
	public int posInAdjacencyMatrix;
	
	public String cph;
	public int x = -1;
	public int y = -1;
	public String herdType;
	public String premisesType;
	
	public Calendar[] onDates;
	public long[] starts;
	public Calendar[] offDates;
	public long[] ends;
	public double[] periodsSpentOnThisHerd;
	
	public Hashtable[] animalsInhabited = new Hashtable[2]; // Pre and post
	
	public double[][] landParcelCentroids;
	
	// Calculating herd size variables
	public Hashtable<String, Integer> animals = new Hashtable<String, Integer>();
	public Hashtable<String, Calendar[]> animalsON = new Hashtable<String, Calendar[]>(); // Calculating herd size
	public Hashtable<String, Calendar[]> animalsOFF = new Hashtable<String, Calendar[]>(); // Calculating herd size
	
	public Location(String id){
		this.locationId = id;
		animalsInhabited[0] = new Hashtable<String, Integer>();
		animalsInhabited[1] = new Hashtable<String, Integer>();
	}
	
	// Setting Methods
	public void setPosInAdjacencyMatrix(int pos){
		this.posInAdjacencyMatrix = pos;
	}
	public void setCph(String holding){
		this.cph = holding;
	}
	public void setX(String value){
		
		if(value.matches("") == false){
			this.x = Integer.parseInt(value);
		}		
	}
	public void setY(String value){
		if(value.matches("") == false){
			this.y = Integer.parseInt(value);
		}
	}
	public void setHerdType(String farmType){
		this.herdType = farmType;
	}
	public void setPremisesType(String type){
		
		if(type.matches("Agricultural Holding")){
			type = "AH";
		}else if(type.matches("Landless Keeper")){
			type = "LK";
		}else if(type.matches("Collection Centre(.*)")){
			type = "CC";
		}else if(type.matches("Export Assembly Centre")){
			type = "";
		}
		
		// Define the hashtable of Acronyms
		Hashtable<String, String> acronymesForTypes = new Hashtable<String, String>();
		acronymesForTypes.put("Agency", "AG");
		acronymesForTypes.put("Agricultural Holding","AH");
		acronymesForTypes.put("AI Sub Centre","AI");
		acronymesForTypes.put("Article 18 Premises","AR");
		acronymesForTypes.put("Calf Collection Centre","CA");
		acronymesForTypes.put("Collection Centre (for BSE material)","CC");
		acronymesForTypes.put("Cutting Room","CR");
		acronymesForTypes.put("Cold Store","CS");
		acronymesForTypes.put("Embryo Transfer Unit","ET");
		acronymesForTypes.put("Export Assembly Centre","EX");
		acronymesForTypes.put("Head Boning Plant","HB");
		acronymesForTypes.put("Hunt Kennel","HK");
		acronymesForTypes.put("Incinerator","IN");
		acronymesForTypes.put("Imported Protein Premises","IP");
		acronymesForTypes.put("Knackers Yard","KY");
		acronymesForTypes.put("Landless Keeper","LK");
		acronymesForTypes.put("Market","MA");
		acronymesForTypes.put("Meat Products Plant","MP");
		acronymesForTypes.put("Protein Processing Plant","PP");
		acronymesForTypes.put("Showground","SG");
		acronymesForTypes.put("Slaughterhouse, Both MP and Cold Store","SM");
		acronymesForTypes.put("Slaughterhouse (Red Meat)","SR");
		acronymesForTypes.put("Semen Shop","SS");
		acronymesForTypes.put("Slaughterhouse (White Meat)","SW");
		acronymesForTypes.put("Waste Food Premises","WF");
		acronymesForTypes.put("No Premises Type Specified","ZZ");
		
		if(acronymesForTypes.get(type) != null){
			type = acronymesForTypes.get(type);
		}
		this.premisesType = type;
	}
	public void addAnimal(int prePostIndex, String animalId){
		this.animalsInhabited[prePostIndex].put(animalId, 1);
	}
	public void appendStayInfo(Calendar on, Calendar off){

		// Check if we have recorded any previous stays
		if(this.onDates == null){
			
			this.onDates = new Calendar[1];
			this.offDates = new Calendar[1];
			this.starts = new long[1];
			this.ends = new long[1];
			this.periodsSpentOnThisHerd = new double[1];
			
			this.onDates[0] = on;
			this.offDates[0] = off;
			this.starts[0] = on.getTimeInMillis();
			this.ends[0] = off.getTimeInMillis();
			this.periodsSpentOnThisHerd[0] = (this.ends[0] - this.starts[0]) / (24 * 60 * 60 * 1000);
		}else{
			
			// Append this information
			this.onDates = CalendarMethods.append(this.onDates, on);
			this.offDates = CalendarMethods.append(this.offDates, off);
			this.starts = ArrayMethods.append(this.starts, on.getTimeInMillis());
			this.ends = ArrayMethods.append(this.ends, off.getTimeInMillis());
			double period = (this.ends[this.ends.length - 1] - this.starts[this.starts.length - 1]) / (24 * 60 * 60 * 1000);
			this.periodsSpentOnThisHerd = ArrayMethods.append(this.periodsSpentOnThisHerd, period);
		}
	}
	public void appendLandParcelCentroid(double[] centroid){
		
		// Check the land parcel centroid array exists
		if(this.landParcelCentroids == null){
			this.landParcelCentroids = new double[0][2];
		}
		
		double[][] newArray = new double[this.landParcelCentroids.length + 1][2];
		
		for(int i = 0; i < this.landParcelCentroids.length; i++){
			newArray[i] = this.landParcelCentroids[i];
		}
		newArray[this.landParcelCentroids.length] = centroid;
		
		this.landParcelCentroids = newArray;
	}
	public void addInhabitant(String id, Calendar date, boolean onMovement){
		
		if(onMovement){
			if(this.animalsON.containsKey(id) == false){
				this.animals.put(id, 1);
				Calendar[] dates = {date};
				this.animalsON.put(id, dates);
			}else{
				this.animalsON.put(id, CalendarMethods.append(animalsON.get(id), date));
			}			
		}else{
			if(this.animalsOFF.containsKey(id) == false){
				this.animals.put(id, 1);
				Calendar[] dates = {date};
				this.animalsOFF.put(id, dates);
			}else{
				this.animalsOFF.put(id, CalendarMethods.append(animalsOFF.get(id), date));
			}			
		}
	}
	
	// Getting Methods
	public String getLocationId(){
		return this.locationId;
	}
	public int getPosInAdjacencyMatrix(){
		return this.posInAdjacencyMatrix;
	}
	public String getCph(){
		return this.cph;
	}
	public int getX(){
		return this.x;
	}
	public int getY(){
		return this.y;
	}
	public String getHerdType(){
		return this.herdType;
	}
	public Calendar[] getOnDates(){
		return this.onDates;
	}
	public Calendar[] getOffDates(){
		return this.offDates;
	}
	public long[] getStarts(){
		return this.starts;
	}
	public long[] getEnds(){
		return this.ends;
	}
	public double[] getPeriodsSpentOnHerd(){
		return this.periodsSpentOnThisHerd;
	}
	public String getPremisesType(){
		return this.premisesType;
	}
	public Hashtable[] getAnimalsInhabited(){
		return this.animalsInhabited;
	}
	public double[][] getLandParcelCentroids(){
		return this.landParcelCentroids;
	}
	public Hashtable<String, Calendar[]> getAnimalsOnMovementDates(){
		return this.animalsON;
	}
	public Hashtable<String, Calendar[]> getAnimalsOffMovementDates(){
		return this.animalsOFF;
	}
	public String[] getInhabitantIds(){
		return HashtableMethods.getKeysString(this.animals);
	}
}
