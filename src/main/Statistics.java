package main;

public class Statistics {
	public double[] times;
	public int timeIndex;
	private Simulation simulation;
	
	public double averageTime,
		timeVariance,
		timeSD,
		predictedTime,
		maximumTime,
		minimumTime,
		lowerQuartileTime,
		upperQuartileTime;
	
	
	public Statistics(int timeCount, Simulation simulation) {
		this.times = new double[timeCount];
		this.simulation = simulation;
	}
	
	public void reset() {
		timeIndex = 0;
	}
	
	public void pushTime(double time) {
		times[timeIndex++] = time;
	}
	/** Bucket values for histogram */
	public long[] bucket(double interval) {
		long[] occurrences = new long[(int) ((maximumTime - minimumTime)/interval) + 1];
		
		for (int t = 0; t < timeIndex; ++t)
			++occurrences[(int) ((times[t] - minimumTime)/interval)];
		
		return occurrences;
	}
	/** Calculate relevant statistics about the simulation */
	public void compute() {
		double sum = 0, squareSum = 0, minimum = 10e60, maximum = -10e60;
		for (int d = 0; d < timeIndex; ++d) {
			if (times[d] < minimum)
				minimum = times[d];
			if (times[d] > maximum)
				maximum = times[d];
			sum += times[d];
			squareSum += times[d] * times[d];
		}
		
		maximumTime = maximum;
		minimumTime = minimum;
		
		lowerQuartileTime = times[(int) (timeIndex * 0.25)];
		upperQuartileTime = times[(int) (timeIndex * 0.75)];
		
		sum /= timeIndex;
		
		averageTime = sum;
		timeSD = Math.sqrt(timeVariance = squareSum/timeIndex - sum);	
		
		double predicted_value = predictedTime =
				1/simulation.diffusion_constant *
				(Math.pow(simulation.cellRadius, 3)/(3 * simulation.targetRadius) + 3 * Math.pow(simulation.cellRadius, 2)/5);
		
		if (Main.VERBOSE_LOGGING) {
			System.out.println("There are " + timeIndex + " recorded times.");
			System.out.println("Average time is " + sum + "s (" + (long) (sum/simulation.tau) + " ticks)");
			System.out.println("The maximum time is " + maximum + "s and the minimum time is " + minimum + "s");
			System.out.println("The predicted average time is " + predicted_value);
		}
	}
	public void print() {
		System.out.println("Simulation time results:\n"
				+ "Average:        " + averageTime + "\n"
				+ "Variance:       " + timeVariance + "\n"
				+ "Deviation:      " + timeSD + "\n"
				+ "Upper quartile: " + upperQuartileTime + "\n"
				+ "Lower quartile: " + lowerQuartileTime + "\n");
	}
}
