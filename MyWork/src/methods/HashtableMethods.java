package methods;

import java.util.Hashtable;
import java.util.Set;

public class HashtableMethods {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		Hashtable<String, Integer> a = new Hashtable<String, Integer>();
		
		a.put("A", 1);
		a.put("B", 1);
		a.put("C", 1);
		a.put("D", 1);
		a.put("E", 1);
		
		Hashtable<String, Integer> b = new Hashtable<String, Integer>();
		
		b.put("F", 1);
		b.put("G", 1);
		b.put("H", 1);
		b.put("I", 1);
		b.put("J", 1);
		
		combineHashtablesUsingStringsForKeys(a, b);
		
		System.out.println(ArrayMethods.toString(HashtableMethods.getKeysString(a), ", "));
		
	}
	
	public static void combineHashtablesUsingStringsForKeys(Hashtable a, Hashtable b){
		
		for(String key : getKeysString(b)){
			a.put(key, b.get(key));
		}
	}
	
	public static void removeKeys(Hashtable a, String[] keys){
		
		for(String key : keys){
			
			if(a.get(key) != null){
				a.remove(key);
			}
		}		
	}
	
	public static int countSharedKeysString(Hashtable a, Hashtable b){
		int count = 0;
		
		String[] keys = getKeysString(a);
		
		for(String key : keys){
			
			if(b.get(key) != null){
				count++;
			}
		}
		
		return count;
	}
	
	public static Hashtable<Integer, Integer> indexArray(int[] array){
		
		// Initialise a hashtable to store the index of each element of the input array
		Hashtable<Integer, Integer> indexed = new Hashtable<Integer, Integer>();
		
		// Index each element of the input array
		for(int i = 0; i < array.length; i++){
			indexed.put(array[i], i);
		}
		
		return indexed;
	}
	
	public static Hashtable<String, Integer> indexArray(String[] array){
		
		// Initialise a hashtable to store the index of each element of the input array
		Hashtable<String, Integer> indexed = new Hashtable<String, Integer>();
		
		// Index each element of the input array
		for(int i = 0; i < array.length; i++){
			indexed.put(array[i], i);
		}
		
		return indexed;
	}
	
	public static Hashtable<Character, Integer> indexArray(char[] array){
		
		// Initialise a hashtable to store the index of each element of the input array
		Hashtable<Character, Integer> indexed = new Hashtable<Character, Integer>();
		
		// Index each element of the input array
		for(int i = 0; i < array.length; i++){
			indexed.put(array[i], i);
		}
		
		return indexed;
	}
	
	public static int[] getKeysInt(Hashtable table){
		
		Set<Integer> keys = table.keySet();
		int[] values = new int[table.size()];
		
		int pos = -1;
		for(int key : keys){
			pos++;
			
			values[pos] = key;
		}
		
		return values;
		
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

	public static char[] getKeysChar(Hashtable table){
		
		Set<Character> keys = table.keySet();
		char[] values = new char[table.size()];
		
		int pos = -1;
		for(char key : keys){
			pos++;
			
			values[pos] = key;
		}
		
		return values;
		
	}

	public static String[] getValuesString(Hashtable<String, String> table, String[] keys){
		String[] values = new String[keys.length];
		
		for(int i = 0; i < keys.length; i++){
			
			values[i] = table.get(keys[i]);
		}
		
		return values;
	}
	public static int[] getValuesInt(Hashtable<String, Integer> table, String[] keys){
		int[] values = new int[keys.length];
		
		for(int i = 0; i < keys.length; i++){
			
			values[i] = table.get(keys[i]);
		}
		
		return values;
	}

	
	public static void print(Hashtable<String, String> table){
		
		String[] keys = getKeysString(table);
		for(String key : keys){
			System.out.println(key + "\t" + table.get(key));
		}
	}
}
