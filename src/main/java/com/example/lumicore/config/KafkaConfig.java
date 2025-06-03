package com.example.lumicore.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    // ──────────────────────────────────────────────────────────────
    // Spring 프로퍼티로부터 Auth Token, Tenancy, User, StreamPoolOCID 읽기
    // ──────────────────────────────────────────────────────────────
    @Value("${OCI_TENANCY}")
    private String ociTenancy;

    @Value("${OCI_USER}")
    private String ociUser;

    @Value("${OCI_STREAM_POOL_OCID}")
    private String ociStreamPoolOcid;

    @Value("${OCI_STREAM_AUTH_TOKEN}")
    private String ociAuthToken;

    // ───────────────────────────────────────────────
    // 1) ConsumerFactory: Kafka Consumer 프로퍼티 정의
    // ───────────────────────────────────────────────
    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();

        // ── 기본 Kafka 설정 ──
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        // ── SASL/PLAIN 인증 (Auth Token 방식) ──
        props.put("security.protocol", "SASL_SSL");
        props.put("sasl.mechanism", "PLAIN");

        //   username: "<tenancyName>/<userName>/<streamPoolId>"
        //   password: "<Auth Token>"
        String jaasConfigConsumer = "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                "username=\"" + ociTenancy + "/" + ociUser + "/" + ociStreamPoolOcid + "\" " +
                "password=\"" + ociAuthToken + "\";";

        props.put("sasl.jaas.config", jaasConfigConsumer);

        // ── Consumer 성능/안정성 최적화 ──
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300_000);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30_000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3_000);

        // ── 연결 안정성 ──
        props.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30_000);
        props.put(ConsumerConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG, 540_000);

        return new DefaultKafkaConsumerFactory<>(props);
    }

    // ───────────────────────────────────────────────
    // 2) ConcurrentKafkaListenerContainerFactory 설정
    // ───────────────────────────────────────────────
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());

        // Manual acknowledgment 모드
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        // 기본 에러 핸들러
        factory.setCommonErrorHandler(new org.springframework.kafka.listener.DefaultErrorHandler());

        // 컨테이너 동시성
        factory.setConcurrency(1);

        return factory;
    }

    // ───────────────────────────────────────────────
    // 3) ProducerFactory: Kafka Producer 프로퍼티 정의
    // ───────────────────────────────────────────────
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> props = new HashMap<>();

        // ── 기본 Kafka Producer 설정 ──
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        // ── SASL/PLAIN 인증 (Auth Token 방식) ──
        props.put("security.protocol", "SASL_SSL");
        props.put("sasl.mechanism", "PLAIN");

        String jaasConfigProducer = "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                "username=\"" + ociTenancy + "/" + ociUser + "/" + ociStreamPoolOcid + "\" " +
                "password=\"" + ociAuthToken + "\";";

        props.put("sasl.jaas.config", jaasConfigProducer);

        // ── Producer 신뢰성/성능 최적화 (트랜잭션 제거) ──
        props.put(ProducerConfig.ACKS_CONFIG, "1"); // all -> 1로 변경
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16_384);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 1);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33_554_432);

        // 트랜잭션 관련 설정 제거
        // props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, false);

        // ── 연결 안정성 ──
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30_000);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120_000);
        props.put(ProducerConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG, 540_000);

        return new DefaultKafkaProducerFactory<>(props);
    }

    // ───────────────────────────────────────────────
    // 4) KafkaTemplate: 메시지 전송용 템플릿
    // ───────────────────────────────────────────────
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
