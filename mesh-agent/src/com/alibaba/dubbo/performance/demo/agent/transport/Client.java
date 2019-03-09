package com.alibaba.dubbo.performance.demo.agent.transport;


import com.alibaba.dubbo.performance.demo.agent.rpc.Endpoint;

/**
 * 点对点通信
 */


public interface Client {

    void init();

    MeshChannel getMeshChannel();

    MeshChannel getMeshChannel(Endpoint endpoint);

}
