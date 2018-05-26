package com.alibaba.dubbo.performance.demo.agent.consumer;

import java.util.concurrent.atomic.AtomicLong;

public class ThroughTimeRecord {

    private AtomicLong throughTimeSum=new AtomicLong(0);
    private AtomicLong count=new AtomicLong(0);
    private AtomicLong currentConnectionNumber = new AtomicLong(0);

    public ThroughTimeRecord(){
    }

    public void Update(long ttInMS) {
        throughTimeSum.addAndGet(ttInMS);
        count.getAndIncrement();
    }

    public long EstimateCapacitry() {
        // no lock, can bear the inconsistency
        long ttSum = throughTimeSum.get();
        long countLong= count.get();
        long currentCon = currentConnectionNumber.get();

        if(countLong == 0){
            // it is a new host, we believe it has very large capacity
            return 0;
        }

        return -ttSum * currentCon/countLong;
    }

    public long Connect(){
        return currentConnectionNumber.incrementAndGet();
    }

    public long DisConnect(){
        return currentConnectionNumber.decrementAndGet();
    }

    @Override
    public String toString() {
        return "{" +
                "ttSum=" + throughTimeSum +
                ", con=" + count +
                ", curCon=" + currentConnectionNumber +
                ", cap="+this.EstimateCapacitry()+
                '}';
    }
}
