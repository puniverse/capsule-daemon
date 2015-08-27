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
import java.util.Arrays;

/**
 *
 * @author circlespainter
 */
public class DaemonAdapter {

	public static final String PROP_INIT = "capsule.daemon.init";
	public static final String PROP_START = "capsule.daemon.start";
	public static final String PROP_STOP = "capsule.daemon.stop";
	public static final String PROP_DESTROY = "capsule.daemon.destroy";

	private static String[] mainArgs;

	public static void init(String args[]) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, ClassNotFoundException {
		mainArgs = (String[]) i(p(PROP_INIT), args);
	}
	public static void start() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, ClassNotFoundException {
		i(p(PROP_START), new Object[] { mainArgs }, STRING_ARRAY_ARG_TYPES);
	}
	public static void stop() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, ClassNotFoundException {
		i(p(PROP_STOP));
	}
	public static void destroy() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, ClassNotFoundException {
		i(p(PROP_DESTROY));
	}

	private static String p(String s) {
		return System.getProperty(s);
	}

	private static Object i(String methodSpec, Object o, Object[] args, Class[] argTypes) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, ClassNotFoundException {
		if (methodSpec != null)
			return m(methodSpec, argTypes).invoke(o, args);
		return null;
	}
	private static Object i(String methodSpec) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, ClassNotFoundException {
		return i(methodSpec, null, null, null);
	}
	private static final Class[] STRING_ARRAY_ARG_TYPES = new Class[]{String[].class};
	private static Object i(String methodSpec, String[] args) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, ClassNotFoundException {
		return i(methodSpec, args, args != null ? STRING_ARRAY_ARG_TYPES : null);
	}
	private static Object i(String methodSpec, Object[] args, Class[] types) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, ClassNotFoundException {
		return i(methodSpec, null, args, types);
	}

	private static Method m(String methodSpec, Class[] types) throws ClassNotFoundException, NoSuchMethodException {
		System.out.println("Method: " + methodSpec + "(" + Arrays.toString(types) + ")");
		final String c = methodSpec.substring(0, methodSpec.lastIndexOf('.'));
		final String m = methodSpec.substring(methodSpec.lastIndexOf('.') + 1);
		return DaemonAdapter.class.getClassLoader().loadClass(c).getMethod(m, types);
	}
}
