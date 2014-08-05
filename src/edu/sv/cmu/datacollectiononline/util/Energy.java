package edu.sv.cmu.datacollectiononline.util;

public class Energy {
	public  double energy_sum;
	public  float average_energy;
	public  int energy_count;
	public  int max_energy;
	public  int max_avg_energy;
	
	public Energy(){
		this.energy_sum = 0;
		this.average_energy = 0;
		this.energy_count = 0;
		this.max_energy = 0;
		this.max_avg_energy = 0;
	}
	public void reset(){
		energy_sum = 0;
		average_energy = 0;
		energy_count = 0;
		max_energy = 0;
		max_avg_energy = 0;
	}
	public void update(double frame_energy){
		energy_sum+=frame_energy;
		energy_count++;
		average_energy =  ((float)energy_sum/energy_count);
		average_energy = (float) (energy_sum/energy_count);
		max_energy = (int) Math.max(frame_energy,max_energy);
		max_avg_energy = (int) Math.max(average_energy, max_avg_energy);

	}
}
