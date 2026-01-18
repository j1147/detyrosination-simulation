#include "ParticleSimulator.h"
#include "Simulation.h"
#include "Particle.h"
#include "Target.h"
#include "Vector.h"
#include <string>
#include <numbers>
#include <vector>
#include <iostream>
#include <chrono>
#include <cmath>

void Simulation::getTargetsFrom(const std::vector<std::vector<double>>& positions, double radius)
{
	for (const std::vector<double>& pos : positions)
	{
		targets.push_back(new Target(pos, radius));
		targets.back()->index = livingTargets++;
	}
}

void Simulation::createParticlesRandomly(size_t particleCount, double buffer)
{
	for (livingParticles = 0; livingParticles < particleCount; ++livingParticles)
	{
		// Ensure particles do not start inside targets
		Vector position = Vector::getUniformVector(0, params.cell_radius - buffer);

		int attempts = 100;
		outer: while (attempts > 0)
		{
			for (Target* target : targets)
				if ((position - Vector(target->x, target->y, target->z)).norm() < target->radius + buffer)
				{
					// Particle is colliding, retry
					position = Vector::getUniformVector(0, params.cell_radius - buffer);
					--attempts;
					goto outer;
				}
			goto success;
		}
		std::cout
			<< "Failed to spawn a particle that wasn't inside "
			<< targets.size()
			<< " targets (particle #"
			<< livingParticles
			<< ")\n";
	success:
		particles.push_back(new Particle(position, params.particle_radius));
	}
}

void Simulation::collision(Particle* particle, const Target* target)
{
	times.push_back(ticks);
}

void Simulation::removeParticle(size_t index)
{
	//std::cout << "removed particle " << livingParticles << '\n';
	deleteList.push_back(particles[index]);

	particles[index] = particles[--livingParticles];
	particles[livingParticles] = nullptr;

	// Log % done based on how many particles are left
	const size_t resolution = particles.size() / 10 == 0 ?
		1 : particles.size() / 10;

	if (livingParticles % resolution == 0)
	{
		if (!prefix.empty())
			std::cout << "[" << prefix << "] ";
		printf("%f%% done\n", 100.0 - livingParticles * 100.0 / particles.size());
	}
}

void Simulation::init()
{
	// Cache formulas for speed
	boundary_adsorption_probability = fmin(
		1.0,
		params.reactivity * sqrt(std::numbers::pi) / sqrt(params.diffusion_coefficient) * sqrt(params.tau)
	);
	mean_step_length = sqrt(2 * params.diffusion_coefficient * params.tau);

	if (livingParticles == 0)
		createParticlesRandomly(params.particle_count, params.target_buffer);

	if (params.cover_targets)
		for (Particle* particle : particles)
			particle->hitList = new bool[livingTargets](); // False-initialized

	// Speedup collision checking by using 3D spatial hashing
	for (Target* target : targets)
	{
		double x = target->x + params.cell_radius,
			y = target->y + params.cell_radius,
			z = target->z + params.cell_radius;

		int minX = (int) ((x - target->radius) / (2 * params.cell_radius) * 4),
			minY = (int) ((y - target->radius) / (2 * params.cell_radius) * 4),
			minZ = (int) ((z - target->radius) / (2 * params.cell_radius) * 4),

			maxX = (int) ((x + target->radius) / (2 * params.cell_radius) * 4),
			maxY = (int) ((y + target->radius) / (2 * params.cell_radius) * 4),
			maxZ = (int) ((z + target->radius) / (2 * params.cell_radius) * 4);

		// Grid is 4x4x4
		minX = minX < 0 ? 0 : minX > 3 ? 3 : minX;
		minY = minY < 0 ? 0 : minY > 3 ? 3 : minY;
		minZ = minZ < 0 ? 0 : minZ > 3 ? 3 : minZ;

		maxX = maxX < 0 ? 0 : maxX > 3 ? 3 : maxX;
		maxY = maxY < 0 ? 0 : maxY > 3 ? 3 : maxY;
		maxZ = maxZ < 0 ? 0 : maxZ > 3 ? 3 : maxZ;

		target->minX = minX;
		target->minY = minY;
		target->minZ = minZ;
		target->maxX = maxX;
		target->maxY = maxY;
		target->maxZ = maxZ;

		for (int cx = minX; cx <= maxX; ++cx)
			for (int cy = minY; cy <= maxY; ++cy)
				for (int cz = minZ; cz <= maxZ; ++cz)
					collisionCells[cx + cy * 4 + cz * 16].push_back(target);
	}
}

void Simulation::log(size_t index, size_t total) const
{
	if (!prefix.empty())
		std::cout << "[" << prefix << "] ";

	std::cout << "Starting simulation with parameters: (" << index << "/" << total << ")\n";
	std::cout
		<< "Number of targets:     " << targets.size() << "\n"
		<< "Number of particles:   " << particles.size() << "\n"
		<< "Cell radius:           " << params.cell_radius << "\n"
		<< "Target radius:         " << targets[0]->radius << "\n"
		<< "Particle radius:       " << params.particle_radius << "\n"
		<< "Diffusion coefficient: " << params.diffusion_coefficient << "\n"
		<< "Time constant:         " << params.tau << "\n"
		<< "Boundary reactivity:   " << params.reactivity << "\n"
		<< "Adsorption chance:     " << boundary_adsorption_probability << "\n";
}

double Simulation::start()
{
	lastTPSCheck = beginning = std::chrono::steady_clock::now();

	for (; livingParticles > 0; ++ticks)
	{
		tick();
		// Prevent overuse of the same random values
		if (ticks % 100 == 99)
			quickShuffleGaussian();
		if (ticks % 1000 == 999)
			shuffleGaussian();
	}

	double sum = 0;
	std::cout << "Recorded survival times:";
	for (long long time : times)
	{
		std::cout << " " << time * params.tau << "s";
		sum += time;
	}
	sum *= params.tau;
	std::cout << '\n';


	long long duration = (std::chrono::steady_clock::now() - beginning).count();
	
	std::cout << "Simulation ended at " << ticks << " ticks\n";

	std::cout << "Completed simulation in "
		<< duration / 1'000'000 << "ms"
		<< " (" << (duration / 1'000'000) / 1000.0 << "s)\n";

	std::cout << "** First cover time: " << sum / times.size() << '\n';

	return sum / times.size();
}

void Simulation::tick()
{
	std::chrono::steady_clock::time_point duration = std::chrono::steady_clock::now();

	if ((duration - lastTPSCheck).count() > 10'000'000'000LL)
	{
		// Used for debugging on the cluster
		if (!prefix.empty())
			std::cout << "[" << prefix << "] ";
		std::cout
			<< "TPS: " << (ticks - lastTPSCheckTick)/10.0
			<< " (@ " << ticks * params.tau << "s)" << '\n';
		lastTPSCheckTick = ticks;
		lastTPSCheck = duration;
	}

	const double radius = params.cell_radius;

	//checkUnreportedCollisions(this);

	for (size_t p = 0; p < livingParticles; ++p)
	{
		Particle* particle = particles[p];
		// Diffusion
		particle->x += particle->dx = fastGaussian() * mean_step_length;
		particle->y += particle->dy = fastGaussian() * mean_step_length;
		particle->z += particle->dz = fastGaussian() * mean_step_length;

		// Reflection on boundary
		if (particle->x * particle->x + particle->y * particle->y + particle->z * particle->z > radius * radius)
		{
			particle->x -= particle->dx;
			particle->y -= particle->dy;
			particle->z -= particle->dz;
		}
	}

	const bool coverTargets = params.cover_targets;

	particle_loop: for (size_t p = 0; p < livingParticles; ++p)
	{
		Particle* particle = particles[p];

		// Collision checking
		const int x = (int) ((particle->x + radius) / (2 * radius) * 4),
			y = (int) ((particle->y + radius) / (2 * radius) * 4),
			z = (int) ((particle->z + radius) / (2 * radius) * 4);

		for (const Target* target : collisionCells[x + y * 4 + z * 16])
		{
			double dx = particle->x - target->x,
				dy = particle->y - target->y,
				dz = particle->z - target->z;

			double distance = dx * dx + dy * dy + dz * dz;

			if (distance < target->radius * target->radius)
			{
				// Particle collided with target
				if (uniformDistribution(rngGenerator) > boundary_adsorption_probability)
				{
					// Particle was deflected
					distance = sqrt(distance);
					particle->x += dx * 2 * (target->radius - distance) / distance;
					particle->y += dy * 2 * (target->radius - distance) / distance;
					particle->z += dz * 2 * (target->radius - distance) / distance;
				}
				else if (coverTargets && particle->hitTargets < livingTargets)
				{
					// Particle was adsorbed
					if (!particle->hitList[target->index])
					{
						particle->hitList[target->index] = true;
						++particle->hitTargets;

						// See that some progress is being made when the simulation is taking too long (2+ days)
						if (p == 0)
							std::cout << '[' << p << "] Hit new target (" << particle->hitTargets << '/' << livingTargets << ")\n";
						if (particle->hitTargets == livingTargets)
						{
							std::cout << "Removing particle\n";
							collision(particle, target);
							removeParticle(p--);
							goto particle_loop;
						}
					}
					// No time is spent inside the target, leave it at a known distance
					distance = sqrt(distance);
					particle->x += dx * (target->radius - distance + params.ejection_distance) / distance;
					particle->y += dy * (target->radius - distance + params.ejection_distance) / distance;
					particle->z += dz * (target->radius - distance + params.ejection_distance) / distance;
				}
				else
				{
					collision(particle, target);
					removeParticle(p--);
					goto particle_loop;
				}
			}
		}
	}
}

Simulation::~Simulation()
{
	for (Target*& t : targets)
		delete t;

	/*for (Particle*& p : particles)
		delete p;*/

	for (Particle*& p : deleteList)
		delete p;
}