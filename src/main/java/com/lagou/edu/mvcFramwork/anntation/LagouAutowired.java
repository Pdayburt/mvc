package com.lagou.edu.mvcFramwork.anntation;

import java.lang.annotation.*;

@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LagouAutowired {
    String value() default "";
}
