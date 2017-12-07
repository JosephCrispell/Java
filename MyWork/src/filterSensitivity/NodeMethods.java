package filterSensitivity;


public class NodeMethods {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	// Methods
	public static Node[] copy(Node[] array){
		Node[] copy = new Node[array.length];
		
		for(int index = 0; index < array.length; index++){
			copy[index] = array[index];
		}
		
		return copy;
	}
	
	public static Node[] deletePosition(Node[] array, int position){
		Node[] newArray = new Node[array.length - 1];
		
		int pos = -1;
		for(int index = 0; index < array.length; index++){
			if(index != position){
				pos++;
				newArray[pos] = array[index];
			}
		}
		
		return newArray;
	}

	public static Node[] append(Node[] array, Node node){
		Node[] newArray = new Node[array.length + 1];
		
		for(int index = 0; index < array.length; index++){
			newArray[index] = array[index];
		}
		newArray[newArray.length - 1] = node;
		
		return newArray;
	}
}
