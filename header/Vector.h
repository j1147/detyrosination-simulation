#pragma once
#include "ParticleSimulator.h"
#include <cmath>

class Vector
{
public:
	double x, y, z;

	Vector() : x(0), y(0), z(0) {};
	Vector(double x, double y, double z) : x(x), y(y), z(z) {};

	double norm() const
	{
		return sqrt(x * x + y * y + z * z);
	}

	double selfDot() const
	{
		return x * x + y * y + z * z;
	}

	void multiply(double scalar)
	{
		x *= scalar;
		y *= scalar;
		z *= scalar;
	}

	Vector operator+(const Vector& other)
	{
		return Vector(x + other.x, y + other.y, z + other.z);
	}

	Vector operator-(const Vector& other)
	{
		return Vector(x - other.x, y - other.y, z - other.z);
	}

	// Creates a vector from a uniform distribution between two spheres of radii minLength & maxLength
	static Vector getUniformVector(double minLength, double maxLength)
	{
		Vector result{
			uniformDistribution(rngGenerator) - 0.5,
			uniformDistribution(rngGenerator) - 0.5,
			uniformDistribution(rngGenerator) - 0.5
		};
		result.multiply(1/result.norm());

		// Ensures uniformity in 3D
		double ratio = minLength / maxLength;
		ratio = fmin(1, fmax(0, ratio));
		ratio = ratio * ratio * ratio;

		double radius = maxLength * cbrt(
			uniformDistribution(rngGenerator) * (1 - ratio) + ratio
		);

		result.multiply(radius);

		return result;
	}
};