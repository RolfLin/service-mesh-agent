package com.alibaba.dubbo.performance.demo.agent.dubbo.agent.provider.consumer.client.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

public class ConsumerAgentHttpServerInitializer extends ChannelInitializer<io.netty.channel.socket.SocketChannel>{

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline p = ch.pipeline();
        p.addLast("encoder", new HttpResponseEncoder());
        p.addLast("decoder", new HttpRequestDecoder());
        p.addLast("aggregator", new HttpObjectAggregator(10 * 1024 * 1024));
        p.addLast(new ConsumerAgentHttpServerHandler());
    }
}
