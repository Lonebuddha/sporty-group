package com.sportygroup.assignment.eventsbetsmatching.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RocketMqBetSettlementProducer implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(RocketMqBetSettlementProducer.class);

    private final DefaultMQProducer producer;
    private final ObjectMapper objectMapper;
    private final String betSettlementsTopic;

    public RocketMqBetSettlementProducer(
        ObjectMapper objectMapper,
        @Value("${rocketmq.name-server}") String nameServer,
        @Value("${rocketmq.producer.group}") String producerGroup,
        @Value("${app.messaging.bet-settlements-topic}") String betSettlementsTopic
    ) {
        this.objectMapper = objectMapper;
        this.betSettlementsTopic = betSettlementsTopic;
        this.producer = new DefaultMQProducer(producerGroup);
        this.producer.setNamesrvAddr(nameServer);
        this.producer.setSendMsgTimeout(10_000);
        this.producer.setRetryTimesWhenSendFailed(3);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        producer.start();
        log.info("RocketMQ producer started for topic={}", betSettlementsTopic);
    }

    public SettlementDispatchResult send(BetSettlementMessage settlementMessage) {
        try {
            byte[] payload = objectMapper.writeValueAsString(settlementMessage).getBytes(StandardCharsets.UTF_8);
            Message message = new Message(
                betSettlementsTopic,
                "bet-settlement",
                settlementMessage.betId().toString(),
                payload
            );
            SendResult sendResult = producer.send(message);
            return new SettlementDispatchResult(sendResult.getMsgId(), sendResult.getSendStatus().name());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to publish settlement message to RocketMQ", exception);
        } catch (MQClientException | RemotingException | MQBrokerException exception) {
            throw new IllegalStateException("Failed to publish settlement message to RocketMQ", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to prepare settlement message for RocketMQ", exception);
        }
    }

    @PreDestroy
    public void shutdown() {
        producer.shutdown();
        log.info("RocketMQ producer shut down");
    }
}
