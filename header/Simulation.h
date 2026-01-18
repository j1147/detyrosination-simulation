#pragma once
#include "Particle.h"
#include "Target.h"
#include "SimulationMeta.h"
#include <vector>
#include <cmath>
#include <chrono>
#include <string>

class Simulation
{
public:
	std::vector<Particle*> particles;
	size_t livingParticles;
	std::vector<Target*> targets;
	size_t livingTargets;

	std::vector<Particle*> deleteList;

	struct SimulationMeta params;

	double boundary_adsorption_probability;
	double mean_step_length;

	long long ticks, lastTPSCheckTick;

	std::chrono::steady_clock::time_point beginning, lastTPSCheck;

	std::vector<const Target*> collisionCells[64];

	std::vector<long long> times;

	const std::string prefix;

	Simulation(const struct SimulationMeta& params) :
		livingParticles(0),
		livingTargets(0),
		params(params),
		boundary_adsorption_probability(0),
		mean_step_length(0),
		ticks(0),
		lastTPSCheckTick(0)
	{};

	Simulation(const struct SimulationMeta& params, const std::string& prefix) :
		livingParticles(0),
		livingTargets(0),
		params(params),
		boundary_adsorption_probability(0),
		mean_step_length(0),
		ticks(0),
		lastTPSCheckTick(0),
		prefix(prefix)
	{};

	void getTargetsFrom(const std::vector<std::vector<double>>& positions, double radius);
	void createParticlesRandomly(size_t particleCount, double buffer);
	void init();

	void collision(Particle* particle, const Target* target);
	void removeParticle(size_t index);

	void log(size_t index, size_t total) const;
	double start();
	void tick();
	
	~Simulation();
};