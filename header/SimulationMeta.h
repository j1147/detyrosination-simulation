#pragma once

struct SimulationMeta
{
	double cell_radius,
		particle_radius,
		target_buffer,
		diffusion_coefficient,
		tau,
		ejection_distance,
		reactivity;
	bool cover_targets;
	size_t particle_count;
};