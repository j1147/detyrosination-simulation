package main;

import java.util.Random;

public class Simulation {
	public Particle[] particles;
	public SphereTarget[] targets;
	
	public int particleCount, targetCount;
	public long maximumSteps, stepTime, ticks;
	
	public Statistics stats;
	
	public boolean waitUntilDone;
	public boolean STOP;
	
	public double cellRadius, targetRadius, particleRadius, diffuse_starting_probability;
	
	public double diffusion_constant, tau, rest_switch_chance, diffusive_switch_chance;
	
	public double time;
	
	public static Random random = new Random();
	public static double gaussian() {
		return random.nextGaussian();
	}
	
	public Simulation(double cellRadius, double targetRadius, double diffusion_constant, double tau) {
		this.particles = new Particle[particleCount = Main.PARTICLE_COUNT];
		this.targets = new SphereTarget[targetCount = Main.TARGET_COUNT];
		
		this.maximumSteps = Main.MAXIMUM_STEPS;
		this.stepTime = Main.STEP_TIME;
		this.waitUntilDone = Main.WAIT_UNTIL_DONE;
		
		this.cellRadius = cellRadius;
		this.targetRadius = targetRadius;
		particleRadius = 0.0;
		
		diffuse_starting_probability = 1.0;
		rest_switch_chance = 0;
		diffusive_switch_chance = 0;
		
		this.diffusion_constant = diffusion_constant;
		
		this.tau = tau;
		
		stats = new Statistics(particleCount, this);
	}
	
	public void start() {
		// Used to schedule the loop
		long lastTickTime = System.nanoTime();
		// The current system time, in nanoseconds
		long now = System.nanoTime();
		ticks = 0;
		time = 0;
		
		if (Main.VERBOSE_LOGGING)
			System.out.println("Starting simulation with parameters:\n"
					+ "Cell radius:           " + cellRadius + "\n"
					+ "Target radius:         " + targetRadius + "\n"
					+ "Particle radius:       " + particleRadius + "\n"
					+ "Diffuse starting %:    " + diffuse_starting_probability * 100 + "%\n"
					+ "Diffusion coefficient: " + diffusion_constant + "\n"
					+ "Time constant:         " + tau + "\n");
		else
			System.out.println("Starting simulation.");

		// Generate particles in random locations
		// Only create the defined amount of particles, see the constructor for the number of particles
		double magnitude, theta, phi;
		double vector[];
		for (int pindex = 0; pindex < particleCount; pindex += 1) {
			// Create a vector of random magnitude that is less than the cell radius in polar coordinates (and is also not touching it), and then convert it to Cartesian coordinates
			// Also do not touch the center targets within 1 step
			magnitude = Math.random() * (cellRadius - 2 * particleRadius - targetRadius - Math.sqrt(tau * diffusion_constant)) + targetRadius + particleRadius + Math.sqrt(tau * diffusion_constant);
			theta = Math.random() * Math.PI * 2;
			phi = Math.random() * Math.PI;
			vector = new double[] { magnitude * Math.sin(phi) * Math.cos(theta), magnitude * Math.sin(phi) * Math.sin(theta), magnitude * Math.cos(phi) };
			
			particles[pindex] = new Particle(vector, particleRadius);
			particles[pindex].isResting = !(Math.random() < diffuse_starting_probability);
		}
		
		
		if (targetCount == 1)
			targets[0] = new SphereTarget(new double[] {0, 0, 0}, targetRadius);
		
		
		// Main loop
		long then = System.nanoTime(); // Performance profiling
		long interval = maximumSteps / 10;
		
		if (waitUntilDone) {
			long initialParticles = particleCount;
			long lastLogged = particleCount;
			interval = particleCount / 10;
			while (particleCount > 0 && !STOP) {
				if ((now = System.nanoTime()) - lastTickTime >= stepTime) {
					lastTickTime = now;
					// Avoid duplicating logs because particles do not disappear every tick
					if ((particleCount * 10)/initialParticles != lastLogged) {
						lastLogged = (particleCount * 10)/initialParticles;
						System.out.println(100.0 - particleCount * 100.0/initialParticles + "% done");
					}
					
					tick();
					++ticks;
					time += tau;
				}
			}
		} else	
			while (ticks < maximumSteps && !STOP) {
				// Set now to the current system time, and then compare its difference with the last tick time to the step time.
				if ((now = System.nanoTime()) - lastTickTime >= stepTime) {
					lastTickTime = now;
					
					if (ticks % interval == 0)
						System.out.println(ticks/(maximumSteps * 1.0) * 100 + "% done");
					
					tick();
					++ticks;
					time += tau;
				}
			}
		long duration = (System.nanoTime() - then)/1_000_000;
		System.out.println("Completed simulation in " + duration + "ms (" + duration/1000.0 + "s) (" + ticks + " ticks)\n");
		
		stats.compute();
	}
	
	
	public void removeParticle(int index) {
		particles[index] = particles[--particleCount];
		if (particleCount == 0 && !waitUntilDone) {
			System.out.println("Simulation ended early at " + ticks + " ticks");
			STOP = true;
		}
	}
	public void collision(Particle particle, SphereTarget target) {
		//stats.pushTime(ticks * tau);
		stats.pushTime(time);
		//System.out.println("Pushing time " + ticks * tau + " at tick " + ticks);
		//System.out.println("Collison occurred. " + particleCount + " " + ticks);
	}
	
	public void tick() {
		Particle particle;
		SphereTarget target;
		
		double magnitude, reflectingMagnitude;
		for (int p = 0; p < particleCount; ++p) {
			particle = particles[p];
			
			if (particle.isResting)
				continue;
			//particle.moveUniformly(diffusion_constant, tau);
			particle.move(diffusion_constant, tau);
			
			// Ensure that particles do not escape the cell, by checking if the magnitude of its position is greater than the cell radius minus its radius (it's touching the edge of the cell)
			if ((magnitude = particle.x * particle.x + particle.y * particle.y + particle.z * particle.z) > (cellRadius - particle.radius) * (cellRadius - particle.radius)) {
				// Reflect back into the domain
				magnitude = Math.sqrt(magnitude);
				reflectingMagnitude = 2 * ((cellRadius - particle.radius) - magnitude)/magnitude;
				particle.x += particle.x * reflectingMagnitude;
				particle.y += particle.y * reflectingMagnitude;
				particle.z += particle.z * reflectingMagnitude;
			}
		}
		
		
		// Check for collisions between particles and targets
		// We only consider target-particles, not particle-particle or target-target
		double dx, dy, dz, distanceMagnitude, difference;
		for (int t = 0, p = 0; t < targetCount; ++t) {
			target = targets[t];
			for (p = 0; p < particleCount; ++p) {
				particle = particles[p];
				
				dx = particle.x - target.x;
				dy = particle.y - target.y;
				dz = particle.z - target.z;
				
				if ((distanceMagnitude = dx * dx + dy * dy + dz * dz) < (particle.radius + target.radius) * (particle.radius + target.radius)) {
					// Collision
					collision(particle, target);
					removeParticle(p--);
				}
			}
		}
	}
	/** Take a list of values and separate them by a commma, and end it with a new line */
	public String doublesToCSV(double... values) {
		String out = "";
		for (int d = 0; d < values.length; ++d)
			out += values[d] + ",";
		if (values.length > 0)
			out = out.substring(0, out.length() - 1);
		return out + "\n";
	}
	/** Export this simulation data as CSV */
	public String toCSV(int SIMULATION_TYPE) {
		switch (SIMULATION_TYPE) {
			case 0:
				return doublesToCSV(tau, stats.averageTime, stats.lowerQuartileTime, stats.upperQuartileTime, stats.predictedTime);
			case 1:
				return doublesToCSV(diffusion_constant, stats.averageTime, stats.lowerQuartileTime, stats.upperQuartileTime, stats.predictedTime);
			case 2:
				double interval = (stats.maximumTime - stats.minimumTime)/(Main.PARTICLE_COUNT/30);
				long[] bucketted = stats.bucket(interval);
				String out = "";
				for (int b = 0; b < bucketted.length; ++b)
					out += (b * interval) + "," + bucketted[b] + "\n";
				return out;
			default:
				return "";
		}
	}
}
