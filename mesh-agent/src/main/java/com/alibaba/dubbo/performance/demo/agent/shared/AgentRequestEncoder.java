package com.alibaba.dubbo.performance.demo.agent.shared;

import com.alibaba.dubbo.performance.demo.agent.dubbo.model.Bytes;
import com.alibaba.dubbo.performance.demo.agent.dubbo.model.JsonUtils;

import com.alibaba.dubbo.performance.demo.agent.dubbo.model.RpcInvocation;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class AgentRequestEncoder extends MessageToByteEncoder {
    // header length.
    protected static final int HEADER_LENGTH = 15;
    // magic header.
    protected static final short MAGIC = (short) 0xaf99;
    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf buffer) throws Exception {
        //System.out.println("encode................");
        AgentRequest req = (AgentRequest)msg;

        // header.
        byte[] header = new byte[HEADER_LENGTH];

        // set magic number.
        Bytes.short2bytes(MAGIC, header);

        // set request id.
        Bytes.long2bytes(req.getRequestId(), header, 2);
        if(req.IsRequest) {
            header[10] = 0x0f;
        }

        // encode request data.
        int savedWriteIndex = buffer.writerIndex();
        buffer.writerIndex(savedWriteIndex + HEADER_LENGTH);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        encodeRequestData(bos, req);

        int len = bos.size();
        buffer.writeBytes(bos.toByteArray());
        Bytes.int2bytes(len, header, 11);

        // write
        buffer.writerIndex(savedWriteIndex);
        buffer.writeBytes(header); // write header.
        buffer.writerIndex(savedWriteIndex + HEADER_LENGTH + len);
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
