package main;

public class Simulation {
	public Particle[] particles;
	public SphereTarget[] targets;
	
	private int particleCount;
	private int targetCount;
	private long maximumSteps;
	private long stepTime;
	/** How many steps have progressed */
	public long ticks;
	
	private double cellRadius;
	private double particleRadius;
	private double targetRadius;
	
	/** The time differential, or how much time passes each tick. It's imaginary, as this simulation proceeds as fast as the computer will run it */
	public static double TAU							= 1;
	/** A constant that defines how far a particle moves during random motion */
	public static double DIFFUSION_CONSTANT				= 10;
	/** Probability that if a particle is resting, it will switch to diffusing */
	public static double RESTING_SWITCH_PROBABILITY		= 0.1;
	/** Probability that if a particle is diffusing, it will switch to resting */
	public static double DIFFUSING_SWITCH_PROBABILITY	= 0.1;
	/** Probability that a particle starts in the diffusion state */
	public static double DIFFUSE_STARTING_PROBABILITY	= 0.5;
	
	/** Gives a Gaussian value for particle movement */
	public static double gaussian() {
		return 2 * (Math.random() - 0.5);
	}
	
	/** Create the simulation with the defined parameters
	 * @param particleCount How many particles to generate and move
	 * @param targetCount How many targets to use
	 * @param maximumSteps How many steps should be calculated before stopping the simulation
	 * @param stepTime The duration, in nanoseconds, that the computer should wait before starting the next tick. If this is shorter than the time taken to calculate a tick, then the next tick will start immediately. Otherwise, there will be a small time gap between the end of a step and the next step
	 * @param cellRadius The radius of the spherical area where the particles can move about
	 * @param particleRadius Radius of each individual particle
	 * @param targetRadius Radius of the spherical targets or the cylindrical radius of tube targets
	 */
	public Simulation(int particleCount, int targetCount, long maximumSteps, long stepTime, double cellRadius, double particleRadius, double targetRadius) {
		this.particleCount = particleCount;
		this.targetCount = targetCount;
		this.maximumSteps = maximumSteps;
		this.ticks = 0;
		this.cellRadius = cellRadius;
		this.particleRadius = particleRadius;
		this.targetRadius = targetRadius;
		this.stepTime = stepTime;
		
		this.particles = new Particle[this.particleCount];
		this.targets = new SphereTarget[this.targetCount];
	}
	/** Begin the simulation */
	public void start() {
		// Used to schedule the loop
		long lastTickTime = System.nanoTime();
		// The current system time, in nanoseconds
		long now = System.nanoTime();
		
		
		
		// Generate particles in random locations
		int pindex = 0;
		// Only create the defined amount of particles, see the constructor for the number of particles
		for (; pindex < particles.length; pindex += 1) {
			// Create a vector of random magnitude that is less than the cell radius in polar coordinates (and is also not touching it), and then convert it to Cartesian coordinates
			double magnitude = Math.random() * (cellRadius - particleRadius);
			double theta = Math.random() * Math.PI * 2;
			double phi = Math.random() * Math.PI;
			double[] vector = new double[] { magnitude * Math.sin(phi) * Math.cos(theta), magnitude * Math.sin(phi) * Math.sin(theta), magnitude * Math.cos(phi) };
			
			particles[pindex] = new Particle(vector, particleRadius);
			particles[pindex].isResting = !(Math.random() < Simulation.DIFFUSE_STARTING_PROBABILITY);
		}
		
		// Generate targets in random locations
		// Spherical targets for now
		int tindex = 0;
		for (; tindex < targets.length; tindex += 1) {
			// Create a vector of random magnitude that is less than the cell radius in polar coordinates, and then convert it to Cartesian coordinates
			double magnitude = Math.random() * cellRadius;
			double theta = Math.random() * Math.PI * 2;
			double phi = Math.random() * Math.PI;
			double[] vector = new double[] { magnitude * Math.sin(phi) * Math.cos(theta), magnitude * Math.sin(phi) * Math.sin(theta), magnitude * Math.cos(phi) };
			
			targets[tindex] = new SphereTarget(vector, targetRadius);
		}
		
		
		
		
		
		// Main loop
		long then = System.nanoTime(); // Performance profiling
		while (ticks < maximumSteps) {
			// Set now to the current system time, and then compare its difference with the last tick time to the step time.
			if ((now = System.nanoTime()) - lastTickTime >= stepTime) {
				lastTickTime = now;
				tick();
				ticks += 1;
			}
		}
		long duration = (System.nanoTime() - then)/1_000_000;
		System.out.println("Completed simulation in " + duration + "ms (" + duration/1000.0 + "s)");
	}
	
	/** Step through the simulation */
	public void tick() {
		// Temporary variable to avoid accessing the array incessantly
		Particle particle;
		SphereTarget target;
		//System.out.println("Tick " + ticks);
		// Loop through all the particles and move them
		double magnitude, reflectingMagnitude;
		for (int p = 0; p < particles.length; p += 1) {
			particle = particles[p];
			
			// Switch between rest and diffusion
			if (particle.isResting && Math.random() * Simulation.TAU < Simulation.RESTING_SWITCH_PROBABILITY)
				particle.isResting = false; // Switch to diffusion from rest
			else if (!particle.isResting && Math.random() * Simulation.TAU < Simulation.DIFFUSING_SWITCH_PROBABILITY)
				particle.isResting = true; // Switch to rest from diffusion
			
			
			if (particle.isBinded || particle.isResting)
				continue;
			particle.move();
			
			// Ensure that particles do not escape the cell, by checking if the magnitude of its position is greater than the cell radius minus its radius (it's touching the edge of the cell)
			if ((magnitude = particle.x * particle.x + particle.y * particle.y + particle.z * particle.z) > (cellRadius - particle.radius) * (cellRadius - particle.radius)) {
				// Reflect back into the domain
				magnitude = Math.sqrt(magnitude);
				reflectingMagnitude = ((cellRadius - particle.radius) - magnitude)/magnitude;
				particle.x += particle.x * reflectingMagnitude;
				particle.y += particle.y * reflectingMagnitude;
				particle.z += particle.z * reflectingMagnitude;
			}
		}
		
		// Check for collisions between particles and targets
		// We only consider target-particles, not particle-particle or target-target
		double dx, dy, dz, distanceMagnitude, difference;
		for (int t = 0, p = 0; t < targets.length; t += 1) {
			target = targets[t];
			for (p = 0; p < particles.length; p += 1) {
				particle = particles[p];
				if (particle.isBinded)
					continue;
				
				dx = particle.x - target.x;
				dy = particle.y - target.y;
				dz = particle.z - target.z;
				
				if ((distanceMagnitude = dx * dx + dy * dy + dz * dz) < (particle.radius + target.radius) * (particle.radius + target.radius)) {
					// Collision
					// Push the particle so that it's back on the edge of the target
					distanceMagnitude = Math.sqrt(distanceMagnitude);
					dx /= distanceMagnitude;
					dy /= distanceMagnitude;
					dz /= distanceMagnitude;
					
					// A little bit further than the immediate edge to avoid issues
					difference = (particle.radius + target.radius - distanceMagnitude) + target.radius * 0.01;
					particle.x += dx * difference;
					particle.y += dy * difference;
					particle.z += dz * difference;
					System.out.println("Collison occurred.");
				}
			}
		}
	}
}
