package pcd.boidActor;

import akka.actor.*;
import pcd.boidActor.BoidProtocol.*;

import java.util.ArrayList;
import java.util.List;

public class BoidsManagerActor extends AbstractActorWithStash {

    private long t0;
    private int framerate;
    private static final int FRAMERATE = 60;

    private BoidsView view;
    private BoidsModel model;

    private int nBoids;
    private List<Boid> updatedBoids;

    private int count = 0;
    private List<ActorRef> boidActors;

    public BoidsManagerActor(BoidsModel model, int nBoids, BoidsView view) {
        this.model = model;
        this.nBoids = nBoids;
        this.view = view;
        this.updatedBoids = new ArrayList<>();
        this.boidActors = new ArrayList<>();
    }

    public static Props props(BoidsModel model, int nBoids, BoidsView view) {
        return Props.create(BoidsManagerActor.class, () -> new BoidsManagerActor(model, nBoids, view));
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(BootSimulation.class, this::onBootSimulation)
                .match(StartSimulation.class, this::onStartSimulation)
                .match(ContinueSimulation.class, msg -> this.stash())
                .match(StopSimulation.class, msg -> this.stash())
                .match(ResetSimulation.class, msg -> this.stash())
                .match(SetSeparationWeight.class, msg -> this.stash())
                .match(SetAlignmentWeight.class, msg -> this.stash())
                .match(SetCohesionWeight.class, msg -> this.stash())
                .match(BoidUpdated.class, msg -> this.stash())
                .build();
    }

    public Receive updateBehavior() {
        return receiveBuilder()
                .match(Tick.class, msg -> self().tell(new ContinueSimulation(), self()))
                .match(StartSimulation.class, this::onStartSimulation)
                .match(ContinueSimulation.class, this::onContinueSimulation)
                .match(StopSimulation.class, this::onStopSimulation)
                .match(SetSeparationWeight.class, this::onSeparationWeight)
                .match(SetAlignmentWeight.class, this::onAlignmentWeight)
                .match(SetCohesionWeight.class, this::onCohesionWeight)
                .match(BoidUpdated.class, msg -> this.stash())
                .match(BootSimulation.class, msg -> this.stash())
                .match(ResetSimulation.class, msg -> this.stash())
                .build();
    }

    public Receive collectUpdateBehavior() {
        return receiveBuilder()
                .match(VelocityCalculated.class, this::onVelocityCalculated)
                .match(BoidUpdated.class, this::onBoidUpdated)
                .match(StartSimulation.class, msg -> this.stash())
                .match(ContinueSimulation.class, msg -> this.stash())
                .match(BootSimulation.class, msg -> this.stash())
                .match(StopSimulation.class, msg -> this.stash())
                .match(ResetSimulation.class, msg -> this.stash())
                .match(SetSeparationWeight.class, msg -> this.stash())
                .match(SetAlignmentWeight.class, msg -> this.stash())
                .match(SetCohesionWeight.class, msg -> this.stash())
                .build();
    }

    public Receive stoppedBehavior() {
        return receiveBuilder()
                .match(StartSimulation.class, this::onStartSimulation)
                .match(StopSimulation.class, this::onStopSimulation)
                .match(ResetSimulation.class, this::onResetSimulation)
                .match(BootSimulation.class, this::onBootSimulation)
                .match(SetSeparationWeight.class, this::onSeparationWeight)
                .match(SetAlignmentWeight.class, this::onAlignmentWeight)
                .match(SetCohesionWeight.class, this::onCohesionWeight)
                .match(ContinueSimulation.class, msg -> this.stash())
                .match(BoidUpdated.class, msg -> this.stash())
                .build();
    }

    private void onBootSimulation(BootSimulation msg) {
        System.out.println("Booting simulation");
        model = msg.model();
        boidActors.clear();
        List<Boid> boids = model.getBoids();
        for (Boid boid : boids) {
            ActorRef boidActor = getContext().actorOf(BoidActor.props(boid, model));
            boidActors.add(boidActor);
        }
    }

    private void onStartSimulation(StartSimulation msg) {
        System.out.println("Starting simulation " + boidActors.size());
        t0 = System.currentTimeMillis();

        for (ActorRef boidActor : boidActors) {
            boidActor.tell(new CalculateVelocity(model.getBoids()), self());
        }

        updatedBoids.clear();
        count = 0;

        this.unstashAll();
        this.getContext().become(collectUpdateBehavior());
        System.out.println("Started simulation");
    }

    private void onContinueSimulation(ContinueSimulation msg) {
        //System.out.println("Starting simulation with " + nBoids + " boids.");
        t0 = System.currentTimeMillis();
        for (ActorRef boidActor : boidActors) {
            boidActor.tell(new CalculateVelocity(model.getBoids()), self());
        }
        updatedBoids.clear();
        count = 0;

        this.unstashAll();
        this.getContext().become(collectUpdateBehavior());
    }

    private void onVelocityCalculated(VelocityCalculated msg) {
        count++;
        if (count < boidActors.size()) {
            return;
        }
        for (ActorRef boidActor : boidActors) {
            boidActor.tell(new UpdateBoid(), self());
        }
        count = 0;
    }

    private void onBoidUpdated(BoidUpdated msg) {
        // System.out.println("Received updated boid: " + msg.boid());
        updatedBoids.add(msg.updatedBoid());
        count++;
        if (count == nBoids) {
            // update gui
            model.setBoids(new ArrayList<>(updatedBoids));
            view.update(framerate);

            long dtElapsed = System.currentTimeMillis() - t0;
            long frameratePeriod = 1000 / FRAMERATE;
            long delay = Math.max(0, frameratePeriod - dtElapsed);

            if (dtElapsed < frameratePeriod) {
                framerate = FRAMERATE;
            } else {
                framerate = (int) (1000 / dtElapsed);
            }

            this.unstashAll();
            this.getContext().become(updateBehavior());

            getContext().system().scheduler().scheduleOnce(
                    scala.concurrent.duration.Duration.create(delay, java.util.concurrent.TimeUnit.MILLISECONDS),
                    self(),
                    new Tick(),
                    getContext().getSystem().dispatcher(),
                    self()
            );


            this.unstashAll();
            // self().tell(new ContinueSimulation(), self());
        }
    }

    private void onStopSimulation(StopSimulation msg) {
        System.out.println("Stopping simulation");
        this.getContext().become(stoppedBehavior());
    }

    private void onResetSimulation(ResetSimulation msg) {
        model.generateBoids(msg.nboids());
        this.nBoids = msg.nboids();

        for (ActorRef boidActor : boidActors) {
            boidActor.tell(PoisonPill.getInstance(), self());
        }

        boidActors.clear();
        this.getSelf().tell(new BootSimulation(model), self());
    }

    private void onSeparationWeight(SetSeparationWeight msg) {
        System.out.println("Setting separation weight to " + msg.weight());
        model.setSeparationWeight(msg.weight());
        for (ActorRef boidActor : boidActors) {
            boidActor.tell(new SetSeparationWeight(msg.weight()), self());
        }
    }

    private void onAlignmentWeight(SetAlignmentWeight msg) {
        System.out.println("Setting alignment weight to " + msg.weight());
        model.setAlignmentWeight(msg.weight());
        for (ActorRef boidActor : boidActors) {
            boidActor.tell(new SetAlignmentWeight(msg.weight()), self());
        }
    }

    private void onCohesionWeight(SetCohesionWeight msg) {
        System.out.println("Setting cohesion weight to " + msg.weight());
        model.setCohesionWeight(msg.weight());
        for (ActorRef boidActor : boidActors) {
            boidActor.tell(new SetCohesionWeight(msg.weight()), self());
        }
    }
}