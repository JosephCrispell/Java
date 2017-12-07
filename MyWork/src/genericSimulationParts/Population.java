package genericSimulationParts;

public class Population {

	public Group[] groups;
	public double[][] groupDistanceMatrix;
	public double[][] individualDistanceMatrix;
	public Individual[] individuals;
	
	public Population(Group[] groupsInPop) {
		
		this.groups = groupsInPop;
	}
	
	public void setGroups(Group[] groupsInPop){
		this.groups = groupsInPop;
		this.individuals = GroupMethods.flatten(groups);
	}
	public void setGroupDistanceMatrix(double[][] distanceMatrix){
		this.groupDistanceMatrix = distanceMatrix;
		this.individualDistanceMatrix = GroupMethods.createIndividualDistanceMatrix(this.individuals, distanceMatrix);
	}
	
	public Group[] getGroups(){
		return this.groups;
	}
	public double[][] getGroupDistanceMatrix(){
		return this.groupDistanceMatrix;
	}
	public double[][] getIndividualDistanceMatrix(){
		return this.individualDistanceMatrix;
	}
	public Individual[] getIndividuals(){
		return individuals;
	}
}
