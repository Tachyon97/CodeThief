package org.managers.antiban;

import org.dreambot.api.methods.input.Camera;
import org.util.ScriptUtils;

public class RotateCameraAction implements AntiBanAction {
    private final AntiBanProfile profile;

    public RotateCameraAction(AntiBanProfile profile) {
        this.profile = profile;
    }

    @Override
    public void execute() {
        int angle = profile.getRandomRotationAngle();
        int tilt = profile.getRandomCameraTilt();

        System.out.println("[AntiBan] Rotating camera to angle:" + angle + ", tilt:" + tilt);

        Camera.rotateTo(angle, tilt);
        ScriptUtils.sleep(profile.getRandomActionDelay());
    }

    @Override
    public String getActionType() {
        return "rotate";
    }
}