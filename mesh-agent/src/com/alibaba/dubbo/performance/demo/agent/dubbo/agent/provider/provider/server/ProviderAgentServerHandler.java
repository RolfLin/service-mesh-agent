package com.alibaba.dubbo.performance.demo.agent.dubbo.agent.provider.provider.server;

import com.alibaba.dubbo.performance.demo.agent.dubbo.agent.provider.provider.client.DubboClient;
import com.alibaba.dubbo.performance.demo.agent.protocol.dubbo.DubboRpcRequest;
import com.alibaba.dubbo.performance.demo.agent.protocol.dubbo.DubboRpcResponse;
import com.alibaba.dubbo.performance.demo.agent.protocol.dubbo.RpcInvocation;
import com.alibaba.dubbo.performance.demo.agent.protocol.pb.DubboMeshProto;
import com.alibaba.dubbo.performance.demo.agent.transport.Client;
import com.alibaba.dubbo.performance.demo.agent.util.JsonUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.collection.LongObjectHashMap;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.FastThreadLocal;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class ProviderAgentServerHandler extends SimpleChannelInboundHandler<DubboMeshProto.AgentRequest>{


    private Logger logger = LoggerFactory.getLogger(ProviderAgentServerHandler.class);

    public static FastThreadLocal<LongObjectHashMap<Promise<DubboMeshProto.AgentResponse>>> promiseHolder = new
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
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if(dubboClientHolder.get() == null){
            DubboClient dubboClient = new DubboClient(ctx.channel().eventLoop());
            dubboClient.init();
            dubboClientHolder.set(dubboClient);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DubboMeshProto.AgentRequest msg) throws Exception {
        Promise<DubboMeshProto.AgentResponse> promise = new DefaultPromise<>(ctx.executor());
        promise.addListener(future -> {
            DubboMeshProto.AgentResponse agentResponse = (DubboMeshProto.AgentResponse) future.get();
            ctx.writeAndFlush(agentResponse);
        });

        // agentRequest 和 agentResponse 的promise进行绑定
        promiseHolder.get().put(msg.getRequestId(), promise);
        // dubboRpcRequest请求通过Dubbo Client发送到Dubbo Provider
        dubboClientHolder.get().getMeshChannel().getChannel().writeAndFlush(messageToMessage(msg));
    }


    private DubboRpcRequest messageToMessage(DubboMeshProto.AgentRequest agentRequest){
        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName(agentRequest.getMethod());
        invocation.setAttachment("path", agentRequest.getInterfaceName());
        invocation.setParameterTypes(agentRequest.getParameterTypesString());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out));
        try {
            JsonUtils.writeObject(agentRequest.getParameter(), writer);
        } catch (IOException e) {
            throw new RuntimeException();
        }

        invocation.setArguments(out.toByteArray());
        DubboRpcRequest dubboRpcRequest = new DubboRpcRequest();
        dubboRpcRequest.setId(agentRequest.getRequestId());
        dubboRpcRequest.setVersion("2.0.0");
        dubboRpcRequest.setTwoWay(true);
        dubboRpcRequest.setData(invocation);
        return dubboRpcRequest;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.channel().close();
    }
}
