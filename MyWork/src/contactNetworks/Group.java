package contactNetworks;

public class Group {

	public Individual[] individuals;
	public int groupId;
	
	public Group(Individual[] individualsInGroup, int id) {

		this.individuals = individualsInGroup;
		this.groupId = id;

	}
	
	public void setIndividuals(Individual[] individualsInGroup){
		this.individuals = individualsInGroup;
	}
	public void setGroupId(int id){
		this.groupId = id;
	}
	
	public Individual[] getIndividuals(){
		return this.individuals;
	}
	public int getGroupId(){
		return this.groupId;
	}

}
