package com.alibaba.dubbo.performance.demo.agent.dubbo.agent.provider.provider.client;

import com.alibaba.dubbo.performance.demo.agent.dubbo.agent.provider.provider.server.ProviderAgentServerHandler;
import com.alibaba.dubbo.performance.demo.agent.protocol.dubbo.DubboRpcResponse;
import com.alibaba.dubbo.performance.demo.agent.protocol.pb.DubboMeshProto;
import com.google.protobuf.ByteString;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class DubboRpcHandler extends SimpleChannelInboundHandler<Object> {

    private static Logger logger = LoggerFactory.getLogger(DubboRpcHandler.class);

    public DubboRpcHandler() {
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
         if(msg instanceof DubboRpcResponse){
            process((DubboRpcResponse) msg);
         } else if(msg instanceof List){
             List<DubboRpcResponse> responses = (List<DubboRpcResponse>) msg;
            for(DubboRpcResponse dubboRpcResponse : responses){
                process(dubboRpcResponse);
            }
         }
    }


    private void process(DubboRpcResponse msg){
        Promise<DubboMeshProto.AgentResponse> promise = ProviderAgentServerHandler.promiseHolder.get().remove(msg.getRequestId());
        if(promise != null){
            promise.trySuccess(messageToMessage(msg));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.channel().close();
    }

    private DubboMeshProto.AgentResponse messageToMessage(DubboRpcResponse dubboRpcResponse){
        return DubboMeshProto.AgentResponse.newBuilder()
                .setRequestId(dubboRpcResponse.getRequestId())
                .setHash(ByteString.copyFrom(dubboRpcResponse.getBytes()))
                .build();
    }
}
