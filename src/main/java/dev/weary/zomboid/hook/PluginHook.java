package dev.weary.zomboid.hook;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import dev.weary.zomboid.agent.Agent;
import dev.weary.zomboid.plugin.ZomboidPlugin;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;

public class PluginHook {
	private static final MethodType BOOTSTRAP_TYPE = MethodType.methodType(
			CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class,
			String.class, String.class
	);

	private static final Handle BOOTSTRAP_HANDLE = new Handle(
			Opcodes.H_INVOKESTATIC,
			Type.getInternalName(PluginHook.class),
			"bootstrap",
			BOOTSTRAP_TYPE.toMethodDescriptorString(),
			false
	);

	public static CallSite bootstrap(MethodHandles.Lookup caller, String name, MethodType type, String pluginId, String hookName) throws NoSuchMethodException, IllegalAccessException {
		NamedHook namedHook = Agent.getPluginManager().getPluginHook(pluginId, hookName);
		return new ConstantCallSite(namedHook.getMethodHandle());
	}

	public static InvokeDynamicInsnNode getInvokeInsn(Class<? extends ZomboidPlugin> pluginClass, String hookName) {
		String pluginId = Agent.getPluginManager().getPluginId(pluginClass);
		NamedHook namedHook = Agent.getPluginManager().getPluginHook(pluginId, hookName);
		if (namedHook == null) {
			throw new RuntimeException("Plugin " + pluginClass.getSimpleName() + " does not have a hook named " + hookName);
		}

		return new InvokeDynamicInsnNode(
			namedHook.getMethodName(),
			namedHook.getMethodHandle().type().toMethodDescriptorString(),
			BOOTSTRAP_HANDLE,
			pluginId, namedHook.getHookName()
		);
	}
}
