package methods;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class CalendarMethods {

	public static void main(String[] args) throws IOException, ParseException {
		String[] dateRangeStrings = {"12/01/2000", "24/01/2000"};
		
		int[] dayMonthYear = {0, 1, 2};
		
		Calendar[] dateRange = parseDates(dateRangeStrings, "/", dayMonthYear, true);
		
		double nDays = calculateNDaysBetweenDates(dateRange[0], dateRange[1]) - 1;
		
		int[] counts = new int[(int) nDays];
		
		Calendar date = parseDate("15/01/2000", "/", dayMonthYear, true);
		
	
		Calendar currentDate = copy(dateRange[0]);
		for(int i = 0; i < (int) nDays; i++){
			currentDate.add(Calendar.DATE, 1);
			System.out.println(toString(currentDate, "-") + "\t" + counts[i]);
		}
	}
	
	public static Calendar parseDateWithMonthInText(String dateString, String sep, int[] dayMonthYear, int monthLength,
			boolean suppress){
		
		if(suppress == false){
			System.out.println("Note method will only work for methods between 1920 and 2020!\nIf year is represented by only two digits");
		}		
		
		String[] parts = dateString.split(sep);
		
		int day = Integer.parseInt(parts[dayMonthYear[0]]);
		int month = convertMonthToInteger(parts[dayMonthYear[1]], monthLength) - 1; // NOTE: Calendar has months recorded from 0 to 11 and not 1 to 12 (year and day_of_month aren't)
		int year = Integer.parseInt(parts[dayMonthYear[2]]);

		if(parts[dayMonthYear[2]].length() == 2 && year < 20){
			year = year + 2000;
		}else if(parts[dayMonthYear[2]].length() == 2){
			year = year + 1900;
		}
		
		Calendar date = new GregorianCalendar(); // If time not changed, it will be the current time!
		date.set(year, month, day); //YYYY, MM, DD
		
		// Set time to be 1 millisecond after midnight of previous day exactly
		date.set(Calendar.HOUR, 0);
		date.set(Calendar.SECOND, 0);
		date.set(Calendar.MILLISECOND, 1);
		
		return date;
	}
	
 	public static int convertMonthToInteger(String month, int length){
		
		String[] months = {"January", "February", "March", "April", "May", "June", "July", 
				"August", "September", "October", "November", "December"};
		
		int output = -1;
		for(int i = 0; i < months.length; i++){
			
			if(month.matches("(?i:" + months[i].substring(0, length) + ")") == true){
				output = i + 1;
				break;
			}
		}
		
		return output;
	}
	
	public static Calendar[] copy(Calendar[] array){
		Calendar[] copy = new Calendar[array.length];
		
		for(int index = 0; index < array.length; index++){
			copy[index] = copy(array[index]);
		}
		
		return copy;
	}
	
	public static int[] getOrder(Calendar[] array){
		
		Calendar[] srtdArray = copy(array);
		int[] orderedIndices = ArrayMethods.seq(0, array.length - 1, 1);
		
		/**
		 * This Method Uses the Bubble Sort Algorithm
		 * 		Described here: http://en.wikipedia.org/wiki/Bubble_sort
		 * 
		 * 	For each element, compare it to the next element. If it is larger than the next element, swap the elements.
		 * 	Do this for each element of the list (except the last). Continue to iterate through the list elements and
		 *  make swaps until no swaps can be made.
		 */
		
		Calendar a;
		Calendar b;
		int aIndex;
		int bIndex;
		
		int swappedHappened = 1;
		while(swappedHappened == 1){ // Continue to compare the List elements until no swaps are made
		
			int swapped = 0;
			for(int index = 0; index < array.length - 1; index++){
				
				// Compare Current Element to Next Element
				if(after(srtdArray[index],srtdArray[index + 1]) == true){
					
					// Swap Current Element is Larger
					a = srtdArray[index];
					b = srtdArray[index + 1];
					
					srtdArray[index] = b;
					srtdArray[index + 1] = a;
					
					aIndex = orderedIndices[index];
					bIndex = orderedIndices[index + 1];
					orderedIndices[index] = bIndex;
					orderedIndices[index + 1] = aIndex;					
					
					// Record that a Swap occurred
					swapped++;
				}
			}
			
			// Check if any swaps happened during the last iteration - if none then finished
			if(swapped == 0){
				swappedHappened = 0;
			}
		}
		
		return orderedIndices;
	}

	public static Calendar[] orderArray(Calendar[] array, int[] order){
		
		Calendar[] orderedArray = new Calendar[array.length];
		
		for(int i = 0; i < order.length; i++){
			orderedArray[i] = array[order[i]];
		}
		
		return orderedArray;
	}

	public static Calendar min(Calendar[] array){
		
		Calendar min = array[0];
		for(int index = 0; index < array.length; index++){
			if(before(array[index],min) == true){
				min = array[index];
			}
		}
		
		return min;
	}
	
	public static Calendar[] combine(Calendar[] a, Calendar[] b){
		
		Calendar[] combined = new Calendar[a.length + b.length];
		
		for(int i = 0; i < a.length; i++){
			combined[i] = a[i];			
		}
		
		for(int i = 0; i < b.length; i++){
			combined[i + a.length] = b[i];
		}
		
		return combined;
	}
	public static String getCurrentDate(String format){
		
		// Get the current date and time
		DateFormat dateFormat = new SimpleDateFormat(format);
		Calendar cal = Calendar.getInstance();
		
		return dateFormat.format(cal.getTime());
				
	}
	
	public static Calendar createDate(int year){
		Calendar date = new GregorianCalendar(); // If time not changed, it will be the current time!
		
		date.set(Calendar.YEAR, year);
		date.set(Calendar.MONTH, 5); // Set to half way through year
		date.set(Calendar.DAY_OF_MONTH, 15); // Set to half way through month
		date.set(Calendar.HOUR, 0);
		date.set(Calendar.SECOND, 0);
		date.set(Calendar.MILLISECOND, 1);
		
		return date;
	}
	
	public static String toString(Calendar[] array, String dateSep, String sep){
		
		// Convert first date to string
		String output = CalendarMethods.toString(array[0], dateSep);
		
		// Convert the rest
		for(int i = 1; i < array.length; i++){
			output = output + sep + CalendarMethods.toString(array[i], dateSep);
		}
		
		return output;
	}
	
	public static double calculateNDaysBetweenDates(Calendar a, Calendar b){
		
		// Initialise a variable to store the number of days that separate the two dates
		double nDays = 0;
		
		if(a.compareTo(b) > 0){
			nDays = (a.getTimeInMillis() - b.getTimeInMillis())   / (24.0 * 60.0 * 60.0 * 1000.0);
		}else{
			nDays = (b.getTimeInMillis() - a.getTimeInMillis())   / (24.0 * 60.0 * 60.0 * 1000.0);
		}
		
		return nDays;
	}
	public static double calculateNDaysBetweenDates(long a, long b){
		
		// Initialise a variable to store the number of days that separate the two dates
		double nDays = 0;
		
		if(a > b){
			nDays = (a - b) / (24.0 * 60.0 * 60.0 * 1000.0);
		}else{
			nDays = (b - a) / (24.0 * 60.0 * 60.0 * 1000.0);
		}
		
		return nDays;
	}
	
	public static Calendar addDaysToDate(Calendar date, int nDays){
		
		Calendar newDate = new GregorianCalendar(); // If time not changed, it will be the current time!
		newDate.set(date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH) + nDays); //YYYY, MM, DD
		
		// Set time to be 1 millisecond after midnight of previous day exactly
		newDate.set(Calendar.HOUR, 0);
		newDate.set(Calendar.SECOND, 0);
		newDate.set(Calendar.MILLISECOND, 1);
		
		return newDate;
	}
	
	public static boolean before(Calendar a, Calendar b){
		
		boolean result = false;
		if(a.compareTo(b) < 0){
			result = true;
		}
		
		return result;
	}
	public static boolean after(Calendar a, Calendar b){
		
		boolean result = false;
		if(a.compareTo(b) > 0){
			result = true;
		}
		
		return result;
	}
	public static boolean equal(Calendar a, Calendar b){
	
		boolean result = false;
		if(a.compareTo(b) == 0){
			result = true;
		}
	
	return result;
}
	
	public static boolean checkIfDateIsWithinRange(Calendar date, Calendar[] dateRange){
		
		boolean result = false;
		if(after(date, dateRange[0]) == true && before(date, dateRange[1]) == true){
			result = true;
		}
		
		return result;
	}
	
	public static boolean checkIfDatesAreWithinRange(Calendar a, Calendar b, int nDays){
		
		// Create the bounds for the upper and lower of window
		Calendar upperDate = addDaysToDate(a, nDays);
		Calendar lowerDate = addDaysToDate(a, -1 * nDays);

		// Initialise a result
		boolean result = false;
		
		// Check if b is after the lower bound and before the upper bound
		if(b.compareTo(lowerDate) >= 0 && b.compareTo(upperDate) <= 0){
			
			result = true;			
		}		
		
		return result;
	}
	
	public static String toString(Calendar date, String sep){
		
		String output = "NA";
		if(date != null){
			output = date.get(Calendar.DAY_OF_MONTH) + sep + (date.get(Calendar.MONTH) + 1) + sep + date.get(Calendar.YEAR);	
		}
		
		return output;
	}
	
	public static Calendar parseDate(String dateString, String sep, int[] dayMonthYear, boolean suppress){
		
		// NOTE: Calendar has months recorded from 0 to 11 and not 1 to 12 (year and day_of_month aren't)
		String[] parts = dateString.split(sep);
		
		int day = Integer.parseInt(parts[dayMonthYear[0]]);
		int month = Integer.parseInt(parts[dayMonthYear[1]]);
		int year = Integer.parseInt(parts[dayMonthYear[2]]);
		
		// If year is represented by 2 digits convert to full year - not full proof!
		if(parts[dayMonthYear[2]].length() == 2){
			
			if(suppress == false){
				System.out.println("Note method will only work for methods between 1920 and 2020!\nIf year is represented by only two digits");
			}
			
			if(year < 20){
				year = year + 2000;
			}else{
				year = year + 1900;
			}
		}

		
		Calendar date = new GregorianCalendar(); // If time not changed, it will be the current time!
		date.set(year, month - 1, day); //YYYY, MM, DD
		
		// Set time to be 1 millisecond after midnight of previous day exactly
		date.set(Calendar.HOUR, 0);
		date.set(Calendar.SECOND, 0);
		date.set(Calendar.MILLISECOND, 1);
				
		return date;
	}

	public static Calendar[] parseDates(String[] dateStrings, String sep, int[] dayMonthYear, boolean suppress){
		
		Calendar[] dates = new Calendar[dateStrings.length];
		
		for(int i = 0; i < dates.length; i++){
			dates[i] = parseDate(dateStrings[i], sep, dayMonthYear, suppress);
		}
		
		return dates;
	}
	
	public static Calendar copy(Calendar date){
		
		Calendar newDate = Calendar.getInstance();
		newDate.setTimeInMillis(date.getTimeInMillis());
		
		return newDate;
	}
	
	public static Calendar[] append(Calendar[] array, Calendar value){
		Calendar[] newArray = new Calendar[array.length + 1];
		
		for(int index = 0; index < array.length; index++){
			newArray[index] = copy(array[index]);
		}
		newArray[newArray.length - 1] = copy(value);
		
		return newArray;
	}
	
	public static Calendar[] subset(Calendar[] array, int start, int end){
		Calendar[] part = new Calendar[end - start + 1];
		
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
