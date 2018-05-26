package com.alibaba.dubbo.performance.demo.agent.shared;

import com.alibaba.dubbo.performance.demo.agent.dubbo.model.Bytes;
import com.alibaba.dubbo.performance.demo.agent.dubbo.model.RpcResponse;
import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.*;

import java.net.URLDecoder;
import java.nio.charset.Charset;

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE;

public class AgentRequest {
    public static final String Special_Interface="com.alibaba.dubbo.performance.demo.provider.IHelloService";
    public static final String Special_Method = "hash";
    public static final String Special_parameterType="Ljava/lang/String;";
    public boolean IsRequest;
    private long forwardStartTime;
    private long requestId;
    private int p_interfaceCode;
    private int p_parameterTypesStringCode;
    private String p_parameter;
    private int p_methodCode;
    private int result;

    public static AgentRequest BuildFromHttp(FullHttpRequest request) {
        AgentRequest agentRequest = new AgentRequest();
        agentRequest.IsRequest=true;

        ByteBuf content = request.content();
//        System.out.println("headers:");
//        for (Map.Entry<String, String> entry : request.headers().entries()) {
//            System.out.println(entry.getKey() + ";" + entry.getValue());
//        }
//        System.out.println("content:");
        String contentStr = content.toString(Charset.forName("UTF-8"));
        String contentStrDecoded = URLDecoder.decode(contentStr);

        String[] paramterAndValues = contentStrDecoded.split("&");
        for (String item : paramterAndValues) {
            String[] kv = item.split("=");
            if("interface".equals(kv[0])){
                if(Special_Interface.equals(kv[1])){
                    agentRequest.setP_interfaceCode(0x01);
                }else{
                    throw new RuntimeException("you are a bad boy: "+kv[1]);
                }
            }else if("parameterTypesString".equals(kv[0])){
                if(Special_parameterType.equals(kv[1])){
                    agentRequest.setP_parameterTypesStringCode(0x01);
                }else{
                    throw new RuntimeException("you are a bad boy: "+kv[1]);
                }
            }else if("parameter".equals(kv[0])){
                agentRequest.setP_parameter(kv[1]);
            }else if("method".equals(kv[0])){
                if(Special_Method.equals(kv[1])){
                    agentRequest.setP_methodCode(0x01);
                }else {
                    throw new RuntimeException("you are a bad boy: " + kv[1]);
                }
            }else{
                throw new RuntimeException("AgentRequest conversion from HttpRequest is failed, unknown parameter: "+kv[0]);
            }
        }

        //System.out.println("send agent request , parameter: "+agentRequest.getP_parameter()+", hashcode "+agentRequest.getP_parameter().hashCode());

        return agentRequest;
    }

    public static AgentRequest FromDubbo(RpcResponse response) {
        AgentRequest ar=new AgentRequest();
        ar.IsRequest=false;
        ar.setRequestId(Long.parseLong(response.getRequestId()));
        String resultStr = null;//new String(response.getBytes());
        byte[] resultByte = response.getBytes();
        //System.out.println(Bytes.byteArrayToHex(resultByte));
        if(resultByte[0] == 0x0a){
            resultStr=new String(resultByte,1,resultByte.length-2);
        }else{
            resultStr=new String(resultByte);
        }
        ar.setResult(Integer.valueOf(resultStr));

        return ar;
    }

    public static String CodeToInterface(int interfacCode){
        if(interfacCode == 0x01){
            return Special_Interface;
        }

        return null;
    }

    public static String CodeToMethod(int methodCode){
        if(methodCode == 0x01){
            return Special_Method;
        }

        return null;
    }

    public static String CodeToParamterType(int parameterTypeCode){
        if(parameterTypeCode == 0x01){
            return Special_parameterType;
        }

        return null;
    }
    public  DefaultFullHttpResponse ConvertToHttp(){
        String resultString = String.valueOf(result);
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.ACCEPTED, Unpooled.wrappedBuffer(resultString.getBytes()));
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

    public int getP_interfaceCode() {
        return p_interfaceCode;
    }

    public void setP_interfaceCode(int p_interfaceCode) {
        this.p_interfaceCode = p_interfaceCode;
    }

    public int getP_parameterTypesStringCode() {
        return p_parameterTypesStringCode;
    }

    public void setP_parameterTypesStringCode(int p_parameterTypesStringCode) {
        this.p_parameterTypesStringCode = p_parameterTypesStringCode;
    }

    public String getP_parameter() {
        return p_parameter;
    }

    public void setP_parameter(String p_parameter) {
        this.p_parameter = p_parameter;
    }


    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }

    public int getP_methodCode() {
        return p_methodCode;
    }

    public void setP_methodCode(int p_methodCode) {
        this.p_methodCode = p_methodCode;
    }
}
