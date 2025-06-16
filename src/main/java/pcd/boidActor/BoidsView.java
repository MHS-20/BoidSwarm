package pcd.boidActor;

import akka.actor.ActorRef;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import pcd.boidActor.BoidProtocol.*;

import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.Hashtable;

public class BoidsView implements ChangeListener {

    private JFrame frame;
    private BoidsPanel boidsPanel;
    private JSlider cohesionSlider, separationSlider, alignmentSlider;
    private BoidsModel model;
    private int width, height;

    private ActorRef manager;
    private int nBoids;
    private boolean isRunning = false;

    private JButton resetButton;
    private JTextField nBoidsTextField;
    private JButton playButton;

    public BoidsView(BoidsModel model, int width, int height, int nBoids) {
        this.model = model;
        this.width = width;
        this.height = height;
        this.nBoids = nBoids;
        this.isRunning = false;

        frame = new JFrame("Boids Simulation");
        frame.setSize(width, height);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel cp = new JPanel();
        LayoutManager layout = new BorderLayout();
        cp.setLayout(layout);

        boidsPanel = new BoidsPanel(this, model);
        cp.add(BorderLayout.CENTER, boidsPanel);

        JPanel slidersPanel = new JPanel();

        cohesionSlider = makeSlider();
        separationSlider = makeSlider();
        alignmentSlider = makeSlider();

        slidersPanel.add(new JLabel("Separation"));
        slidersPanel.add(separationSlider);
        slidersPanel.add(new JLabel("Alignment"));
        slidersPanel.add(alignmentSlider);
        slidersPanel.add(new JLabel("Cohesion"));
        slidersPanel.add(cohesionSlider);

        cp.add(BorderLayout.SOUTH, slidersPanel);

        frame.setContentPane(cp);
        frame.setVisible(true);

        nBoidsTextField = new JTextField(String.valueOf(this.nBoids), 10);
        nBoidsTextField.setForeground(Color.BLACK);

        nBoidsTextField.addActionListener(l -> {
            nBoidsTextField.setForeground(Color.WHITE);
            String text = nBoidsTextField.getText();
            if (!isNumeric(text)) {
                nBoidsTextField.setBackground(Color.ORANGE);
                nBoidsTextField.setText("Int only");
            } else {
                nBoidsTextField.setBackground(Color.WHITE);
                nBoidsTextField.setForeground(Color.GREEN);
                this.nBoids = Integer.parseInt(nBoidsTextField.getText());
            }
        });

        resetButton = new JButton("Reset");
        resetButton.addActionListener(e -> {
            manager.tell(new BoidProtocol.ResetSimulation(this.nBoids), ActorRef.noSender());
        });

        playButton = new JButton("Resume");
        playButton.addActionListener(e -> {
            if (isRunning) {
                isRunning = false;
                playButton.setText("Resume");
                resetButton.setEnabled(true);
                manager.tell(new BoidProtocol.StopSimulation(), ActorRef.noSender());
            } else {
                isRunning = true;
                playButton.setText("Suspend");
                nBoidsTextField.setForeground(Color.BLACK);
                resetButton.setEnabled(false);
                manager.tell(new BoidProtocol.StartSimulation(), ActorRef.noSender());
            }
        });

        slidersPanel.add(playButton);
        slidersPanel.add(new JLabel("Size"));
        slidersPanel.add(nBoidsTextField);
        slidersPanel.add(resetButton);
        slidersPanel.add(new JLabel("Separation"));
        slidersPanel.add(separationSlider);
        slidersPanel.add(new JLabel("Alignment"));
        slidersPanel.add(alignmentSlider);
        slidersPanel.add(new JLabel("Cohesion"));
        slidersPanel.add(cohesionSlider);

        cp.add(BorderLayout.SOUTH, slidersPanel);
        frame.setContentPane(cp);
        frame.setVisible(true);
    }

    public void setManager(ActorRef manager) {
        this.manager = manager;
    }

    private JSlider makeSlider() {
        var slider = new JSlider(JSlider.HORIZONTAL, 0, 20, 10);
        slider.setMajorTickSpacing(10);
        slider.setMinorTickSpacing(1);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        Hashtable labelTable = new Hashtable<>();
        labelTable.put(0, new JLabel("0"));
        labelTable.put(10, new JLabel("1"));
        labelTable.put(20, new JLabel("2"));
        slider.setLabelTable(labelTable);
        slider.setPaintLabels(true);
        slider.addChangeListener(this);
        return slider;
    }

    public void update(int frameRate) {
        try {
            SwingUtilities.invokeAndWait(() -> {
                boidsPanel.setFrameRate(frameRate);
                boidsPanel.repaint();
            });
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (e.getSource() == separationSlider) {
            var val = separationSlider.getValue();
            manager.tell(new SetSeparationWeight(0.1 * val), ActorRef.noSender());
        } else if (e.getSource() == cohesionSlider) {
            var val = cohesionSlider.getValue();
            manager.tell(new SetCohesionWeight(0.1 * val), ActorRef.noSender());
        } else {
            var val = alignmentSlider.getValue();
            manager.tell(new SetAlignmentWeight(0.1 * val), ActorRef.noSender());
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isNumeric(String text) {
        if (text == null) {
            return false;
        }
        try {
            Integer.parseInt(text);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }
}