
package com.kongzhong.basic.zipkin.plugin.mybatis;

import com.kongzhong.basic.zipkin.TraceConstants;
import com.kongzhong.basic.zipkin.TraceContext;
import com.kongzhong.basic.zipkin.agent.AbstractAgent;
import com.kongzhong.basic.zipkin.agent.InitializeAgent;
import com.kongzhong.basic.zipkin.util.AppConfiguration;
import com.kongzhong.basic.zipkin.util.Ids;
import com.kongzhong.basic.zipkin.util.ServerInfo;
import com.twitter.zipkin.gen.Annotation;
import com.twitter.zipkin.gen.BinaryAnnotation;
import com.twitter.zipkin.gen.Endpoint;
import com.twitter.zipkin.gen.Span;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.sql.Connection;
import java.util.List;
import java.util.Properties;

/**
 * Created by IFT8 on 2017/12/12.
 */
@Intercepts({@Signature(
        type = Executor.class,
        method = "update",
        args = {MappedStatement.class, Object.class}
), @Signature(
        type = Executor.class,
        method = "query",
        args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class}
), @Signature(
        type = Executor.class,
        method = "query",
        args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}
)})
@Slf4j
public class TraceMyBatisInterceptor implements Interceptor {
    private AbstractAgent agent;

    private boolean inited = false;

    public TraceMyBatisInterceptor() {
        init(null, null);
    }

    public TraceMyBatisInterceptor(String url, String topic) {
        init(url, topic);
    }

    private void init(String url, String topic) {
        log.info("TraceMyBatisInterceptor 初始化...");

        try {
            this.agent = InitializeAgent.getAgent();
            if (null == this.agent && url != null && topic != null) {
                this.agent = InitializeAgent.initAndGetAgent(url, topic);
            }
        } catch (Exception e) {
            log.error("初始化Trace客户端失败", e);
        }

        if (this.agent != null) {
            this.inited = true;
            log.info("TraceMyBatisInterceptor 初始化完成");
        } else {
            log.info("TraceMyBatisInterceptor 初始化失败 url={} topic={}", url, topic);
        }
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // new trace
        Span span = this.startTrace();

        Object target = invocation.getTarget();
        if (target instanceof Executor && span != null) {
            Executor executor = (Executor) target;
            Object[] args = invocation.getArgs();

            try {
                if (args != null && args.length > 0 && args[0] instanceof MappedStatement) {
                    MappedStatement statement = (MappedStatement) args[0];
                    String id = statement.getId();
                    String mapper = id;
                    int dotIndex = id.lastIndexOf(".");
                    if (dotIndex != -1) {
                        mapper = id.substring(0, dotIndex);
                    }

                    Connection connection = executor.getTransaction().getConnection();
                    String url = connection.getMetaData().getURL();

                    span.addToBinary_annotations(BinaryAnnotation.create("SQL.mapper", mapper, null));
                    span.addToBinary_annotations(BinaryAnnotation.create("SQL.database", url, null));
                    span.addToBinary_annotations(BinaryAnnotation.create("SQL.method", statement.getSqlCommandType().name(), null));
                    span.addToBinary_annotations(BinaryAnnotation.create("SQL.value", statement.getBoundSql(null).getSql(), null));
                }
            } catch (Exception e) {
                log.error("Trace DB [解析异常]", e);
            } finally {
                //send trace
                this.endTrace(span);
                // clear trace context
                TraceContext.clear();
            }
        }
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }

    private Span startTrace() {
        long id = Ids.get();
        TraceContext.setSpanId(id);

        if (!this.inited) {
            return null;
        }
        Span apiSpan = new Span();
        // span basic data
        long timestamp = System.currentTimeMillis() * 1000;

        apiSpan.setId(id);
        apiSpan.setTrace_id(TraceContext.getTraceId());
        apiSpan.setName(AppConfiguration.getAppId());
        apiSpan.setTimestamp(timestamp);

        // sr annotation
        apiSpan.addToAnnotations(
                Annotation.create(timestamp, TraceConstants.ANNO_SR,
                        Endpoint.create(AppConfiguration.getAppId(), ServerInfo.IP4)));

        TraceContext.setRootSpan(apiSpan);
        if (log.isDebugEnabled()) {
            log.debug("Trace DB name: {}", apiSpan.getName());
            TraceContext.print();
        }
        // prepare trace context
        TraceContext.addSpanAndUpdate(apiSpan);

        return apiSpan;
    }

    private void endTrace(Span span) {
        try {
            if (!this.inited || span == null) {
                return;
            }
            // end span

            long times = (System.currentTimeMillis() * 1000) - span.getTimestamp();
            // ss annotation
            long time = System.currentTimeMillis() * 1000;
            span.addToAnnotations(
                    Annotation.create(time, TraceConstants.ANNO_SS,
                            Endpoint.create(AppConfiguration.getAppId(), ServerInfo.IP4)));

            span.setDuration(times);

            // send trace spans
            try {
                List<Span> spans = TraceContext.getSpans();
                agent.send(spans);
                if (log.isDebugEnabled()) {
                    log.debug("DB Send trace data {}.", TraceContext.getSpans());
                }
            } catch (Exception e) {
                log.error("DB 发送到Trace失败", e);
            }

            if (log.isDebugEnabled()) {
                log.debug("DB Trace clear. traceId={}", TraceContext.getTraceId());
                TraceContext.print();
            }
        } catch (Exception e) {
            log.error("endTrace error ", e);
        }
    }
}
