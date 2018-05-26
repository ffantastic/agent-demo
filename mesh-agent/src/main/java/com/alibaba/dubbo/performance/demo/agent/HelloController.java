package com.alibaba.dubbo.performance.demo.agent;

import com.alibaba.dubbo.performance.demo.agent.consumer.LocalLoadBalancer;
import com.alibaba.dubbo.performance.demo.agent.registry.Endpoint;
import com.alibaba.dubbo.performance.demo.agent.registry.EtcdRegistry;
import com.alibaba.dubbo.performance.demo.agent.registry.IRegistry;
import okhttp3.*;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.ListenableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;
import java.util.Random;


public class HelloController {

    private Logger logger = LoggerFactory.getLogger(HelloController.class);

    private IRegistry registry = new EtcdRegistry(System.getProperty("etcd.url"));

    private Random random = new Random();
    private List<Endpoint> endpoints = null;
    private Object lock = new Object();
    private OkHttpClient httpClient;
    private AsyncHttpClient asyncHttpClient = org.asynchttpclient.Dsl.asyncHttpClient();

    private LocalLoadBalancer lb = LocalLoadBalancer.GetInstance();

/*    @PostConstruct
    public void init() {
        logger.info("PostConstruct: initial http connection pool");
        // 使用100个连接，默认是5个。
        // okhttp使用http 1.1，默认打开keep-alive
        ConnectionPool pool = new ConnectionPool(100, 5L, TimeUnit.MINUTES);

        httpClient = new OkHttpClient.Builder()
                .connectionPool(pool)
                .connectTimeout(60, TimeUnit.SECONDS)       //设置连接超时
                .readTimeout(60, TimeUnit.SECONDS)          //不考虑超时
                .writeTimeout(60, TimeUnit.SECONDS)          //不考虑超时
                .retryOnConnectionFailure(true)
                .build();
    }*/


    public DeferredResult<Object> invoke(@RequestParam("interface") String interfaceName,
                                         @RequestParam("method") String method,
                                         @RequestParam("parameterTypesString") String parameterTypesString,
                                         @RequestParam("parameter") String parameter) throws Exception {
        String type = System.getProperty("type");   // 获取type参数
        DeferredResult<Object> result = new DeferredResult<>();
        if ("consumer".equals(type)) {
            return consumer(interfaceName, method, parameterTypesString, parameter);
        } else if ("provider".equals(type)) {
            result.setResult(provider(interfaceName, method, parameterTypesString, parameter));
            return result;
        } else {
            result.setResult("Environment variable type is needed to set to provider or consumer.");
            return result;
        }
    }

    public byte[] provider(String interfaceName, String method, String parameterTypesString, String parameter) throws Exception {

        Object result = null;//rpcClient.invoke(interfaceName, method, parameterTypesString, parameter);
        return (byte[]) result;
    }


    public DeferredResult<Object> consumer(String interfaceName, String method, String parameterTypesString, String parameter) throws Exception {

        if (null == endpoints) {
            synchronized (lock) {
                if (null == endpoints) {
                    endpoints = registry.find("com.alibaba.dubbo.performance.demo.provider.IHelloService");
                    for (Endpoint ep : endpoints) {
                        logger.info("[LB] add host: " + ep.getHost() + ":" + ep.getPort());
                        this.lb.UpdateTTR(ep.getHost() + ":" + ep.getPort(), 0);
                    }
                }
            }
        }

        //logger.info("Endpoint size: "+endpoints.size());

        // 简单的负载均衡，随机取一个
        final String endpointStr = this.lb.GetHost();
        String[] hostAndPort = endpointStr.split(":");

        String url = "http://" + hostAndPort[0] + ":" + hostAndPort[1];

     /*   Endpoint endpoint = endpoints.get(random.nextInt(endpoints.size()));
        String url = "http://" + endpoint.getHost() + ":" +endpoint.getPort();*/

        org.asynchttpclient.Request request = org.asynchttpclient.Dsl.post(url)
                .addFormParam("interface", "com.alibaba.dubbo.performance.demo.provider.IHelloService")
                .addFormParam("method", method)
                .addFormParam("parameterTypesString", parameterTypesString)
                .addFormParam("parameter", parameter)
                .build();

        final long requestStartTime = System.currentTimeMillis();
        DeferredResult<Object> result = new DeferredResult<>();
        ListenableFuture<org.asynchttpclient.Response> responseFuture = asyncHttpClient.executeRequest(request);

        Runnable callback = () -> {
            try {
                byte[] bytes = responseFuture.get().getResponseBody().getBytes();
                String s = new String(bytes);
                HelloController.this.lb.UpdateTTR(endpointStr, System.currentTimeMillis() - requestStartTime);
                result.setResult(Integer.valueOf(s));

            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        responseFuture.addListener(callback, null);

        return result;
    }
}
