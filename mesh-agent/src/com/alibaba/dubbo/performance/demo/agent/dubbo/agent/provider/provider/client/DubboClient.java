package com.alibaba.dubbo.performance.demo.agent.dubbo.agent.provider.provider.client;

import com.alibaba.dubbo.performance.demo.agent.rpc.Endpoint;
import com.alibaba.dubbo.performance.demo.agent.transport.Client;
import com.alibaba.dubbo.performance.demo.agent.transport.MeshChannel;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.EventLoop;

public class DubboClient implements Client{

    private static final String REMOTE_HOST = "127.0.0.1";

    private static final int REMOTE_PORT = Integer.valueOf(System.getProperty("dubbo.protocol.port"));

    private static final Endpoint providerEndpoint = new Endpoint(REMOTE_HOST, REMOTE_PORT);

    private EventLoop eventLoop;

    public DubboClient(EventLoop eventLoop) {
        this.eventLoop = eventLoop;
    }

    @Override
    public void init() {
        Bootstrap b = new Bootstrap();
        b.group(eventLoop)
                .channel()
    }

    @Override
    public MeshChannel getMeshChannel() {
        return null;
    }

    @Override
    public MeshChannel getMeshChannel(Endpoint endpoint) {
        return null;
    }
}
