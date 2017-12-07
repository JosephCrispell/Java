package woodchesterBadgers;

import java.util.Arrays;
import java.util.Calendar;

import geneticDistances.Sequence;
import methods.ArrayMethods;

public class SampleInfo {

	
	public String wbId;
	public String tattoo;
	public Calendar date = Calendar.getInstance(); 
	public String badgerGroup;
	public String[] sampleInfo;
	public String spoligotype;
	public Sequence sequence;
	public String sampleType;
	
	public SampleInfo(String id, String tat, String dateString, String group, String type, String sample, String[] otherInfo){
		
		this.wbId = id;
		this.tattoo = tat;
		parseDate(dateString);
		parseGroup(group);
		this.spoligotype = type;
		this.sampleInfo = otherInfo;
		this.sampleType = sample;
	}
	
	// Setting Methods
	public void setSequence(Sequence seq){
		this.sequence = seq;
	}
	
	// Getting Methods
	public String getWbId(){
		return this.wbId;
	}
	public String getTattoo(){
		return this.tattoo;
	}
	public Calendar getDate(){
		return this.date;
	}
	public String getBadgerGroup(){
		return this.badgerGroup;
	}
	public String getSpoligotype(){
		return this.spoligotype;
	}
	public String[] getSampleInfo(){
		return this.sampleInfo;
	}
	public Sequence getSequence(){
		return this.sequence;
	}
	public String getSampleType(){
		return this.sampleType;
	}
	
	
	public void parseGroup(String group){
		
		String[] parts = group.split(" ");
		
		String name = "";
		for(String part : parts){
			name = name + part;
		}
		
		this.badgerGroup = name;
	}
	
	public void parseDate(String dateString){
		
		String[] parts = dateString.split("/");
	
		this.date.set(Integer.parseInt(parts[2]),Integer.parseInt(parts[1]), Integer.parseInt(parts[0]));
		
	}
	
	
	// General Methods
	public static SampleInfo[] append(SampleInfo[] array, SampleInfo value){
		SampleInfo[] newArray = new SampleInfo[array.length + 1];
		
		for(int index = 0; index < array.length; index++){
			newArray[index] = array[index];
		}
		newArray[newArray.length - 1] = value;
		
		return newArray;
	}
}
