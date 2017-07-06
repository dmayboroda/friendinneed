package com.friendinneed.ua.friendinneed;

import com.friendinneed.ua.friendinneed.model.AccelerometerDataSample;
import com.friendinneed.ua.friendinneed.model.DataSample;
import com.friendinneed.ua.friendinneed.model.GyroscopeDataSample;

import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.friendinneed.ua.friendinneed.model.DataSample.DATA_SAMPLE_TYPE_ACCELEROMETER;
import static com.friendinneed.ua.friendinneed.model.DataSample.DATA_SAMPLE_TYPE_GYROSCOPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class DataQueueUnitTest {
    private static final int QUEUE_TIMEOUT_TWO_SEC_IN_MILLIS = 1000;
    private static final double DOUBLE_DELTA = 0.0d;
    private final float initAccX = 0.0001f;
    private final float initAccY = 0.0002f;
    private final float initAccZ = 0.0003f;
    private final float initGyroX = 0.0004f;
    private final float initGyroY = 0.0005f;
    private final float initGyroZ = 0.0006f;
    private DataQueue queue;

    @Test
    public void queueAddAccelerometerElement_isCorrect() throws Exception {
        queue = new DataQueue(QUEUE_TIMEOUT_TWO_SEC_IN_MILLIS);
        float accX = initAccX;
        float accY = initAccY;
        float accZ = initAccZ;
        final DataSample sampleAccelerometer = new AccelerometerDataSample(accX, accY, accZ);
        queue.addSample(sampleAccelerometer);
        assertEquals(queue.size(), 1);
        assertEquals(queue.dump()[0].getDataType(), DATA_SAMPLE_TYPE_ACCELEROMETER);
        assertNotEquals(queue.dump()[0].getDataType(), DATA_SAMPLE_TYPE_GYROSCOPE);
        assertEquals(AccelerometerDataSample.fromDataSample(queue.dump()[0]).getAccX(), accX, DOUBLE_DELTA);
        assertEquals(AccelerometerDataSample.fromDataSample(queue.dump()[0]).getAccY(), accY, DOUBLE_DELTA);
        assertEquals(AccelerometerDataSample.fromDataSample(queue.dump()[0]).getAccZ(), accZ, DOUBLE_DELTA);
    }

    @Test
    public void queueAddGyroscopeElement_isCorrect() throws Exception {
        queue = new DataQueue(QUEUE_TIMEOUT_TWO_SEC_IN_MILLIS);
        float gyroX = initGyroX;
        float gyroY = initGyroY;
        float gyroZ = initGyroZ;
        final DataSample sampleGyroscope = new GyroscopeDataSample(gyroX, gyroY, gyroZ);
        queue.addSample(sampleGyroscope);
        assertEquals(queue.size(), 1);
        assertEquals(queue.dump()[0].getDataType(), DATA_SAMPLE_TYPE_GYROSCOPE);
        assertNotEquals(queue.dump()[0].getDataType(), DATA_SAMPLE_TYPE_ACCELEROMETER);
        assertEquals(GyroscopeDataSample.fromDataSample(queue.dump()[0]).getGyroX(), gyroX, DOUBLE_DELTA);
        assertEquals(GyroscopeDataSample.fromDataSample(queue.dump()[0]).getGyroY(), gyroY, DOUBLE_DELTA);
        assertEquals(GyroscopeDataSample.fromDataSample(queue.dump()[0]).getGyroZ(), gyroZ, DOUBLE_DELTA);
    }

    @Test
    public void queueTimeout_isCorrect() throws Exception {
        queue = new DataQueue(QUEUE_TIMEOUT_TWO_SEC_IN_MILLIS);
        float gyroX = initGyroX;
        float gyroY = initGyroY;
        float gyroZ = initGyroZ;
        float accX = initAccX;
        float accY = initAccY;
        float accZ = initAccZ;
        final DataSample sampleGyroscope = new GyroscopeDataSample(gyroX, gyroY, gyroZ);
        queue.addSample(sampleGyroscope);
        assertEquals(queue.size(), 1);
        Thread.sleep(QUEUE_TIMEOUT_TWO_SEC_IN_MILLIS + 1);
        final DataSample sampleAccelerometer = new AccelerometerDataSample(accX, accY, accZ);
        queue.addSample(sampleAccelerometer);
        assertEquals(queue.size(), 1);
        assertEquals(queue.dump()[0].getDataType(), DATA_SAMPLE_TYPE_ACCELEROMETER);
        assertNotEquals(queue.dump()[0].getDataType(), DATA_SAMPLE_TYPE_GYROSCOPE);
        assertEquals(AccelerometerDataSample.fromDataSample(queue.dump()[0]).getAccX(), accX, DOUBLE_DELTA);
        assertEquals(AccelerometerDataSample.fromDataSample(queue.dump()[0]).getAccY(), accY, DOUBLE_DELTA);
        assertEquals(AccelerometerDataSample.fromDataSample(queue.dump()[0]).getAccZ(), accZ, DOUBLE_DELTA);
    }

    @Test
    public void queueTimeoutAdvanced_isCorrect() throws Exception {
        queue = new DataQueue(QUEUE_TIMEOUT_TWO_SEC_IN_MILLIS);
        float gyroX = initGyroX;
        float gyroY = initGyroY;
        float gyroZ = initGyroZ;
        float accX = initAccX;
        float accY = initAccY;
        float accZ = initAccZ;

        for (int i = 0; i < 1000; i++) {
            if (i % 2 == 0) {
                final DataSample sampleGyroscope = new GyroscopeDataSample(gyroX, gyroY, gyroZ);
                queue.addSample(sampleGyroscope);
                gyroX *= 2;
                gyroY *= 2;
                gyroZ *= 2;
            } else {
                final DataSample sampleAccelerometer = new AccelerometerDataSample(accX, accY, accZ);
                queue.addSample(sampleAccelerometer);
                accX *= 2;
                accY *= 2;
                accZ *= 2;
            }
        }

        Thread.sleep(3000);
        final DataSample sampleAccelerometer = new AccelerometerDataSample(initAccX, initAccY, initAccZ);
        queue.addSample(sampleAccelerometer);
        assertEquals(queue.size(), 1);
        assertEquals(queue.dump()[0].getDataType(), DATA_SAMPLE_TYPE_ACCELEROMETER);
        assertNotEquals(queue.dump()[0].getDataType(), DATA_SAMPLE_TYPE_GYROSCOPE);
        assertEquals(AccelerometerDataSample.fromDataSample(queue.dump()[0]).getAccX(), initAccX, DOUBLE_DELTA);
        assertEquals(AccelerometerDataSample.fromDataSample(queue.dump()[0]).getAccY(), initAccY, DOUBLE_DELTA);
        assertEquals(AccelerometerDataSample.fromDataSample(queue.dump()[0]).getAccZ(), initAccZ, DOUBLE_DELTA);
    }


    @Test
    public void queueDump_isCorrect() throws Exception {
        queue = new DataQueue(QUEUE_TIMEOUT_TWO_SEC_IN_MILLIS);
        Callable<Boolean> task = new Callable<Boolean>() {
            @Override
            public Boolean call() {
                addElementsToQueue(10000);
                return true;
            }
        };

        addElementsToQueueWithIncrementation(10000);

        List<Callable<Boolean>> tasks = Collections.nCopies(1, task);
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        executorService.invokeAll(tasks);
        DataSample[] samples = queue.dump();

        assertEquals(GyroscopeDataSample.fromDataSample(samples[0]).getGyroX(), initGyroX, DOUBLE_DELTA);
        assertEquals(GyroscopeDataSample.fromDataSample(samples[0]).getGyroY(), initGyroY, DOUBLE_DELTA);
        assertEquals(GyroscopeDataSample.fromDataSample(samples[0]).getGyroZ(), initGyroZ, DOUBLE_DELTA);

        assertEquals(GyroscopeDataSample.fromDataSample(samples[2]).getGyroX(), initGyroX*2, DOUBLE_DELTA);
        assertEquals(GyroscopeDataSample.fromDataSample(samples[2]).getGyroY(), initGyroY*2, DOUBLE_DELTA);
        assertEquals(GyroscopeDataSample.fromDataSample(samples[2]).getGyroZ(), initGyroZ*2, DOUBLE_DELTA);

        Thread.sleep(10000);

        DataSample[] newSamples = queue.dump();

        assertEquals(GyroscopeDataSample.fromDataSample(newSamples[0]).getGyroX(), initGyroX, DOUBLE_DELTA);
        assertEquals(GyroscopeDataSample.fromDataSample(newSamples[0]).getGyroY(), initGyroY, DOUBLE_DELTA);
        assertEquals(GyroscopeDataSample.fromDataSample(newSamples[0]).getGyroZ(), initGyroZ, DOUBLE_DELTA);

        assertEquals(GyroscopeDataSample.fromDataSample(newSamples[2]).getGyroX(), initGyroX*2, DOUBLE_DELTA);
        assertEquals(GyroscopeDataSample.fromDataSample(newSamples[2]).getGyroY(), initGyroY*2, DOUBLE_DELTA);
        assertEquals(GyroscopeDataSample.fromDataSample(newSamples[2]).getGyroZ(), initGyroZ*2, DOUBLE_DELTA);
    }

    private void addElementsToQueueWithIncrementation(int count) {
        addElementsToQueue(count, true);
    }

    private void addElementsToQueue(int count) {
        addElementsToQueue(count, false);
    }

    private void addElementsToQueue(int count, boolean isWithIncremenrartion) {
        float gyroX = initGyroX;
        float gyroY = initGyroY;
        float gyroZ = initGyroZ;
        float accX = initAccX;
        float accY = initAccY;
        float accZ = initAccZ;

        for (int i = 0; i < count; i++) {
            if (i % 2 == 0) {
                final DataSample sampleGyroscope = new GyroscopeDataSample(gyroX, gyroY, gyroZ);
                queue.addSample(sampleGyroscope);
                if (isWithIncremenrartion) {
                    gyroX *= 2;
                    gyroY *= 2;
                    gyroZ *= 2;
                }
            } else {
                final DataSample sampleAccelerometer = new AccelerometerDataSample(accX, accY, accZ);
                queue.addSample(sampleAccelerometer);
                if (isWithIncremenrartion) {
                    accX *= 2;
                    accY *= 2;
                    accZ *= 2;
                }
            }
        }
    }
}