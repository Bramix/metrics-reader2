package com.bramix.perfomance.tracker.metricsreader.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;

@Configuration
@Slf4j
public class RedisConsumerConfig {
    @Value("${stream.jira.key}")
    private String jiraStreamKey;

    @Value("${github.stream.name}")
    private String githubStreamKey;
    @Autowired
    private StreamListener<String, ObjectRecord<String, String>> jiraReportRedisConsumer;
    @Autowired
    private StreamListener<String, ObjectRecord<String, String>> githubReportRedisConsumer;


    @Bean
    public Subscription subscription(RedisConnectionFactory redisConnectionFactory) throws UnknownHostException {
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, ObjectRecord<String, String>> options = StreamMessageListenerContainer
                .StreamMessageListenerContainerOptions.builder().pollTimeout(Duration.ofSeconds(1)).targetType(String.class).build();
        StreamMessageListenerContainer<String, ObjectRecord<String, String>>  listenerContainer = StreamMessageListenerContainer
                .create(redisConnectionFactory, options);
        try {
            redisConnectionFactory.getConnection()
                    .xGroupCreate(jiraStreamKey.getBytes(), jiraStreamKey, ReadOffset.from("0-0"), true);
        } catch (RedisSystemException exception) {
            log.warn(exception.getCause().getMessage());
        }
        Subscription subscription = listenerContainer.receive(Consumer.from(jiraStreamKey, InetAddress.getLocalHost().getHostName()),
                StreamOffset.create(jiraStreamKey, ReadOffset.lastConsumed()), jiraReportRedisConsumer);
        listenerContainer.start();
        return subscription;
    }

    @Bean
    public Subscription gitHubSubscription(RedisConnectionFactory redisConnectionFactory) throws UnknownHostException {
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, ObjectRecord<String, String>> options = StreamMessageListenerContainer
                .StreamMessageListenerContainerOptions.builder().pollTimeout(Duration.ofSeconds(1)).targetType(String.class).build();
        StreamMessageListenerContainer<String, ObjectRecord<String, String>>  listenerContainer = StreamMessageListenerContainer
                .create(redisConnectionFactory, options);
        try {
            redisConnectionFactory.getConnection()
                    .xGroupCreate(githubStreamKey.getBytes(), githubStreamKey, ReadOffset.from("0-0"), true);
        } catch (RedisSystemException exception) {
            log.warn(exception.getCause().getMessage());
        }
        Subscription subscription = listenerContainer.receive(Consumer.from(githubStreamKey, InetAddress.getLocalHost().getHostName()),
                StreamOffset.create(githubStreamKey, ReadOffset.lastConsumed()), githubReportRedisConsumer);
        listenerContainer.start();
        return subscription;
    }
}
