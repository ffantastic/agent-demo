package com.alibaba.dubbo.performance.demo.agent.shared;

import com.alibaba.dubbo.performance.demo.agent.dubbo.DubboRpcDecoder;
import com.alibaba.dubbo.performance.demo.agent.dubbo.model.Bytes;
import com.alibaba.dubbo.performance.demo.agent.dubbo.model.RpcResponse;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

public class AgentRequestDecoder extends ByteToMessageDecoder {
    // header length.
    protected static final int HEADER_LENGTH = 15;
    String type ;
    public AgentRequestDecoder(String type){
        this.type=type;
        System.out.println("AgentRequestDecoder"+type);
    }
    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) {
        System.out.println(type+" decode................");

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
                    System.out.println(type+" avail: "+byteBuf.readableBytes());
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

        byte[] header = new byte[HEADER_LENGTH];
        byteBuf.readBytes(header);
        byte[] dataLen = Arrays.copyOfRange(header, 11, 15);
        int len = Bytes.bytes2int(dataLen);
        int tt = len + HEADER_LENGTH;
        if (readable < tt) {
            return AgentRequestDecoder.DecodeResult.NEED_MORE_INPUT;
        }

        AgentRequest agentRequest = new AgentRequest();
        if (header[10] == 0x0f) {
            agentRequest.IsRequest = true;
        }

        agentRequest.setRequestId(Bytes.bytes2long(header, 2));

        InputStream in = new ByteBufInputStream(byteBuf, len - 1);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        agentRequest.setForwardStartTime(Long.parseLong(reader.readLine()));
        agentRequest.setKeepAlive(Boolean.parseBoolean(reader.readLine()));

        if (agentRequest.IsRequest) {
            agentRequest.setP_interface(reader.readLine());
            agentRequest.setP_parameterTypesString(reader.readLine());
            agentRequest.setP_parameter(reader.readLine());
            agentRequest.setP_method(reader.readLine());
        } else {
            agentRequest.setResult(reader.readLine().getBytes());
        }

        in.close();
        return agentRequest;
    }
}
