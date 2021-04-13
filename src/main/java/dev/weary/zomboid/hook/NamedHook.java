package dev.weary.zomboid.hook;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import dev.weary.zomboid.plugin.ZomboidPlugin;

public class NamedHook {
	private static final int PUBLIC_STATIC = Modifier.PUBLIC | Modifier.STATIC;

	private final String hookName;
	private final String methodName;
	private final MethodHandle methodHandle;

	public String getHookName() {
		return hookName;
	}

	public String getMethodName() {
		return methodName;
	}

	public MethodHandle getMethodHandle() {
		return methodHandle;
	}

	private NamedHook(String hookName, String methodName, MethodHandle methodHandle) {
		this.hookName = hookName;
		this.methodName = methodName;
		this.methodHandle = methodHandle;
	}

	public static NamedHook fromMethod(Class<? extends ZomboidPlugin> pluginClass, Method hookMethod) {
		Hook hookInfo = methodToHook(hookMethod);
		if (hookInfo == null) {
			return null;
		}

		if ((hookMethod.getModifiers() & PUBLIC_STATIC) == 0) {
			throw new RuntimeException("Hook method " + hookMethod.getName() + " in class " + pluginClass.getSimpleName() + " must be declared public and static");
		}

		MethodType methodType = MethodType.methodType(hookMethod.getReturnType(), hookMethod.getParameterTypes());
		MethodHandle methodHandle;
		try {
			methodHandle = MethodHandles.lookup().findStatic(pluginClass, hookMethod.getName(), methodType);
		}
		catch (Exception e) {
			throw new RuntimeException("Couldn't lookup hook method " + hookMethod.getName() + " in class " + pluginClass.getSimpleName(), e);
		}

		return new NamedHook(hookInfo.value(), hookMethod.getName(), methodHandle);
	}

	public static List<NamedHook> fromClass(Class<? extends ZomboidPlugin> pluginClass) {
		return Arrays.stream(pluginClass.getDeclaredMethods())
			.map(hookMethod -> fromMethod(pluginClass, hookMethod))
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
	}

	private static Hook methodToHook(Method hookMethod) {
		try {
			return hookMethod.getAnnotation(Hook.class);
		}
		catch (Exception ignored) {}
		return null;
	}
}
