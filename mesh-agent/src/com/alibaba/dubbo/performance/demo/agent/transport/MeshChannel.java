package com.alibaba.dubbo.performance.demo.agent.transport;

import com.alibaba.dubbo.performance.demo.agent.rpc.Endpoint;
import io.netty.channel.Channel;

public class MeshChannel {

    private Channel channel;
    private Endpoint endpoint;

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public void setEndpoint(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    public Channel getChannel() {

        return channel;
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }
}
