package com.kongzhong.basic.zipkin.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Created by IFT8 on 2017/3/31.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AppConfiguration {
    private static volatile String appId;
    private static final String UNKNOWN_HOST = "(unknown)";

    static {
        appId = System.getProperty("APPID");
        if (appId == null || appId.length() == 0) {
            appId = UNKNOWN_HOST;
        }
    }

    public static String getAppId() {
        return appId;
    }
}
