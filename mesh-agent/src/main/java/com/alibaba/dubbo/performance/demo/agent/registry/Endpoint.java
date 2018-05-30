package com.alibaba.dubbo.performance.demo.agent.registry;

public class Endpoint {
    private final String host;
    private final int port;
    private final int weight;

    public Endpoint(String host,int port,int weight){
        this.host = host;
        this.port = port;
        this.weight = weight;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String toString(){
        return host + ":" + port+":"+weight;
    }

    public boolean equals(Object o){
        if (!(o instanceof Endpoint)){
            return false;
        }
        Endpoint other = (Endpoint) o;
        return other.host.equals(this.host) && other.port == this.port;
    }

    public int hashCode(){
        return host.hashCode() + port;
    }

    public int getWeight() {
        return weight;
    }
}
