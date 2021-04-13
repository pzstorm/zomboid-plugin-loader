package dev.weary.zomboid.plugin;

import java.io.Closeable;
import java.io.File;
import java.util.Set;

public interface PluginClassLoader extends Closeable {
	Class<?> findClass(String className, boolean checkOtherPlugins) throws ClassNotFoundException;
	Set<String> getCachedClasses();
}
