package com.alibaba.dubbo.performance.demo.agent.consumer;

import com.alibaba.dubbo.performance.demo.agent.registry.Endpoint;
import com.alibaba.dubbo.performance.demo.agent.registry.EtcdRegistry;
import com.alibaba.dubbo.performance.demo.agent.registry.IRegistry;
import com.alibaba.dubbo.performance.demo.agent.registry.LocalEtcdRegistry;
import com.alibaba.dubbo.performance.demo.agent.shared.AgentRequest;
import com.alibaba.dubbo.performance.demo.agent.shared.AgentRequestDecoder;
import com.alibaba.dubbo.performance.demo.agent.shared.AgentRequestEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
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

    private IRegistry registry =new EtcdRegistry(System.getProperty("etcd.url"));// new LocalEtcdRegistry();//

    private Map<String, BackendConnection> backendConnectionMap = new HashMap<>();

    private AtomicLong idGen = new AtomicLong(0);

    private ConcurrentHashMap<Long, ForwardMetaInfo> forwardingReq = new ConcurrentHashMap<>();

    private LocalLoadBalancer loadBalancer;

    public void Init(EventLoopGroup eventloopGroup) throws Exception {
        logger.info("BackendManager initialization start.");

        this.loadBalancer = LocalLoadBalancer.GetInstance();

        List<Endpoint> endpoints = registry.find("com.alibaba.dubbo.performance.demo.provider.IHelloService");
        for (Endpoint ep : endpoints) {
            String endpointStr = ep.getHost() + ":" + ep.getPort();
            logger.info("BackendManager found and add host: " + endpointStr);
            this.loadBalancer.UpdateTTR(endpointStr, 0);
            backendConnectionMap.put(endpointStr, new BackendConnection(ep.getHost(), ep.getPort(), eventloopGroup));
        }

        CountDownLatch latch = new CountDownLatch(endpoints.size());
        for (Map.Entry<String, BackendConnection> entry : backendConnectionMap.entrySet()) {
            new Thread(() -> {
                logger.info("Bind to backend server: " + entry.getKey());
                entry.getValue().Bind(latch);
            }).start();
        }
        try {
            logger.info("wait for all {} connection to finish binding", endpoints.size());
            latch.await();
        } catch (InterruptedException exception) {
            exception.printStackTrace();
            logger.error(exception.getMessage());
        }

        logger.info("BackendManager initialization succeed!");
    }

    public Long ForwardToBackend(FullHttpRequest request, ChannelHandlerContext inboundChannel) {
        Long nextId = idGen.incrementAndGet();
        // select a backend
        String backendHostName = this.loadBalancer.GetHost();
        // map next id to meta data about this forwarding, it is to be used for writing response back from backend
        forwardingReq.put(nextId, new ForwardMetaInfo(backendHostName, inboundChannel));

        // protocol conversion
        AgentRequest agentRequest = AgentRequest.BuildFromHttp(request);

        //forward request
        BackendConnection backend = backendConnectionMap.get(backendHostName);
        agentRequest.setForwardStartTime(System.currentTimeMillis());
        agentRequest.setRequestId(nextId);
        backend.channel.writeAndFlush(agentRequest);

        return nextId;
    }

    public ForwardMetaInfo FinishBackendForwarding(Long requestId, long throughtTime) {
        ForwardMetaInfo metaInfo = this.forwardingReq.get(requestId);
        if (metaInfo == null) {
            logger.error("Forward meta information is lost!!! request id: " + requestId);
            return null;
        }

        this.loadBalancer.UpdateTTR(metaInfo.forwardHost,throughtTime);
        this.forwardingReq.remove(requestId);

        return metaInfo;
    }

    public static class ForwardMetaInfo {
        public String forwardHost;
        public ChannelHandlerContext inboundChannel;


        public ForwardMetaInfo(String host, ChannelHandlerContext channel ) {
            this.forwardHost = host;
            this.inboundChannel = channel;
        }
    }

    private class BackendConnection {
        private String host;
        private int port;

        Bootstrap bootstrap = new Bootstrap();
        Channel channel;

        public BackendConnection(String host, int port, EventLoopGroup eventloopGroup) {
            this.host = host;
            this.port = port;

            this.Init(eventloopGroup);
        }

        public void Init(EventLoopGroup eventloopGroup) {
            bootstrap = bootstrap.group(eventloopGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(
                                    new AgentRequestEncoder(),
                                     new AgentRequestDecoder("Consumer"),
//                                    new ObjectEncoder(),
//                                    new ObjectDecoder(ClassResolvers.cacheDisabled(null)),
                                    new ConsumerAgentBackendHandler(BackendManager.this));
                        }
                    })
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator.DEFAULT);
                    //.option(ChannelOption.AUTO_READ, false);
        }

        public void Bind(final CountDownLatch latch) {
            boolean bindSuccess = false;
            while(!bindSuccess){
                try{
                    logger.info("try to bind localhost: "+port);

                    ChannelFuture f = bootstrap.connect(host, port).sync();
                    channel = f.channel();
                    bindSuccess=true;
                    latch.countDown();
                }catch (Exception ex){
                    logger.error("binding to port "+port+" failed, try again after 50ms");
                    try {
                        Thread.sleep(50);
                    }catch (InterruptedException e){
                        // swallow
                    }
                }
            }

        }
    }

}
