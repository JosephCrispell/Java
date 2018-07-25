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
		StringBuffer string = new StringBuffer(array.get(0));
		
		for(int i = 1; i < array.size(); i++){
			string.append(sep);
			string.append(array.get(i));
		}
		
		return string.toString();
	}
	public static String toStringStr(ArrayList<String> array){
		StringBuffer string = new StringBuffer(array.get(0));
		
		for(int i = 1; i < array.size(); i++){
			string.append(array.get(i));
		}
		
		return string.toString();
	}
	public static String toStringChar(ArrayList<Character> array){
		StringBuffer string = new StringBuffer(array.get(0));
		
		for(int i = 1; i < array.size(); i++){
			string.append(array.get(i));
		}
		
		return string.toString();
	}

	public static int find(ArrayList<Character> array, char value){
		
		int index = -1;
		for(int i = 0; i < array.size(); i++) {

			if(array.get(i) == value) {
				index = i;
				break;
			}
		}
		
		return index;
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
	
	public static ArrayList<String> copyString(ArrayList<String> array){
		
		ArrayList<String> copy = new ArrayList<String>();
		for(String value : array){
			copy.add(value);
		}
		
		return copy;
	}
	public static ArrayList<Character> copyChar(ArrayList<Character> array){
		
		ArrayList<Character> copy = new ArrayList<Character>();
		for(char value : array){
			copy.add(value);
		}
		
		return copy;
	}
	public static ArrayList<Double> copyDouble(ArrayList<Double> array){
		
		ArrayList<Double> copy = new ArrayList<Double>();
		for(double value : array){
			copy.add(value);
		}
		
		return copy;
	}
	
	public static ArrayList<String> getUncommon(ArrayList<String> a, ArrayList<String> b){
		ArrayList<String> output = copyString(a);
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
	
	public static ArrayList<Character> toArrayList(char[] array){
		
		ArrayList<Character> arrayList = new ArrayList<Character>();
		for(char value : array) {
			arrayList.add(value);
		}
		
		return arrayList;
	}
	
	public static ArrayList<Character> subsetChar(ArrayList<Character> array, int start, int end){
		
		return new ArrayList<Character>(array.subList(start, end));
	}
	public static ArrayList<String> subsetString(ArrayList<String> array, int start, int end){
		
		return new ArrayList<String>(array.subList(start, end));
	}
}
