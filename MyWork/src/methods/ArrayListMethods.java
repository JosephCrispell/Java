package methods;

import java.util.ArrayList;
import java.util.Hashtable;

public class ArrayListMethods {

	public static void main(String[] args){
		
		ArrayList<String> array = new ArrayList<String>();
		array.add("a");
		array.add("b");
		array.add("c");
		array.add("d");
		
		System.out.println(toString(array, ", ") + "\t" + array.size());
		
		remove(array, "a");
		
		System.out.println(toString(array, ", ") + "\t" + array.size());
	}
	
	public static String toString(ArrayList<String> array, String sep){
		String string = array.get(0);
		
		for(int i = 1; i < array.size(); i++){
			string = string + sep + array.get(i);
		}
		
		return string;
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
}