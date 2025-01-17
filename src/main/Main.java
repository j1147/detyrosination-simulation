package main;

public class Main {
	// Various debugging measures
	// Log when a particle bounces off the edge of the cell
	public static boolean BOUNCE_MESSAGES = false;
	
	public static void main(String[] args) {
		// Simulation parameters, see the simulation file for more information
		int particleCount = 1;
		int targetCount = 1;
		
		long maximumSteps = 1000000;
		long stepTime = 0; // Set to 0 for back to back calculations (no waiting between ticks, but this makes any visualization impossible). 16ms is 60 FPS
		stepTime *= 1000 * 1000; // Convert milliseconds to nanoseconds
		
		double cellRadius = 1000;
		double particleRadius = 10;
		double targetRadius = 20;
		
		Simulation simulator = new Simulation(particleCount, targetCount, maximumSteps, stepTime, cellRadius, particleRadius, targetRadius);
		simulator.start();
	}
}
