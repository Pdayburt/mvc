package com.lagou.edu.mvcFramwork.servlet;


import com.lagou.edu.mvcFramwork.anntation.LagouAutowired;
import com.lagou.edu.mvcFramwork.anntation.LagouController;
import com.lagou.edu.mvcFramwork.anntation.LagouRequestMapping;
import com.lagou.edu.mvcFramwork.anntation.LagouService;
import com.lagou.edu.mvcFramwork.pojo.Handler;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LgDispatcherServlet extends HttpServlet {

    private Properties properties = new Properties();

    /**
     * 缓存扫描到的全限定类名
     */
    private List<String> classNameList = new ArrayList<>();

    //IOC 容器
    private Map<String, Object> ioc = new HashMap<>();

    //handlerMapping 存储url和method之间的映射关系
    // private Map<String,Method> handlerMapping = new HashMap<>();
    private List<Handler> handlerMapping = new ArrayList<>();

    @Override
    public void init(ServletConfig config) throws ServletException {
        //加载配置文件
        String contextConfigLocation = config.getInitParameter("contextConfigLocation");
        try {
            doLoadConfig(contextConfigLocation);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //扫描注解
        doScan(properties.getProperty("scanPackage"));

        //初始化bean
        try {
            doInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        //依赖注入
        try {
            doAutowired();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        /**
         * 构造一个handleMapping处理映射起 将配置好的url和method建立映射关系
         * 最关键的环节
         */

        try {
            doInitHandleMapping();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        System.out.println("LgDispatcherServlet 初始化完成");
        //等待请求进入处理

    }

    private void doInitHandleMapping() throws InstantiationException, IllegalAccessException {
        if (ioc.isEmpty()) return;
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            //获取IOC中当前遍历的对象的class
            Class<?> aClass = entry.getValue().getClass();
            String baseUrl = null;
            if (aClass.isAnnotationPresent(LagouController.class)) {
                if (aClass.isAnnotationPresent(LagouRequestMapping.class)) {
                    LagouRequestMapping annotation = aClass.getAnnotation(LagouRequestMapping.class);
                    baseUrl = annotation.value();//  == /demo
                }
            }
            Method[] methods = aClass.getMethods();
            for (int i = 0; i < methods.length; i++) {
                Method method = methods[i];
                if (method.isAnnotationPresent(LagouRequestMapping.class)) {
                    LagouRequestMapping annotation = method.getAnnotation(LagouRequestMapping.class);
                    String methodUrl = annotation.value();
                    String url = baseUrl + methodUrl; //  /demo/query
                    //建立URL和method之间的关系 handlerMapping.put(url,method);
                    /**
                     * 将method有关的信息全部封装
                     */
                    Handler handler = new Handler(entry.getValue(), method, Pattern.compile(url));
                    /**
                     * 处理参数信息
                     * HttpServletRequest request,
                     * HttpServletResponse response,
                     * String name
                     */
                    Parameter[] parameters = method.getParameters();
                    Map<String, Integer> paramIndexMapping = handler.getParamIndexMapping();
                    for (int j = 0; j < parameters.length; j++) {
                        Parameter parameter = parameters[j];
                        //如果request、response对象那么参数民称写HttpServletRequest HttpServletResponse
                        if (parameter.getType() == HttpServletRequest.class || parameter.getType() == HttpServletResponse.class) {
                            paramIndexMapping.put(parameter.getType().getName(), j);
                        } else {
                            paramIndexMapping.put(parameter.getName(), j);//<name,2>
                        }
                    }
                    handlerMapping.add(handler);

                }
            }
        }
    }


    /**
     * 实现依赖注入
     */
    private void doAutowired() throws IllegalAccessException {

        if (ioc.isEmpty()) {
            //有对象才进行依赖注入
            //遍历ioc中的对下岗 查看对象总的字段是否有@autowired
            for (Map.Entry<String, Object> entry : ioc.entrySet()) {
                //获取bean对象中的字段信息
                Field[] declaredFields = entry.getValue().getClass().getDeclaredFields();
                for (int i = 0; i < declaredFields.length; i++) {//@LagouAutowired private IDemoService demoService;
                    Field declaredField = declaredFields[i];
                    if (declaredField.isAnnotationPresent(LagouAutowired.class)) {
                        LagouAutowired lagouAutowired = declaredField.getAnnotation(LagouAutowired.class);
                        String beanName = lagouAutowired.value();
                        //没有具体的beanName 根据类型注入(接口注入)
                        if (beanName.isEmpty()) {
                            beanName = declaredField.getType().getName();
                        }
                        //开启赋值
                        declaredField.setAccessible(true);
                        declaredField.set(entry.getValue(), ioc.get(beanName));

                    }

                }
            }
        }
    }

    /**
     * IOC容器
     * 基于className缓存的类的全限定类名 以及反射技术 完成对象创建和管理
     */

    private void doInstance() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        if (classNameList.isEmpty()) return;
        for (int i = 0; i < classNameList.size(); i++) {
            String className = classNameList.get(i);  //com.lagou.demo.controller.DEmoController
            Class<?> aClass = Class.forName(className);
            /**
             * 区分controller service
             */
            if (aClass.isAnnotationPresent(LagouController.class)) {
                /**
                 * 此处不做过多处理 不取value 直接那类的首字母小写作为id 保存值ioc
                 */
                String simpleName = aClass.getSimpleName(); // DemoController
                String lowerFirstSimpleName = lowerFirst(simpleName);
                Object o = aClass.newInstance();
                ioc.put(lowerFirstSimpleName, o);
            } else if (aClass.isAnnotationPresent(LagouService.class)) {
                LagouService annotation = aClass.getAnnotation(LagouService.class);
                String beanName = annotation.value();
                if (!beanName.isEmpty()) {
                    ioc.put(beanName, aClass.newInstance());
                } else {
                    beanName = lowerFirst(aClass.getSimpleName());
                    ioc.put(beanName, aClass.newInstance());
                }
                /**
                 * service一般是面向接口开发的 此时在一接口名韦id再放入一份进ioc 一边Autowired的使用
                 */
                Class<?>[] interfaces = aClass.getInterfaces();
                for (int j = 0; j < interfaces.length; j++) {
                    Class<?> anInterface = interfaces[j];
                    //以接口的全类限定名注入com.lagou.demo.Controller.service.IDemoService
                    ioc.put(anInterface.getName(), aClass.newInstance());
                }

            }
        }
    }

//    public static void main(String[] args) {
//        LagouService annotation = DemoServiceImpl.class.getAnnotation(LagouService.class);
//        Class<DemoServiceImpl> demoServiceClass = DemoServiceImpl.class;
//        Class<?>[] interfaces = demoServiceClass.getInterfaces();
//        Arrays.stream(interfaces).forEach(s->System.out.println(s.getName()));
//
//    }

    public String lowerFirst(String str) {
        char[] charArray = str.toCharArray();
        if ('A' <= charArray[0] && charArray[0] <= 'Z') {
            charArray[0] += 32;
        }
        return String.valueOf(charArray);
    }


    /**
     * 扫描类
     * scanPackage ： com.lagou.demo package--->磁盘上的文件（file） com/lagou/demo
     *
     * @param scanPackage
     */
    private void doScan(String scanPackage) {
        String classPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
        String scanPackageDiskPath = classPath + scanPackage.replaceAll(".", "/");
        File pack = new File(scanPackageDiskPath);
        File[] files = pack.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                doScan(scanPackage + "." + file.getName());//com.lagou.demo.controller
            } else if (file.getName().endsWith(".class")) {
                String className = scanPackage + "." + file.getName().replaceAll(".class", "");
                classNameList.add(className);
            }
        }


    }


    private void doLoadConfig(String contextConfigLocation) throws IOException {
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);

        properties.load(resourceAsStream);


    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//        //处理请求根据url找到对应的额method进行调用
//        String requestURI = req.getRequestURI();
//        Method method = handlerMapping.get(requestURI);
//        //反射调用 如何通过方法找到对象？
////        method.invoke()

        Handler handler = getHandler(req);
        if (handler == null) {
            resp.getWriter().write("404 not found");
            return;
        }
        //最终调用method方法
        Method method = handler.getMethod();
        Class<?>[] parameterTypes = method.getParameterTypes();
        //参数数组 反射调用
        Object[] paraValues = new Object[parameterTypes.length];
        //下参数数组中塞值 并且保证和行参顺序一直
        Map<String, String[]> parameterMap = req.getParameterMap();

        Map<String, Integer> paramIndexMapping = handler.getParamIndexMapping();

        //遍历req中的所有参数
        for (Map.Entry<String, String[]> param : parameterMap.entrySet()) {
            //name=1&name=2
            String value = StringUtils.join(param.getValue(), ",");//如同 1,2
            //如果参数和方法中的参数匹配上了 -->填充数据
            if (paramIndexMapping.containsKey(param.getKey())){
                Integer index = paramIndexMapping.get(param.getKey()); //2
                paraValues[index] = value;//前台传入的值填充至对应的位置 （普通参数 req reps 特殊处理）
            }
        }
        Integer requestIndex = paramIndexMapping.get(HttpServletRequest.class.getName());
        paraValues[requestIndex] = requestIndex;
        Integer responseIndex = paramIndexMapping.get(HttpServletResponse.class.getName());
        paraValues[responseIndex] = responseIndex;

        Object controller = handler.getController();

        try {
            method.invoke(controller,paraValues);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private Handler getHandler(HttpServletRequest req) {
        if (handlerMapping.isEmpty()) return null;
        String requestURI = req.getRequestURI();
        for (Handler handler : handlerMapping) {
            Matcher matcher = handler.getPattern().matcher(requestURI);
            if (matcher.matches()) return handler;
        }
        return null;
    }


}
