package dev.weary.zomboid.hook;

import java.lang.invoke.*;
import java.util.Arrays;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;

public class NativeHook {
	public static native void callVoid(String name, Object... args);
	public static native int callInt(String name, Object... args);
	public static native Object callObject(String name, Object... args);

	public static CallSite bootstrap(MethodHandles.Lookup caller, String name, MethodType type) throws Exception {
		if (type.returnType() == int.class) {
			MethodHandle callInt = getNativeMethod("callInt", int.class);
			return new ConstantCallSite(callInt.bindTo(name).asVarargsCollector(Object[].class).asType(type));
		}

		return null;
	}

	private static MethodHandle getNativeMethod(String callName, Class<?> returnType) throws Exception {
		return MethodHandles.lookup().findStatic(NativeHook.class, callName,
			MethodType.methodType(returnType, String.class, Object[].class));
	}

	private static final MethodType BOOTSTRAP_TYPE = MethodType.methodType(
			CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class
	);

	private static final Handle BOOTSTRAP_HANDLE = new Handle(
			Opcodes.H_INVOKESTATIC,
			Type.getInternalName(NativeHook.class),
			"bootstrap",
			BOOTSTRAP_TYPE.toMethodDescriptorString(),
			false
	);



	public static InvokeDynamicInsnNode getInvokeInsn(String name, String desc) {
		return new InvokeDynamicInsnNode(name, desc, BOOTSTRAP_HANDLE);
	}
}
