package methods;

import java.util.ArrayList;
import java.util.Hashtable;

public class ArrayListMethods {

	public static void main(String[] args){
		
		String test = "D034,Yes,CUB,1997,FEMALE,10-Mar-99,1999,NA,NA,NA,5.7,M.BOVIS,\"11,17\",NA,NA,,,,,,,,,,,Super excretor";
		
		ArrayList<Integer> indices = getIndicesOfCharacterInString(test, '\"');
		
		System.out.println(toStringInt(indices, ", "));
	}
	
	public static ArrayList<Integer> getIndicesOfCharacterInString(String string, char character){
		
		// Split the string into its characters
		char[] characters = string.toCharArray();
		
		// Initialise an array to store the indices of the character
		ArrayList<Integer> indices = new ArrayList<Integer>();
		
		// Examine each character in string
		for(int i = 0; i < characters.length; i++) {
			
			// Check if current character is equal to the input character
			if(characters[i] == character) {
				indices.add(i);
			}
		}
		
		return(indices);
	}
	
	public static ArrayList<Character> toArrayList(String string){
		
		// Initialise an arraylist to store the characters
        ArrayList<Character> array = new ArrayList<Character>(string.length());

        // Add each character of the string into the array list
        for (int x = 0; x < string.length(); x ++){
        	array.add(string.charAt(x));
        }
        
        return array;
	}
	
	public static Hashtable<Integer, Integer> indexArrayListInteger(ArrayList<Integer> array){
		
		Hashtable<Integer, Integer> indexed = new Hashtable<Integer, Integer>();
		for(int i = 0; i < array.size(); i++) {
			indexed.put(array.get(i), i);
		}
		
		return indexed;
	}
	
	public static String toString(ArrayList<String> array, String sep){
		StringBuilder string = new StringBuilder(array.get(0));
		
		for(int i = 1; i < array.size(); i++){
			string.append(sep);
			string.append(array.get(i));
		}
		
		return string.toString();
	}
	public static String toStringInt(ArrayList<Integer> array, String sep){
		StringBuilder string = new StringBuilder(array.size());
		string.append(array.get(0));
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
		StringBuilder string = new StringBuilder(array.size());
		for(int i = 0; i < array.size(); i++){
			string.append(array.get(i));
		}
		
		return string.toString();
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
