package org.example.controller;

import org.example.service.BadServiceClass;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;

@Service
public class SomeBadController {
    private BadServiceClass badServiceClass;
    private TransactionTemplate transactionTemplate;

    public Integer invokeWithBadArgs(Map<String, String> badArgs) {
        return Integer.MAX_VALUE;
    }
}
