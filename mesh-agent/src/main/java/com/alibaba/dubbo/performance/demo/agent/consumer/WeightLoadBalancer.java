package com.alibaba.dubbo.performance.demo.agent.consumer;

import com.alibaba.dubbo.performance.demo.agent.registry.Endpoint;

import java.util.List;
import java.util.Random;

public class WeightLoadBalancer {
    private String _1;
    private String _3;
    private String _6;

    private Random random = new Random();

    /**
     * ugly hard code.
     * @param backends
     */

    public WeightLoadBalancer(List<Endpoint> backends){
        if(backends.size() != 3){
            throw new RuntimeException("endpoint size should be 3, actually :"+backends.size());
        }

        for(Endpoint ep  : backends){
            if(ep.getWeight() == 1){
                _1 = MakeKey(ep);
            }else if(ep.getWeight() == 3){
                _3 = MakeKey(ep);
            }else if(ep.getWeight() == 4 ){
                _6= MakeKey(ep);
            }else{
                throw new RuntimeException("Unknown weight "+ep.getWeight()+" :"+ep);
            }
        }
    }

    private String MakeKey(Endpoint ep){
        return ep.getHost()+":"+ep.getPort();
    }

    public String GetHost(){
        int num  = random.nextInt(10);
        if(num ==0 ){
            return _1;
        }else if(num <=3){
            return _3;
        }else{
            return _6;
        }
    }
}
