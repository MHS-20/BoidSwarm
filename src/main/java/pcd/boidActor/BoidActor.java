package pcd.boidActor;

import akka.actor.AbstractActor;
import akka.actor.Props;
import pcd.boidActor.BoidProtocol.*;

public class BoidActor extends AbstractActor {
    private Boid boid;
    private BoidsModel model;

    public BoidActor(Boid boid, BoidsModel model) {
        this.boid = new Boid(boid.getPos(), boid.getVel());
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

    public static Props props(Boid boid, BoidsModel model) {
        return Props.create(BoidActor.class, () -> new BoidActor(boid, model));
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(StartUpdate.class, this::onStartUpdate)
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

    public void onStartUpdate(StartUpdate msg) {
        model.setBoids(msg.boids());
        boid.update(model);
        getSender().tell(new UpdatedBoid(new Boid(boid.getPos(), boid.getVel())), getSelf());
    }
}