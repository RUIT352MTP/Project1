//Timothy Choi
//Michael Shafran
//Prasant Sinha

package RUBTClient;


public class Counter implements Comparable<Counter>{
	
	private int id;
	public int getId(){
		return this.id;
	}
	public void setId(int id){
		this.id = id;
	}

	private int count;
	public int getCount(){
		return count;
	}

	public Counter(int id){
		this.id = id;
		this.count = 0;
	}

	public int compareTo(Counter counter) {
		return (this.count - counter.getCount());
	}
	public void increment(){
		this.count++;
	}

}
