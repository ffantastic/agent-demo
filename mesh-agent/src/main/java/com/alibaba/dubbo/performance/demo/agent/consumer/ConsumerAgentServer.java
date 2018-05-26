package com.alibaba.dubbo.performance.demo.agent.consumer;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsumerAgentServer {
    static final int LOCAL_PORT = Integer.parseInt(System.getProperty("localPort", "20000"));

    private Logger logger = LoggerFactory.getLogger(ConsumerAgentServer.class);

    public void Start() throws Exception {
        final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        final EventLoopGroup workerGroup = new NioEventLoopGroup(4);

        try {
            final BackendManager bm = new BackendManager();
            bm.Init(workerGroup);
            logger.info("Consumer Agent Server starting... binding port,{}", LOCAL_PORT);
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    //.handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new HttpResponseEncoder());
                            ch.pipeline().addLast(new HttpRequestDecoder());
                            ch.pipeline().addLast("aggegator", new HttpObjectAggregator(1024 * 1024));
                            //TODO cache ConsumerAgentFrontendHandler ?
                            ch.pipeline().addLast(new ConsumerAgentFrontendHandler(bm));
                        }
                    });//.childOption(ChannelOption.AUTO_READ, false);

            Channel ch = b.bind(LOCAL_PORT).sync().channel();
            logger.info("Consumer Agent Server started!");
            ch.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
