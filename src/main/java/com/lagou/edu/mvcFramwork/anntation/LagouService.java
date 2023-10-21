package com.lagou.edu.mvcFramwork.anntation;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface LagouService {
    String value() default "";
}
