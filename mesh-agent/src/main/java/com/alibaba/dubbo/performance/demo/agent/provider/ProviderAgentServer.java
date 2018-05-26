package com.alibaba.dubbo.performance.demo.agent.provider;

import com.alibaba.dubbo.performance.demo.agent.dubbo.ConnecManager;
import com.alibaba.dubbo.performance.demo.agent.shared.AgentRequestDecoder;
import com.alibaba.dubbo.performance.demo.agent.shared.AgentRequestEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProviderAgentServer {
    static final int LOCAL_PORT = Integer.parseInt(System.getProperty("localPort", "30000"));

    private Logger logger = LoggerFactory.getLogger(ProviderAgentServer.class);

    public void Start() throws Exception {
        final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        final EventLoopGroup workerGroup = new NioEventLoopGroup(4);

        try {
            final ConnecManager connecManager = new ConnecManager();
            connecManager.Init(workerGroup);
            logger.info("Provider Agent Server starting... binding port,{}", LOCAL_PORT);
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    //.handler(new LoggingHandler(LogLevel.INFO))
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new AgentRequestDecoder("provider"));
                            ch.pipeline().addLast(new AgentRequestEncoder());
                            //TODO cache ProviderAgentFrontendHandler ?
                            ch.pipeline().addLast(new ProviderAgentFrontendHandler(connecManager));
                        }
                    });//.childOption(ChannelOption.AUTO_READ, false);

            Channel ch = b.bind(LOCAL_PORT).sync().channel();
            logger.info("Provider Agent Server started!");
            ch.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
