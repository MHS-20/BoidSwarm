package pcd.boidActor;

import java.util.List;

public interface BoidProtocol {
    // Simulation Workflow
    public static record CalculateVelocity(List<Boid> boids) {}
    public static record VelocityCalculated() {}

    public static record UpdateBoid () {}
    public static record BoidUpdated(Boid updatedBoid) {}

    // Simulation Control
    public static record BootSimulation(BoidsModel model) {}
    public static record ContinueSimulation () {}
    public static record StartSimulation () {}
    public static record StopSimulation () {}
    public static record ResetSimulation(List<Boid> boids) {}

    // Model weights
    public static record SetSeparationWeight(double weight) {}
    public static record SetAlignmentWeight(double weight) {}
    public static record SetCohesionWeight(double weight) {}
}