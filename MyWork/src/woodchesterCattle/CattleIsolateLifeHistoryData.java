package woodchesterCattle;

import java.util.Hashtable;

public class CattleIsolateLifeHistoryData {

	public Hashtable<String, IsolateData> isolates;
	public Hashtable<String, String> eartagsForStrainIds;
	public Hashtable<String, Location> locations;
	public MovementNetwork networkInfo;
	
	public CattleIsolateLifeHistoryData(Hashtable<String, IsolateData> isolateInfo, 
			Hashtable<String, Location> locationInfo, MovementNetwork network, Hashtable<String, String> eartags){
		
		this.isolates = isolateInfo;
		this.locations = locationInfo;
		this.networkInfo = network;
		this.eartagsForStrainIds = eartags;
	}
	
	// Setting Methods
	public void setIsolates(Hashtable<String, IsolateData> isolateInfo){
		this.isolates = isolateInfo;
	}
	public void setLocations(Hashtable<String, Location> locationInfo){
		this.locations = locationInfo;
	}
	public void setNetworkInfo(MovementNetwork network){
		this.networkInfo = network;
	}
	public void setEartagsForStrainIds(Hashtable<String, String> eartags){
		this.eartagsForStrainIds = eartags;
	}
	
	// Getting Methods
	public Hashtable<String, IsolateData> getIsolates(){
		return this.isolates;
	}
	public Hashtable<String, Location> getLocations(){
		return this.locations;
	}
	public MovementNetwork getNetworkInfo(){
		return this.networkInfo;
	}
	public Hashtable<String, String> getEartagsForStrainIds(){
		return this.eartagsForStrainIds;
	}
	
}
