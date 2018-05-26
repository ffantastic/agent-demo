package com.alibaba.dubbo.performance.demo.agent;

import com.alibaba.dubbo.performance.demo.agent.consumer.ConsumerAgentServer;
import com.alibaba.dubbo.performance.demo.agent.provider.ProviderAgentServer;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;


@Component
public class BackendStarter implements CommandLineRunner {
    @Override
    public void run(String... strings) throws Exception {
        String type = System.getProperty("type");   // 获取type参数
        if ("consumer".equals(type)) {
            new ConsumerAgentServer().Start();
        } else if ("provider".equals(type)) {
            new ProviderAgentServer().Start();
        } else {
            LocalTest();
            // throw new RuntimeException("Environment variable type is needed to set to provider or consumer.");
        }
    }


    private void LocalTest() throws Exception {
        new Thread(()->{
            try {
                new ProviderAgentServer().Start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        new ConsumerAgentServer().Start();
    }
}
