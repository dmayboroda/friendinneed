package com.friendinneed.ua.friendinneed;

import android.support.annotation.NonNull;

import com.friendinneed.ua.friendinneed.model.DataSample;

import java.util.LinkedList;

class DataQueue {
    private static long timeout;
    private static final LinkedList<DataSample> dataQueue = new LinkedList<>();

    DataQueue(long timeout) {
        DataQueue.timeout = timeout;
    }

    public DataSample[] dump() {
        return dataQueue.toArray(new DataSample[dataQueue.size()]);
    }

    public void addSample(@NonNull final DataSample sample) {

        dataQueue.addLast(sample);

        while (dataQueue.getFirst().getTimestamp() < (sample.getTimestamp() - timeout)) {
            dataQueue.removeFirst();
        }
    }

    public int size() {
        return dataQueue.size();
    }
}
