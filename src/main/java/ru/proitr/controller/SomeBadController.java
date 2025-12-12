package ru.proitr.controller;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import ru.proitr.service.BadServiceClass;

import java.util.Map;

@Service
public class SomeBadController {
    private BadServiceClass badServiceClass;
    private TransactionTemplate transactionTemplate;

    public Integer invokeWithBadArgs(Map<String, String> badArgs) {
        return Integer.MAX_VALUE;
    }
}
