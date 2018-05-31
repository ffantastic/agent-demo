package com.alibaba.dubbo.performance.demo.agent.dubbo;

import com.alibaba.dubbo.performance.demo.agent.consumer.ConsumerAgentBackendHandler;
import com.alibaba.dubbo.performance.demo.agent.dubbo.model.RpcFuture;
import com.alibaba.dubbo.performance.demo.agent.dubbo.model.RpcRequestHolder;
import com.alibaba.dubbo.performance.demo.agent.dubbo.model.RpcResponse;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcClientHandler extends SimpleChannelInboundHandler<RpcResponse> {

    private Logger logger = LoggerFactory.getLogger(RpcClientHandler.class);
    private ConnecManager conncMgr;
    
    public RpcClientHandler(ConnecManager conncMgr){
        this.conncMgr=conncMgr;
    }
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcResponse response) {
        this.conncMgr.FinishProviderForwardingAndWriteResponse(response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        //cause.printStackTrace();
        logger.error("exception caught in RpcClientHandler",cause);
    }
}
