package org.example.controller;

import org.example.Secure;
import org.springframework.transaction.annotation.Transactional;

public interface SomeBadControllerInterface {

    @Secure
    @Transactional
    void invokeWithBadAnnotation();

}
