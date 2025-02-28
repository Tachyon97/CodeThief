package org.managers.antiban;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.Random;

public class AntiBanProfile {
    private static final double DEFAULT_CAMERA_ROTATION_PROB_MIN = 0.10;
    private static final double DEFAULT_CAMERA_ROTATION_PROB_MAX = 0.25;

    private static final double DEFAULT_SKILL_CHECK_PROB_MIN = 0.03;
    private static final double DEFAULT_SKILL_CHECK_PROB_MAX = 0.08;

    private static final double DEFAULT_BREAK_PROB_MIN = 0.01;
    private static final double DEFAULT_BREAK_PROB_MAX = 0.04;

    private static final double DEFAULT_MOUSE_MOVEMENT_PROB_MIN = 0.05;
    private static final double DEFAULT_MOUSE_MOVEMENT_PROB_MAX = 0.15;

    private static final double DEFAULT_MISCLICK_PROB_MIN = 0.01;
    private static final double DEFAULT_MISCLICK_PROB_MAX = 0.03;

    private final String username;
    private final String profileSeed;
    private Random seededRandom;

    private final double cameraRotationProbability;
    private final double skillCheckProbability;
    private final double breakProbability;
    private final double mouseMovementProbability;
    private final double misclickProbability;

    private final int cameraRotationStyle;
    private final int mouseMovementStyle;
    private final int breakStyle;
    private final int skillCheckStyle;

    private final int minBreakInterval;
    private final int maxBreakInterval;
    private final int minActionDelay;
    private final int maxActionDelay;

    private final double mouseCurveFactor;
    private final double mouseSpeedFactor;
    private final double mouseHesitationFactor;

    private static final String DREAMBOT_DIR = System.getProperty("user.home") + "/DreamBot/Scripts/";
    private static final String PROFILE_DIRECTORY = DREAMBOT_DIR + "CodeScripts/CodeThief/anti_ban_profiles/";

    public AntiBanProfile(String username, int intensity) {
        this.username = username;

        Map<String, Object> loadedProfile = loadProfile(username);

        if (loadedProfile != null && !loadedProfile.isEmpty()) {
            System.out.println("[AntiBan] Loaded existing profile for " + username);
            this.profileSeed = (String) loadedProfile.get("profileSeed");

            this.cameraRotationProbability = (double) loadedProfile.get("cameraRotationProbability");
            this.skillCheckProbability = (double) loadedProfile.get("skillCheckProbability");
            this.breakProbability = (double) loadedProfile.get("breakProbability");
            this.mouseMovementProbability = (double) loadedProfile.get("mouseMovementProbability");
            this.misclickProbability = (double) loadedProfile.get("misclickProbability");

            this.cameraRotationStyle = (int) loadedProfile.get("cameraRotationStyle");
            this.mouseMovementStyle = (int) loadedProfile.get("mouseMovementStyle");
            this.breakStyle = (int) loadedProfile.get("breakStyle");
            this.skillCheckStyle = (int) loadedProfile.get("skillCheckStyle");

            this.minBreakInterval = (int) loadedProfile.get("minBreakInterval");
            this.maxBreakInterval = (int) loadedProfile.get("maxBreakInterval");
            this.minActionDelay = (int) loadedProfile.get("minActionDelay");
            this.maxActionDelay = (int) loadedProfile.get("maxActionDelay");

            if (loadedProfile.containsKey("mouseCurveFactor")) {
                this.mouseCurveFactor = (double) loadedProfile.get("mouseCurveFactor");
            } else {
                this.mouseCurveFactor = 0.7 + (mouseMovementStyle * 0.2) + (Math.random() * 0.3);
            }

            if (loadedProfile.containsKey("mouseSpeedFactor")) {
                this.mouseSpeedFactor = (double) loadedProfile.get("mouseSpeedFactor");
            } else {
                this.mouseSpeedFactor = 0.8 + (breakStyle * 0.1) + (Math.random() * 0.4);
            }

            if (loadedProfile.containsKey("mouseHesitationFactor")) {
                this.mouseHesitationFactor = (double) loadedProfile.get("mouseHesitationFactor");
            } else {
                this.mouseHesitationFactor = 0.5 + (Math.random() * 0.5);
            }
        } else {
            System.out.println("[AntiBan] Generating new profile for " + username);
            this.profileSeed = generateProfileSeed(username);

            this.seededRandom = new Random(this.profileSeed.hashCode());

            double intensityModifier = 0.5 + ((double) intensity / 100 * 1.5);

            this.cameraRotationProbability = generateProbability(
                    DEFAULT_CAMERA_ROTATION_PROB_MIN,
                    DEFAULT_CAMERA_ROTATION_PROB_MAX,
                    intensityModifier);

            this.skillCheckProbability = generateProbability(
                    DEFAULT_SKILL_CHECK_PROB_MIN,
                    DEFAULT_SKILL_CHECK_PROB_MAX,
                    intensityModifier);

            this.breakProbability = generateProbability(
                    DEFAULT_BREAK_PROB_MIN,
                    DEFAULT_BREAK_PROB_MAX,
                    intensityModifier);

            this.mouseMovementProbability = generateProbability(
                    DEFAULT_MOUSE_MOVEMENT_PROB_MIN,
                    DEFAULT_MOUSE_MOVEMENT_PROB_MAX,
                    intensityModifier);

            this.misclickProbability = generateProbability(
                    DEFAULT_MISCLICK_PROB_MIN,
                    DEFAULT_MISCLICK_PROB_MAX,
                    intensityModifier);

            this.cameraRotationStyle = seededRandom.nextInt(4);
            this.mouseMovementStyle = seededRandom.nextInt(4);
            this.breakStyle = seededRandom.nextInt(4);
            this.skillCheckStyle = seededRandom.nextInt(4);

            this.minBreakInterval = 10000 + seededRandom.nextInt(20000);
            this.maxBreakInterval = minBreakInterval + 20000 + seededRandom.nextInt(40000);
            this.minActionDelay = 500 + seededRandom.nextInt(500);
            this.maxActionDelay = minActionDelay + 500 + seededRandom.nextInt(1000);

            this.mouseCurveFactor = 0.7 + (mouseMovementStyle * 0.2) + (seededRandom.nextDouble() * 0.3);
            this.mouseSpeedFactor = 0.8 + (breakStyle * 0.1) + (seededRandom.nextDouble() * 0.4);
            this.mouseHesitationFactor = 0.5 + (seededRandom.nextDouble() * 0.5);

            saveProfile();

            System.out.println("[AntiBan] Profile created with seed: " + this.profileSeed);
        }

        this.seededRandom = new Random(this.profileSeed.hashCode());
    }

    private String generateProfileSeed(String username) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(username.getBytes());

            Random r = new Random();
            byte[] randomBytes = new byte[8];
            r.nextBytes(randomBytes);

            byte[] combined = new byte[hash.length + randomBytes.length];
            System.arraycopy(hash, 0, combined, 0, hash.length);
            System.arraycopy(randomBytes, 0, combined, hash.length, randomBytes.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (NoSuchAlgorithmException e) {
            return username + "_" + System.currentTimeMillis();
        }
    }

    private double generateProbability(double min, double max, double intensityModifier) {
        double range = max - min;
        double baseValue = min + (seededRandom.nextDouble() * range);

        double value = baseValue * intensityModifier;
        return Math.min(Math.max(value, min), max * 1.5);
    }

    private void saveProfile() {
        try {
            File directory = new File(PROFILE_DIRECTORY);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            String filename = PROFILE_DIRECTORY + sanitizeFilename(username) + "_profile.txt";
            File profileFile = new File(filename);

            StringBuilder content = new StringBuilder();
            content.append("profileSeed=").append(profileSeed).append("\n");
            content.append("cameraRotationProbability=").append(cameraRotationProbability).append("\n");
            content.append("skillCheckProbability=").append(skillCheckProbability).append("\n");
            content.append("breakProbability=").append(breakProbability).append("\n");
            content.append("mouseMovementProbability=").append(mouseMovementProbability).append("\n");
            content.append("misclickProbability=").append(misclickProbability).append("\n");
            content.append("cameraRotationStyle=").append(cameraRotationStyle).append("\n");
            content.append("mouseMovementStyle=").append(mouseMovementStyle).append("\n");
            content.append("breakStyle=").append(breakStyle).append("\n");
            content.append("skillCheckStyle=").append(skillCheckStyle).append("\n");
            content.append("minBreakInterval=").append(minBreakInterval).append("\n");
            content.append("maxBreakInterval=").append(maxBreakInterval).append("\n");
            content.append("minActionDelay=").append(minActionDelay).append("\n");
            content.append("maxActionDelay=").append(maxActionDelay).append("\n");
            content.append("mouseCurveFactor=").append(mouseCurveFactor).append("\n");
            content.append("mouseSpeedFactor=").append(mouseSpeedFactor).append("\n");
            content.append("mouseHesitationFactor=").append(mouseHesitationFactor).append("\n");

            try (FileWriter writer = new FileWriter(profileFile)) {
                writer.write(content.toString());
                System.out.println("[AntiBan] Profile saved to: " + profileFile.getAbsolutePath());
            } catch (IOException e) {
                System.out.println("[AntiBan] Failed to save profile: " + e.getMessage());
            }
        } catch (Exception e) {
            System.out.println("[AntiBan] Critical error in saveProfile method: " + e.getMessage());
        }
    }

    private Map<String, Object> loadProfile(String username) {
        try {
            String filename = PROFILE_DIRECTORY + sanitizeFilename(username) + "_profile.txt";
            File file = new File(filename);

            if (!file.exists()) {
                System.out.println("[AntiBan] No existing profile found at: " + file.getAbsolutePath());
                return null;
            }
        } catch (Exception e) {
            System.out.println("[AntiBan] Error loading profile: " + e.getMessage());
            return null;
        }
        return Map.of();
    }

    private String sanitizeFilename(String username) {
        return username.replaceAll("[^a-zA-Z0-9]", "_");
    }

    public int getRandomRotationAngle() {
        switch (cameraRotationStyle) {
            case 0:
                return seededRandom.nextInt(360);
            case 1:
                return seededRandom.nextInt(4) * 90 + (seededRandom.nextInt(30) - 15);
            case 2:
                return seededRandom.nextInt(180) + 90;
            case 3:
                return seededRandom.nextInt(60) - 30;
            default:
                return seededRandom.nextInt(360);
        }
    }

    public int getRandomCameraTilt() {
        switch (cameraRotationStyle) {
            case 0:
                return seededRandom.nextInt(70) + 30;
            case 1:
                return seededRandom.nextInt(30) + 45;
            case 2:
                return seededRandom.nextInt(30) + 70;
            case 3:
                return seededRandom.nextInt(30) + 30;
            default:
                return seededRandom.nextInt(70) + 30;
        }
    }

    public int getRandomBreakDuration() {
        switch (breakStyle) {
            case 0:
                return 1000 + seededRandom.nextInt(2000);
            case 1:
                return 2000 + seededRandom.nextInt(3000);
            case 2:
                return 3000 + seededRandom.nextInt(5000);
            case 3:
                return 1000 + seededRandom.nextInt(7000);
            default:
                return 2000 + seededRandom.nextInt(3000);
        }
    }

    public int getRandomActionDelay() {
        return minActionDelay + seededRandom.nextInt(maxActionDelay - minActionDelay);
    }

    public int getRandomCheckInterval() {
        return minBreakInterval + seededRandom.nextInt(maxBreakInterval - minBreakInterval);
    }

    public Point getRandomPointNear(int centerX, int centerY, int radius) {
        switch (mouseMovementStyle) {
            case 0:
                double angle = seededRandom.nextDouble() * 2 * Math.PI;
                double distance = seededRandom.nextDouble() * radius;
                int x = (int) (centerX + distance * Math.cos(angle));
                int y = (int) (centerY + distance * Math.sin(angle));
                return new Point(x, y);
            case 1:
                int gridX = centerX + (seededRandom.nextInt(radius * 2) - radius);
                int gridY = centerY + (seededRandom.nextInt(radius * 2) - radius);
                gridX = Math.round(gridX / 5.0f) * 5;
                gridY = Math.round(gridY / 5.0f) * 5;
                return new Point(gridX, gridY);
            case 2:
                int rectX = centerX + (seededRandom.nextInt(radius * 2) - radius);
                int rectY = centerY + (seededRandom.nextInt((int) (radius * 1.5)) - (int) (radius * 0.75));
                return new Point(rectX, rectY);
            case 3:
                int jitterX = centerX + (seededRandom.nextInt(radius) - radius / 2);
                int jitterY = centerY + (seededRandom.nextInt(radius) - radius / 2);
                return new Point(jitterX, jitterY);
            default:
                return new Point(
                        centerX + (seededRandom.nextInt(radius * 2) - radius),
                        centerY + (seededRandom.nextInt(radius * 2) - radius)
                );
        }
    }

    public double getCameraRotationProbability() {
        return cameraRotationProbability;
    }

    public double getSkillCheckProbability() {
        return skillCheckProbability;
    }

    public double getBreakProbability() {
        return breakProbability;
    }

    public double getMouseMovementProbability() {
        return mouseMovementProbability;
    }

    public double getMisclickProbability() {
        return misclickProbability;
    }

    public int getCameraRotationStyle() {
        return cameraRotationStyle;
    }

    public int getMouseMovementStyle() {
        return mouseMovementStyle;
    }

    public int getBreakStyle() {
        return breakStyle;
    }

    public int getSkillCheckStyle() {
        return skillCheckStyle;
    }

    public double getMouseCurveFactor() {
        return mouseCurveFactor;
    }

    public double getMouseSpeedFactor() {
        return mouseSpeedFactor;
    }

    public double getMouseHesitationFactor() {
        return mouseHesitationFactor;
    }

    public int getHesitationTime() {
        return (int) (mouseHesitationFactor * 300);
    }

    public String getProfileSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Anti-Ban Profile for: ").append(username).append("\n");
        summary.append("Camera Rotation: ").append(formatProbability(cameraRotationProbability)).append("\n");
        summary.append("Skill Checking: ").append(formatProbability(skillCheckProbability)).append("\n");
        summary.append("Break Frequency: ").append(formatProbability(breakProbability)).append("\n");
        summary.append("Mouse Movement: ").append(formatProbability(mouseMovementProbability)).append("\n");
        summary.append("Misclick Rate: ").append(formatProbability(misclickProbability)).append("\n");
        return summary.toString();
    }

    private String formatProbability(double probability) {
        return String.format("%.1f%%", probability * 100);
    }
}