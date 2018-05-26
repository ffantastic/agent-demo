package com.alibaba.dubbo.performance.demo.agent.consumer;

import com.alibaba.dubbo.performance.demo.agent.shared.AgentRequest;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE;

public class ConsumerAgentBackendHandler extends SimpleChannelInboundHandler<AgentRequest> {
    private static boolean isKeepAlive=false;
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
            // System.out.println("result: "+agentRequest.getResult());
            DefaultHttpResponse response = agentRequest.ConvertToHttp();
            response.headers().setInt(CONTENT_LENGTH, ((DefaultFullHttpResponse) response).content().readableBytes());
            if(isKeepAlive){
                response.headers().set(CONNECTION, KEEP_ALIVE);
                inboundChannel.writeAndFlush(response);
            }else{
                inboundChannel.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            }
        }
    }
}
