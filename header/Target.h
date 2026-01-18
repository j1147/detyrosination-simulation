#pragma once
#include <vector>

class Target
{
public:
	double x, y, z, radius;

	size_t index;

	int minX, minY, minZ,
		maxX, maxY, maxZ;

	Target(const std::vector<double>& position, double radius) :
		x(position[0]),
		y(position[1]),
		z(position[2]),
		radius(radius),
		index(0),

		minX(0), minY(0), minZ(0),
		maxX(0), maxY(0), maxZ(0)
	{};
};