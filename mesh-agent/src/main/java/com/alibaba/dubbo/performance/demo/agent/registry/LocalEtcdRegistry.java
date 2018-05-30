package com.alibaba.dubbo.performance.demo.agent.registry;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.Arrays;
import java.util.List;

public class LocalEtcdRegistry implements IRegistry {
    @Override
    public void register(String serviceName, int port,int weight) throws Exception {
        throw new NotImplementedException();
    }

    @Override
    public List<Endpoint> find(String serviceName) throws Exception {
        return Arrays.asList(new Endpoint("127.0.0.1",30000,1));
    }
}
