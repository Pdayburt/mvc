package com.lagou.demo.Controller.service.impl;

import com.lagou.demo.Controller.service.IDemoService;
import com.lagou.edu.mvcFramwork.anntation.LagouService;

@LagouService
public class DemoServiceImpl implements IDemoService {
    @Override
    public String get(String name) {
        System.out.println("DemoServiceImpl---->get------->name --->" + name);
        return name;
    }
}
