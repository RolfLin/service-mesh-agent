package com.alibaba.dubbo.performance.demo.agent.dubbo.agent.provider.consumer.client.server;

import com.alibaba.dubbo.performance.demo.agent.dubbo.agent.provider.consumer.client.client.ConsumerAgentClient;
import com.alibaba.dubbo.performance.demo.agent.protocol.pb.DubboMeshProto;
import com.alibaba.dubbo.performance.demo.agent.rpc.Endpoint;
import com.alibaba.dubbo.performance.demo.agent.transport.MeshChannel;
import com.alibaba.dubbo.performance.demo.agent.util.RequestParser;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.AsciiString;
import io.netty.util.collection.LongObjectHashMap;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.FastThreadLocal;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

public class ConsumerAgentHttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest>{

    private static final AsciiString CONTENT_TYPE = AsciiString.cached("Content-Type");
    private static final AsciiString CONTENT_LENGTH = AsciiString.cached("Content-Length");
    private static final AsciiString CONNECTION = AsciiString.cached("Connection");
    private static final AsciiString KEEP_ALIVE = AsciiString.cached("keep-alive");

    public ConsumerAgentHttpServerHandler() {
    }

    private Logger logger = LoggerFactory.getLogger(ConsumerAgentHttpServerHandler.class);

    public static AtomicLong requestIdGenerator = new AtomicLong(0);

    public static AtomicInteger handlerCnt = new AtomicInteger(0);

    // 在consumer中出入站为同个线程，使用ThreadLocal，避免锁带来的消耗
    public static FastThreadLocal<LongObjectHashMap<Promise>> promiseHolder = new FastThreadLocal<LongObjectHashMap<Promise>>(){
        @Override
        protected LongObjectHashMap<Promise> initialValue() throws Exception {
            return new LongObjectHashMap<>();
        }
    };

    private Endpoint channelConsistenceHashEndpoint;


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        int handler = handlerCnt.incrementAndGet();
        // 根据
        this.channelConsistenceHashEndpoint = ConsumerAgentHttpServer.remoteEndpoints[handler % ConsumerAgentHttpServer.remoteEndpoints.length];
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        processRequest(ctx, msg);
    }

    private void processRequest(ChannelHandlerContext ctx, FullHttpRequest req){
        Map<String, String> requestParams = RequestParser.fastParse(req);

        // 自定义通信协议，将http报文进行转换
        DubboMeshProto.AgentRequest agentRequest = DubboMeshProto.AgentRequest.newBuilder().setRequestId(requestIdGenerator.incrementAndGet())
                .setInterfaceName(requestParams.get("interface"))
                .setMethod(requestParams.get("method"))
                .setParameterTypesString(requestParams.get("parameterTypesString"))
                .setParameter(requestParams.get("parameter"))
                .build();

        this.call(ctx, agentRequest);
    }

    private void call(ChannelHandlerContext ctx, DubboMeshProto.AgentRequest request){
        // 异步等待返回的agentResponse结果
        Promise<DubboMeshProto.AgentResponse> agentResponsePromise = new DefaultPromise<>(ctx.executor());
        agentResponsePromise.addListener(future -> {
            // 获取从Client端返回的hash值
            DubboMeshProto.AgentResponse agentResponse = (DubboMeshProto.AgentResponse) future.get();
            // 设置header参数
            FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK);
            response.headers().set(CONTENT_TYPE, "text/plain");
            response.headers().setInt(CONTENT_LENGTH, response.content().readableBytes());
            response.headers().set(CONNECTION, KEEP_ALIVE);
            ctx.write(response);
        });
        // 将异步响应的ID与eventLoop、channel绑定
        promiseHolder.get().put(request.getRequestId(), agentResponsePromise);

        // 通过eventLoop找到同个channel下的consumer agent client，
        // 再通过consumer agent Server指定的endpoint，发送消息给endpoint对应channel下的provider agent Server
        MeshChannel meshChannel = ConsumerAgentClient.get(ctx.channel().eventLoop().toString()).getMeshChannel(channelConsistenceHashEndpoint);
        meshChannel.getChannel().writeAndFlush(request);

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.info("Http服务器响应出错", cause);
        ctx.channel().close();
    }
}
