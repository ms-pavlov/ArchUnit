package org.example.repository;

import org.example.EaistRequestContext;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

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
