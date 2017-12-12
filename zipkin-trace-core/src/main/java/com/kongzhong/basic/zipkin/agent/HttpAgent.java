package com.kongzhong.basic.zipkin.agent;

import com.github.kristofa.brave.SpanCollectorMetricsHandler;
import com.kongzhong.basic.zipkin.SimpleMetricsHandler;
import com.kongzhong.basic.zipkin.collector.HttpSpanCollector;
import lombok.extern.slf4j.Slf4j;

/**
 * HttpAgent
 */
@Slf4j
public class HttpAgent extends AbstractAgent {

    public HttpAgent(String server) {
        SpanCollectorMetricsHandler metrics = new SimpleMetricsHandler();
        // set flush interval to 0 so that tests can drive flushing explicitly
        HttpSpanCollector.Config config =
                HttpSpanCollector.Config.builder()
                        .compressionEnabled(true)
                        .flushInterval(0)
                        .build();

        super.collector = HttpSpanCollector.create(server, config, metrics);
        log.info("HttpAgent 初始化完成. server={}", server);
    }
}