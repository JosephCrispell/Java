package VNTR10;

import java.util.Calendar;

public class Episode {
	String id;
	Calendar[] startEnd;
	
	public Episode(String episodeId, Calendar[] startEndDates){
		this.id = episodeId;
		this.startEnd = new Calendar[2];
		this.startEnd[0] = startEndDates[0];
		if(startEndDates[1] != null){
			this.startEnd[1] = startEndDates[1];
		}
	}
	
	// Getting methods
	public String getId(){
		return this.id;
	}
	public Calendar[] getStartEnd(){
		return this.startEnd;
	}
}
