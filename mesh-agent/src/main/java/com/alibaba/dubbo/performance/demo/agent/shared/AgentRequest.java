package com.alibaba.dubbo.performance.demo.agent.shared;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.*;

import java.net.URLDecoder;
import java.nio.charset.Charset;

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE;

public class AgentRequest {
    public boolean IsRequest;
    private long forwardStartTime;
    private long requestId;
    private String p_interface;
    private String p_parameterTypesString;
    private String p_parameter;
    private String p_method;
    private boolean keepAlive ;
    private byte[] result;

    public static AgentRequest BuildFromHttp(FullHttpRequest request) {
        AgentRequest agentRequest = new AgentRequest();
        agentRequest.IsRequest=true;
        agentRequest.setKeepAlive( HttpUtil.isKeepAlive(request));

        ByteBuf content = request.content();
//        System.out.println("headers:");
//        for (Map.Entry<String, String> entry : request.headers().entries()) {
//            System.out.println(entry.getKey() + ";" + entry.getValue());
//        }
//        System.out.println("content:");
        String contentStr = content.toString(Charset.forName("UTF-8"));
        String contentStrDecoded = URLDecoder.decode(contentStr);
//        System.out.println(contentStrDecoded);
        String[] paramterAndValues = contentStrDecoded.split("&");
        for (String item : paramterAndValues) {
            String[] kv = item.split("=");
            if("interface".equals(kv[0])){
                agentRequest.setP_interface(kv[1]);
            }else if("parameterTypesString".equals(kv[0])){
                agentRequest.setP_parameterTypesString(kv[1]);
            }else if("parameter".equals(kv[0])){
                agentRequest.setP_parameter(kv[1]);
            }else if("method".equals(kv[0])){
                agentRequest.setP_method(kv[1]);
            }else{
                throw new RuntimeException("AgentRequest conversion from HttpRequest is failed, unknown parameter: "+kv[0]);
            }
        }

        return agentRequest;
    }

    public  DefaultFullHttpResponse ConvertToHttp(){

        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.ACCEPTED, Unpooled.wrappedBuffer(result));
//        ctx.writeAndFlush(response);
//        ctx.close();

//        if (!keepAlive) {
//            ctx.write(response).addListener(ChannelFutureListener.CLOSE);
//        } else {
//            response.headers().set(CONNECTION, KEEP_ALIVE);
//            ctx.write(response);
//        }

        return response;
    }

    public long getForwardStartTime() {
        return forwardStartTime;
    }

    public void setForwardStartTime(long forwardStartTime) {
        this.forwardStartTime = forwardStartTime;
    }

    public long getRequestId() {
        return requestId;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    public String getP_interface() {
        return p_interface;
    }

    public void setP_interface(String p_interface) {
        this.p_interface = p_interface;
    }

    public String getP_parameterTypesString() {
        return p_parameterTypesString;
    }

    public void setP_parameterTypesString(String p_parameterTypesString) {
        this.p_parameterTypesString = p_parameterTypesString;
    }

    public String getP_parameter() {
        return p_parameter;
    }

    public void setP_parameter(String p_parameter) {
        this.p_parameter = p_parameter;
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    public byte[] getResult() {
        return result;
    }

    public void setResult(byte[] result) {
        this.result = result;
    }

    public String getP_method() {
        return p_method;
    }

    public void setP_method(String p_method) {
        this.p_method = p_method;
    }
}
