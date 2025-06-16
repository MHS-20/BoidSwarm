package pcd.boidActor;

import akka.actor.AbstractActor;
import akka.actor.Props;
import pcd.boidActor.BoidProtocol.*;

public class BoidActor extends AbstractActor {
    private Boid boid;
    private BoidsModel model;

    public BoidActor(Boid boid, BoidsModel model) {
        this.boid = new Boid(boid.getPos(), boid.getVel());
        // this.boid = boid;

        // this.model = model;
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
        boid.calculateVelocity(model);
        getSender().tell(new VelocityCalculated(), getSelf());
    }

    public void onUpdateBoid(UpdateBoid msg) {
        boid.updateVelocity(model);
        boid.updatePosition(model);
        // getSender().tell(new BoidUpdated(boid), getSelf());
        getSender().tell(new BoidUpdated(new Boid(new P2d(boid.getPos().x(), boid.getPos().y()),
                new V2d(boid.getVel().x(), boid.getVel().y()))), getSelf());

    }
}