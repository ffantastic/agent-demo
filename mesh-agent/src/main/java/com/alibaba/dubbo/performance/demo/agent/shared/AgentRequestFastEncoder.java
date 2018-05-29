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

    String type ;
    public AgentRequestFastEncoder(String type){
        this.type=type;
        System.out.println("AgentRequestFastEncoder"+type);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf buffer) throws Exception {
        // System.out.println(type+" encode................");
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

        int bodyStartIndex = buffer.writerIndex();
        buffer.writeLong(req.getForwardStartTime());

        if(req.IsRequest){
            // req.getHttpContent() is then called release() automatically
            byte[] content = new byte[req.getHttpContent().readableBytes()];
            req.getHttpContent().readBytes(content);
            System.out.println(req.getRequestId()+":"+Bytes.byteArrayToHex(content));
            // buffer.writeBytes(req.getHttpContent());
            buffer.writeBytes(content);
        }else{
            buffer.writeInt(req.getResult());
        }

        int len = buffer.writerIndex()-bodyStartIndex;
        // System.out.println("encode byte length "+len);
        Bytes.int2bytes(len, header, 11);

        // write
        buffer.writerIndex(savedWriteIndex);
        buffer.writeBytes(header); // write header.
        buffer.writerIndex(savedWriteIndex + HEADER_LENGTH + len);

    }
}
