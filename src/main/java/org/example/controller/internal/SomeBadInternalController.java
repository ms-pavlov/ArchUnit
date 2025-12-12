package org.example.controller.internal;

import jakarta.servlet.http.HttpServletRequest;
import org.example.Secure;
import org.example.client.SomeClient;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;

public class SomeBadInternalController {

    private KafkaTemplate<?, ?> kafkaTemplate;
    private SomeClient someClient;

    @Secure
    public Integer invokeWithBadArgs(Map<String, String> badArgs, HttpServletRequest request) {
        return Integer.MAX_VALUE;
    }

}
