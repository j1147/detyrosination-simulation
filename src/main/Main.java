package main;

public class Main {
	public static void main(String[] args) {
		// Simulation parameters, see the simulation file for more information
		long maximumSteps = 100;
		long stepTime = 10 * 1000;
		double cellRadius = 1000;
		double particleRadius = 10;
		double targetRadius = 20;
		
		Simulation simulator = new Simulation(maximumSteps, stepTime, cellRadius, particleRadius, targetRadius);
		simulator.start();
	}
}
