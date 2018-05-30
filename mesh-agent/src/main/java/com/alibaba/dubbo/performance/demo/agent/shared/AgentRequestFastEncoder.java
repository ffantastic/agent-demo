package com.alibaba.dubbo.performance.demo.agent.shared;

import com.alibaba.dubbo.performance.demo.agent.dubbo.model.Bytes;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class AgentRequestFastEncoder extends MessageToByteEncoder {
    // header length.
    protected static final int HEADER_LENGTH = 15;
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
        }else{
            buffer.writeByte(0);
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        encodeRequestData(bos, req);

        int len = bos.size();

        buffer.writeInt(len);
        buffer.writeBytes(bos.toByteArray());
    }

    public void encodeRequestData(OutputStream out, AgentRequest data) throws Exception {

        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out));
        writer.println(data.getForwardStartTime());
        //writer.println(data.isKeepAlive());

        if(data.IsRequest){
            writer.println(data.getP_interfaceCode());
            writer.println(data.getP_parameterTypesStringCode());
            writer.println(data.getP_parameter());
            writer.println(data.getP_methodCode());
        }else{
            writer.println(data.getResult());
        }

        writer.flush();
    }
}
