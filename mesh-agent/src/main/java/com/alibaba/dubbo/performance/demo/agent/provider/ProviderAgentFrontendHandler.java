package com.alibaba.dubbo.performance.demo.agent.provider;


import com.alibaba.dubbo.performance.demo.agent.dubbo.model.Bytes;
import com.alibaba.dubbo.performance.demo.agent.shared.AgentRequest;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


    public class ProviderAgentFrontendHandler extends SimpleChannelInboundHandler<AgentRequest> {
        private Logger logger = LoggerFactory.getLogger(ProviderAgentFrontendHandler.class);

        public ProviderAgentFrontendHandler( ) {
            System.out.println("ProviderAgentFrontendHandler constructor");
        }

        @Override
        protected void channelRead0(ChannelHandlerContext channelHandlerContext, AgentRequest agentRequest) throws Exception {
            logger.info("Provider agent receive request id : "+agentRequest.getRequestId());
            System.out.println("keepalive:"+agentRequest.isKeepAlive());
            System.out.println("IsRequest:"+agentRequest.IsRequest);
            System.out.println("parameter:"+agentRequest.getP_parameter());
            System.out.println("ForwardStartTime:"+agentRequest.getForwardStartTime());
            System.out.println("interface:"+agentRequest.getP_interface());
            System.out.println("method:"+agentRequest.getP_method());
            System.out.println("parameterTypesString:"+agentRequest.getP_parameterTypesString());

            int hashcode = agentRequest.getP_parameter().hashCode();
            byte[] result = new byte[4];
            Bytes.int2bytes(hashcode,result,0);
            agentRequest.setResult(result);
            agentRequest.IsRequest=false;
            channelHandlerContext.channel().writeAndFlush(agentRequest);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            logger.error("exception caught in provider frontend handler");
        }
    }

