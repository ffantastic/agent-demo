package com.alibaba.dubbo.performance.demo.agent.dubbo;

import com.alibaba.dubbo.performance.demo.agent.dubbo.model.Bytes;
import com.alibaba.dubbo.performance.demo.agent.dubbo.model.JsonUtils;
import com.alibaba.dubbo.performance.demo.agent.dubbo.model.Request;
import com.alibaba.dubbo.performance.demo.agent.dubbo.model.RpcInvocation;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class DubboRpcEncoder extends MessageToByteEncoder{
    private Logger logger = LoggerFactory.getLogger(DubboRpcEncoder.class);
    // header length.
    protected static final int HEADER_LENGTH = 16;
    // magic header.
    protected static final short MAGIC = (short) 0xdabb;
    // message flag.
    protected static final byte FLAG_REQUEST = (byte) 0x80;
    protected static final byte FLAG_TWOWAY = (byte) 0x40;
    protected static final byte FLAG_EVENT = (byte) 0x20;

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf buffer) throws Exception {
        try {
            Request req = (Request) msg;

            // set magic number.
            buffer.writeShort(MAGIC);
            // set request and serialization flag.
            byte flag = (byte) (FLAG_REQUEST | 6);

            if (req.isTwoWay()) flag |= FLAG_TWOWAY;
            if (req.isEvent()) flag |= FLAG_EVENT;

            buffer.writeByte(flag);

            // skip a byte
            buffer.writeByte(0);

            // set request id.
            buffer.writeLong(req.getId());

            // encode request data.
            //int lenStartIndex = buffer.writerIndex();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            encodeRequestData(bos, req.getData());

            int len = bos.size();
            buffer.writeInt(len);
            buffer.writeBytes(bos.toByteArray());

        }catch (Throwable cause){
            logger.error("exception caught in encoder ",cause);
        }
    }

    public void encodeRequestData(OutputStream out, Object data) throws Exception {
        RpcInvocation inv = (RpcInvocation)data;

        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out));

        JsonUtils.writeObject(inv.getAttachment("dubbo", "2.0.1"), writer);
        JsonUtils.writeObject(inv.getAttachment("path"), writer);
        JsonUtils.writeObject(inv.getAttachment("version"), writer);
        JsonUtils.writeObject(inv.getMethodName(), writer);
        JsonUtils.writeObject(inv.getParameterTypes(), writer);

        JsonUtils.writeBytes(inv.getArguments(), writer);
        JsonUtils.writeObject(inv.getAttachments(), writer);
    }

}
