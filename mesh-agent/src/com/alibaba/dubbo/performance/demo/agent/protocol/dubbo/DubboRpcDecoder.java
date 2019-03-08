package com.alibaba.dubbo.performance.demo.agent.protocol.dubbo;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class DubboRpcDecoder extends ByteToMessageDecoder{
    // header length.
    protected static final int HEADER_LENGTH = 16;


    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> list) throws Exception {
        int readble = byteBuf.readableBytes();

        // 确保拿到一个完整的header
        if(readble < HEADER_LENGTH){
            return ;
        }
        byteBuf.markReaderIndex();

        byteBuf.skipBytes(3);
        // 跳过前部的 magic high, magic low ,req/res, 2way, event, serialization ID，共三个字节的无需返回参数，
        // 获取位于第四个字节的status
        byte status = byteBuf.readByte();
        // 8个字节
        long requestId = byteBuf.readLong();
        // 4个字节
        int len = byteBuf.readInt();
        if(byteBuf.readableBytes() < len){
            byteBuf.resetReaderIndex();
            return;
        }
        DubboRpcResponse response = new DubboRpcResponse();
        if(status != 20){
            // 状态码不是20时，代表响应结果有问题，直接返回一个字节的结果
            response.setBytes(new byte[1]);
        } else {
            byte[] bytes = new byte[len - 3];
            // +2 ??
            byteBuf.getBytes(byteBuf.readerIndex() + 2, bytes);
            response.setBytes(bytes);
        }
        byteBuf.skipBytes(len);
        response.setRequestId(requestId);
        list.add(response);
    }
}
