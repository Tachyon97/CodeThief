package org;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.Random;

/**
 * Handles account-specific anti-ban profiles to ensure unique but consistent behavior.
 * Uses XML for profile storage for better readability and maintenance.
 */
public class AntiBanProfile {
    // Default ranges for anti-ban parameters
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

    // Anti-ban parameters
    private final String username;
    private final String profileSeed;
    private Random seededRandom;

    // Anti-ban values
    private final double cameraRotationProbability;
    private final double skillCheckProbability;
    private final double breakProbability;
    private final double mouseMovementProbability;
    private final double misclickProbability;

    // Advanced pattern parameters
    private final int cameraRotationStyle; // 0-3: different rotation patterns
    private final int mouseMovementStyle; // 0-3: different mouse movement styles
    private final int breakStyle; // 0-3: different break patterns
    private final int skillCheckStyle; // 0-3: different skill checking patterns

    // Timing parameters (ms)
    private final int minBreakInterval;
    private final int maxBreakInterval;
    private final int minActionDelay;
    private final int maxActionDelay;

    // Profile storage
    private static final String DREAMBOT_DIR = System.getProperty("user.home") + "/DreamBot/Scripts/";
    private static final String PROFILE_DIRECTORY = DREAMBOT_DIR + "CodeScripts/CodeThief/anti_ban_profiles/";

    /**
     * Creates or loads an anti-ban profile for a specific account
     *
     * @param username  The account username
     * @param intensity The overall anti-ban intensity (0-100)
     */
    public AntiBanProfile(String username, int intensity) {
        this.username = username;

        // Try to load existing profile
        Map<String, Object> loadedProfile = loadProfile(username);

        if (loadedProfile != null && !loadedProfile.isEmpty()) {
            // Use existing profile
            System.out.println("[AntiBan] Loaded existing profile for " + username);
            this.profileSeed = (String) loadedProfile.get("profileSeed");

            // Load all parameters
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
        } else {
            // Generate new profile
            System.out.println("[AntiBan] Generating new profile for " + username);
            this.profileSeed = generateProfileSeed(username);

            // Create seeded random for consistent generation
            this.seededRandom = new Random(this.profileSeed.hashCode());

            // Apply intensity modifier (0.5 to 2.0)
            double intensityModifier = 0.5 + ((double) intensity / 100 * 1.5);

            // Generate probabilities with ranges adjusted by intensity
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

            // Generate style parameters (0-3 for each)
            this.cameraRotationStyle = seededRandom.nextInt(4);
            this.mouseMovementStyle = seededRandom.nextInt(4);
            this.breakStyle = seededRandom.nextInt(4);
            this.skillCheckStyle = seededRandom.nextInt(4);

            // Generate timing parameters
            this.minBreakInterval = 10000 + seededRandom.nextInt(20000); // 10-30s
            this.maxBreakInterval = minBreakInterval + 20000 + seededRandom.nextInt(40000); // min + (20-60s)
            this.minActionDelay = 500 + seededRandom.nextInt(500); // 500-1000ms
            this.maxActionDelay = minActionDelay + 500 + seededRandom.nextInt(1000); // min + (500-1500ms)

            // Save the new profile
            saveProfile();

            // Debug output
            System.out.println("[AntiBan] Profile created with seed: " + this.profileSeed);
        }

        // Initialize seeded random (needed in case we loaded from file)
        this.seededRandom = new Random(this.profileSeed.hashCode());
    }

    /**
     * Generates a unique but consistent seed for a username
     *
     * @param username The account username
     * @return A seed string
     */
    private String generateProfileSeed(String username) {
        try {
            // Create a hash of the username for consistency
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(username.getBytes());

            // Add some randomness for uniqueness
            Random r = new Random();
            byte[] randomBytes = new byte[8];
            r.nextBytes(randomBytes);

            // Combine hash and randomness
            byte[] combined = new byte[hash.length + randomBytes.length];
            System.arraycopy(hash, 0, combined, 0, hash.length);
            System.arraycopy(randomBytes, 0, combined, hash.length, randomBytes.length);

            // Convert to base64 for readability
            return Base64.getEncoder().encodeToString(combined);
        } catch (NoSuchAlgorithmException e) {
            // Fallback to simpler method
            return username + "_" + System.currentTimeMillis();
        }
    }

    /**
     * Generates a probability within a range, adjusted by intensity
     *
     * @param min               Minimum probability
     * @param max               Maximum probability
     * @param intensityModifier Modifier based on user-selected intensity
     * @return A probability value
     */
    private double generateProbability(double min, double max, double intensityModifier) {
        double range = max - min;
        double baseValue = min + (seededRandom.nextDouble() * range);

        // Apply intensity modifier (capped at reasonable limits)
        double value = baseValue * intensityModifier;
        return Math.min(Math.max(value, min), max * 1.5); // Cap at 150% of max
    }

    /**
     * Saves the profile to disk as XML
     */
// Update the saveProfile method with more detailed logging and a simplified fallback method
// Add this at the end of the saveProfile method in AntiBanProfile.java

    /**
     * Saves the profile to disk as XML with enhanced error handling
     */
    private void saveProfile() {
        try {
            System.out.println("[AntiBan] DreamBot directory: " + DREAMBOT_DIR);

            // Create directory structure if it doesn't exist
            File directory = new File(PROFILE_DIRECTORY);
            if (!directory.exists()) {
                System.out.println("[AntiBan] Creating profile directory: " + directory.getAbsolutePath());
                boolean dirCreated = directory.mkdirs();
                if (dirCreated) {
                    System.out.println("[AntiBan] Successfully created profile directory");
                } else {
                    System.err.println("[AntiBan] Failed to create directory, attempting individual folders...");

                    // Detailed error messages for each directory creation
                    String[] pathComponents = {
                            DREAMBOT_DIR + "CodeScripts",
                            DREAMBOT_DIR + "CodeScripts/CodeThief",
                            DREAMBOT_DIR + "CodeScripts/CodeThief/anti_ban_profiles"
                    };

                    for (String path : pathComponents) {
                        File dir = new File(path);
                        if (!dir.exists()) {
                            boolean created = dir.mkdir();
                            System.out.println("[AntiBan] Creating directory " + path + ": " + (created ? "SUCCESS" : "FAILED"));
                        } else {
                            System.out.println("[AntiBan] Directory already exists: " + path);
                        }
                    }
                }
            }

            // Determine the filename
            String filename = PROFILE_DIRECTORY + sanitizeFilename(username) + ".xml";
            File profileFile = new File(filename);

            System.out.println("[AntiBan] Will save profile to: " + profileFile.getAbsolutePath());
            System.out.println("[AntiBan] Parent directory exists? " + profileFile.getParentFile().exists());
            System.out.println("[AntiBan] Can write to parent? " + profileFile.getParentFile().canWrite());

            try {
                // ---------- FIRST ATTEMPT: Try using the DOM approach ----------
                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
                Document doc = docBuilder.newDocument();

                // Create the root element
                Element rootElement = doc.createElement("AntiBanProfile");
                doc.appendChild(rootElement);

                // Add metadata
                Element metadataElement = doc.createElement("Metadata");
                rootElement.appendChild(metadataElement);

                Element usernameElement = doc.createElement("Username");
                usernameElement.setTextContent(username);
                metadataElement.appendChild(usernameElement);

                Element seedElement = doc.createElement("ProfileSeed");
                seedElement.setTextContent(profileSeed);
                metadataElement.appendChild(seedElement);

                // Add probabilities section
                Element probabilitiesElement = doc.createElement("Probabilities");
                rootElement.appendChild(probabilitiesElement);

                addDoubleElement(doc, probabilitiesElement, "CameraRotation", cameraRotationProbability);
                addDoubleElement(doc, probabilitiesElement, "SkillCheck", skillCheckProbability);
                addDoubleElement(doc, probabilitiesElement, "Break", breakProbability);
                addDoubleElement(doc, probabilitiesElement, "MouseMovement", mouseMovementProbability);
                addDoubleElement(doc, probabilitiesElement, "Misclick", misclickProbability);

                // Add styles section
                Element stylesElement = doc.createElement("Styles");
                rootElement.appendChild(stylesElement);

                addIntElement(doc, stylesElement, "CameraRotation", cameraRotationStyle);
                addIntElement(doc, stylesElement, "MouseMovement", mouseMovementStyle);
                addIntElement(doc, stylesElement, "Break", breakStyle);
                addIntElement(doc, stylesElement, "SkillCheck", skillCheckStyle);

                // Add timing section
                Element timingElement = doc.createElement("Timing");
                rootElement.appendChild(timingElement);

                addIntElement(doc, timingElement, "MinBreakInterval", minBreakInterval);
                addIntElement(doc, timingElement, "MaxBreakInterval", maxBreakInterval);
                addIntElement(doc, timingElement, "MinActionDelay", minActionDelay);
                addIntElement(doc, timingElement, "MaxActionDelay", maxActionDelay);

                // Try to write the file
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();

                // These properties might be causing issues - try with simple settings
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty(OutputKeys.METHOD, "xml");
                transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

                DOMSource source = new DOMSource(doc);
                StreamResult result = new StreamResult(profileFile);

                System.out.println("[AntiBan] About to write XML file using Transformer...");
                transformer.transform(source, result);
                System.out.println("[AntiBan] XML file written successfully");

            } catch (Exception e) {
                System.err.println("[AntiBan] Error writing XML with DOM: " + e.getMessage());
                e.printStackTrace();

                // ---------- SECOND ATTEMPT: Try a simpler approach ----------
                System.out.println("[AntiBan] Trying fallback method with direct XML writing...");

                // Create a simple XML manually
                StringBuilder xml = new StringBuilder();
                xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
                xml.append("<AntiBanProfile>\n");

                // Metadata
                xml.append("  <Metadata>\n");
                xml.append("    <Username>").append(username).append("</Username>\n");
                xml.append("    <ProfileSeed>").append(profileSeed).append("</ProfileSeed>\n");
                xml.append("  </Metadata>\n");

                // Probabilities
                xml.append("  <Probabilities>\n");
                xml.append("    <CameraRotation>").append(cameraRotationProbability).append("</CameraRotation>\n");
                xml.append("    <SkillCheck>").append(skillCheckProbability).append("</SkillCheck>\n");
                xml.append("    <Break>").append(breakProbability).append("</Break>\n");
                xml.append("    <MouseMovement>").append(mouseMovementProbability).append("</MouseMovement>\n");
                xml.append("    <Misclick>").append(misclickProbability).append("</Misclick>\n");
                xml.append("  </Probabilities>\n");

                // Styles
                xml.append("  <Styles>\n");
                xml.append("    <CameraRotation>").append(cameraRotationStyle).append("</CameraRotation>\n");
                xml.append("    <MouseMovement>").append(mouseMovementStyle).append("</MouseMovement>\n");
                xml.append("    <Break>").append(breakStyle).append("</Break>\n");
                xml.append("    <SkillCheck>").append(skillCheckStyle).append("</SkillCheck>\n");
                xml.append("  </Styles>\n");

                // Timing
                xml.append("  <Timing>\n");
                xml.append("    <MinBreakInterval>").append(minBreakInterval).append("</MinBreakInterval>\n");
                xml.append("    <MaxBreakInterval>").append(maxBreakInterval).append("</MaxBreakInterval>\n");
                xml.append("    <MinActionDelay>").append(minActionDelay).append("</MinActionDelay>\n");
                xml.append("    <MaxActionDelay>").append(maxActionDelay).append("</MaxActionDelay>\n");
                xml.append("  </Timing>\n");

                xml.append("</AntiBanProfile>");

                try (FileWriter writer = new FileWriter(profileFile)) {
                    System.out.println("[AntiBan] Writing XML directly with FileWriter...");
                    writer.write(xml.toString());
                    System.out.println("[AntiBan] XML file written successfully with fallback method");
                } catch (IOException ioException) {
                    System.err.println("[AntiBan] ERROR: Even fallback XML writing failed: " + ioException.getMessage());
                    ioException.printStackTrace();

                    // ---------- THIRD ATTEMPT: Check file permissions and try even simpler approach ----------
                    try {
                        System.out.println("[AntiBan] Final attempt - trying to write a simple test file");
                        File testFile = new File(PROFILE_DIRECTORY + "test.txt");
                        try (FileWriter testWriter = new FileWriter(testFile)) {
                            testWriter.write("Test file\nUsername: " + username);
                        }
                        System.out.println("[AntiBan] Test file created at: " + testFile.getAbsolutePath());
                        System.out.println("[AntiBan] Test file exists? " + testFile.exists());
                    } catch (IOException testException) {
                        System.err.println("[AntiBan] Cannot even write test file. This is likely a permission issue: " + testException.getMessage());
                    }
                }
            }

            // Check if file was actually created
            if (profileFile.exists()) {
                System.out.println("[AntiBan] SUCCESS: Profile file verified to exist at: " + profileFile.getAbsolutePath());
                System.out.println("[AntiBan] File size: " + profileFile.length() + " bytes");
            } else {
                System.err.println("[AntiBan] ERROR: Profile file was not created at: " + profileFile.getAbsolutePath());
            }

        } catch (Exception e) {
            System.err.println("[AntiBan] Critical error in saveProfile method: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Helper method to add a double element to the XML
    private void addDoubleElement(Document doc, Element parent, String name, double value) {
        Element element = doc.createElement(name);
        element.setTextContent(String.valueOf(value));
        parent.appendChild(element);
    }

    // Helper method to add an integer element to the XML
    private void addIntElement(Document doc, Element parent, String name, int value) {
        Element element = doc.createElement(name);
        element.setTextContent(String.valueOf(value));
        parent.appendChild(element);
    }

    /**
     * Loads a profile from disk using XML
     *
     * @param username The account username
     * @return A map of profile parameters or null if not found
     */
    private Map<String, Object> loadProfile(String username) {
        try {
            String filename = PROFILE_DIRECTORY + sanitizeFilename(username) + ".xml";
            File file = new File(filename);

            if (!file.exists()) {
                System.out.println("[AntiBan] No existing profile found at: " + file.getAbsolutePath());
                return null;
            }

            System.out.println("[AntiBan] Loading profile from: " + file.getAbsolutePath());

            // Rest of the method remains the same...
        } catch (Exception e) {
            System.err.println("[AntiBan] Error loading profile: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
        return Map.of();
    }

    // Helper method to get a double value from the XML
    private double getDoubleFromXml(Document doc, String tagName) throws Exception {
        NodeList nodeList = doc.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            return Double.parseDouble(nodeList.item(0).getTextContent());
        }
        throw new Exception("Missing " + tagName + " in XML");
    }

    // Helper method to get an integer value from the XML
    private int getIntFromXml(Document doc, String tagName) throws Exception {
        NodeList nodeList = doc.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            return Integer.parseInt(nodeList.item(0).getTextContent());
        }
        throw new Exception("Missing " + tagName + " in XML");
    }

    // Helper method to get an integer from a specific parent section in the XML
    private int getIntFromXml(Document doc, String tagName, String parentSection) throws Exception {
        NodeList parentNodes = doc.getElementsByTagName(parentSection);
        if (parentNodes.getLength() > 0) {
            Element parentElement = (Element) parentNodes.item(0);
            NodeList nodes = parentElement.getElementsByTagName(tagName);
            if (nodes.getLength() > 0) {
                return Integer.parseInt(nodes.item(0).getTextContent());
            }
        }
        throw new Exception("Missing " + tagName + " in " + parentSection + " section");
    }

    /**
     * Sanitizes a username for file storage
     *
     * @param username The username to sanitize
     * @return A safe filename
     */
    private String sanitizeFilename(String username) {
        return username.replaceAll("[^a-zA-Z0-9]", "_");
    }

    /**
     * Gets a random rotation angle based on style
     *
     * @return A rotation angle
     */
    public int getRandomRotationAngle() {
        switch (cameraRotationStyle) {
            case 0: // Full 360 style
                return seededRandom.nextInt(360);
            case 1: // 90-degree preference
                return seededRandom.nextInt(4) * 90 + (seededRandom.nextInt(30) - 15);
            case 2: // Limited range style
                return seededRandom.nextInt(180) + 90; // 90-270 range
            case 3: // Micro-adjustments
                return seededRandom.nextInt(60) - 30; // -30 to +30 from current
            default:
                return seededRandom.nextInt(360);
        }
    }

    /**
     * Gets a random camera tilt based on style
     *
     * @return A camera tilt value
     */
    public int getRandomCameraTilt() {
        switch (cameraRotationStyle) {
            case 0: // Wide range
                return seededRandom.nextInt(70) + 30; // 30-100
            case 1: // Mid-level focus
                return seededRandom.nextInt(30) + 45; // 45-75
            case 2: // Top-down preference
                return seededRandom.nextInt(30) + 70; // 70-100
            case 3: // Low angle preference
                return seededRandom.nextInt(30) + 30; // 30-60
            default:
                return seededRandom.nextInt(70) + 30;
        }
    }

    /**
     * Gets a random break duration based on style
     *
     * @return Break duration in milliseconds
     */
    public int getRandomBreakDuration() {
        switch (breakStyle) {
            case 0: // Short breaks
                return 1000 + seededRandom.nextInt(2000);
            case 1: // Medium breaks
                return 2000 + seededRandom.nextInt(3000);
            case 2: // Long breaks
                return 3000 + seededRandom.nextInt(5000);
            case 3: // Variable breaks
                return 1000 + seededRandom.nextInt(7000);
            default:
                return 2000 + seededRandom.nextInt(3000);
        }
    }

    /**
     * Gets a random action delay based on profile
     *
     * @return Delay in milliseconds
     */
    public int getRandomActionDelay() {
        return minActionDelay + seededRandom.nextInt(maxActionDelay - minActionDelay);
    }

    /**
     * Gets a random check interval based on profile
     *
     * @return Interval in milliseconds
     */
    public int getRandomCheckInterval() {
        return minBreakInterval + seededRandom.nextInt(maxBreakInterval - minBreakInterval);
    }

    /**
     * Gets a Point for random mouse movement based on style
     *
     * @param centerX Center X coordinate
     * @param centerY Center Y coordinate
     * @param radius  Maximum distance from center
     * @return Random point within radius of center
     */
    public Point getRandomPointNear(int centerX, int centerY, int radius) {
        switch (mouseMovementStyle) {
            case 0: // Full random within circle
                double angle = seededRandom.nextDouble() * 2 * Math.PI;
                double distance = seededRandom.nextDouble() * radius;
                int x = (int) (centerX + distance * Math.cos(angle));
                int y = (int) (centerY + distance * Math.sin(angle));
                return new Point(x, y);
            case 1: // Grid-like pattern
                int gridX = centerX + (seededRandom.nextInt(radius * 2) - radius);
                int gridY = centerY + (seededRandom.nextInt(radius * 2) - radius);
                gridX = Math.round(gridX / 5.0f) * 5; // Snap to grid
                gridY = Math.round(gridY / 5.0f) * 5;
                return new Point(gridX, gridY);
            case 2: // Rectangle pattern
                int rectX = centerX + (seededRandom.nextInt(radius * 2) - radius);
                int rectY = centerY + (seededRandom.nextInt((int) (radius * 1.5)) - (int) (radius * 0.75));
                return new Point(rectX, rectY);
            case 3: // Small jittery movements
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

    // Getters for all parameters

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

    /**
     * Gets a summary of the profile for display
     *
     * @return Profile summary string
     */
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

    /**
     * Formats a probability for display
     *
     * @param probability The probability to format
     * @return Formatted string
     */
    private String formatProbability(double probability) {
        return String.format("%.1f%%", probability * 100);
    }
}