package com.alibaba.dubbo.performance.demo.agent.registry;

import com.alibaba.dubbo.performance.demo.agent.rpc.Endpoint;
import com.coreos.jetcd.Client;
import com.coreos.jetcd.KV;
import com.coreos.jetcd.Lease;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.data.KeyValue;
import com.coreos.jetcd.kv.GetResponse;
import com.coreos.jetcd.options.GetOption;
import com.coreos.jetcd.options.PutOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class EtcdRegistry implements IRegistry {
    private Logger logger = LoggerFactory.getLogger(EtcdRegistry.class);

    private final String rootPath = "dubbomesh";
    // 租约的作用：分布式环境中维持缓存的一致性的一种协议
    private Lease lease;
    // 键值相关操作
    private KV kv;
    private long leaseId;

    public EtcdRegistry(String registeryAddress){
        Client client = Client.builder().endpoints(registeryAddress).build();
        this.lease = client.getLeaseClient();
        this.kv = client.getKVClient();
        try {
            this.leaseId = lease.grant(30).get().getID();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        keepAlive();

        String type = System.getProperty("type");
        if ("provider".equals(type)) {
            // 如果是provider， 去etcd注册服务
            int port = Integer.valueOf(System.getProperty("server.port"));
            try {
                register("com.alibaba.dubbo.performance.demo.provider.IHelloService", port + 50);
                logger.info("provider-agent provider register to etcd at port {}", port + 50);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void register(String serviceName, int port) throws Exception {
        // 服务注册的key为： /dubbomesh/com.some.package.IHelloService/192.168.100.100:2000
        String strKey = MessageFormat.format("/{0}/{1}/{2}:{3}", rootPath, serviceName, IpHelper.getHostIp(), String.valueOf(port));
        ByteSequence key = ByteSequence.fromString(strKey);
        String weight = System.getProperty("lb.weight");
        ByteSequence val;
        if(StringUtils.isEmpty(weight)){
            weight = "1";
            val = ByteSequence.fromString(weight);
            logger.warn("未设置provider权重，默认设置为1");
        } else {
            val = ByteSequence.fromString(weight);
        }
        kv.put(key, val, PutOption.newBuilder().withLeaseId(leaseId).build()).get();
        logger.info("Register a new service at:{},weight:{}", strKey, weight);
    }

    // 该EtcdRegistry没有使用etcd的Watch机制来监听etcd的事件
    // 添加watch，在本地内存缓存地址列表，可减少网络调用的次数
    // 使用的是简单的随机负载均衡，如果provider性能不一致，随机策略会影响性能

    // 发送心跳到ETCD，标明该host是活着的
    public void keepAlive() {
        Executors.newSingleThreadExecutor().submit(
                () -> {
                    Lease.KeepAliveListener listener = lease.keepAlive(leaseId);
                    try {
                        listener.listen();
                        logger.info("KeepAlive lease : " + leaseId + "; Hex format : " + Long.toHexString(leaseId));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
        });
    }

    public List<Endpoint> find(String serviceName) throws ExecutionException, InterruptedException {
        String strKey = MessageFormat.format("/{0}/{1}", rootPath, serviceName);
        ByteSequence key = ByteSequence.fromString(strKey);
        GetResponse response = kv.get(key, GetOption.newBuilder().withPrefix(key).build()).get();

        List<Endpoint> endpoints = new ArrayList<>();

        for(KeyValue kv : response.getKvs()) {
            String s = kv.getKey().toStringUtf8();
            int index = s.lastIndexOf("/");
            String endpointStr = s.substring(index + 1, s.length());

            String host = endpointStr.split(":")[0];
            int port = Integer.valueOf(endpointStr.split(":")[1]);
            int weight = Integer.parseInt(kv.getValue().toStringUtf8());
            Endpoint endpoint = new Endpoint(host, port);
            endpoint.setWeight(weight);
            endpoints.add(endpoint);
        }
        logger.info("endpoints size : {}", endpoints.size());
        logger.info("endpoints contents : {}", endpoints.size());
        return endpoints;
    }

}
