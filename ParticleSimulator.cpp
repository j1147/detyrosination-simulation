#include "ParticleSimulator.h"
#include "SimulationMeta.h"
#include "Simulation.h"
#include <iostream>
#include <vector>
#include <random>
#include <chrono>
#include <fstream>

std::mt19937 rngGenerator;
std::uniform_real_distribution<double> uniformDistribution;
std::normal_distribution<double> normalDistribution;
long shuffleOffset = 0;

constexpr size_t gaussians = 249'989;

double cachedGaussians[gaussians];
size_t gaussianIndex = 0;

// Get target positions from file
static std::vector<std::vector<double>> getTargetPositions(std::string path)
{
	FILE* stream = fopen(path.c_str(), "r");
	const size_t line_size = 64;

	char line[line_size]{};
	char* last = line;

	size_t targets = 0;
	std::vector<std::vector<double>> positions{};

	if (fgets(line, line_size, stream))
		targets = strtol(last, &last, 10);

	if (targets <= 0)
	{
		std::cout << "Failed to read number of targets";
		return positions;
	}

	// Every line contains X,Y,Z
	while (positions.size() < targets && fgets(line, line_size, stream))
	{
		last = line;
		double x = strtod(last, &last);
		++last;
		double y = strtod(last, &last);
		++last;
		double z = strtod(last, &last);

		std::vector<double> position = { x, y, z };
		positions.push_back(position);
	}

	return positions;
}

int main(int argcount, char* args[])
{
	std::string prefix{};
	size_t particle_count = 0;

	if (argcount >= 2)
	{
		std::cout << "Detected running in SLURM\n";
		// Used for debugging in the cluster
		std::cout << "Prefix: " << (prefix = args[1]) << '\n';
		if (argcount >= 3)
			std::cout << "Overriding particle count to " << (particle_count = std::stoi(args[2])) << '\n';
	}
	else
		std::cout << "Not running in SLURM\n";

	rngGenerator.seed(
		(unsigned int) std::chrono::system_clock::now().time_since_epoch().count()
	);
	
	// Pregenerate a large list of random values for speed
	uniformDistribution = std::uniform_real_distribution<double>(0.0, 1.0);
	normalDistribution = std::normal_distribution<double>(0.0, 1.0);

	for (size_t i = 0; i < gaussians; ++i)
		cachedGaussians[i] = normalDistribution(rngGenerator);

	
	std::vector<std::vector<double>> positions = getTargetPositions("targets.txt");

	// Changing variable for this run: reactivity
	double reactivities[28]{};
	size_t index = 2; // skipping super low values because it takes too long
	for (double& entry : reactivities)
		entry = pow(10, index++/29.0 * 4.5 - 1.5);

	/*for (const double entry : reactivities)
		std::cout << entry << '\n';*/

	index = 0;
	std::ofstream csv(prefix + "_CoverTime_01_13_26.txt");

	for (double reactivity : reactivities)
	{
		struct SimulationMeta meta = {
			.cell_radius = 1,
			.particle_radius = 0,
			.target_buffer = 0.01,
			.diffusion_coefficient = 1,
			.tau = 1e-6,
			.ejection_distance = 0.01,
			.reactivity = reactivity,
			.cover_targets = true,
			.particle_count = particle_count <= 0 ? 200 : particle_count
		};

		// Generate targets
		const double target_radius = 0.05;
		Simulation simulator(meta, prefix);
		simulator.getTargetsFrom(positions, target_radius);

		// Begin simulation
		simulator.init();
		simulator.log(index + 1, sizeof(reactivities)/sizeof(double));

		// Record results
		double coverTime = simulator.start();

		csv
			<< meta.reactivity << ','
			<< coverTime << '\n';

		++index;
	}

	csv.close();

	return 0;
}

double fastGaussian()
{
	return cachedGaussians[gaussianIndex = (gaussianIndex + 1) % gaussians];
}

// Resolve issues with randomicity by periodically regenerating some values
void shuffleGaussian()
{
	shuffleOffset++;
	for (size_t i = (shuffleOffset % 10) * 10; i < gaussians - 100; i += 100)
	{
		cachedGaussians[i] = normalDistribution(rngGenerator);
		cachedGaussians[i + 1] = normalDistribution(rngGenerator);
		cachedGaussians[i + 2] = normalDistribution(rngGenerator);
		cachedGaussians[i + 3] = normalDistribution(rngGenerator);
		cachedGaussians[i + 4] = normalDistribution(rngGenerator);
		cachedGaussians[i + 5] = normalDistribution(rngGenerator);
		cachedGaussians[i + 6] = normalDistribution(rngGenerator);
		cachedGaussians[i + 7] = normalDistribution(rngGenerator);
		cachedGaussians[i + 8] = normalDistribution(rngGenerator);
		cachedGaussians[i + 9] = normalDistribution(rngGenerator);
	}
}

// Resolve issues with randomicity by periodically changing the order of some values
void quickShuffleGaussian()
{
	double swap;
	for (size_t i = (size_t)(uniformDistribution(rngGenerator) * 32); i < gaussians - 32; i += 32)
	{
		swap = cachedGaussians[i + 0];
		cachedGaussians[i + 0] = cachedGaussians[i + 31];
		cachedGaussians[i + 31] = swap;

		swap = cachedGaussians[i + 7];
		cachedGaussians[i + 7] = cachedGaussians[i + 23];
		cachedGaussians[i + 23] = swap;

		cachedGaussians[i + 15] = -cachedGaussians[i + 15];
	}
}
