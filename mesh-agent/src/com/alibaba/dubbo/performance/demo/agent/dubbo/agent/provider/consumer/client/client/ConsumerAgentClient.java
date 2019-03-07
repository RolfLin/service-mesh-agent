package com.alibaba.dubbo.performance.demo.agent.dubbo.agent.provider.consumer.client.client;

import com.alibaba.dubbo.performance.demo.agent.cluster.loadbalance.LoadBalance;
import com.alibaba.dubbo.performance.demo.agent.cluster.loadbalance.WeightRoundRobinLoadBalance;
import com.alibaba.dubbo.performance.demo.agent.protocol.pb.DubboMeshProto;
import com.alibaba.dubbo.performance.demo.agent.registry.EndpointHolder;
import com.alibaba.dubbo.performance.demo.agent.rpc.Endpoint;
import com.alibaba.dubbo.performance.demo.agent.transport.Client;
import com.alibaba.dubbo.performance.demo.agent.transport.MeshChannel;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConsumerAgentClient implements Client{

    private static Map<String, ConsumerAgentClient> threadBoundClientMap = new HashMap<>();

    public static void put(String eventLoopName, ConsumerAgentClient consumerAgentClient){
        threadBoundClientMap.put(eventLoopName, consumerAgentClient);
    }

    public static ConsumerAgentClient get(String eventLoopName) {
        return threadBoundClientMap.get(eventLoopName);
    }

    private Map<Endpoint, MeshChannel> channelMap = new HashMap<>(3);
    private LoadBalance loadBalance;
    private volatile boolean available = false;
    private EventLoop sharedEventLoop;

    public ConsumerAgentClient(EventLoop sharedEventLoop){
        this.sharedEventLoop = sharedEventLoop;
    }

    @Override
    public void init() {
        this.loadBalance = new WeightRoundRobinLoadBalance();
        List<Endpoint> endpoints = EndpointHolder.getEndpoints();
        this.loadBalance.onRefresh(endpoints);
        for(Endpoint endpoint : endpoints){
            // Channel提供了一组用于传输的API，主要包括网络的读/写，客户端主动发起连接、关闭连接，服务端绑定端口，获取通讯双方的网络地址
            Channel channel = connect(endpoint);
            MeshChannel meshChannel = new MeshChannel();
            meshChannel.setChannel(channel);
            meshChannel.setEndpoint(endpoint);
            channelMap.put(endpoint, meshChannel);
        }
        available = true;
    }

    @Override
    public MeshChannel getMeshChannel(Endpoint endpoint) {
        if(available){
            return channelMap.get(endpoint);
        }
        throw new RuntimeException("Client 不可用");
    }

    @Override
    public MeshChannel getMeshChannel() {
        if(available){
            Endpoint endpoint = loadBalance.select();
            return channelMap.get(endpoint);
        }
        throw new RuntimeException("Client 不可用");
    }

    private Channel connect(Endpoint endpoint){
        Bootstrap b = new Bootstrap();
        b.group(sharedEventLoop)
                .channel(Epoll.isAvailable() ? EpollSocketChannel.class : NioServerSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast("", new ProtobufVarint32FrameDecoder())
                                .addLast("", new ProtobufDecoder(DubboMeshProto.AgentResponse.getDefaultInstance()))
                                .addLast("", new ProtobufVarint32LengthFieldPrepender())
                                .addLast("", new ProtobufEncoder())
                                .addLast(new ConsumerAgentClientHandler());
                    }
                });
        // 连接三种不同provider对应的provider agent server
        ChannelFuture channelFuture = b.connect(endpoint.getHost(), endpoint.getPort());
        return channelFuture.channel();
    }
}
