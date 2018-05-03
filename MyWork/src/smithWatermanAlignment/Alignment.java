package smithWatermanAlignment;

import methods.ArrayMethods;

public class Alignment {

	public char[][] alignment;
	public int score;
	
	public Alignment(char[] a, char[] b, int value){
		this.alignment = new char[2][a.length];
		this.alignment[0] = a;
		this.alignment[1] = b;
		this.score = value;
	}
	
	// Getting methods
	public char[][] getAlignment(){
		return this.alignment;
	}
	public int getScore(){
		return this.score;
	}
	
	// General methods
	public void print(){
		System.out.println("Alignment score = " + this.score);
		System.out.println(ArrayMethods.toString(this.alignment[0]));
		for(int i = 0; i < this.alignment[1].length; i++){
			if(this.alignment[0][i] == this.alignment[1][i]){
				System.out.print("|");
			}else{
				System.out.print(" ");
			}
		}
		System.out.println("\n" + ArrayMethods.toString(this.alignment[1]));
	}

}
