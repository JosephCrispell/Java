package methods;
import java.util.Arrays;


@SuppressWarnings("unused")
public class GraphMethods {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// Methods Testing Area
		int[][] edgeList = { {0,1}, {0,3}, {1,4}, {9,2}, {2,4}, {3,5}, {4,6}, {45,5} };
		int[][] adjacencyMatrix = buildAdjacencyMatrix(edgeList);
		int[][] distanceMatrix = buildDistanceMatrix(adjacencyMatrix);
		
		int[][] newEdgeList = convertVertexIdsInEdgeList(edgeList);
		
		MatrixMethods.print(newEdgeList);
		System.out.println();

	}
	
	public static int[][] convertVertexIdsInEdgeList(int[][] edgeList){
		
		int[][] newEdgeList = MatrixMethods.copy(edgeList);

		// Find Unique Vertex Ids
		int[] vertexIds = findUniqueVertices(edgeList);
		
		// Give Equivalent Vertex Ids
		int[] newVertexIds = ArrayMethods.range(0, vertexIds.length - 1, 1);
		
		// Substitute in the Vertex Ids
		for(int i = 0; i < edgeList.length; i++){
			for(int j = 0; j < 2; j++){
				
				// Find Vertex Id in Old List
				int index = -1;
				for(int id : vertexIds){
					index++;
					if(id == newEdgeList[i][j]){
						break;
					}
				}
				
				// Replace the Vertex Id with new Id
				newEdgeList[i][j] = newVertexIds[index];
			}
		}
		
		return newEdgeList;
	}
	
	public static int[][] buildAdjacencyMatrix(int[][] edgeList){
		
		// EdgeList must be in the Format: { {0,1}, {1,2}, {2,3}, ...}
		// Must start at 0
		
		// Need to derive List of Vertices
		int[] vertexIds = getVertexIds(edgeList);
		
		// Now Build Adjacency Matrix
		int[][] adjacencyMatrix = new int[vertexIds.length][vertexIds.length];
		adjacencyMatrix = MatrixMethods.fill(adjacencyMatrix, 0);
		
		for(int[] edge : edgeList){
			adjacencyMatrix[edge[0]][edge[1]] = 1;
			adjacencyMatrix[edge[1]][edge[0]] = 1;
		}
		
		return adjacencyMatrix;
	}
	
	public static int[][] buildDistanceMatrix(int[][] adjacencyMatrix){
		
		int[][] distanceMatrix = MatrixMethods.copy(adjacencyMatrix);
		
		for(int i = 0; i < adjacencyMatrix.length; i++){
			
			for(int j = 0; j < adjacencyMatrix[0].length; j++){
				
				// Ignore Self Edges or Directly Connected Vertices
				if(i == j || distanceMatrix[i][j] == 1){
					continue;
				}
				
				// i and j are not directly connected
				// Are any of j's neighbours connected to i?
				for(int k = 0; k < distanceMatrix[j].length; k++){
					
					// Only interested in vertices which are connected to j
					if(distanceMatrix[j][k] == 0 || j == k){
						continue;
					}
					
					// Is k connected to i?
					if(distanceMatrix[i][k] != 0){
						
						// Calculate Distance between i and j
						int distance = distanceMatrix[i][k] + distanceMatrix[j][k];
						
						// Note that want the shortest distance between i and j - so update if found shorter path
						if(distanceMatrix[i][j] == 0 || distanceMatrix[i][j] > distance){
							distanceMatrix[i][j] = distance;
							distanceMatrix[j][i] = distance; // Symmetric
						}
					}
				}	
			}
		}
				
		return distanceMatrix;
	}
	
	public static double[][] buildDistanceMatrixDouble(int[][] adjacencyMatrix){
		
		double[][] distanceMatrix = MatrixMethods.copyConvert2Double(adjacencyMatrix);
		
		for(int i = 0; i < adjacencyMatrix.length; i++){
			
			for(int j = 0; j < adjacencyMatrix[0].length; j++){
				
				// Ignore Self Edges or Directly Connected Vertices
				if(i == j || distanceMatrix[i][j] == 1){
					continue;
				}
				
				// i and j are not directly connected
				// Are any of j's neighbours connected to i?
				for(int k = 0; k < distanceMatrix[j].length; k++){
					
					// Only interested in vertices which are connected to j
					if(distanceMatrix[j][k] == 0 || j == k){
						continue;
					}
					
					// Is k connected to i?
					if(distanceMatrix[i][k] != 0){
						
						// Calculate Distance between i and j
						double distance = distanceMatrix[i][k] + distanceMatrix[j][k];
						
						// Note that want the shortest distance between i and j - so update if found shorter path
						if(distanceMatrix[i][j] == 0 || distanceMatrix[i][j] > distance){
							distanceMatrix[i][j] = distance;
							distanceMatrix[j][i] = distance; // Symmetric
						}
					}
				}	
			}
		}
				
		return distanceMatrix;
	}
	
	public static int[] getVertexIds(int[][] edgelist){
		int max = 0;
		
		for(int[] row : edgelist){
			for(int element : row){
				
				if(element > max){
					max = element;
				}
			}
		}
		
		return ArrayMethods.range(0, max, 1);
	}

	public static int vertexDegree(int[][] adjacencyMatrix, int vertex){
		return ArrayMethods.sum(adjacencyMatrix[vertex]);
	}

	public static int[] findUniqueVertices(int[][] edgeList){
		int[] unqVertexIds = {};
		for(int[] edge : edgeList){
			
			for(int vertex : edge){
				int found = 0;
				
				for(int id : unqVertexIds){
					if(	id == vertex ){
						found++;
					}
				}
				
				if(found == 0){
					unqVertexIds = ArrayMethods.append(unqVertexIds, vertex);
				}
			}
		}
		
		return ArrayMethods.sort(unqVertexIds);
	}
}
