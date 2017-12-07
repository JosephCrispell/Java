package woodchesterCattle;

import java.util.Calendar;
import java.util.Hashtable;

import methods.ArrayMethods;
import methods.CalendarMethods;

public class IsolateData {

	
	public String eartag;
	public String cph;
	public String cphh;
	public Calendar cultureDate;
	public String strainId;
	public Calendar breakdownDate;
	public String locationId;
	
	public Calendar[] testDates = new Calendar[9];
	public String[] testResults = new String[9];
	public int testIndex = -1;
	public boolean testInfoSubsetted = false;
	
	public String movementId;
	public Calendar birth;
	public Calendar death;
	public long start = -1;
	public long end = -1;
	
	public Movement[] movementRecords;
	int movementPos = -1;
	int limit;
	
	public String[] otherInfo;
	
	public Hashtable<String, Location> infoForHerdsInhabited = new Hashtable<String, Location>(); // A list of herds that the sampeld animal spent time on
	public String mainHerd; // The herd the cow spent the most time on
		
	public IsolateData(String tag, String herd, Calendar day, String id, String[] array) {
		
		this.eartag = tag;
		this.cphh = herd;
		this.cultureDate = day;
		this.strainId = id;
		this.otherInfo = array;
	}

	// Setting Methods
	public void setEartag(String tag){
		this.eartag = tag;
	}
	public void setCphh(String herd){
		this.cphh = herd;
	}
	public void setCultureDate(Calendar day){
		this.cultureDate = day;
	}
	public void setStrainId(String id){
		this.strainId = id;
	}
	public void setOtherInfo(String[] array){
		this.otherInfo = array;
	}
	public void setCph(String holding){
		this.cph = holding;
	}
	public void setMovementId(String id){
		this.movementId = id;
	}
	public void setBirth(Calendar day){
		this.birth = day;
	}
	public void setDeath(Calendar day){
		this.death = day;
	}
	public void setBreakdownDate(Calendar day){
		this.breakdownDate = day;
	}
	public void appendMovement(Movement record, int size){
		
		// Check that the movement records array exists
		if(this.movementRecords == null){
			this.limit = size;
			this.movementRecords = new Movement[limit];
		}
		
		// Move to the next position
		this.movementPos++;
		
		// Check haven't reached the end of the array
		if(this.movementPos < this.movementRecords.length){
			
			this.movementRecords[this.movementPos] = record;
		}else{
			
			Movement[] newArray = new Movement[this.movementRecords.length + this.limit];
			for(int i = 0; i < this.movementRecords.length; i++){
				newArray[i] = this.movementRecords[i];
			}
			newArray[this.movementPos] = record;
		}
	}
	public void setMainherd(String herd){
		this.mainHerd = herd;
	}
	public void setMovementRecords(Movement[] movements){
		this.movementRecords = movements;
		this.movementPos = movements.length - 1;
	}
	public void setInfoForHerdsInhabited(Hashtable<String, Location> infoForHerds){
		this.infoForHerdsInhabited = infoForHerds;
	}
	public void setStart(long time){
		this.start = time;
	}
	public void setEnd(long time){
		this.end = time;
	}
	public void setLocationId(String id){
		this.locationId = id;
	}
	public void addTestData(Calendar date, String result){
		this.testIndex++;
		
		if(this.testIndex < this.testDates.length){
			this.testDates[this.testIndex] = date;
			this.testResults[this.testIndex] = result;
		}else{
			Calendar[] newDatesArray = new Calendar[this.testDates.length * 2];
			String[] newResultsArray = new String[this.testResults.length * 2];
			for(int i = 0; i < this.testDates.length; i++){
				newDatesArray[i] = this.testDates[i];
				newResultsArray[i] = this.testResults[i];
			}
			newDatesArray[this.testIndex] = date;
			newResultsArray[this.testIndex] = result;
			
			this.testDates = newDatesArray;
			this.testResults = newResultsArray;
		}
	}
	
	// Getting Methods
	public String getEartag(){
		return this.eartag;
	}
	public String getCphh(){
		return this.cphh;
	}
	public Calendar getCultureDate(){
		return this.cultureDate;
	}
	public String getStrainId(){
		return this.strainId;
	}
	public String[] getOtherInfo(){
		return this.otherInfo;
	}
	public String getCph(){
		return this.cph;
	}
	public String getMovementId(){
		return this.movementId;
	}
	public Calendar getBreakdownDate(){
		return this.breakdownDate;
	}
	public Movement[] getMovementRecords(){
		
		// Check if we need to subset
		if(this.movementRecords.length > 0 && this.movementRecords[this.movementRecords.length - 1] == null){
			this.movementRecords = Movement.subset(this.movementRecords, 0, this.movementPos);
		}
		
		return movementRecords;
	}
	public String getMainHerd(){
		return this.mainHerd;
	}
	public Calendar getBirth(){
		return this.birth;
	}
	public Calendar getDeath(){
		return this.death;
	}
	public int getNMovements(){
		return this.movementPos + 1;
	}
	public Hashtable<String, Location> getInfoForHerdsInhabited(){
		return this.infoForHerdsInhabited;
	}
	public long getStart(){
		return this.start;
	}
	public long getEnd(){
		return this.end;
	}
	public Calendar getStartDate(){
		
		Calendar startDate = Calendar.getInstance();
		startDate.setTimeInMillis(this.start);
		return startDate;
	}
	public Calendar getEndDate(){
		
		Calendar endDate = Calendar.getInstance();
		endDate.setTimeInMillis(this.end);
		return endDate;
	}
	public String getLocationId(){
		return this.locationId;
	}
	public Calendar[] getTestDates(){
		
		if(this.testInfoSubsetted == false){
			this.testDates = CalendarMethods.subset(this.testDates, 0, this.testIndex);
			this.testResults = ArrayMethods.subset(this.testResults, 0, this.testIndex);
			this.testInfoSubsetted = true;
		}
		
		return this.testDates;
	}
	public String[] getTestResults(){
		
		if(this.testInfoSubsetted == false){
			this.testDates = CalendarMethods.subset(this.testDates, 0, this.testIndex);
			this.testResults = ArrayMethods.subset(this.testResults, 0, this.testIndex);
			this.testInfoSubsetted = true;
		}
		
		return this.testResults;
	}
	public int getNTests(){
		return this.testIndex + 1;
	}
	
	// General Methods
	public static IsolateData[] append(IsolateData[] array, IsolateData value){
		IsolateData[] newArray = new IsolateData[array.length + 1];
		
		for(int index = 0; index < array.length; index++){
			newArray[index] = array[index];
		}
		newArray[newArray.length - 1] = value;
		
		return newArray;
	}
}
