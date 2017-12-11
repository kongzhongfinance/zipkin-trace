package com.kongzhong.basic.zipkin.agent;

import lombok.extern.slf4j.Slf4j;

/**
 * Global initialization agent
 *
 * @author biezhi
 * @date 2017/12/11
 */
@Slf4j
public class InitializeAgent {

    private static AbstractAgent abstractAgent;

    public static synchronized AbstractAgent initAndGetAgent(String url, String topic) {
        if (null == abstractAgent) {
            try {
                KafkaAgent kafkaAgent = new KafkaAgent(url, topic);
                InitializeAgent.abstractAgent = kafkaAgent;
            } catch (Exception e) {
                log.error("Kafka Agent 初始化失败", e);
            }
        }
        return InitializeAgent.abstractAgent;
    }

    public static AbstractAgent getAgent() {
        return abstractAgent;
    }

}
