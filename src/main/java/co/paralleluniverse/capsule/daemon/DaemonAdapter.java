/*
 * Capsule
 * Copyright (c) 2015, Parallel Universe Software Co. and Contributors. All rights reserved.
 *
 * This program and the accompanying materials are licensed under the terms
 * of the Eclipse Public License v1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package co.paralleluniverse.capsule.daemon;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author circlespainter
 */
public class DaemonAdapter {

    public static final String PROP_INIT_CLASS = "capsule.daemon.initClass";
    public static final String PROP_INIT_METHOD = "capsule.daemon.initMethod";
    public static final String PROP_START_CLASS = "capsule.daemon.startClass";
    public static final String PROP_START_METHOD = "capsule.daemon.startMethod";
    public static final String PROP_STOP_CLASS = "capsule.daemon.stopClass";
    public static final String PROP_STOP_METHOD = "capsule.daemon.stopMethod";
    public static final String PROP_DESTROY_CLASS = "capsule.daemon.destroyClass";
    public static final String PROP_DESTROY_METHOD = "capsule.daemon.destroyMethod";

    private static String[] mainArgs;

    public static void init(String args[]) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, ClassNotFoundException {
        mainArgs = (String[]) i(p(PROP_INIT_CLASS), p(PROP_INIT_METHOD), args);
    }

    public static void main(String[] args) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, ClassNotFoundException {
        i(p(PROP_START_CLASS), p(PROP_START_METHOD), new Object[]{args}, STRING_ARRAY_ARG_TYPES);
    }

    public static void start() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, ClassNotFoundException {
        main(mainArgs);
    }

    public static void stop() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, ClassNotFoundException {
        i(p(PROP_STOP_CLASS), p(PROP_STOP_METHOD));
    }

    public static void destroy() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, ClassNotFoundException {
        i(p(PROP_DESTROY_CLASS), p(PROP_DESTROY_METHOD));
    }

    private static String p(String s) {
        return System.getProperty(s);
    }

    private static Object i(String className, String methodName, Object o, Object[] args, Class[] argTypes) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, ClassNotFoundException {
        if (className != null && methodName != null)
            return m(className, methodName, argTypes).invoke(o, args);
        return args;
    }

    private static Object i(String className, String methodName) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, ClassNotFoundException {
        return i(className, methodName, null, null, null);
    }

    private static final Class[] STRING_ARRAY_ARG_TYPES = new Class[]{String[].class};

    private static Object i(String className, String methodName, String[] args) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, ClassNotFoundException {
        return i(className, methodName, args, args != null ? STRING_ARRAY_ARG_TYPES : null);
    }

    private static Object i(String className, String methodName, Object[] args, Class[] types) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, ClassNotFoundException {
        return i(className, methodName, null, args, types);
    }

    private static Method m(String className, String methodName, Class[] types) throws ClassNotFoundException, NoSuchMethodException {
        return DaemonAdapter.class.getClassLoader().loadClass(className).getMethod(methodName, types);
    }
}
