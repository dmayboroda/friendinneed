package com.friendinneed.ua.friendinneed;

import android.support.annotation.NonNull;

import com.friendinneed.ua.friendinneed.model.DataSample;

import java.util.LinkedList;

class DataQueue {
    private static long timeout;
    private static final LinkedList<DataSample> dateQueue = new LinkedList<>();

    DataQueue(long timeout) {
        DataQueue.timeout = timeout;
    }

    public LinkedList<DataSample> dump() {
        return dateQueue;
    }

    public void addSample(@NonNull final DataSample sample) {

        dateQueue.addLast(sample);

        while (dateQueue.getFirst().getTimestamp() < (sample.getTimestamp() - timeout)) {
            dateQueue.removeFirst();
        }
    }

    public int size() {
        return dateQueue.size();
    }
}
