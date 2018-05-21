package com.alibaba.dubbo.performance.demo.agent;

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
                record = new ThroughTimeRecord();
                ttrMap.put(key,record);

                // this operation is not frequent
                synchronized (this){
                    keys.add(key);
                }
            }

            record.Update(ttInMS);
    }

    public String GetHost(){
        if(keys.size() == 0){
            return null;
        }
        String key_minTT=keys.get(0);
        String key_secMinTT =keys.get(0);
        long minTT = ttrMap.get(key_minTT).GetAverageThroughtTime();
        long secMinTT = ttrMap.get(key_secMinTT).GetAverageThroughtTime();
        StringBuilder debug = new StringBuilder(key_minTT+":"+minTT);
        for(int i=1;i<keys.size();i++){
            String key = keys.get(i);
            long tt =  ttrMap.get(key).GetAverageThroughtTime();
            debug.append(","+key+":"+tt);
            if(tt<minTT){
                minTT = tt;
                key_minTT = key;
            }else if(tt<secMinTT){
                secMinTT = tt;
                key_secMinTT = key;
            }
        }

        if(key_minTT == key_secMinTT){
            debug.append(", win:"+key_minTT);
            logger.info(debug.toString());
            return key_minTT;
        }

        // p = 0.8 to use host with min through time;
        if(random.nextInt(10)<=8){
            debug.append(", win:"+key_minTT);
            logger.info(debug.toString());
            return key_minTT;
        }
        debug.append(", win:"+key_secMinTT);
        logger.info(debug.toString());
        return key_secMinTT;
    }
}
