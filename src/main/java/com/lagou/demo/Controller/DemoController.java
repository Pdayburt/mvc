package com.lagou.demo.Controller;

import com.lagou.demo.Controller.service.IDemoService;
import com.lagou.edu.mvcFramwork.anntation.LagouAutowired;
import com.lagou.edu.mvcFramwork.anntation.LagouController;
import com.lagou.edu.mvcFramwork.anntation.LagouRequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@LagouController
@LagouRequestMapping("/demo")
public class DemoController {
    @LagouAutowired
    private IDemoService demoService;

    /**
     * url /demo/query
     * @param request
     * @param response
     * @param name
     * @return
     */
    @LagouRequestMapping("/query")
    public String query(HttpServletRequest request,
                        HttpServletResponse response,
                        String name){
        return demoService.get(name);
    }

}
