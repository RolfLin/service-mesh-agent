package com.alibaba.dubbo.performance.demo.agent.cluster.loadbalance;

import com.alibaba.dubbo.performance.demo.agent.rpc.Endpoint;

import java.util.List;

public interface LoadBalance {

    Endpoint select();

    void onRefresh(List<Endpoint> endpoints);
}
