package methods;

import java.util.ArrayList;
import java.util.Hashtable;

public class ArrayListMethods {

	public static void main(String[] args){
		
		ArrayList<String> a = new ArrayList<String>();
		a.add("1:A");
		a.add("1:C");
		a.add("2:T");
		a.add("2:G");
		a.add("3:A");
		a.add("3:C");
		
		ArrayList<String> b = new ArrayList<String>();
		b.add("1:A");
		b.add("1:C");
		b.add("2:T");
		b.add("2:G");
		b.add("3:A");
		
		System.out.println(toString(intersect(a, b), ", "));
	}
	
	public static String toString(ArrayList<String> array, String sep){
		String string = array.get(0);
		
		for(int i = 1; i < array.size(); i++){
			string = string + sep + array.get(i);
		}
		
		return string;
	}

	
	public static ArrayList<String> intersect(ArrayList<String> a, ArrayList<String> b){
		
		// Initialise an array list to store the elements found in both a and b
		ArrayList<String> intersect = new ArrayList<String>();
		
		// Examine every element in a and check if in b
		for(String element : a){
			
			if(b.contains(element)){
				intersect.add(element);
			}
		}
		
		return intersect;
	}
	
	public static boolean compare(ArrayList<String> a, ArrayList<String> b){
		
		// Initialise a variable to store the result
		boolean result = false;
		
		// Check if input arrays are the same length
		if(a.size() == b.size()){
			
			// Check whether all of a are present in b
			int count = 0;
			for(int i = 0; i < a.size(); i++){
				if(b.contains(a.get(i)) == true){
					count++;
				}else{
					break;
				}
			}
			
			if(count == a.size()){
				result = true;
			}
		}
		
		return result;
	}
	
	public static ArrayList<String> copy(ArrayList<String> array){
		
		ArrayList<String> copy = new ArrayList<String>();
		for(String value : array){
			copy.add(value);
		}
		
		return copy;
	}
	
	public static ArrayList<String> getUncommon(ArrayList<String> a, ArrayList<String> b){
		ArrayList<String> output = copy(a);
		remove(output, b);
		
		return output;
	}
	
	public static void remove(ArrayList<String> array, String value){
		
		int index = array.indexOf(value);
		if(index != -1){
			array.remove(index);
		}
	}
	public static void remove(ArrayList<String> array, ArrayList<String> values){
		
		for(String value : values){
			
			remove(array, value);
		}
	}
	
	public static ArrayList<String> subset(ArrayList<String> array, int start, int end){
		ArrayList<String> part = new ArrayList<String>();

		for(int index = start; index <= end; index++){
			
			part.add(array.get(index));
		}
		
		return part;
	}
}
