package com.yunshu.zhixue.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.yunshu.zhixue.common.constant.AppConstants;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/**
 * JSON 序列化约定：接口时间统一按 {@code yyyy-MM-dd HH:mm:ss}（Asia/Shanghai）输出。
 *
 * <p>库内统一 UTC 存储，服务端在 DTO 转换时完成时区换算（database-rules §3.5）。</p>
 */
@Configuration
public class JacksonConfig {

    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
        return builder -> builder
                .timeZone(TimeZone.getTimeZone(AppConstants.BIZ_ZONE))
                .serializers(new LocalDateTimeSerializer(formatter))
                .deserializers(new LocalDateTimeDeserializer(formatter))
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
