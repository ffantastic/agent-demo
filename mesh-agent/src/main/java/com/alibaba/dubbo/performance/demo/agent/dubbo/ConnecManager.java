package com.alibaba.dubbo.performance.demo.agent.dubbo;

import com.alibaba.dubbo.performance.demo.agent.consumer.BackendConnection;
import com.alibaba.dubbo.performance.demo.agent.dubbo.model.*;
import com.alibaba.dubbo.performance.demo.agent.registry.EtcdRegistry;
import com.alibaba.dubbo.performance.demo.agent.registry.IRegistry;
import com.alibaba.dubbo.performance.demo.agent.registry.LocalEtcdRegistry;
import com.alibaba.dubbo.performance.demo.agent.shared.AgentRequest;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class ConnecManager {

    private Logger logger = LoggerFactory.getLogger(ConnecManager.class);

    private IRegistry registry =new EtcdRegistry(System.getProperty("etcd.url"));//new LocalEtcdRegistry();//

    //private Bootstrap bootstrap;

    private BackendConnection backendConnection;
    //private Channel channel;

    private AtomicReference<ChannelHandlerContext> inboundChannel = new AtomicReference<>(null);

    private ConcurrentHashMap<Long, Long> upstreamMetaMap = new ConcurrentHashMap<>();

    public ConnecManager() {
    }

    public void Init(EventLoopGroup eventloopGroup) throws Exception {
        logger.info("ConnecManager initialization start.");
        int port = Integer.valueOf(System.getProperty("dubbo.protocol.port"));
        backendConnection = new BackendConnection("127.0.0.1",port,1);
        backendConnection.Init(eventloopGroup,new RpcClientInitializer(this));
        final CountDownLatch latch =  new CountDownLatch(1);
        backendConnection.Bind(latch);
        latch.await();


//        bootstrap = new Bootstrap()
//                .group(eventloopGroup)
//                .option(ChannelOption.SO_KEEPALIVE, true)
//                .option(ChannelOption.TCP_NODELAY, true)
//                .option(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator.DEFAULT)
//                .channel(NioSocketChannel.class)
//                .handler(new RpcClientInitializer(this));
//
//
//        boolean bindSuccess = false;
//        while(!bindSuccess) {
//            logger.info("try to bind localhost: "+port);
//            try {
//                channel = bootstrap.connect("127.0.0.1", port).sync().channel();
//                bindSuccess=true;
//            }catch (Exception ex){
//                ex.printStackTrace();
//                logger.error("binding to port "+port+" failed, try again after 50ms");
//                Thread.sleep(50);
//            }
//        }
    }

    public boolean SetInboundChannel(ChannelHandlerContext inbound){
        return inboundChannel.compareAndSet(null,inbound);
    }

    public void ForwardToProvider(AgentRequest request){
        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName(AgentRequest.CodeToMethod(request.getP_methodCode()));
        invocation.setAttachment("path", AgentRequest.CodeToInterface(request.getP_interfaceCode()));
        invocation.setParameterTypes(AgentRequest.CodeToParamterType(request.getP_parameterTypesStringCode()));    // Dubbo内部用"Ljava/lang/String"来表示参数类型是String

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out));
        try {
            JsonUtils.writeObject(request.getP_parameter(), writer);
            invocation.setArguments(out.toByteArray());
        }catch (Exception ex){
            logger.error("ForwardToProvider: Exception while setting req[{}] parameter:{}, will return empty response ",request.getRequestId(),request.getP_parameter());
            this.FinishProviderForwardingAndWriteResponse(RpcResponse.EmptyReponse(request.getRequestId()));
            return;
        }
        Request req = new Request();
        req.setVersion("2.0.0");
        req.setTwoWay(true);
        req.setData(invocation);
        req.setId(request.getRequestId());

        logger.info("requestId=" + req.getId());

        upstreamMetaMap.put(req.getId(),request.getForwardStartTime());
        backendConnection.SelectChannel().writeAndFlush(req);
    }

    public void FinishProviderForwardingAndWriteResponse(RpcResponse response) {
        long requestId = Long.parseLong(response.getRequestId());
        Long metaInfo = this.upstreamMetaMap.get(requestId);
        if (metaInfo == null) {
            logger.error("Forward meta information is lost!!! request id: " + requestId);
            return;
        }

        this.upstreamMetaMap.remove(requestId);

        AgentRequest agent = AgentRequest.FromDubbo(response);
        agent.setForwardStartTime(metaInfo);

        inboundChannel.get().writeAndFlush(agent);
    }

}
