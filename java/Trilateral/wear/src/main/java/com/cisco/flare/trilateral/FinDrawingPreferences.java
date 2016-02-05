package com.cisco.flare.trilateral;

/**
 * Static functions used for drawing fins
 */
public class FinDrawingPreferences {
    private static FinDrawingPreferences instance;
    private static double gravitationFactor = 0.5;
    private static double nearSize = 40; // 100;
    private static double farSize = 5; // 15;
    private static double nearOpacity = 1;
    private static double farOpacity = 0.25;
    private static double nearDistance = 1; // 2.5;
    private static double farDistance = 12; // 7;
    private static double watchAngle = 0; // move this to watch device data

    private final static int FACTOR_TYPE_OPACITY = 0;
    private final static int FACTOR_TYPE_FIN_SIZE = 1;

    public static FinDrawingPreferences getInstance() {
        if (null == instance) {
            instance = new FinDrawingPreferences();
        }
        return instance;
    }
    private FinDrawingPreferences() {}

    public final double getWatchAngle() { return watchAngle; }

    /**
     * Acceleration depending on the gravitation factor for the bottom half of the circle (0 - 180)
     * Linear transform for the top half of the circle (180-360)
     * Screen angles are: 0=right, 90=bottom, 180=left, 270=top
     * @param angleInDegrees    angle in degrees. Negative values are accepted
     * @return the modified angle (in degrees)
     */
    public static double transformAngleDegrees(double angleInDegrees) {
        double modifiedAngle = angleInDegrees % 360;
        while (modifiedAngle < 0) {
            modifiedAngle += 360;
        }
        if (modifiedAngle < 180) {
            double angleToRemove;
            boolean invert;
            if (modifiedAngle <= 90) {
                angleToRemove = 0;
                invert = false;
            }
            else {
                angleToRemove = 90;
                invert = true;
            }
            double angleBetween0And1 = (modifiedAngle - angleToRemove) / 90;
            double acceleration = Math.pow((invert ? angleBetween0And1 : 1.0 - angleBetween0And1), 2.0 * gravitationFactor);
            modifiedAngle = 90 * (invert ? acceleration : 1.0 - acceleration);
            modifiedAngle += angleToRemove;
        }
        return modifiedAngle;
    }

    /**
     * Calculates the fin size depending on the distance between the user and the object
     * @param distance  distance between user and object
     * @return fin size
     */
    public static double finSize(final double distance) {
        return finFactor(FACTOR_TYPE_FIN_SIZE, distance);
    }

    /**
     * Calculates the fin opacity depending on the distance between the user and the object
     * @param distance  distance between user and object
     * @return fin opacity
     */
    public static double finOpacity(final double distance) {
        return finFactor(FACTOR_TYPE_OPACITY, distance);
    }

    private static double finFactor(int type, final double distance) {
        double nearValue, farValue;
        switch (type) {
            case FACTOR_TYPE_FIN_SIZE:
                nearValue = nearSize;
                farValue = farSize;
                break;
            case FACTOR_TYPE_OPACITY:
                nearValue = nearOpacity;
                farValue = farOpacity;
                break;
            default:
                return 0;
        }

        if (distance <= nearDistance) {
            return nearValue;
        }
        else if (distance >= farDistance) {
            return farValue;
        }

        double distRange = nearDistance - farDistance;
        double distFromFar = distance - farDistance;
        double sizeRange = nearValue - farValue;
        return farValue + (distFromFar/distRange)*sizeRange;
    }
}
