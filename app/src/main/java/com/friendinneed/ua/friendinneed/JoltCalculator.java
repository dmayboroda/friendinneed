package com.friendinneed.ua.friendinneed;

public class JoltCalculator {

    public static final int ALLOWED_ERRORS_THRESHOLD = 50;
    public static final double GRAVITY = 9.8;
    private static final double G_POINT_MIN = 6.0 * GRAVITY;
    private static final double G_POINT_MAX = 10.0 * GRAVITY;

    private final DataQueue queue;

    private JoltState joltState = JoltState.FREE_FALL;

    JoltCalculator(DataQueue queue) {
        this.queue = queue;
    }

    private static int joltErrors = 0;
    private static int hugeHitsCount = 0;

    boolean checkForJolt(float accX, float accY, float accZ) {
        double gValue = calculateGValue(accX, accY, accZ);
        long filledQueuePercentage = queue.getTimeElapsedSinceStartPercents();
        if (filledQueuePercentage < 30 && !isFreeFall(gValue)) {
            joltErrors++;
        } else if (filledQueuePercentage >= 30 && filledQueuePercentage < 90 && !isJumpingBackAndForce(gValue)) {
            joltErrors++;
        } else if (filledQueuePercentage > 90 && !stillOnFloor(gValue)) {
            joltErrors++;
        }
        if (joltErrors > ALLOWED_ERRORS_THRESHOLD) {
            queue.clear();
            hugeHitsCount = 0;
        }
        return filledQueuePercentage > 95 && hugeHitsCount != 0;
    }

    boolean checkForJolt2(float accX, float accY, float accZ) {
        double gValue = calculateGValue(accX, accY, accZ);
        long filledQueuePercentage = queue.getTimeElapsedSinceStartPercents();
        if (joltState == JoltState.FREE_FALL) {
            if (!isFreeFall(gValue)) {
                if (filledQueuePercentage > 10) {
                    joltState = JoltState.JUMPING_BACK_AND_FORCE;
                } else {
                    joltErrors++;
                }
            }
        } else if (joltState == JoltState.JUMPING_BACK_AND_FORCE) {
            if (!isJumpingBackAndForce(gValue)) {
                if (filledQueuePercentage > 70) {
                    joltState = JoltState.STILL_ON_FLOOR;
                } else {
                    joltErrors++;
                }
            }
        } else if (joltState == JoltState.STILL_ON_FLOOR) {
            if (!stillOnFloor(gValue)) {
                joltErrors++;
            }
        }
        if (joltErrors > ALLOWED_ERRORS_THRESHOLD) {
            queue.clear();
            hugeHitsCount = 0;
        }
        return filledQueuePercentage > 95 && hugeHitsCount != 0;
    }

    private boolean stillOnFloor(double gValue) {
        return gValue >= 0.9 * GRAVITY && gValue <= 1.1 * GRAVITY;
    }

    private boolean isJumpingBackAndForce(double gValue) {
        if (gValue > G_POINT_MAX) {
            return false;
        } else if (gValue > G_POINT_MIN && gValue < G_POINT_MAX) {
            hugeHitsCount++;
        }
        return true;
    }

    private boolean isFreeFall(double gValue) {
        return gValue >= 0 && gValue <= 0.1 * GRAVITY;
    }

    static double calculateGValue(float accX, float accY, float accZ) {
        double sum = (accX * accX) + (accY * accY) + (accZ * accZ);
        return Math.sqrt(sum);
    }

    private enum JoltState {
        FREE_FALL,
        JUMPING_BACK_AND_FORCE,
        STILL_ON_FLOOR;
    }
}
