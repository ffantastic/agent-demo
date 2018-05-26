package com.alibaba.dubbo.performance.demo.agent.consumer;

import com.alibaba.dubbo.performance.demo.agent.dubbo.model.Bytes;
import com.alibaba.dubbo.performance.demo.agent.shared.AgentRequest;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class ConsumerAgentBackendHandler extends SimpleChannelInboundHandler<AgentRequest> {

    private BackendManager bm;

    public ConsumerAgentBackendHandler(BackendManager backendManager) {
        System.out.println("ConsumerAgentBackendHandler constructor");
        this.bm = backendManager;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, AgentRequest agentRequest) throws Exception {
        long forwardEndTime = System.currentTimeMillis();
        long requestId = agentRequest.getRequestId();
        BackendManager.ForwardMetaInfo metaInfo = bm.FinishBackendForwarding(requestId,forwardEndTime-agentRequest.getForwardStartTime());
        if(metaInfo != null){
            Channel inboundChannel = metaInfo.inboundChannel;
            System.out.println("result: "+Bytes.byteArrayToHex(agentRequest.getResult()));
            inboundChannel.writeAndFlush(agentRequest.ConvertToHttp()).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
