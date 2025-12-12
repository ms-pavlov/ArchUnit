package ru.proitr.controller;

import org.springframework.transaction.annotation.Transactional;
import ru.proitr.Secure;

public interface SomeBadControllerInterface {

    @Secure
    @Transactional
    void invokeWithBadAnnotation();

}
