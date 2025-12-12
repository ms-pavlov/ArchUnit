package ru.proitr.service;

import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.proitr.controller.internal.SomeBadInternalController;
import ru.proitr.generated.tables.daos.DAOImpl;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class BadServiceClass {

    @Value("${java(Integer.ZERO)}")
    private Integer value;

    private final ExecutorService executors = Executors.newCachedThreadPool();

    private SomeBadInternalController someBadInternalController;
    private DAOImpl dao;
    private DSLContext dslContext;

    @Autowired
    public BadServiceClass() throws RuntimeException {
        throw new RuntimeException();
    }

    @Value("${java(Integer.ZERO)}")
    public Integer getZero(Integer zero) throws Error {
        return zero;
    }
}
