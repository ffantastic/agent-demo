package com.alibaba.dubbo.performance.demo.agent.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class LocalLoadBalancer {
    private Logger logger = LoggerFactory.getLogger(LocalLoadBalancer.class);
    private static LocalLoadBalancer holder=null;
    private static Object lock=new Object();
    private ConcurrentHashMap<String,ThroughTimeRecord> ttrMap;
    private List<String> keys ;
    private Random random;

    private LocalLoadBalancer(){
        ttrMap=new ConcurrentHashMap<>();
        keys = new ArrayList<>();
        random = new Random();
    }

    public static LocalLoadBalancer GetInstance(){
        if(holder==null){
            synchronized (lock){
                if(holder == null){
                    holder = new LocalLoadBalancer();
                }
            }
        }

        return holder;
    }

    public void UpdateTTR(String key,long ttInMS){
            ThroughTimeRecord record = ttrMap.get(key);
            if(record == null){
                // this operation is not frequent
                synchronized (this){
                    record = ttrMap.get(key);
                    if(record == null){
                        record = new ThroughTimeRecord();
                        record.Connect();
                        ttrMap.put(key,record);
                        keys.add(key);
                    }
                }
            }

            record.DisConnect();
            record.Update(ttInMS);
    }

    public String GetHost(){
        if(keys.size() == 0){
            return null;
        }
        String key_maxCap= keys.get(0);
        ThroughTimeRecord val_maxCap = ttrMap.get(key_maxCap);
        long maxCap = val_maxCap.EstimateCapacitry();
        //StringBuilder debug = new StringBuilder(keys.get(0)+":"+val_maxCap);

        for(int i=1;i<keys.size();i++){
            String key_record =  keys.get(i);
            ThroughTimeRecord val_record = ttrMap.get(key_record);
            long cap =  val_record.EstimateCapacitry();
            //debug.append(","+keys.get(i)+":"+val_record);
            if(cap>maxCap){
                maxCap = cap;
                key_maxCap = key_record;
                val_maxCap = val_record;
            }
        }

        // p = 0.7 to use host with min through time;
        if(random.nextInt(10)<=6){
            //debug.append(", win:"+key_maxCap);
           // logger.info(debug.toString());
            val_maxCap.Connect();
            return key_maxCap;
        }

        int selectedIndex = random.nextInt(keys.size());
       // debug.append(", win:"+keys.get(selectedIndex));
        ttrMap.get(keys.get(selectedIndex)).Connect();
       // logger.info(debug.toString());
        return keys.get(selectedIndex);
    }

    public String GetRandomHost(){
        if(keys.size() == 0){
            return null;
        }

        return keys.get(random.nextInt(keys.size()));
    }
}
