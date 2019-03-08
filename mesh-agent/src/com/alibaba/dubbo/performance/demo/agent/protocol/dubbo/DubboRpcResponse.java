package com.alibaba.dubbo.performance.demo.agent.protocol.dubbo;

/**
 * Dubbo消息返回的有效部分，只需记录对应的AgentServer发出的请求ID，和解析完毕的
 * 字符串哈希值即可
 */

public class DubboRpcResponse {

    private long requestId;

    private byte[] bytes;

    public long getRequestId() {
        return requestId;
    }

    public void setRequestId(long requestId) {

        this.requestId = requestId;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public byte[] getBytes() {

        return bytes;
    }

}
