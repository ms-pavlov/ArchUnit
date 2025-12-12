package ru.proitr.controller.internal;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.kafka.core.KafkaTemplate;
import ru.proitr.Secure;
import ru.proitr.client.SomeClient;

import java.util.Map;

public class SomeBadInternalController {

    private KafkaTemplate<?, ?> kafkaTemplate;
    private SomeClient someClient;

    @Secure
    public Integer invokeWithBadArgs(Map<String, String> badArgs, HttpServletRequest request) {
        return Integer.MAX_VALUE;
    }

}
