package com.alibaba.dubbo.performance.demo.agent;

import java.util.concurrent.atomic.AtomicLong;

public class ThroughTimeRecord {

    private AtomicLong throughTimeSum=new AtomicLong(0);
    private AtomicLong count=new AtomicLong(0);

    public ThroughTimeRecord(){
    }

    public void Update(long ttInMS) {
        throughTimeSum.addAndGet(ttInMS);
        count.getAndIncrement();
    }

    public long GetAverageThroughtTime() {
        long sum = throughTimeSum.get();
        long countLong= count.get();

        if(countLong == 0){
            // it is a new host, just use it;
            return -1;
        }

        return sum/countLong;
    }
}
