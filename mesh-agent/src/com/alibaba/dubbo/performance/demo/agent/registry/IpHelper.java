package com.alibaba.dubbo.performance.demo.agent.registry;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class IpHelper {

    public static String getHostIp() throws UnknownHostException {

        String ip = InetAddress.getLocalHost().getHostAddress();
        return ip;
    }
}
