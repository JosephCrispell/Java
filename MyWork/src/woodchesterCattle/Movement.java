package woodchesterCattle;

import java.util.Calendar;

public class Movement {

	public String movementId;
	public int movementNumber = -1;
	public Calendar date;
	public String offLocation;
	public String onLocation;
	public boolean isBirth;
	public boolean isDeath;
	public int stayLength = -1;
	
	public Movement(String id, String number, Calendar day, String off, String on,
			boolean birth, boolean death, String nDays) {
		
		this.movementId = id;
		this.date = day;
		this.offLocation = off;
		this.onLocation = on;
		this.isBirth = birth;
		this.isDeath = death;
		
		if(nDays.matches("") == false){
			this.stayLength = Integer.parseInt(nDays);
		}
		if(number.matches("") == false){
			this.movementNumber = Integer.parseInt(number);
		}
	}
	
	// Getting Methods
	public String getMovementId(){
		return this.movementId;
	}
	public int getMovementNumber(){
		return this.movementNumber;
	}
	public Calendar getDate(){
		return this.date;
	}
	public String getOffLocation(){
		return this.offLocation;
	}
	public String getOnLocation(){
		return this.onLocation;
	}
	public boolean getIsBirth(){
		return this.isBirth;
	}
	public boolean getIsDeath(){
		return this.isDeath;
	}
	public int getStayLength(){
		return this.stayLength;
	}
	
	// General Methods
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
