package com.friendinneed.ua.friendinneed;

import android.util.Log;

public class JoltCalculator {

  public static final int ALLOWED_ERRORS_NUMBER_THRESHOLD = 70;
  public static final double GRAVITY = 9.8;
  private static final double G_POINT_MIN = 3.0 * GRAVITY;
  private static final double G_POINT_MIN_SMALL = 1.5 * GRAVITY;
  private static final double G_POINT_MAX = 10.0 * GRAVITY;
  public static final double FREE_FALL_LIMIT = 0.5;
  private static final double EPSILON = 0.5;
  private final String TAG = JoltCalculator.class.getSimpleName();

  private final DataQueue queue;

  private JoltState joltState = JoltState.FREE_FALL;

  JoltCalculator(DataQueue queue) {
    this.queue = queue;
  }

  private static int joltErrors = 0;
  private static int hugeHitsCount = 0;

  boolean checkForJolt2(float accX, float accY, float accZ) {
    double gValue = calculateGValue(accX, accY, accZ);
    Log.v(TAG, "current vector: " + gValue + ", state: " + joltState.name());
    long filledQueuePercentage = queue.getTimeElapsedSinceStartPercents();
    if (filledQueuePercentage < 30 && !isFreeFall(gValue)) {
      joltErrors++;
    } else if (filledQueuePercentage >= 30 && filledQueuePercentage < 90 && !isJumpingBackAndForce(gValue)) {
      joltErrors++;
    } else if (filledQueuePercentage > 90 && !stillOnFloor(gValue)) {
      joltErrors++;
    }
    if (joltErrors > ALLOWED_ERRORS_NUMBER_THRESHOLD) {
      queue.clear();
      hugeHitsCount = 0;
    }
    return filledQueuePercentage > 95 && hugeHitsCount != 0;
  }

  boolean checkForJolt(float accX, float accY, float accZ) {

    double gValue = calculateGValue(accX, accY, accZ);
    long filledQueuePercentage = queue.getTimeElapsedSinceStartPercents();
    if (filledQueuePercentage<95)
    Log.v(TAG, "current vector: " + gValue + ", state: " + joltState.name() + " errors: "+joltErrors + " filled % " +
        filledQueuePercentage);
    if (joltState == JoltState.FREE_FALL) {
      if (!isFreeFall(gValue)) {
        if (filledQueuePercentage > 20) {
          joltState = JoltState.JUMPING_BACK_AND_FORCE;
        } else {
          joltErrors++;
        }
      }
    } else if (joltState == JoltState.JUMPING_BACK_AND_FORCE) {
      if (!isJumpingBackAndForce2(gValue) ) {
        if (filledQueuePercentage > 31) {
          joltState = JoltState.STILL_ON_FLOOR;
        } else {
          joltErrors++;
        }
      }
    } else if (joltState == JoltState.FREE_FALL_AGAIN) {
      if (!isFreeFall(gValue)) {
        if (filledQueuePercentage > 29) {
          joltState = JoltState.JUMP_AGAIN;
        } else {
          joltErrors++;
        }
      }
    } else if (joltState == JoltState.JUMP_AGAIN) {
      if (!isSmallJumping(gValue)) {
        if (filledQueuePercentage > 31) {
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
    if (joltErrors > ALLOWED_ERRORS_NUMBER_THRESHOLD) {
      queue.clear();
      hugeHitsCount = 0;
      joltErrors = 0;
      joltState=JoltState.FREE_FALL;
    }
    if (filledQueuePercentage > 95 && hugeHitsCount != 0){
      hugeHitsCount = 0;
      joltErrors = 0;
      queue.prepareSample();
      return true;
    } else {
      return false;
    }
  }

  private boolean isJumpingBackAndForce2(double gValue) {
    if (gValue > G_POINT_MIN && gValue < G_POINT_MAX) {
      hugeHitsCount++;
    }
    return !stillOnFloor(gValue);
  }

  private boolean isSmallJumping(double gValue) {
    if (gValue > G_POINT_MAX || gValue < G_POINT_MIN_SMALL) {
      return false;
    } else if (gValue > G_POINT_MIN_SMALL && gValue < G_POINT_MAX) {
      hugeHitsCount++;
    }
    return true;
  }


  private boolean stillOnFloor(double gValue) {
    return gValue >= (1 - EPSILON) * GRAVITY && gValue <= (1 + EPSILON) * GRAVITY;
  }

  private boolean isJumpingBackAndForce(double gValue) {
    if (gValue > G_POINT_MAX || gValue < G_POINT_MIN) {
      return false;
    } else if (gValue > G_POINT_MIN && gValue < G_POINT_MAX) {
      hugeHitsCount++;
    }
    return true;
  }

  private boolean isFreeFall(double gValue) {
    return gValue >= 0 && gValue <= FREE_FALL_LIMIT * GRAVITY;
  }

  static double calculateGValue(float accX, float accY, float accZ) {
    double sum = (accX * accX) + (accY * accY) + (accZ * accZ);
    return Math.sqrt(sum);
  }

  private enum JoltState {
    FREE_FALL,
    JUMPING_BACK_AND_FORCE,
    FREE_FALL_AGAIN,
    JUMP_AGAIN,
    STILL_ON_FLOOR;
  }
}
