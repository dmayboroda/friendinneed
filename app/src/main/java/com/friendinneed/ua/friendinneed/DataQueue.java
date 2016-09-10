package com.friendinneed.ua.friendinneed;

import android.support.annotation.NonNull;

import com.friendinneed.ua.friendinneed.model.DataSample;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

class DataQueue {
    private static AtomicBoolean isDumping = new AtomicBoolean();
    private static long timeout;
    private static final LinkedList<DataSample> dataQueue = new LinkedList<>();

    DataQueue(long timeout) {
        DataQueue.timeout = timeout;
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
            dataQueue.addLast(sample);
            while (dataQueue.getFirst().getTimestamp() < (sample.getTimestamp() - timeout)) {
                dataQueue.removeFirst();
            }
        }
    }

    public int size() {
        return dataQueue.size();
    }
}
