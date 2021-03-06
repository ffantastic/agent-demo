package com.alibaba.dubbo.performance.demo.agent.provider;

import com.alibaba.dubbo.performance.demo.agent.dubbo.ConnecManager;
import com.alibaba.dubbo.performance.demo.agent.shared.AgentRequest;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


    public class ProviderAgentFrontendHandler extends SimpleChannelInboundHandler<AgentRequest> {
        private Logger logger = LoggerFactory.getLogger(ProviderAgentFrontendHandler.class);
        private ConnecManager conncMgr;

        public ProviderAgentFrontendHandler( ConnecManager connMgr) {
            this.conncMgr = connMgr;
            System.out.println("ProviderAgentFrontendHandler constructor");
        }

//        @Override
//        public void channelActive(ChannelHandlerContext ctx) throws Exception {
//            super.channelActive(ctx);
//            if(conncMgr.SetInboundChannel(ctx)){
//                logger.info("ProviderAgentFrontendHandler successfully set channel to ConnectionManager");
//            }
//        }

        @Override
        protected void channelRead0(ChannelHandlerContext channelHandlerContext, AgentRequest agentRequest) throws Exception {
           // logger.info("Provider agent receive request id : "+agentRequest.getRequestId());

            if(agentRequest.isDecodeFailed()){
                logger.error("Decode failed, request id {}, will send response 0 directly",agentRequest.getRequestId());
                agentRequest.setResult(0);
                agentRequest.IsRequest=false;
                channelHandlerContext.writeAndFlush(agentRequest);
            }else{
                this.conncMgr.ForwardToProvider(agentRequest,channelHandlerContext);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            logger.error("exception caught in provider frontend handler",cause);
        }
    }

