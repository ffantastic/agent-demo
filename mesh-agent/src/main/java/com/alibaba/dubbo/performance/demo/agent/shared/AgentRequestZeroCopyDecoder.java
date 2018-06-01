package com.alibaba.dubbo.performance.demo.agent.shared;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.ReferenceCountUtil;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

public class AgentRequestZeroCopyDecoder extends ByteToMessageDecoder {
    // header length.
    protected static final int HEADER_LENGTH = 15;
    String type ;
    public AgentRequestZeroCopyDecoder(String type){
        this.type=type;
        System.out.println("AgentRequestZeroCopyDecoder"+type);
    }
    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) {
         //System.out.println(type+" decode................");

        try {
            do {
                int savedReaderIndex = byteBuf.readerIndex();
                Object msg = null;
                try {
                    msg = decode2(byteBuf);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
                if (msg == AgentRequestDecoder.DecodeResult.NEED_MORE_INPUT) {
                    byteBuf.readerIndex(savedReaderIndex);
                    // System.out.println(type+" avail: "+byteBuf.readableBytes());
                    break;
                }

                list.add(msg);
            } while (byteBuf.isReadable());
        } finally {
            if (byteBuf.isReadable()) {
                byteBuf.discardReadBytes();
            }
        }

        //list.add(decode2(byteBuf));
    }

    enum DecodeResult {
        NEED_MORE_INPUT, SKIP_INPUT
    }

    private Object decode2(ByteBuf byteBuf) throws Exception {

        int readable = byteBuf.readableBytes();

        if (readable < HEADER_LENGTH) {
            return AgentRequestDecoder.DecodeResult.NEED_MORE_INPUT;
        }

        int headerStarIndex = byteBuf.readerIndex();
//        byte[] magic = new byte[2];
//        magic[0]=header[0];
//        magic[1]=header[1];
//        System.out.println("MAGIC NUMBER\t"+type+" : "+Bytes.byteArrayToHex(magic));

        byteBuf.readerIndex(headerStarIndex+11);
        int len  = byteBuf.readInt();
        int tt = len + HEADER_LENGTH;
        if (readable < tt) {
            return AgentRequestDecoder.DecodeResult.NEED_MORE_INPUT;
        }

        AgentRequest agentRequest = new AgentRequest();
        byteBuf.readerIndex(headerStarIndex+2);
        agentRequest.setRequestId(byteBuf.readLong());
        byte isRequestByte = byteBuf.readByte();
        if (isRequestByte == 0x0f) {
            agentRequest.IsRequest = true;
        }

        byteBuf.readerIndex(headerStarIndex+HEADER_LENGTH);

        if (agentRequest.IsRequest) {
            agentRequest.setHttpContent(byteBuf.readBytes(len));
            agentRequest.DecodeHttpContent();
            // bytebuf from readBytes() or copy() are not derived bytebuf, need to be released
            // http://netty.io/wiki/reference-counted-objects.html
            ReferenceCountUtil.release(agentRequest.getHttpContent());
        } else {
            agentRequest.setResult(byteBuf.readInt());
        }

        return agentRequest;
    }
}
