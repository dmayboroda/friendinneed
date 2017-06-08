package com.friendinneed.ua.friendinneed;

import android.support.annotation.NonNull;

import com.friendinneed.ua.friendinneed.model.DataSample;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

class DataQueue {
    private static AtomicBoolean isDumping = new AtomicBoolean();
    private long timeout;
    private final LinkedList<DataSample> dataQueue = new LinkedList<>();
    private final LinkedList<DataSample> dataQueueBuffer = new LinkedList<>();

    DataQueue(long timeout) {
        this.timeout = timeout;
        isDumping.set(false);
    }

    public DataSample[] dump() {
        isDumping.set(true);
        DataSample[] dataSampleArray = dataQueue.toArray(new DataSample[dataQueue.size()]);
        isDumping.lazySet(false);
        return dataSampleArray;
    }

    public void addSample(@NonNull final DataSample sample) {
        if (!isDumping.get()) {
            while (dataQueueBuffer.size() != 0) {
                dataQueue.addLast(dataQueueBuffer.removeFirst());
            }
            dataQueue.addLast(sample);
            while (dataQueue.getFirst().getTimestamp() < (sample.getTimestamp() - timeout)) {
                dataQueue.removeFirst();
            }
        } else {
            dataQueueBuffer.addLast(sample);
        }
    }

    public void clear() {
        dataQueue.clear();
    }

    public int size() {
        return dataQueue.size();
    }

    public long getTimeElapsedSinceStartPercents() {
        return 100 * (dataQueue.getLast().getTimestamp() - dataQueue.getFirst().getTimestamp()) / timeout;
    }
}
