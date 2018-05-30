package com.alibaba.dubbo.performance.demo.agent.dubbo;

import com.alibaba.dubbo.performance.demo.agent.registry.LocalEtcdRegistry;
import com.alibaba.dubbo.performance.demo.agent.shared.BackendConnection;
import com.alibaba.dubbo.performance.demo.agent.dubbo.model.*;
import com.alibaba.dubbo.performance.demo.agent.registry.EtcdRegistry;
import com.alibaba.dubbo.performance.demo.agent.registry.IRegistry;
import com.alibaba.dubbo.performance.demo.agent.shared.AgentRequest;
import com.alibaba.dubbo.performance.demo.agent.shared.RequestCache;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;


public class ConnecManager {

    private Logger logger = LoggerFactory.getLogger(ConnecManager.class);

    private IRegistry registry =new EtcdRegistry(System.getProperty("etcd.url"));// new LocalEtcdRegistry();//
    private BackendConnection backendConnection;
    private RequestCache<UpstreamMetaInfo> upstreamRequestCache = new RequestCache<>();

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
    }

    public void ForwardToProvider(AgentRequest request,ChannelHandlerContext inbound){
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

        // logger.info("requestId=" + req.getId());

        Channel outboundChannel =  backendConnection.SelectChannel(inbound);
        upstreamRequestCache.Cache(req.getId(),new UpstreamMetaInfo(request.getForwardStartTime(),inbound),inbound.channel().eventLoop() == outboundChannel.eventLoop());
        outboundChannel.writeAndFlush(req);
    }

    public void FinishProviderForwardingAndWriteResponse(RpcResponse response) {
        long requestId = Long.parseLong(response.getRequestId());
        UpstreamMetaInfo metaInfo = null;
        try {
            metaInfo = upstreamRequestCache.Remove(requestId);
        } catch (Exception e) {
            logger.error("upstream meta information is lost!!! request id: {}" , requestId,e);
        }

        if (metaInfo == null) {
            return;
        }

        AgentRequest agent = AgentRequest.FromDubbo(response);
        agent.setForwardStartTime(metaInfo.ForwardStartTime);
        metaInfo.InboundChannel.writeAndFlush(agent);
    }

    public static class UpstreamMetaInfo{
        Long ForwardStartTime;
        ChannelHandlerContext InboundChannel;

        public UpstreamMetaInfo(Long forwardStartTime,ChannelHandlerContext inboundChannel){
            this.ForwardStartTime = forwardStartTime;
            this.InboundChannel = inboundChannel;
        }
    }

}
