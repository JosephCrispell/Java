package homoplasyFinderV2;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.Set;

public class Methods {
	
	public static String getCurrentDate(String format){
		
		// Get the current date and time
		DateFormat dateFormat = new SimpleDateFormat(format);
		Calendar cal = Calendar.getInstance();
		
		return dateFormat.format(cal.getTime());
	}
	
	public static Hashtable<Integer, Integer> indexArrayListInteger(ArrayList<Integer> array){
		
		Hashtable<Integer, Integer> indexed = new Hashtable<Integer, Integer>();
		for(int i = 0; i < array.size(); i++) {
			indexed.put(array.get(i), i);
		}
		
		return indexed;
	}
	
	public static char[] deletePositions(char[] array, Hashtable<Integer, Integer> positionsToIgnore){
		
		// Initialise a new array to store the sequence
		char[] output = new char[array.length - positionsToIgnore.size()];
		int pos = -1;
		for(int i = 0; i < array.length; i++){
			
			if(positionsToIgnore.containsKey(i) == true){
				continue;
			}
			
			pos++;
			output[pos] = array[i];
		}
		
		return output;
	}
	
	public static String toString(char[] array){
		StringBuilder string = new StringBuilder();
		
		for(int i = 0; i < array.length; i++){
			string.append(array[i]);
		}
		
		return string.toString();
	}
	
	public static String toString(String[] array, String sep){
		
		if(array.length == 0) {
			return "";
		}
		
		String string = array[0];
		
		for(int i = 1; i < array.length; i++){
			string = string + sep + array[i];
		}
		
		return string;
	}
	
	public static String toString(int[] array, String sep){
		
		if(array.length == 0) {
			return "";
		}
		
		String string = Integer.toString(array[0]);
		
		for(int i = 1; i < array.length; i++){
			string = string + sep + Integer.toString(array[i]);
		}
		
		return string;
	}
	
	public static String[] getKeysString(Hashtable table){
		
		Set<String> keys = table.keySet();
		String[] values = new String[table.size()];
		
		int pos = -1;
		for(String key : keys){
			pos++;
			
			values[pos] = key;
		}
		
		return values;
		
	}
	
	public static String toStringDouble(ArrayList<Double> array, String sep){
		StringBuilder string = new StringBuilder(array.size());
		string.append(array.get(0));
		for(int i = 1; i < array.size(); i++){
			string.append(sep);
			string.append(array.get(i));
		}
		
		return string.toString();
	}

	public static String toStringChar(ArrayList<Character> array){
		StringBuilder string = new StringBuilder(array.size());
		for(int i = 0; i < array.size(); i++){
			string.append(array.get(i));
		}
		
		return string.toString();
	}
	
	public static String toStringInt(ArrayList<Integer> array, String sep){
		StringBuilder string = new StringBuilder(array.size());
		string.append(array.get(0));
		for(int i = 1; i < array.size(); i++){
			string.append(sep + array.get(i));
		}
		
		return string.toString();
	}

	public static ArrayList<Character> subsetChar(ArrayList<Character> array, int start, int end){
		
		return new ArrayList<Character>(array.subList(start, end));
	}
	
	public static ArrayList<Character> toArrayList(char[] array){
		
		ArrayList<Character> arrayList = new ArrayList<Character>();
		for(char value : array) {
			arrayList.add(value);
		}
		
		return arrayList;
	}
	
	public static ArrayList<Double> copyDouble(ArrayList<Double> array){
		
		ArrayList<Double> copy = new ArrayList<Double>();
		for(double value : array){
			copy.add(value);
		}
		
		return copy;
	}
}
