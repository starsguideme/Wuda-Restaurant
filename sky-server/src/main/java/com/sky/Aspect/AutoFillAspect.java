package com.sky.Aspect;

import com.sky.Annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.aspectj.weaver.JoinPointSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Aspect
@Component
@Slf4j
public class AutoFillAspect {
    /**
     * 拦截需要自动填充的函数
     *
     */
  @Pointcut("execution(* com.sky.mapper.*.*(..)) &&@annotation(com.sky.Annotation.AutoFill)")
    public void autoFillPointCut() {
    }
    // 再填充前判断是否需要填充
    @Before("autoFillPointCut()")
    public void autoFill(JoinPoint joinPoint)
    {
        log.info("开始进行数据填充");
        //获取当前的方法参数
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        //获取方法上的注解
        AutoFill autofill = signature.getMethod().getAnnotation(AutoFill.class);
        //获取当前注解中定义的操作类型
        OperationType operationType = autofill.value();
        //获取当前方法参数对象
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return;
        }
        Object entity = args[0];
        LocalDateTime now = LocalDateTime.now();
        Long currentId= BaseContext.getCurrentId();
        if (operationType == OperationType.INSERT) {
            //为插入操作的字段赋值
            //设置创建时间、更新时间、创建人、更新人
            //log.info("为{}准备赋值",object.getClass());
            //log.info("为{}准备赋值",object.getClass().getSimpleName());
            //log.info("为{}准备赋值",object.getClass().getSimpleName());
            try {
                Method setCreateTime = entity.getClass().getMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);
                Method setUpdateTime = entity.getClass().getMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setCreateUser = entity.getClass().getMethod(AutoFillConstant.SET_CREATE_USER, Long.class);
                Method setUpdateUser = entity.getClass().getMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);
                setCreateTime.invoke(entity, now);
                setUpdateTime.invoke(entity, now);
                setCreateUser.invoke(entity, currentId);
                setUpdateUser.invoke(entity, currentId);
            } catch (Exception e) {
                e.printStackTrace();
            }
         }
        else if(operationType == OperationType.UPDATE)
        {
            try {
                Method setUpdateTime = entity.getClass().getMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);
                setUpdateTime.invoke(entity, now);
                setUpdateUser.invoke(entity, currentId);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
