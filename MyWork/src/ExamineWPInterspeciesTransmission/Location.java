package ExamineWPInterspeciesTransmission;

import java.util.Hashtable;

public class Location {

	public String locationId;
	
	public String cph;
	public int x = -1;
	public int y = -1;
	public String herdType;
	public String premisesType;
	
	public Location(String id, String CPH, String xCoord, String yCoord, 
			String herdDescription, String premisesDescription){
		this.locationId = id;
		
		this.cph = CPH;
		if(xCoord != null && xCoord.matches("") == false){
			this.x = Integer.parseInt(xCoord);
		}
		if(yCoord != null && yCoord.matches("") == false){
			this.y = Integer.parseInt(yCoord);
		}
		this.herdType = herdDescription;
		this.premisesType = premisesDescription;
		if(premisesDescription != null && premisesDescription.length() > 2){
			parsePremisesType(premisesDescription);
		}		
	}
	
	// Setting Methods
	public void parsePremisesType(String type){
		
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
		acronymesForTypes.put("Slaughterhouse","SR");
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
	
	public void setX(int value){
		this.x = value;
	}
	
	public void setY(int value){
		this.y = value;
	}
	
	public void setPremisesType(String premisesDescription){
		this.premisesType = premisesDescription;
		if(premisesDescription != null && premisesDescription.length() > 2){
			parsePremisesType(premisesDescription);
		}		
	}
	
	public void setCph(String value){
		this.cph = value;
	}
	
	public void setHerdType(String type){
		this.herdType = type;
	}
	
	// Getting Methods
	public String getLocationId(){
		return this.locationId;
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
	public String getPremisesType(){
		return this.premisesType;
	}

}

