package main;

public class Simulation {
	public Particle[] particles;
	public SphereTarget[] targets;
	
	
	private long maximumSteps;
	private long stepTime;
	/** How many steps have progressed */
	public long ticks;
	
	private double cellRadius;
	private double particleRadius;
	private double targetRadius;
	
	/** Create the simulation with the defined parameters
	 * @param maximumSteps How many steps should be calculated before stopping the simulation
	 * @param stepTime The duration, in nanoseconds, that the computer should wait before starting the next tick. If this is shorter than the time taken to calculate a tick, then the next tick will start immediately. Otherwise, there will be a small time gap between the end of the step and the next step
	 * @param cellRadius The radius of the spherical area where the particles can move about
	 * @param particleRadius Radius of each individual particle
	 * @param targetRadius Radius of the spherical targets or the cylindrical radius of tube targets
	 */
	public Simulation(long maximumSteps, long stepTime, double cellRadius, double particleRadius, double targetRadius) {
		this.maximumSteps = maximumSteps;
		this.ticks = 0;
		this.cellRadius = cellRadius;
		this.particleRadius = particleRadius;
		this.targetRadius = targetRadius;
	}
	
	public void start() {
		// Begin the simulation
		// Used to schedule the loop
		long lastTickTime = System.nanoTime();
		// The current system time, in nanoseconds
		long now = System.nanoTime();
		
		
		
		// Generate particles in random locations
		int PARTICLE_COUNT = 1;
		int pindex = 0;
		// Only create the defined amount of particles
		for (; pindex < PARTICLE_COUNT; pindex += 1) {
			// Create a vector of random magnitude that is less than the cell radius in polar coordinates (and is also not touching it), and then convert it to Cartesian coordinates
			double magnitude = Math.random() * (cellRadius - particleRadius);
			double theta = Math.random() * Math.PI * 2;
			double phi = Math.random() * Math.PI;
			double[] vector = new double[] { magnitude * Math.sin(phi) * Math.cos(theta), magnitude * Math.sin(phi) * Math.sin(theta), magnitude * Math.cos(phi) };
			
			particles[pindex] = new Particle(vector, particleRadius);
		}
		
		// Generate targets in random locations
		// Spherical targets for now
		int TARGET_COUNT = 1;
		int tindex = 0;
		for (; tindex < TARGET_COUNT; tindex += 1) {
			// Create a vector of random magnitude that is less than the cell radius in polar coordinates, and then convert it to Cartesian coordinates
			double magnitude = Math.random() * cellRadius;
			double theta = Math.random() * Math.PI * 2;
			double phi = Math.random() * Math.PI;
			double[] vector = new double[] { magnitude * Math.sin(phi) * Math.cos(theta), magnitude * Math.sin(phi) * Math.sin(theta), magnitude * Math.cos(phi) };
			
			targets[tindex] = new SphereTarget(vector, targetRadius);
		}
		
		
		
		
		
		
		
		// Main loop
		while (ticks < maximumSteps) {
			// Set now to the current system time, and then compare its difference with the last tick time.
			if ((now = System.nanoTime()) - lastTickTime >= stepTime) {
				lastTickTime = now;
				tick();
				ticks += 1;
			}
		}
	}
	
	/** Step through the simulation */
	public void tick() {
		// Temporary variable to avoid accessing the array incessantly
		Particle particle;
		System.out.println("Tick " + ticks);
		// Loop through all the particles and move them
		for (int p = 0; p < particles.length; p += 1) {
			particle = particles[p];
			particle.move();
			
			// Ensure that particles do not escape the cell, by checking if the magnitude of its position is greater than the cell radius minus its radius (it's touching the edge of the cell)
			if (particle.x * particle.x + particle.y * particle.y + particle.z * particle.z > (cellRadius + particle.radius) * (cellRadius + particle.radius)) {
				// For now move it closer to the center of the cell by a fixed ratio
				particle.x *= 0.99;
				particle.y *= 0.99;
				particle.z *= 0.99;
			}
		}
		
		// Check for collisions between particles and targets
	}
}
