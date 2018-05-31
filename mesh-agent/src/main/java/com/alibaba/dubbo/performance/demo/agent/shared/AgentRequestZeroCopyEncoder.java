package com.alibaba.dubbo.performance.demo.agent.shared;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class AgentRequestZeroCopyEncoder extends MessageToByteEncoder {
    // magic header.
    protected static final short MAGIC = (short) 0xaf99;
    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf buffer) throws Exception {
        //System.out.println("encode................");
        AgentRequest req = (AgentRequest)msg;

        buffer.writeShort(MAGIC);
        buffer.writeLong(req.getRequestId());
        if(req.IsRequest){
            buffer.writeByte(0x0f);
            buffer.writeInt(req.getHttpContent().readableBytes());
            buffer.writeBytes(req.getHttpContent());
        }else{
            buffer.writeByte(0);
            buffer.writeInt(4);
            buffer.writeInt(req.getResult());
        }
    }
}
