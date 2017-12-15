
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
        }
        log.info("TraceMyBatisInterceptor inited=[{}] url={} topic={}", this.inited, url, topic);
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {


        Object target = invocation.getTarget();
        if (target instanceof Executor) {
            Executor executor = (Executor) target;
            Object[] args = invocation.getArgs();

            try {
                if (args != null && args.length > 0 && args[0] instanceof MappedStatement) {
                    MappedStatement statement = (MappedStatement) args[0];
                    String mapper = statement.getId();

                    Object parameterObject = null;
                    if (invocation.getArgs().length > 1) {
                        parameterObject = invocation.getArgs()[1];
                    }

                    Connection connection = executor.getTransaction().getConnection();
                    String url = connection.getMetaData().getURL();

                    // new trace
                    Span span = this.startTrace(url, mapper);
                    if (span != null) {
                        span.addToBinary_annotations(BinaryAnnotation.create("SQL.mapper", mapper, null));
                        span.addToBinary_annotations(BinaryAnnotation.create("SQL.database", url, null));
                        span.addToBinary_annotations(BinaryAnnotation.create("SQL.method", statement.getSqlCommandType().name(), null));
                        span.addToBinary_annotations(BinaryAnnotation.create("SQL.sql", statement.getBoundSql(parameterObject).getSql(), null));
                        //end trace
                        this.endTrace(span);
                    }
                }
            } catch (Exception e) {
                log.error("Trace DB [解析异常]", e);
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

    private Span startTrace(String name, String method) {
        long id = Ids.get();
        TraceContext.setSpanId(id);

        if (!this.inited) {
            return null;
        }
        Span apiSpan = new Span();
        // span basic data
        long timestamp = System.currentTimeMillis() * 1000;

        apiSpan.setName(method);

        apiSpan.setId(id);
        apiSpan.setTrace_id(TraceContext.getTraceId());
        apiSpan.setTimestamp(timestamp);

        // cs annotation
        apiSpan.addToAnnotations(
                Annotation.create(timestamp, TraceConstants.ANNO_CS,
                        Endpoint.create(name, ServerInfo.IP4)));

        TraceContext.print();

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
            // cr annotation
            long time = System.currentTimeMillis() * 1000;
            span.addToAnnotations(
                    Annotation.create(time, TraceConstants.ANNO_CR,
                            Endpoint.create(AppConfiguration.getAppId(), ServerInfo.IP4)));

            span.setDuration(times);

        } catch (Exception e) {
            log.error("endTrace error ", e);
        }
    }
}
