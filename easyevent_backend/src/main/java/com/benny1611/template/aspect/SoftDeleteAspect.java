package com.benny1611.template.aspect;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class SoftDeleteAspect {

    @PersistenceContext
    private EntityManager entityManager;

    @Before("execution(* com.benny1611.easyevent.service.*.*(..)) " +
            "&& !execution(* com.benny1611.easyevent.auth.*.*(..))")
    public void enableSoftDeleteFilter() {
        entityManager.unwrap(Session.class).enableFilter("deletedUserFilter");
    }
}
