package com.remark.platform.config;

import com.remark.platform.events.ReportSentMessage;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Bean
    ConsumerFactory<String, ReportSentMessage> reportSentConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${spring.application.name}") String applicationName
    ) {
        Map<String, Object> config = commonConsumerConfig(bootstrapServers, applicationName);
        JsonDeserializer<ReportSentMessage> deserializer = new JsonDeserializer<>(ReportSentMessage.class, false);
        deserializer.addTrustedPackages("com.remark.platform.events");
        return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), deserializer);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, ReportSentMessage> reportSentKafkaListenerContainerFactory(
            ConsumerFactory<String, ReportSentMessage> reportSentConsumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, ReportSentMessage> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(reportSentConsumerFactory);
        return factory;
    }

    private Map<String, Object> commonConsumerConfig(String bootstrapServers, String applicationName) {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, applicationName);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return config;
    }
}
