package com.kongzhong.basic.zipkin.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Created by IFT8 on 2017/12/12.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AppConfiguration {
    private static volatile String appId = System.getProperty("APPID");
    private static volatile String spanLimitSize = System.getProperty("SpanLimitSize");
    private static final String UNKNOWN_HOST = "(unknown)";

    public static String getAppId() {
        return appId;
    }

    public static int getSpanLimitSize() {
        return Integer.parseInt(spanLimitSize);
    }

    static {
        if (appId == null || appId.length() == 0) {
            appId = UNKNOWN_HOST;
        }
        if (spanLimitSize == null || spanLimitSize.length() == 0) {
            spanLimitSize = 100 + "";
        }
    }
}