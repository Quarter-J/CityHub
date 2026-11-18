package com.hmdp;

import com.hmdp.config.QueueConfig;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class QueueConfigTest {

    @Test
    void normalQueueDeadLettersFailuresWithoutShortTtl() {
        Queue queue = new QueueConfig().queueA();

        assertEquals(QueueConfig.Y_DEAD_LETTER_EXCHANGE,
                queue.getArguments().get("x-dead-letter-exchange"));
        assertEquals("YD", queue.getArguments().get("x-dead-letter-routing-key"));
        assertFalse(queue.getArguments().containsKey("x-message-ttl"));
    }
}
