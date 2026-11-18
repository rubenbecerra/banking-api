package com.banking.shared.outbox.infrastructure.scheduling;

import com.banking.shared.outbox.application.OutboxRelay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "outbox.relay.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelayJob {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayJob.class);

    private final OutboxRelay relay;
    private final int batchSize;

    public OutboxRelayJob(OutboxRelay relay,
                          @Value("${outbox.relay.batch-size:100}") int batchSize) {
        this.relay = relay;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${outbox.relay.interval-ms:500}")
    public void relayOutbox() {
        try {
            int published = relay.relayOnce(batchSize);
            if (published > 0) {
                log.info("Published {} outbox events", published);
            }
        } catch (RuntimeException e) {
            log.warn("Outbox relay failed; will retry on next tick", e);
        }
    }
}
