package org.example.notification.common.config;

import org.example.notification.service.NotificationSseRedisSubscriber;
import org.example.notification.service.NotificationSseService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory redisConnectionFactory,
            NotificationSseRedisSubscriber notificationSseRedisSubscriber
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.addMessageListener(
                notificationSseRedisSubscriber,
                new ChannelTopic(NotificationSseService.NOTIFICATION_SSE_CHANNEL)
        );
        return container;
    }
}
