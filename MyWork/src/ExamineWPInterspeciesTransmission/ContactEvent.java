package ExamineWPInterspeciesTransmission;

import java.util.Calendar;

public class ContactEvent {

	public String idOfAnimal;
	public String idOfContact;
	Calendar[] startDates;
	Calendar[] endDates;
	public double timeTogether;
	String[] contactGroupsOrHerds;
	
	public ContactEvent(String id, String contactId, Calendar[] starts, Calendar[] ends, double length, String[] locationIds){
		this.idOfAnimal = id;
		this.idOfContact = contactId;
		this.startDates = starts;
		this.endDates = ends;
		this.timeTogether = length;
		this.contactGroupsOrHerds = locationIds;
	}
	
	// Getting Methods
	public String getIdOfAnimal(){
		return this.idOfAnimal;
	}
	public String getIdOfContact(){
		return this.idOfContact;
	}
	public Calendar[] getStartDates(){
		return this.startDates;
	}
	public Calendar[] getEndDates(){
		return this.endDates;
	}
	public double getTimeTogether(){
		return this.timeTogether;
	}
	public String[] getContactGroupsOrHerds(){
		return this.contactGroupsOrHerds;
	}
	
	// General Methods
	public static ContactEvent[] subset(ContactEvent[] array, int start, int end){
		
		ContactEvent[] part = new ContactEvent[end - start + 1];
		
		int pos = -1;
		for(int index = 0; index < array.length; index++){
			
			if(index >= start && index <= end){
				pos++;
				part[pos] = array[index];
			}
		}
		
		return part;
	}
	
	public static  ContactEvent[] append(ContactEvent[] array, ContactEvent value){
		ContactEvent[] newArray = new ContactEvent[array.length + 1];
		
		for(int index = 0; index < array.length; index++){
			newArray[index] = array[index];
		}
		newArray[newArray.length - 1] = value;
		
		return newArray;
	}
	
	public static ContactEvent[] combine(ContactEvent[] a, ContactEvent[] b){
		
		ContactEvent[] combined = new ContactEvent[a.length + b.length];
		
		for(int i = 0; i < a.length; i++){
			combined[i] = a[i];			
		}
		
		for(int i = 0; i < b.length; i++){
			combined[i + a.length] = b[i];
		}
		
		return combined;
	}
}
