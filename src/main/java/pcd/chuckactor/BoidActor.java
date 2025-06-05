package pcd.chuckactor;

import akka.actor.AbstractActor;
import akka.actor.Props;
import pcd.chuckactor.BoidProtocol.*;

import java.util.List;

public class BoidActor extends AbstractActor {
    private BoidsModel model;
    private List<Boid> chunk;

    public BoidActor(List<Boid> chunk, BoidsModel model) {
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
        return Props.create(BoidActor.class, () -> new BoidActor(chunk, model));
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
        for(Boid boid : chunk) {
            boid.update(model);
        }
        getSender().tell(new UpdatedBoid(chunk), getSelf());
    }
}