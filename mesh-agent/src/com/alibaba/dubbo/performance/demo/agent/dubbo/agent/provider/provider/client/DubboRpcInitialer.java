package com.alibaba.dubbo.performance.demo.agent.dubbo.agent.provider.provider.client;

import com.alibaba.dubbo.performance.demo.agent.protocol.dubbo.DubboRpcDecoder;
import com.alibaba.dubbo.performance.demo.agent.protocol.dubbo.DubboRpcEncoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

public class DubboRpcInitialer extends ChannelInitializer<SocketChannel>{

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline()
                .addLast(new DubboRpcEncoder())
                .addLast(new DubboRpcDecoder())
                .addLast(new DubboRpcHandler());
    }
}
