package dev.weary.zomboid.plugin;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

public class PluginHandler {
    private final Instrumentation instrumentation;
    private final List<ZomboidPlugin> loadedPlugins = new ArrayList<>();

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

        JarFile pluginJarFile = new JarFile(pluginJar);
        PluginMetadata pluginMetadata = new PluginMetadata(pluginJarFile.getManifest().getMainAttributes());

        URLClassLoader temporaryLoader = new URLClassLoader(new URL[] { pluginJar.toURI().toURL() });

        Class<?> mainClass;
        try {
            mainClass = Class.forName(pluginMetadata.getMainClassName(), true, temporaryLoader);
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException("Can't find main class " + pluginMetadata.getMainClassName(), e);
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

        // Append to system class path
        instrumentation.appendToSystemClassLoaderSearch(pluginJarFile);

        // Initialize plugin
        pluginInstance.pluginId = pluginMetadata.getPluginId();
        loadedPlugins.add(pluginInstance);

        return pluginInstance;
    }

    public void enablePlugin(ZomboidPlugin plugin) throws UnmodifiableClassException, ClassNotFoundException {
        if (!plugin.isEnabled) {

            if (!loadedPlugins.contains(plugin)) {
                loadedPlugins.add(plugin);
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
            instrumentation.redefineClasses(plugin.getAffectedClasses().toArray(new ClassDefinition[0]));
        }
    }

    public void disablePlugin(ZomboidPlugin plugin) throws UnmodifiableClassException, ClassNotFoundException {
        if (plugin.isEnabled) {


            // Run disable code
            try {
                plugin.onDisable();
                plugin.isEnabled = false;
            }
            catch (Exception e) {
                throw new RuntimeException("Error disabling plugin " + plugin.pluginId, e);
            }

            // Remove from plugins list
            loadedPlugins.remove(plugin);

            // Remove plugin transformers
            for (ClassTransformer classTransformer: plugin.getClassTransformers()) {
                instrumentation.removeTransformer(classTransformer);
            }

            // Reload affected classes
            instrumentation.redefineClasses(plugin.getAffectedClasses().toArray(new ClassDefinition[0]));
        }
    }
}
