package com.lagou.edu.mvcFramwork.pojo;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 封装handler方法相关的信息
 */
public class Handler {
    private Object Controller;
    private Method method;
    private Pattern pattern;//spring中url是支持正则的
    /**
     * /参数顺序 为了进行参数绑定 key是参数名 value代表是第几个参数
     */
    private Map<String,Integer> paramIndexMapping;

    public Handler(Object controller, Method method, Pattern pattern) {
        Controller = controller;
        this.method = method;
        this.pattern = pattern;
        this.paramIndexMapping = new HashMap<>();
    }

    public Object getController() {
        return Controller;
    }

    public void setController(Object controller) {
        Controller = controller;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    public Map<String, Integer> getParamIndexMapping() {
        return paramIndexMapping;
    }

    public void setParamIndexMapping(Map<String, Integer> paramIndexMapping) {
        this.paramIndexMapping = paramIndexMapping;
    }
}
