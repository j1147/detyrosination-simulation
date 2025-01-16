package main;

public class Particle {
	public double x;
	public double y;
	public double z;
	public double radius;
	
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
	
	public void move() {
		
	}
}
