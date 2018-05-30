package com.alibaba.dubbo.performance.demo.agent.shared;

import com.alibaba.dubbo.performance.demo.agent.consumer.BackendManager;
import com.alibaba.dubbo.performance.demo.agent.consumer.ConsumerAgentBackendHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class BackendConnection {
    private Logger logger = LoggerFactory.getLogger(BackendConnection.class);

    private String host;
    private int port;
    private Bootstrap bootstrap = new Bootstrap();
    private int channelNumber;
    private List<Channel> channels = new ArrayList<>();
    private Map<EventLoop, List<Channel>> channelMaps = new HashMap<>();
    private AtomicInteger totalConnection = new AtomicInteger(0);
    private Random channelIndexWithSameEventloop = new Random();

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
                                new AgentRequestFastEncoder(),
                                new AgentRequestDecoder("Consumer"),
                                new ConsumerAgentBackendHandler(backendManager));
                    }
                })
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator.DEFAULT);
        //.option(ChannelOption.AUTO_READ, false);
    }

    public void Init(EventLoopGroup eventloopGroup, ChannelInitializer<SocketChannel> channelInitializer) {
        bootstrap = bootstrap.group(eventloopGroup)
                .channel(NioSocketChannel.class)
                .handler(channelInitializer)
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
                MapChannel(ch);

                if (channels.size() >= channelNumber) {
                    bindSuccess = true;
                    PrintChannelMap();
                    latch.countDown();
                }
            } catch (Exception ex) {
                logger.error("binding to {}:{} failed, try again after 50ms", host, port);
                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    // swallow
                }
            }
        }

    }

    private void MapChannel(Channel ch) {
        EventLoop eventLoop = ch.eventLoop();
        List<Channel> chs = this.channelMaps.get(eventLoop);
        if (chs == null) {
            chs = new ArrayList<>();
            this.channelMaps.put(eventLoop, chs);
        }

        chs.add(ch);
    }

    private void PrintChannelMap() {
        logger.info("{} channela are mapped to {} eventloop", channels.size(), channelMaps.size());
        for (Map.Entry<EventLoop, List<Channel>> pair : channelMaps.entrySet()) {
            logger.info("Eventloop [{}] has {} channel", pair.getKey().hashCode(), pair.getValue().size());
        }
    }

    public Channel SelectChannel(ChannelHandlerContext inbound) {
        Channel reuslt = SelectChannelWithSameEventLoop(inbound.channel().eventLoop());
        if (reuslt != null) {
            return reuslt;
        }

        int selectedChannelIndex = totalConnection.getAndIncrement() % channelNumber;
        // System.out.println("BackendConnection select channel No."+selectedChannelIndex);
        return this.channels.get(selectedChannelIndex);
    }

    private Channel SelectChannelWithSameEventLoop(EventLoop eventloop) {
        List<Channel> chs = channelMaps.get(eventloop);
        if (chs == null) {
            return null;
        }

       // logger.info("use channel from eventloop {} to forward",eventloop.hashCode());
        return chs.get(channelIndexWithSameEventloop.nextInt(chs.size()));
    }
}