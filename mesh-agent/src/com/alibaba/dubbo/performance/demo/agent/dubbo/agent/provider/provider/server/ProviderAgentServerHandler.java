package com.alibaba.dubbo.performance.demo.agent.dubbo.agent.provider.provider.server;

import com.alibaba.dubbo.performance.demo.agent.protocol.pb.DubboMeshProto;
import com.alibaba.dubbo.performance.demo.agent.transport.Client;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.collection.LongObjectHashMap;
import io.netty.util.concurrent.FastThreadLocal;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProviderAgentServerHandler extends SimpleChannelInboundHandler<DubboMeshProto.AgentRequest>{


    private Logger logger = LoggerFactory.getLogger(ProviderAgentServerHandler.class);

    public static FastThreadLocal<LongObjectHashMap<Promise<DubboMeshProto.AgentResponse>>> promise = new
            FastThreadLocal<LongObjectHashMap<Promise<DubboMeshProto.AgentResponse>>>(){
                @Override
                protected LongObjectHashMap<Promise<DubboMeshProto.AgentResponse>> initialValue() throws Exception {
                    return new LongObjectHashMap<>();
                }
            };
    private static FastThreadLocal<Client> dubboClientHolder = new FastThreadLocal<>();

    public ProviderAgentServerHandler() {
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DubboMeshProto.AgentRequest msg) throws Exception {

    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if(dubboClientHolder.get() == null){
            Dubbo
        }
    }
}
