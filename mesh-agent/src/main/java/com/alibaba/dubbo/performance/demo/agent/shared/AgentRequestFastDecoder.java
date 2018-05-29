package com.alibaba.dubbo.performance.demo.agent.shared;

import com.alibaba.dubbo.performance.demo.agent.dubbo.model.Bytes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

public class AgentRequestFastDecoder extends ByteToMessageDecoder {
    // header length.
    protected static final int HEADER_LENGTH = 15;
    String type ;
    public AgentRequestFastDecoder(String type){
        this.type=type;
        System.out.println("AgentRequestDecoder"+type);
    }
    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) {
       // System.out.println(type+" decode................");

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
                if (msg == AgentRequestFastDecoder.DecodeResult.NEED_MORE_INPUT) {
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
            return AgentRequestFastDecoder.DecodeResult.NEED_MORE_INPUT;
        }

        byte[] header = new byte[HEADER_LENGTH];
        byteBuf.readBytes(header);

//        byte[] magic = new byte[2];
//        magic[0]=header[0];
//        magic[1]=header[1];
//        System.out.println("MAGIC NUMBER\t"+type+" : "+Bytes.byteArrayToHex(magic));

        if(header[0] != (byte) 0xaf || header[1] != (byte)0x99){
            System.out.println("!!!Wrong Magic!!!");
        }

        byte[] dataLen = Arrays.copyOfRange(header, 11, 15);
        int len = Bytes.bytes2int(dataLen);
        int tt = len + HEADER_LENGTH;
        if (readable < tt) {
            return AgentRequestFastDecoder.DecodeResult.NEED_MORE_INPUT;
        }

        int decodeEndIndex = byteBuf.readerIndex()+len;

        AgentRequest agentRequest = new AgentRequest();
        if (header[10] == 0x0f) {
            agentRequest.IsRequest = true;
        }

        agentRequest.setRequestId(Bytes.bytes2long(header, 2));

        agentRequest.setForwardStartTime(byteBuf.readLong());

        if (agentRequest.IsRequest) {
            // slice-produced bytebuf doesn't need to be released.
            byte[] bb = new byte[len-8];
           byteBuf.readBytes(bb);
            System.out.println(agentRequest.getRequestId() + ":"+Bytes.byteArrayToHex(bb));
            //agentRequest.setHttpContent( byteBuf.slice(byteBuf.readerIndex(),len-8));
            agentRequest.setHttpContent(Unpooled.wrappedBuffer(bb));
            agentRequest.DecodeHttpContent();
            byteBuf.readerIndex(decodeEndIndex);
        } else {
            agentRequest.setResult(byteBuf.readInt());
        }

        return agentRequest;
    }
}
