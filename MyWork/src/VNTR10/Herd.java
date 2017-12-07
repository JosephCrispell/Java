package VNTR10;

import java.util.Calendar;

import methods.ArrayMethods;
import methods.CalendarMethods;

public class Herd {
	
	public String id;
	public int index;
	
	public Episode[] episodes;
	public int episodePos = -1;
	public int limit;
	
	public Calendar[] onDates;
	public long[] starts;
	public Calendar[] offDates;
	public long[] ends;
	public double[] periodsSpentOnThisHerd;
	
	public Herd(String herdId){
		this.id = herdId;
	}
	
	// Setting methods
	public void setIndex(int i){
		this.index = i;
	}
	public void setId(String herdId){
		this.id = herdId;
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
	public void appendEpisode(Episode episode, int size){
		
		// Check that the episodes array exists
		if(this.episodes == null){
			this.limit = size;
			this.episodes = new Episode[limit];
		}
		
		// Move to the next position
		this.episodePos++;
		
		// Check haven't reached the end of the array
		if(this.episodePos < this.episodes.length){
			
			this.episodes[this.episodePos] = episode;
		}else{
			
			Episode[] newArray = new Episode[this.episodes.length + this.limit];
			for(int i = 0; i < this.episodes.length; i++){
				newArray[i] = this.episodes[i];
			}
			newArray[this.episodePos] = episode;
		}
	}
	
	// Getting methods
	public int getNEpisodes(){
		return this.episodePos + 1;
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
	public String getId(){
		return this.id;
	}
	public Episode[] getEpisodes(){
		
		// Check if we need to subset
		if(this.episodes.length > 0 && this.episodes[this.episodes.length - 1] == null){
			this.episodes = subset(this.episodes, 0, this.episodePos);
		}
		
		return this.episodes;
	}
	public int getIndex(){
		return this.index;
	}
	
	// General methods
	public static Episode[] subset(Episode[] array, int start, int end){
		Episode[] part = new Episode[end - start + 1];
		
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
