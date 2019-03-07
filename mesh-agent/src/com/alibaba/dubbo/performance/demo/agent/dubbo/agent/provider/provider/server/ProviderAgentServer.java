package com.alibaba.dubbo.performance.demo.agent.dubbo.agent.provider.provider.server;

import com.alibaba.dubbo.performance.demo.agent.registry.EtcdRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProviderAgentServer {
    private Logger logger = LoggerFactory.getLogger(ProviderAgentServer.class);

    private EventLoopGroup bossGroup = Epoll.isAvailable() ? new EpollEventLoopGroup(1) : new NioEventLoopGroup(1);
    private EventLoopGroup workerGroup = Epoll.isAvailable() ? new EpollEventLoopGroup(1) : new NioEventLoopGroup(1);

     private ServerBootstrap serverBootstrap;

    /**
     * 启动 Provider Agent 服务器
     */

    public void startServer(){
        new EtcdRegistry(System.getProperty("etcd.url"));

        serverBootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(Epoll.isAvailable() ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                .childHandler(new ProviderAgentServerHandler())
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true);
        int port = Integer.valueOf(System.getProperty("server.port"));

        try {
            Channel channel = serverBootstrap.bind().sync().channel();
            logger.info("provider-agent provider is ready to receive request from consumer-agent\n" +
                "export at 127.0.0.1:{}", port + 50);
            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            logger.error("provider-agent启动失败", e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            logger.info("provider-agent provider was closed");
        }



    }
}
