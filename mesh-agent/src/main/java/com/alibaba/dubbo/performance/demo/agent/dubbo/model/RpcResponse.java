package com.alibaba.dubbo.performance.demo.agent.dubbo.model;

public class RpcResponse {

    private String requestId;
    private byte[] bytes;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public static RpcResponse EmptyReponse(long reqId){
        RpcResponse rpcResponse = new RpcResponse();
        rpcResponse.setRequestId(String.valueOf(reqId));
        rpcResponse.setBytes(String.valueOf(0).getBytes());
        return rpcResponse;
    }
}
