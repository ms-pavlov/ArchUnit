package ru.proitr.repository;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import ru.proitr.EaistRequestContext;

@Repository
public class BadRepository {

    private DSLContext dslContext;

    private EaistRequestContext eaistRequestContext;

    public BadRepository(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    public void invokeBadRequest() {
        dslContext.query("")
                .execute();
    }
}
