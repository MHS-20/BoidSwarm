package pcd.workerActor;

import akka.actor.AbstractActor;
import akka.actor.Props;
import pcd.workerActor.BoidProtocol.*;

import java.util.List;

public class WorkerActor extends AbstractActor {
    private BoidsModel model;
    private List<Boid> chunk;

    public WorkerActor(List<Boid> chunk, BoidsModel model) {
        //this.chunk = chunk;
        this.chunk = chunk;

        this.model = new BoidsModel(model.getBoids().size(),
                model.getSeparationWeight(),
                model.getAlignmentWeight(),
                model.getCohesionWeight(),
                model.getWidth(),
                model.getHeight(),
                model.getMaxSpeed(),
                model.getPerceptionRadius(),
                model.getAvoidRadius());
    }

    public static Props props(List<Boid> chunk, BoidsModel model) {
        return Props.create(WorkerActor.class, () -> new WorkerActor(chunk, model));
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(CalculateVelocity.class, this::onCalculateVelocity)
                .match(UpdateBoid.class, this::onUpdateBoid)
                .match(SetSeparationWeight.class, msg -> {
                    model.setSeparationWeight(msg.weight());
                })
                .match(SetAlignmentWeight.class, msg -> {
                    model.setAlignmentWeight(msg.weight());
                })
                .match(SetCohesionWeight.class, msg -> {
                    model.setCohesionWeight(msg.weight());
                })
                .build();
    }

    public void onCalculateVelocity(CalculateVelocity msg) {
        model.setBoids(msg.boids());
        for (Boid boid : chunk) {
            boid.calculateVelocity(model);
        }
        getSender().tell(new VelocityCalculated(), getSelf());
    }

    public void onUpdateBoid(UpdateBoid msg) {
        for (Boid boid : chunk) {
            boid.updateVelocity(model);
            boid.updatePosition(model);
        }
        getSender().tell(new BoidUpdated(chunk), getSelf());
    }
}