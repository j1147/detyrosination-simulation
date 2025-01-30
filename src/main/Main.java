package main;

import java.io.FileWriter;
import java.io.IOException;

public class Main {
	
	public static final int
		PARTICLE_COUNT = 2000,
		TARGET_COUNT = 1;
	public static final long
		MAXIMUM_STEPS = 400000,
		// Convert milliseconds to nanoseconds
		STEP_TIME = (1000 * 1000) * 0; // Set to 0 for back to back calculations (no waiting between ticks, but this makes any visualization impossible). 16ms is 60 FPS
	public static final boolean
		WAIT_UNTIL_DONE = true, // Wait until all particles have hit rather than doing a predefined number of ticks
		VERBOSE_LOGGING = true;
	
	// 5000 -> 0
	// 10000 -> PI
	// 0 -> -PI
	public static final double[]
			cachedSine = new double[10000],
			cachedCosine = new double[10000];
	public static double sin(double x) {
		return cachedSine[(int) (((x - Math.PI) % Math.PI) * cachedSine.length/(2 * Math.PI))];
	}
	public static double cos(double x) {
		return cachedCosine[(int) (((x - Math.PI) % Math.PI) * cachedSine.length/(2 * Math.PI))];
	}
	
	
	public static void main(String[] args) {
		
		// Pre generate sine values to avoid calculating later
		for (int i = 0; i < cachedSine.length; ++i) {
			// todo
		}
		
		
		
		double defaultCellRadius = 1.0,
				defaultTargetRadius = 0.1,
				defaultDiffusionConstant = 0.5;
		
		Simulation simulator;
		
		int SIMULATION_TYPE = 0;
		String csv = "Parameters," + SIMULATION_TYPE + "," + PARTICLE_COUNT;
		
		// It's coded this way so that what is tested can be easily switched with 1 line of code
		switch (SIMULATION_TYPE) {
			case 0: // Changing time constant only
				/*
				double timeConstants[] = new double[] { 0.1, 0.075, 0.05, 0.025, 0.01, 0.0075, 0.005, 0.0025, 0.001 };
				for (int s = 0; s < timeConstants.length; ++s) {
					simulator = new Simulation(cellRadius, targetRadius, diffusionConstant, timeConstants[s]);
					simulator.start();
					simulator.stats.print();
					csv += simulator.toCSV();
				}*/
				csv += "," + defaultCellRadius + "," + defaultTargetRadius + "," + defaultDiffusionConstant + "\n";
				csv += "Title,Changing Time Constant\n";
				
				double timeConstant = 1.0/10_000;
				double ratio = 1.20226;
				int simulations = 50;
				for (int s = 0; s < simulations; ++s) {
					System.out.println("[Simulation " + s + "]");
					simulator = new Simulation(defaultCellRadius, defaultTargetRadius, defaultDiffusionConstant, timeConstant);
					simulator.start();
					simulator.stats.print();
					csv += simulator.toCSV(SIMULATION_TYPE);
					timeConstant *= ratio;
				}
				break;
			case 1: // Changing diffusion constant only
				double diffusionConstant = 0.001;
				csv += "," + defaultCellRadius + "," + defaultTargetRadius + "," + 0.014 + "\n";
				csv += "Title,Changing Diffusion Constant\n";
				for (int s = 0; s < 21; ++s) {
					System.out.println("[Simulation " + s + "]");
					simulator = new Simulation(defaultCellRadius, defaultTargetRadius, diffusionConstant, 0.014);
					simulator.start();
					simulator.stats.print();
					csv += simulator.toCSV(SIMULATION_TYPE);
					diffusionConstant += s == 0 ? 0.04 : 0.05;
				}
				break;
			case 2: // See distribution of time
				csv += "," + defaultCellRadius + "," + defaultTargetRadius + "," + 0.1 + "," + 0.014 + "\n";
				csv += "Title,Simulation Time Distribution\n";
				/*
				// ########### Was used to test the distribution of the gaussian function. ignore
				int distributionOccurrences[] = new int[1000];
				double scaling = 100;
				for (int n = 0, index; n < 100000; ++n) {
					// 0 -> 500
					// 1 -> 600
					// -1 -> 400
					double gaussian = Simulation.gaussian();
					index = (int) (gaussian * scaling) + distributionOccurrences.length/2;
					if (index >= 0 && index < distributionOccurrences.length)
						++distributionOccurrences[index];
				}
				for (int i = 0; i < distributionOccurrences.length; ++i)
					csv += (i - distributionOccurrences.length/2)/scaling + "," + distributionOccurrences[i] + "\n";
				// ##################
				//*/
				simulator = new Simulation(defaultCellRadius, defaultTargetRadius, 0.1, 0.014);
				simulator.start();
				simulator.stats.print();
				csv += simulator.toCSV(SIMULATION_TYPE);
				break;
			default:
				// Test parameters without exporting anything, used for debugging
				//simulator = new Simulation(defaultCellRadius, defaultTargetRadius, defaultDiffusionConstant, 0.014);
				//simulator.start();
				
				double magnitude, reflectingMagnitude, radius = 0;
				double x = 9,
						y = 6,
						z = 0,
						cellRadius = 10;
				
				System.out.println("Before: (" + x + ", " + y + ", " + z + ") " + Math.sqrt(x * x + y * y + z * z));
				if ((magnitude = x * x + y * y + z * z) > (cellRadius - radius) * (cellRadius - radius)) {
					// Reflect back into the domain
					magnitude = Math.sqrt(magnitude);
					reflectingMagnitude = 2 * ((cellRadius - radius) - magnitude)/magnitude;
					System.out.println("Reflect by " + reflectingMagnitude + " " + reflectingMagnitude * Math.sqrt(x * x + y * y + z * z));
					x += x * reflectingMagnitude;
					y += y * reflectingMagnitude;
					z += z * reflectingMagnitude;
				}
				System.out.println("After: (" + x + ", " + y + ", " + z + ") " + Math.sqrt(x * x + y * y + z * z));
				return;
		}
		
		
		export(csv);
	}
	/** Used to test the difference between circular vs axial gaussian movement */
	public static void testDistribution() {
		double xmean = 0, ymean = 0, xsquare = 0, ysquare = 0;
		//*
		for (int i = 0; i < 1000; ++i) {
			double theta = Math.random() * 2 * Math.PI;
			double magnitude = Simulation.gaussian();
			//System.out.println(theta);
			//System.out.println("(" + Math.cos(theta) * magnitude + ", " + Math.sin(magnitude) + ")"); weird shape
			xmean += Math.cos(theta) * magnitude;
			ymean += Math.sin(theta) * magnitude;
			xsquare += Math.pow(Math.cos(theta) * magnitude, 2);
			ysquare += Math.pow(Math.sin(theta) * magnitude, 2);
			System.out.println("(" + Math.cos(theta) * magnitude + ", " + Math.sin(theta) * magnitude + ")");
		}
		//*/
		
		//*
		for (int i = 0; i < 1000; ++i) {
			//System.out.println(theta);
			//System.out.println("(" + Math.cos(theta) * magnitude + ", " + Math.sin(magnitude) + ")"); weird shape
			double magnitudeX = Simulation.gaussian(), magnitudeY = Simulation.gaussian();
			xmean += magnitudeX;
			ymean += magnitudeY;
			xsquare += Math.pow(magnitudeX, 2);
			ysquare += Math.pow(magnitudeY, 2);
			System.out.println("(" + magnitudeX + ", " + magnitudeY + ")");
		}
		//*/
		
		xmean /= 1000;
		ymean /= 1000;
		System.out.println("X mean: " + xmean + ", Y mean: " + ymean);
		System.out.println("X SD: " + (xsquare/1000 - xmean) + ", Y SD: " + (ysquare/1000 - ymean));
	}
	
	public static void export(String data) {
		try {
			FileWriter writer = new FileWriter("C:/Users/giveaway/simulation_data.csv");
			System.out.println("Exporting data...");
			writer.write(data);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
