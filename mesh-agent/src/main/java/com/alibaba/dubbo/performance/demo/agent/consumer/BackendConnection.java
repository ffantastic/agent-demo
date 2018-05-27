package com.alibaba.dubbo.performance.demo.agent.consumer;

import com.alibaba.dubbo.performance.demo.agent.shared.AgentRequestDecoder;
import com.alibaba.dubbo.performance.demo.agent.shared.AgentRequestEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class BackendConnection {
    private Logger logger = LoggerFactory.getLogger(BackendConnection.class);

    private String host;
    private int port;
    private Bootstrap bootstrap = new Bootstrap();
    private int channelNumber;
    private List<Channel> channels = new ArrayList<>();
    private AtomicInteger totalConnection = new AtomicInteger(0);

    public BackendConnection(String host, int port, int channelNumber) {
        this.host = host;
        this.port = port;
        this.channelNumber = channelNumber;
    }

    public void Init(EventLoopGroup eventloopGroup, BackendManager backendManager) {
        bootstrap = bootstrap.group(eventloopGroup)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline().addLast(
                                new AgentRequestEncoder(),
                                new AgentRequestDecoder("Consumer"),
                                new ConsumerAgentBackendHandler(backendManager));
                    }
                })
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator.DEFAULT);
        //.option(ChannelOption.AUTO_READ, false);
    }

    public void Bind(final CountDownLatch latch) {
        boolean bindSuccess = false;
        while (!bindSuccess) {
            try {
                logger.info("try to bind {}:{}, channel number [{}/{}] ", host, port, channels.size() + 1, channelNumber);

                ChannelFuture f = bootstrap.connect(host, port).sync();
                Channel ch = f.channel();
                channels.add(ch);
                if (channels.size() >= channelNumber) {
                    bindSuccess = true;
                    latch.countDown();
                }
            } catch (Exception ex) {
                logger.error("binding to {}:{} failed, try again after 50ms",host,port);
                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    // swallow
                }
            }
        }

    }

    public Channel SelectChannel() {
        int selectedChannelIndex = totalConnection.getAndIncrement() % channelNumber;
        // System.out.println("BackendConnection select channel No."+selectedChannelIndex);
        return this.channels.get(selectedChannelIndex);
    }
}