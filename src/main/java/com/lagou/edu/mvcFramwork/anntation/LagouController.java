package com.lagou.edu.mvcFramwork.anntation;

import java.lang.annotation.*;


@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface LagouController {
    String value() default "";
}
