package main;

public class TubeTarget extends Particle {
	public double endX;
	public double endY;
	public double endZ;
	/** Creates a cylindrical target. This is a segment of a cylinder that has its base at its initial position and the top lies at its endpoint. 
	 * @param vector An array of 3 numbers specifying its initial position
	 * @param radius The radius of the cylinder
	 * @param vectorEndpoint This is the location of the top of the cylinder. The vector between this and its position is a line that travels directly through the center of the cylinder, so this also decides the orientation of the cylinder.
	 */
	public TubeTarget(double[] vector, double radius, double[] vectorEndpoint) {
		// This runs the constructor in the SphereTarget file which runs the one in the Particle file. It only initializes the value and is done to save code.
		super(vector, radius);
		
		this.endX = vectorEndpoint[0];
		this.endY = vectorEndpoint[1];
		this.endZ = vectorEndpoint[2];
	}
}
