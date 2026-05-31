package com.bookstore.app.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@EnableCaching
@Configuration
public class CacheConfig {

    private static final Random RANDOM = new Random();

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = defaultCacheConfig(Duration.ofMinutes(10));

        Map<String, RedisCacheConfiguration> configs = new HashMap<>();
        configs.put("book:detail", defaultCacheConfig(Duration.ofMinutes(30)));
        configs.put("book:hot", defaultCacheConfig(Duration.ofMinutes(10)));
        configs.put("book:new", defaultCacheConfig(Duration.ofMinutes(10)));
        configs.put("category:tree", defaultCacheConfig(Duration.ofMinutes(60)));
        configs.put("seckill:running", defaultCacheConfig(Duration.ofMinutes(1)));
        configs.put("seckill:upcoming", defaultCacheConfig(Duration.ofMinutes(1)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(configs)
                .transactionAware()
                .build();
    }

    private RedisCacheConfiguration defaultCacheConfig(Duration baseTtl) {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(Object.class).build();
        om.activateDefaultTyping(ptv, DefaultTyping.NON_FINAL);

        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(om, Object.class);

        Duration ttl = baseTtl.plusSeconds(RANDOM.nextInt(60));

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(serializer));
    }
}
