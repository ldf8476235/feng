package com.yyds.feng.common.aspect;

import com.google.gson.Gson;
import com.yyds.feng.common.util.HttpContextUtils;
import lombok.extern.slf4j.Slf4j;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

/**
 * @Desc
 * @Author Lee
 * @Date 2022/6/23 16:38
 */
@Slf4j
@Aspect
@Component
public class SysLogAspect {


//    @Pointcut("execution(* com.ec.cies.controller.*.*(..))")
//    public void logPointCut() {
//
//    }

//    @Pointcut("@annotation(io.swagger.annotations.ApiOperation)")
//    public void logPointCut() {
//
//    }

//    @Around("logPointCut()")
//    public Object around(ProceedingJoinPoint point) throws Throwable {
//        log.info("<-----------Start----------->");
//        MethodSignature signature = (MethodSignature) point.getSignature();
//        String userName = "feng";
//        log.info("user:"+ userName);
//        HttpServletRequest httpServletRequest = HttpContextUtils.getHttpServletRequest();
//        log.info("IP:"+ HttpContextUtils.getIPAddress());
//        log.info("url:"+ httpServletRequest.getRequestURI());
//        Method method = signature.getMethod();
//        Object[] args = point.getArgs();
//        try{
//            String params = new Gson().toJson(args);
//            log.info("args:"+ params);
//        }catch (Exception e){
//
//        }
//        //执行方法
//        long beginTime = System.currentTimeMillis();
//        Object result = point.proceed();
//        long time = System.currentTimeMillis() - beginTime;
//        if(result.toString().length()<1000){
//            log.info("return args:" + result.toString());
//        }else{
//            log.info("return args length:" + result.toString().length());
//        }
//        log.info("<---------End("+time+"ms)--------->",time);
//        return result;
//    }
}