package com.alibaba.dubbo.performance.demo.agent.dubbo.agent.provider.consumer.client.client;

import com.alibaba.dubbo.performance.demo.agent.dubbo.agent.provider.consumer.client.server.ConsumerAgentHttpServer;
import com.alibaba.dubbo.performance.demo.agent.dubbo.agent.provider.consumer.client.server.ConsumerAgentHttpServerHandler;
import com.alibaba.dubbo.performance.demo.agent.protocol.pb.DubboMeshProto;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsumerAgentClientHandler extends SimpleChannelInboundHandler<DubboMeshProto.AgentResponse>{

    private Logger logger = LoggerFactory.getLogger(ConsumerAgentClientHandler.class);

    /**
     * 只异步接收provider agent server返回的agentResponce
     * @param ctx
     * @param msg
     */

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DubboMeshProto.AgentResponse msg) throws Exception {
        callback(msg);
    }

    private void callback(DubboMeshProto.AgentResponse agentResponse){
        Promise promise = ConsumerAgentHttpServerHandler.promiseHolder.get().remove(agentResponse.getRequestId());
        if(promise != null){
            promise.trySuccess(agentResponse);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("ConsumerAgentClientHandler 异常", cause);
        ctx.channel().close();
    }
}
