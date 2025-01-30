package main;

public class Particle {
	public double x;
	public double y;
	public double z;
	public double radius;
	
	/** The resting state. If true, the particle does not move, otherwise it is pushed around by Brownian motion */
	public boolean isResting;
	
	/** Initialize a new particle/target. Vector is a 3 element array of numbers that defines its starting position, and radius is a number that either defines its spherical radius or its cylindrical radius, depending on the type of particle/target
	 * @param vector
	 * @param radius
	 */
	public Particle(double[] vector, double radius) {
		this.x = vector[0];
		this.y = vector[1];
		this.z = vector[2];
		this.radius = radius;
	}
	/** Move uniformly in every **axial** direction */
	public void move(double diffusion_constant, double tau) {
		double magnitude = Math.sqrt(2 * diffusion_constant * tau);
		x += Simulation.gaussian() * magnitude;
		y += Simulation.gaussian() * magnitude;
		z += Simulation.gaussian() * magnitude;
	}
	/** Move uniformly in a true circle. Currently only used for testing */
	public void moveUniformly(double diffusion_constant, double tau) {
		double magnitude = Simulation.gaussian() * Math.sqrt(2 * diffusion_constant * tau);
		double theta = Math.random() * Math.PI * 2;
		double phi = Math.random() * Math.PI;
		
		x += magnitude * Math.sin(phi) * Math.cos(theta);
		y += magnitude * Math.sin(phi) * Math.sin(theta);
		z += magnitude * Math.cos(phi);
	}
}
