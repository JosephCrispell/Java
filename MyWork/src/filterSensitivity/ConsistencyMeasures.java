package filterSensitivity;

public class ConsistencyMeasures {

	public DistanceMatrix nearestNeighbourConsistency;
	public DistanceMatrix herdProportionsConsistency;
	public DistanceMatrix herdNearestNeighbourConsistency;
	
	public ConsistencyMeasures(DistanceMatrix NN, DistanceMatrix NNHP, DistanceMatrix HNN){
		this.nearestNeighbourConsistency = NN;
		this.herdProportionsConsistency = NNHP;
		this.herdNearestNeighbourConsistency = HNN;
	}

	// Setting Methods
	public void setNearestNeighbourConsistency(DistanceMatrix NN){
		this.nearestNeighbourConsistency = NN;
	}
	
	public void setHerdProportionsConsistency(DistanceMatrix NNHP){
		this.herdProportionsConsistency = NNHP;
	}
	
	public void setHerdNearestNeighbourConsistency(DistanceMatrix HNN){
		this.herdNearestNeighbourConsistency = HNN;
	}
	    
	// Getting Methods
	public DistanceMatrix getNearestNeighbourConsistency(){
		return nearestNeighbourConsistency;
	}
	
	public DistanceMatrix getHerdProportionsConsistency(){
		return herdProportionsConsistency;
	}
	
	public DistanceMatrix getHerdNearestNeighbourConsistency(){
		return herdNearestNeighbourConsistency;
	}
}
