package dev.weary.zomboid.plugin;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * Allows the game to see plugin classes
 */
public class GameClassLoader extends URLClassLoader {
	private final PluginHandler pluginHandler;

	public GameClassLoader(ClassLoader parentLoader, PluginHandler pluginHandler) {
		super(new URL[]{}, parentLoader);
		this.pluginHandler = pluginHandler;
	}

	@Override
	protected Class<?> findClass(String className) throws ClassNotFoundException {
		Class<?> pluginClass = pluginHandler.getPluginClass(className);
		if (pluginClass == null) {
			pluginClass = super.findClass(className);
		}

		System.out.printf(":: findClass(%s) -> %s\n", className, pluginClass.hashCode());
		return pluginClass;
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		Class<?> classObject = super.loadClass(name, resolve);
		System.out.printf(":: loadClass(%s, %s) -> %s\n", name, resolve, classObject.hashCode());
		return classObject;
	}
}
