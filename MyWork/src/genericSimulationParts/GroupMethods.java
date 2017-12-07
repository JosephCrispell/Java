package genericSimulationParts;

public class GroupMethods {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	
	public static Individual[] flatten(Group[] groups){
		
		Individual[] individuals = new Individual[99999];
		int pos = -1;
		
		for(Group group : groups){
			for(Individual individual : group.getIndividuals()){
				pos++;
				individuals[pos] = individual;
			}
		}
		
		return IndividualMethods.subset(individuals, 0, pos);
		
	}
	
	public static double[][] createIndividualDistanceMatrix(Individual[] population, double[][] groupDistanceMatrix){
		
		// Initialise Individual Distance Matrix
		double[][] individualDistanceMatrix = new double[population.length][population.length];
		
		for(int i = 0; i < population.length; i++){
			
			population[i].setIndex(i);
			
			for(int j = 0; j < population.length; j++){
				
				// Skip comparing the same individuals
				if(i == j || individualDistanceMatrix[i][j] != 0){
					continue;
				}
				
				// Are these individuals in the same group?
				if(population[i].getGroupIndex() == population[j].getGroupIndex()){
					individualDistanceMatrix[i][j] = 1;
					individualDistanceMatrix[j][i] = 1;
				}else{
					// How far are their respective groups apart?
					individualDistanceMatrix[i][j] = groupDistanceMatrix[population[i].getGroupIndex()][population[j].getGroupIndex()];
					individualDistanceMatrix[j][i] = groupDistanceMatrix[population[j].getGroupIndex()][population[i].getGroupIndex()];
				}
				
			}
		}
		
		return individualDistanceMatrix;
	}

	
}
