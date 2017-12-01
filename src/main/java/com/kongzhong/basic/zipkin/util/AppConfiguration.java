package com.kongzhong.basic.zipkin.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AppConfiguration {
    private static volatile String appId = System.getProperty("APPID");
    private static final String UNKNOWN_HOST = "(unknown)";

    public static String getAppId() {
        return appId;
    }

    static {
        if (appId == null || appId.length() == 0) {
            appId = "(unknown)";
        }
    }
}