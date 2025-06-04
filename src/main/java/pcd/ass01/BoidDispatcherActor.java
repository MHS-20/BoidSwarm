package pcd.ass01;

import akka.actor.*;
import pcd.ass01.BoidProtocol.*;

import java.util.List;

public class BoidDispatcherActor extends AbstractActor {
    private final List<ActorRef> assignedBoids;

    public BoidDispatcherActor(List<ActorRef> assignedBoids) {
        this.assignedBoids = assignedBoids;
    }

    public static Props props(List<ActorRef> assignedBoids) {
        return Props.create(BoidDispatcherActor.class, () -> new BoidDispatcherActor(assignedBoids));
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(StartUpdate.class, msg -> {
                    for (ActorRef boid : assignedBoids) {
                        boid.tell(msg, sender());
                    }
                })
                .match(ResetSimulation.class, msg -> {
                    for (ActorRef boid : assignedBoids) {
                        boid.tell(PoisonPill.getInstance(), self());
                    }
                })
                .build();
    }
}