package com.alibaba.dubbo.performance.demo.agent.dubbo.agent.provider.consumer.client.server;

import com.alibaba.dubbo.performance.demo.agent.cluster.loadbalance.WeightRoundRobinLoadBalance;
import com.alibaba.dubbo.performance.demo.agent.dubbo.agent.provider.consumer.client.client.ConsumerAgentClient;
import com.alibaba.dubbo.performance.demo.agent.registry.EndpointHolder;
import com.alibaba.dubbo.performance.demo.agent.rpc.Endpoint;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.EventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public final class ConsumerAgentHttpServer {

    private Logger logger = LoggerFactory.getLogger(ConsumerAgentHttpServer.class);

    private EventLoopGroup bossGroup = Epoll.isAvailable() ? new EpollEventLoopGroup() : new NioEventLoopGroup();
    // 入站服务端的eventLoopGroup
    private EventLoopGroup workerGroup = Epoll.isAvailable() ? new EpollEventLoopGroup() : new NioEventLoopGroup();

    private ServerBootstrap bootstrap;

    static final int PORT = Integer.parseInt(System.getProperty("server.port"));

    public static Endpoint[] remoteEndpoints;

    /**
     * 启动服务器接收来自 consumer 的 http 请求
     */

    public void startServer() {
        try {
            initThreadBoundClinet(workerGroup);
            // 从注册中心提取provider的最新状态
            extractEndpoints();

            //NioEventLoopGroup是个线程组，它包含了一组NIO线程，专门用于网络事件的处理，
            //实际上它们就是Reactor线程组。
            //这里创建两个的原因是一个用于服务端接受客户端的连接，另一个用于进行SocketChannel的网络读写。
            bootstrap = new ServerBootstrap();
            //调用ServerBootstrap的group方法，将两个NIO线程组当作入参传递到ServerBootstrap中。
            //接着设置创建的Channel为NioServerSocketChannel，它的功能对应于JDK NIO类库中的ServerSocketChannel类。
            //最后绑定I/O事件的处理类ChildChannelHandler，它的作用类似于Reactor模式中的handler类，
            //主要用于处理网络I/O事件，例如记录日志、对消息进行编解码等。
            bootstrap.option(ChannelOption.SO_BACKLOG, 1024);
            bootstrap.group(bossGroup, workerGroup)
                    // 选用IO模型
                    .channel(Epoll.isAvailable() ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                    .childHandler(new ConsumerAgentHttpServerInitializer())
                    .childOption(ChannelOption.TCP_NODELAY, true);
            //绑定端口，同步等待成功
            //服务端启动辅助类配置完成之后，调用它的bind方法绑定监听端口
            //随后，调用它的同步阻塞方法sync等待绑定操作完成。
            //完成之后Netty会返回一个ChannelFuture，它的功能类似于JDK的java.util.concurrent.Future，主要用于异步操作的通知回调。
            Channel ch = bootstrap.bind(PORT).sync().channel();
            logger.info("consumer-agent provider is ready to receive request from consumer\n" +
                    "export at http://127.0.0.1:{}", PORT);    //等待服务端监听端口关闭
            //使用f.channel().closeFuture().sync()方法进行阻塞,等待服务端链路关闭之后main函数才退出。
            ch.closeFuture().sync();
        } catch (Exception e) {
            logger.error("consumer-agent start failed", e);
        } finally {
            //优雅退出，释放线程池资源
            //调用NIO线程组的shutdownGracefully进行优雅退出，它会释放跟shutdownGracefully相关联的资源。

            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            logger.info("consumer-agent provider was closed");
        }
    }

    private void extractEndpoints(){
        WeightRoundRobinLoadBalance loadBalance = new WeightRoundRobinLoadBalance();
        List<Endpoint> endpoints = EndpointHolder.getEndpoints();
        loadBalance.onRefresh(endpoints);
        remoteEndpoints = loadBalance.getOriginEndpoints();
    }

    // 为出站客户端预选创建好的channel
    public void initThreadBoundClinet(EventLoopGroup eventLoopGroup){
        for(EventExecutor eventExecutors : eventLoopGroup){
            if(eventExecutors instanceof EventLoop){
                ConsumerAgentClient consumerAgentClient = new ConsumerAgentClient((EventLoop) eventExecutors);
                consumerAgentClient.init();
                ConsumerAgentClient.put(eventExecutors.toString(), consumerAgentClient);
            }
        }
    }
}
