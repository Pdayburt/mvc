package com.lagou.edu.mvcFramwork.anntation;

import java.lang.annotation.*;

@Documented
@Target({ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface LagouRequestMapping {
    String value() default "";
}
