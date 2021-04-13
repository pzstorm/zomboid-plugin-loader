package dev.weary.zomboid.plugin;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Field;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.security.SecureClassLoader;
import java.security.cert.Certificate;
import java.util.*;

import dev.weary.zomboid.hook.NamedHook;
import dev.weary.zomboid.util.reflect.ReflectionUtil;

public class PluginHandler {
    private final Instrumentation instrumentation;
    private final List<ZomboidPlugin> loadedPlugins = new ArrayList<>();
    private final Map<Class<?>, String> pluginClassNameToId = new HashMap<>();
    private final Map<String, Class<?>> allPluginClasses = new HashMap<>();
    private final Map<String, PluginClassLoader> allPluginClassLoaders = new HashMap<>();
    private final Map<String, List<NamedHook>> allPluginHooks = new HashMap<>();

    public PluginHandler(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
    }

    public List<ZomboidPlugin> getLoadedPlugins() {
        return loadedPlugins;
    }

    public ZomboidPlugin loadPlugin(File pluginJar) throws IOException {
        if (!pluginJar.exists()) {
            throw new RuntimeException("Plugin JAR " + pluginJar.getPath() + " doesn't exist");
        }

        PluginMetadata pluginMetadata = new PluginMetadata(pluginJar);
        ClassLoader pluginClassLoader = new UrlPluginClassLoader(this, pluginJar);

        Class<?> mainClass;
        try {
            mainClass = Class.forName(pluginMetadata.getMainClassName(), true, pluginClassLoader);
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException("Can't load main class " + pluginMetadata.getMainClassName(), e);
        }

        Class<? extends ZomboidPlugin> pluginClass;
        try {
            pluginClass = mainClass.asSubclass(ZomboidPlugin.class);
        }
        catch (ClassCastException e) {
            throw new RuntimeException("Main class " + pluginMetadata.getMainClassName() + " does not extend ZomboidPlugin", e);
        }

        ZomboidPlugin pluginInstance;
        try {
            pluginInstance = pluginClass.newInstance();
        }
        catch (Exception e) {
            throw new RuntimeException("Can't instantiate class " + pluginMetadata.getMainClassName(), e);
        }

        allPluginHooks.put(pluginMetadata.getPluginId(), NamedHook.fromClass(pluginClass));
        pluginClassNameToId.put(pluginClass, pluginMetadata.getPluginId());

        // Initialize plugin
        pluginInstance.pluginId = pluginMetadata.getPluginId();
        pluginInstance.classLoader = (PluginClassLoader) pluginClassLoader;
        loadedPlugins.add(pluginInstance);

        return pluginInstance;
    }

    public String getPluginId(Class<? extends ZomboidPlugin> pluginClass) {
        return pluginClassNameToId.get(pluginClass);
    }

    public NamedHook getNamedHook(String pluginId, String hookName) {
        return allPluginHooks.getOrDefault(pluginId, Collections.emptyList()).stream()
                .filter(namedHook -> namedHook.getHookName().equals(hookName))
                .findFirst().orElse(null);
    }

    void removePluginClass(String className) {
        allPluginClasses.remove(className);
    }

    void addPluginClass(String className, final Class<?> classObject) {
        if (!allPluginClasses.containsKey(className)) {
            allPluginClasses.put(className, classObject);
        }
    }

    Class<?> getPluginClass(String className) {
        Class<?> pluginClass = allPluginClasses.get(className);
        if (pluginClass != null) {
            return pluginClass;
        }

        for (String pluginId: allPluginClassLoaders.keySet()) {
            PluginClassLoader pluginClassLoader = allPluginClassLoaders.get(pluginId);

            try {
                pluginClass = pluginClassLoader.findClass(className, false);
            }
            catch (Exception ignored) {}

            if (pluginClass != null) {
                return pluginClass;
            }
        }

        return null;
    }

    public void enablePlugin(ZomboidPlugin plugin) throws UnmodifiableClassException {
        if (!plugin.isEnabled) {

            if (!loadedPlugins.contains(plugin)) {
                loadedPlugins.add(plugin);
            }

            if (!allPluginClassLoaders.containsKey(plugin.pluginId)) {
                allPluginClassLoaders.put(plugin.pluginId, plugin.classLoader);
            }

            // Run enable code
            try {
                plugin.onEnable();
                plugin.isEnabled = true;
            }
            catch (Exception e) {
                throw new RuntimeException("Error enabling plugin " + plugin.pluginId, e);
            }

            // Add plugin transformers
            for (ClassTransformer classTransformer: plugin.getClassTransformers()) {
                instrumentation.addTransformer(classTransformer, true);
            }

            // Reload affected classes
            reloadAffectedClasses(instrumentation, plugin);
        }
    }

    public void disablePlugin(ZomboidPlugin plugin) throws UnmodifiableClassException {
        if (plugin.isEnabled) {

            // Run disable code
            try {
                plugin.onDisable();
                plugin.isEnabled = false;
            }
            catch (Exception e) {
                throw new RuntimeException("Error disabling plugin " + plugin.pluginId, e);
            }

            // Remove class loader
            allPluginClassLoaders.remove(plugin.pluginId);

            // Remove all plugin classes
            for (String className : plugin.classLoader.getCachedClasses()) {
                removePluginClass(className);
            }

            try {
                plugin.classLoader.close();
                plugin.classLoader.getCachedClasses().clear();

                /*Field fieldClasses = ReflectionUtil.getField(ClassLoader.class, "classes");
                ((Vector<Class<?>>) fieldClasses.get(plugin.classLoader)).clear();

                Field fieldPdCache = ReflectionUtil.getField(SecureClassLoader.class, "pdcache");
                ((HashMap<CodeSource, ProtectionDomain>) fieldPdCache.get(plugin.classLoader)).clear();

                Field fieldPackages = ReflectionUtil.getField(ClassLoader.class, "packages");
                ((HashMap<String, Package>) fieldPackages.get(plugin.classLoader)).clear();

                Field fieldDefaultDomain = ReflectionUtil.getField(ClassLoader.class, "defaultDomain");
                fieldDefaultDomain.set(plugin.classLoader, null);

                Field fieldAssertionLock = ReflectionUtil.getField(ClassLoader.class, "assertionLock");
                fieldAssertionLock.set(plugin.classLoader, null);

                Field fieldPackage2certs = ReflectionUtil.getField(ClassLoader.class, "package2certs");
                ((Map<String, Certificate[]>) fieldPackage2certs.get(plugin.classLoader)).clear();*/
            }
            catch (Exception e) {
                throw new RuntimeException("Error closing plugin's class loader", e);
            }

            // Remove from plugins list
            loadedPlugins.remove(plugin);

            // Remove class to id mapping
            pluginClassNameToId.remove(plugin.getClass());

            // Remove plugin hooks
            allPluginHooks.get(plugin.pluginId).clear();
            allPluginHooks.remove(plugin.pluginId);

            // Remove plugin transformers
            for (ClassTransformer classTransformer: plugin.getClassTransformers()) {
                instrumentation.removeTransformer(classTransformer);
            }

            // Reload affected classes
            reloadAffectedClasses(instrumentation, plugin);

            // Clear transformer references
            plugin.affectedClasses.clear();
            plugin.classTransformers.clear();

        }
    }

    private enum ReloadStrategy {
        REDEFINE, RETRANSFORM
    }

    private static final ReloadStrategy RELOAD_STRATEGY = ReloadStrategy.RETRANSFORM;

    private static void reloadAffectedClasses(Instrumentation instrumentation, ZomboidPlugin plugin) {
        try {
            if (RELOAD_STRATEGY == ReloadStrategy.REDEFINE) {
                ClassDefinition[] classDefinitions = plugin.getClassDefinitions();
                if (classDefinitions != null) {
                    instrumentation.redefineClasses(classDefinitions);
                }
            }
            else {
                Class<?>[] affectedClasses = plugin.getAffectedClasses();
                if (affectedClasses != null) {
                    instrumentation.retransformClasses(affectedClasses);
                }
            }
        }
        catch (Exception e) {
            throw new RuntimeException("Could not reload affected classes", e);
        }
    }
}
