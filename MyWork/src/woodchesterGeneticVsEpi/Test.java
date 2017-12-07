package woodchesterGeneticVsEpi;

import java.io.IOException;
import java.util.Hashtable;

public class Test {
	public static void main(String[] args) throws IOException{
		
		char[] letters = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I'};
		
		int[][] adjacencyMatrix = {	{0, 0, 0, 4, 0, 0, 0, 0, 0},
									{0, 0, 1, 0, 0, 0, 0, 0, 0},
									{0, 0, 0, 0, 0, 0, 0, 0, 0},
									{0, 1, 0, 0, 1, 0, 0, 0, 0},
									{0, 1, 0, 0, 0, 0, 1, 0, 0},
									{0, 1, 0, 0, 0, 0, 0, 0, 0},
									{0, 0, 0, 0, 0, 0, 0, 0, 0},
									{1, 0, 0, 0, 0, 0, 0, 0, 0},
									{0, 0, 0, 0, 0, 0, 1, 0, 0}	};
		int start = 0;
		int[][] shortestPaths = CompareIsolates.findShortestPathsFromNode(start, adjacencyMatrix);
		
		for(int i = 0; i < shortestPaths.length; i++){
			
			if(shortestPaths[i].length != 0){
				
				System.out.print(letters[start] + " -> " + letters[i] + "\t---\t" + toString(shortestPaths[i], ", ", letters));
				System.out.print("\t\t" + CompareIsolates.calculateMeanNMovementsOnEdgesOfShortestPath(shortestPaths[i], i, adjacencyMatrix) + "\n");
			}else{
				System.out.println(letters[start] + " -> " + letters[i] + "\t---\t");
			}
			
		}
	}
	
	public static String toString(int[] array, String sep, char[] letters){
		String string = letters[array[0]] + "";
		
		for(int i = 1; i < array.length; i++){
			string = string + sep + letters[array[i]];
		}
		
		return string;
	}
}
