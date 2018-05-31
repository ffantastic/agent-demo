package com.alibaba.dubbo.performance.demo.agent.shared;

import com.alibaba.dubbo.performance.demo.agent.dubbo.ConnecManager;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class AgentRequestZeroCopyEncoder extends MessageToByteEncoder {
    private Logger logger = LoggerFactory.getLogger(AgentRequestZeroCopyEncoder.class);
    // magic header.
    protected static final short MAGIC = (short) 0xaf99;
    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf buffer) throws Exception {
//        try {
            //System.out.println("encode................");
            AgentRequest req = (AgentRequest) msg;

            buffer.writeShort(MAGIC);
            buffer.writeLong(req.getRequestId());
            if (req.IsRequest) {
                buffer.writeByte(0x0f);
                buffer.writeInt(req.getHttpContent().readableBytes());
                buffer.writeBytes(req.getHttpContent());
                ReferenceCountUtil.release(req.getHttpContent());
            } else {
                buffer.writeByte(0);
                buffer.writeInt(4);
                buffer.writeInt(req.getResult());
            }
//        }catch (Throwable cause){
//            logger.error("Exception caught in Encoder ",cause);
//        }
    }
}
