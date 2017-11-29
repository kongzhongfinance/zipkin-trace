package com.kongzhong.basic.zipkin.util;

import com.twitter.zipkin.gen.Span;

/**
 * @author biezhi
 * @date 2017/11/29
 */
public class ZipkinUtils {

    public static void startTrace() {
        Span span = new Span();
        long id   = Ids.get();
        span.setId(id);
        span.setTrace_id(id);
        span.setParent_id(id);
    }

    public static void endTrace() {

    }

}
