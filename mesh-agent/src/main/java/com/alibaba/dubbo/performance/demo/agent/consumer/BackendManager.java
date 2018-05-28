package com.alibaba.dubbo.performance.demo.agent.consumer;

import com.alibaba.dubbo.performance.demo.agent.registry.Endpoint;
import com.alibaba.dubbo.performance.demo.agent.registry.EtcdRegistry;
import com.alibaba.dubbo.performance.demo.agent.registry.IRegistry;
import com.alibaba.dubbo.performance.demo.agent.registry.LocalEtcdRegistry;
import com.alibaba.dubbo.performance.demo.agent.shared.AgentRequest;
import io.netty.channel.*;
import io.netty.handler.codec.http.FullHttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

public class BackendManager {
    private Logger logger = LoggerFactory.getLogger(BackendManager.class);

    private IRegistry registry =  new EtcdRegistry(System.getProperty("etcd.url"));//new LocalEtcdRegistry();//

    private Map<String, BackendConnection> backendConnectionMap = new HashMap<>();

    private AtomicLong idGen = new AtomicLong(0);

    private ThreadLocal<Map<Long,ForwardMetaInfo>> localForwardingReq  = new ThreadLocal<Map<Long,ForwardMetaInfo>>(){

        @Override
        protected Map<Long, ForwardMetaInfo> initialValue() {
            return new HashMap<>();
        }
    };

   // private ConcurrentHashMap<Long, ForwardMetaInfo> forwardingReq = new ConcurrentHashMap<>();

    private LocalLoadBalancer loadBalancer;

    public void Init(EventLoopGroup eventloopGroup) throws Exception {
        logger.info("BackendManager initialization start.");

        this.loadBalancer = LocalLoadBalancer.GetInstance();

        List<Endpoint> endpoints = registry.find("com.alibaba.dubbo.performance.demo.provider.IHelloService");
        for (Endpoint ep : endpoints) {
            String endpointStr = ep.getHost() + ":" + ep.getPort();
            logger.info("BackendManager found and add host: " + endpointStr);
            this.loadBalancer.UpdateTTR(endpointStr, 0);
            // for each provider agent, there are 4 tcp long connections with it.
            BackendConnection backendConnection = new BackendConnection(ep.getHost(), ep.getPort(), 6);
            backendConnection.Init(eventloopGroup,this);
            backendConnectionMap.put(endpointStr, backendConnection);
        }

        CountDownLatch latch = new CountDownLatch(endpoints.size());
        for (Map.Entry<String, BackendConnection> entry : backendConnectionMap.entrySet()) {
            new Thread(() -> {
                logger.info("Bind to backend server: " + entry.getKey());
                entry.getValue().Bind(latch);
            }).start();
        }
        try {
            logger.info("wait for all {} bootstrap to finish binding", endpoints.size());
            latch.await();
        } catch (InterruptedException exception) {
            exception.printStackTrace();
            logger.error(exception.getMessage());
        }

        logger.info("BackendManager initialization succeed!");
    }

    public Long ForwardToBackend(FullHttpRequest request, ChannelHandlerContext inboundChannel) {

        // protocol conversion, if throw a exception, no request id will be assigned.
        AgentRequest agentRequest = AgentRequest.BuildFromHttp(request);

        Long nextId = idGen.incrementAndGet();
        // select a backend
        String backendHostName = this.loadBalancer.GetRandomHost();
        // map next id to meta data about this forwarding, it is to be used for writing response back from backend
        localForwardingReq.get().put(nextId, new ForwardMetaInfo(backendHostName, inboundChannel));

        //forward request
        BackendConnection backend = backendConnectionMap.get(backendHostName);
        agentRequest.setForwardStartTime(System.currentTimeMillis());
        agentRequest.setRequestId(nextId);

        // select a channel from a backend
        backend.SelectChannel(inboundChannel).writeAndFlush(agentRequest);

        return nextId;
    }

    public ForwardMetaInfo FinishBackendForwarding(Long requestId, long throughtTime) {
        Map<Long,ForwardMetaInfo> forwardingReq = localForwardingReq.get();
        ForwardMetaInfo metaInfo = forwardingReq.get(requestId);
        if (metaInfo == null) {
            logger.error("Forward meta information is lost!!! request id: " + requestId);
            return null;
        }

        this.loadBalancer.UpdateTTR(metaInfo.forwardHost, throughtTime);
        forwardingReq.remove(requestId);

        return metaInfo;
    }

    public static class ForwardMetaInfo {
        public String forwardHost;
        public ChannelHandlerContext inboundChannel;


        public ForwardMetaInfo(String host, ChannelHandlerContext channel) {
            this.forwardHost = host;
            this.inboundChannel = channel;
        }
    }
}
