#pragma once
#include <random>
#include <chrono>
extern std::mt19937 rngGenerator;
extern std::uniform_real_distribution<double> uniformDistribution;
extern std::normal_distribution<double> normalDistribution;
extern long shuffleOffset;

double fastGaussian();
void shuffleGaussian();
void quickShuffleGaussian();