package dev.weary.zomboid.plugin;

import java.io.File;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import dev.weary.zomboid.util.reflect.ReflectionUtil;

/**
 * Allows plugins to see other plugins' classes and game classes
 */
public class UrlPluginClassLoader extends URLClassLoader implements PluginClassLoader {
	private final PluginHandler pluginHandler;
	private final Map<String, Class<?>> cachedClasses;

	@Override
	protected void finalize() throws Throwable {
		System.out.println("** UrlPluginClassLoader is finalizing!");
		super.finalize();
	}

	UrlPluginClassLoader(PluginHandler pluginHandler, File pluginJar) throws MalformedURLException {
		super(new URL[]{ pluginJar.toURI().toURL() });
		this.pluginHandler = pluginHandler;
		this.cachedClasses = new HashMap<>();
	}

	@Override
	protected Class<?> findClass(String className) throws ClassNotFoundException {
		return findClass(className, true);
	}

	public Class<?> findClass(String className, boolean checkOtherPlugins) throws ClassNotFoundException {
		Class<?> cachedClass = cachedClasses.get(className);
		if (cachedClass == null) {
			if (checkOtherPlugins) {
				cachedClass = pluginHandler.getPluginClass(className);
			}

			if (cachedClass == null) {
				cachedClass = super.findClass(className);

				if (cachedClass != null) {
					pluginHandler.addPluginClass(className, cachedClass);
				}
			}

			cachedClasses.put(className, cachedClass);
		}

		return cachedClass;
	}

	public Set<String> getCachedClasses() {
		return cachedClasses.keySet();
	}
}
