/*
 * GSC (Global Social Chain), a blockchain fit for mass adoption and
 * a sustainable token economy model, is the decentralized global social
 * chain with highly secure, low latency, and near-zero fee transactional system.
 *
 * gsc-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * License GSC-Core is under the GNU General Public License v3. See LICENSE.
 */

package org.gsc.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.springframework.util.ReflectionUtils;

public class ReflectUtils {

  public static Object getFieldObject(Object target, String fieldName) {
    Field field = ReflectionUtils.findField(target.getClass(), fieldName);
    ReflectionUtils.makeAccessible(field);
    return ReflectionUtils.getField(field, target);
  }

  public static <T> T getFieldValue(Object target, String fieldName) {
    Field field = ReflectionUtils.findField(target.getClass(), fieldName);
    ReflectionUtils.makeAccessible(field);
    return (T) ReflectionUtils.getField(field, target);
  }

  public static <T> T invokeMethod(Object target, String methodName) {
    Method method = ReflectionUtils.findMethod(target.getClass(), methodName);
    ReflectionUtils.makeAccessible(method);
    return (T) ReflectionUtils.invokeMethod(method, target);
  }
  
  public static void setFieldValue(Object target, String fieldName, Object value) {
    Field field = ReflectionUtils.findField(target.getClass(), fieldName);
    ReflectionUtils.makeAccessible(field);
    ReflectionUtils.setField(field, target, value);
  }

  public static void invokeMethod(Object target, String methodName, Class[] param, Object... args) {
    Method method = ReflectionUtils.findMethod(target.getClass(), methodName, param);
    ReflectionUtils.makeAccessible(method);
    ReflectionUtils.invokeMethod(method, target, args);
  }

}
