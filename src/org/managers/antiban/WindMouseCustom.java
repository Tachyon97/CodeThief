package org.managers.antiban;

import org.dreambot.api.input.Mouse;
import org.dreambot.api.input.event.impl.mouse.MouseButton;
import org.dreambot.api.input.mouse.algorithm.MouseAlgorithm;
import org.dreambot.api.input.mouse.destination.AbstractMouseDestination;
import org.dreambot.api.methods.Calculations;
import org.dreambot.api.utilities.Logger;
import org.util.ScriptUtils;

import java.awt.*;
import java.util.Random;

public class WindMouseCustom implements MouseAlgorithm {
    private final Random random = new Random();

    private double bezierDeviation = 1.0;
    private double overshootProbability = 0.3;
    private double overshootDistance = 1.0;
    private double mouseSpeed = 0.6;

    @Override
    public boolean handleMovement(AbstractMouseDestination abstractMouseDestination) {
        Point suitPos = abstractMouseDestination.getSuitablePoint();
        mouseMovement(suitPos);
        return distance(Mouse.getPosition(), suitPos) < 2;
    }

    @Override
    public boolean handleClick(MouseButton mouseButton) {
        return Mouse.getDefaultMouseAlgorithm().handleClick(mouseButton);
    }

    public void mouseMovement(Point point) {
        Point curPos = Mouse.getPosition();
        boolean shouldOvershoot = random.nextDouble() < overshootProbability;

        if (shouldOvershoot) {
            Point overshootPoint = getOvershootPoint(curPos, point);
            moveMouseBezier(curPos, overshootPoint);
            ScriptUtils.sleep((int) (50 * mouseSpeed), (int) (150 * mouseSpeed));
            moveMouseBezier(overshootPoint, point);
        } else {
            moveMouseBezier(curPos, point);
        }
    }

    private Point[] generateControlPoints(Point start, Point end) {
        Point[] controlPoints = new Point[4];
        controlPoints[0] = start;
        controlPoints[3] = end;

        int midX = (start.x + end.x) / 2;
        int midY = (start.y + end.y) / 2;

        double distance = distance(start, end);
        int deviation = (int) (distance * 0.5 * bezierDeviation);
        deviation = Math.max(10, Math.min(100, deviation));

        controlPoints[1] = new Point(midX + random.nextInt(deviation) - deviation / 2,
                midY + random.nextInt(deviation) - deviation / 2);
        controlPoints[2] = new Point(midX + random.nextInt(deviation) - deviation / 2,
                midY + random.nextInt(deviation) - deviation / 2);

        return controlPoints;
    }

    private void moveMouseBezier(Point start, Point end) {
        Point[] controlPoints = generateControlPoints(start, end);
        double distance = distance(start, end);
        int steps = (int) Math.max(30, Math.min(50, distance / 2));

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / (double) steps;
            int x = (int) (Math.pow(1 - t, 3) * controlPoints[0].x +
                    3 * Math.pow(1 - t, 2) * t * controlPoints[1].x +
                    3 * (1 - t) * Math.pow(t, 2) * controlPoints[2].x +
                    Math.pow(t, 3) * controlPoints[3].x);
            int y = (int) (Math.pow(1 - t, 3) * controlPoints[0].y +
                    3 * Math.pow(1 - t, 2) * t * controlPoints[1].y +
                    3 * (1 - t) * Math.pow(t, 2) * controlPoints[2].y +
                    Math.pow(t, 3) * controlPoints[3].y);

            Mouse.hop(new Point(x, y));

            try {
                int delay = (int) (Calculations.random(5, 15) * mouseSpeed);
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Logger.log(e.getMessage());
            }
        }
    }

    private Point getOvershootPoint(Point start, Point end) {
        int baseOvershootDistance = (int) (30 * overshootDistance);
        int overshootDistanceVar = (int) (30 * overshootDistance);
        int overshootAmount = baseOvershootDistance + random.nextInt(overshootDistanceVar);

        double angle = Math.atan2(end.y - start.y, end.x - start.x);

        int overshootX = (int) (end.x + overshootAmount * Math.cos(angle));
        int overshootY = (int) (end.y + overshootAmount * Math.sin(angle));

        return new Point(overshootX, overshootY);
    }

    public double distance(Point p1, Point p2) {
        return Math.sqrt((p2.y - p1.y) * (p2.y - p1.y) + (p2.x - p1.x) * (p2.x - p1.x));
    }

    public void configureFromProfile(AntiBanProfile profile) {
        this.bezierDeviation = 0.5 + (profile.getMouseMovementStyle() / 4.0);
        this.overshootProbability = 0.1 + (profile.getMouseMovementProbability() * 0.5);
        this.overshootDistance = 0.7 + (profile.getMisclickProbability() * 5.0);
        this.mouseSpeed = 0.6 + (profile.getBreakStyle() * 0.1) + (Math.random() * 0.4);
    }
}