#pragma once
#include "Vector.h"
#include <vector>

class Particle
{
public:
	double x, y, z, radius;
	size_t index, hitTargets;
	bool* hitList;
	double dx, dy, dz;

	Particle(const std::vector<double>& position, double radius) :
		x(position[0]),
		y(position[1]),
		z(position[2]),
		radius(radius),
		index(0),
		hitTargets(0),
		hitList(nullptr),
		dx(0),
		dy(0),
		dz(0)
	{};

	Particle(const Vector& position, double radius) :
		x(position.x),
		y(position.y),
		z(position.z),
		radius(radius),
		index(0),
		hitTargets(0),
		hitList(nullptr),
		dx(0),
		dy(0),
		dz(0)
	{};
};