package com.kongzhong.basic.zipkin.util;


import java.lang.management.ManagementFactory;

/**
 * ServerInfo
 */
public class ServerInfo {

    public static final int IP4 = NetUtils.ip2Num(NetUtils.getSiteIp());

    public static final int PID = Integer.parseInt(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
}
