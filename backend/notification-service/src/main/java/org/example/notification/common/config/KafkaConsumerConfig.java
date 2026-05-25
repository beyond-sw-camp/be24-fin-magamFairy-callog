package org.example.notification.common.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.example.notification.event.NotificationEvent;
import org.example.notification.kafka.NotificationDeadLetterEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

// 알림 이벤트 Kafka 소비와 DLT 발행에 필요한 직렬화 설정입니다.
@Configuration
public class KafkaConsumerConfig {

    // NotificationEvent JSON 메시지를 Kafka에서 읽기 위한 listener factory입니다.
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, NotificationEvent> notificationEventKafkaListenerContainerFactory(
            @Value("${spring.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers,
            @Value("${notification.kafka.group-id:notification-service}") String groupId
    ) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        JsonDeserializer<NotificationEvent> valueDeserializer = new JsonDeserializer<>(NotificationEvent.class);
        valueDeserializer.addTrustedPackages("org.example.notification.event");
        valueDeserializer.ignoreTypeHeaders();

        DefaultKafkaConsumerFactory<String, NotificationEvent> consumerFactory =
                new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), valueDeserializer);

        ConcurrentKafkaListenerContainerFactory<String, NotificationEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }

    // DLT 메시지를 JSON으로 Kafka에 발행하기 위한 KafkaTemplate입니다.
    @Bean
    public KafkaTemplate<String, NotificationDeadLetterEvent> notificationDeadLetterKafkaTemplate(
            ProducerFactory<String, NotificationDeadLetterEvent> notificationDeadLetterProducerFactory
    ) {
        return new KafkaTemplate<>(notificationDeadLetterProducerFactory);
    }

    @Bean
    public ProducerFactory<String, NotificationDeadLetterEvent> notificationDeadLetterProducerFactory(
            @Value("${spring.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers
    ) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(props);
    }
}
