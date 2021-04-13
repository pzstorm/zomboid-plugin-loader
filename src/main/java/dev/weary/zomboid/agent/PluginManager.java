package dev.weary.zomboid.agent;

import dev.weary.zomboid.hook.NamedHook;
import dev.weary.zomboid.lua.EventDispatcher;
import dev.weary.zomboid.plugin.PluginHandler;
import dev.weary.zomboid.plugin.ZomboidPlugin;

import java.io.File;
import java.lang.instrument.Instrumentation;

public class PluginManager {
    private final PluginHandler pluginHandler;
    private final EventDispatcher eventDispatcher;

    public PluginManager(Instrumentation instrumentation) {
        this.pluginHandler = new PluginHandler(instrumentation);
        this.eventDispatcher = new EventDispatcher(instrumentation, pluginHandler);
    }

    public NamedHook getPluginHook(String pluginId, String hookName) {
        return pluginHandler.getNamedHook(pluginId, hookName);
    }

    public String getPluginId(Class<? extends ZomboidPlugin> pluginClass) {
        return pluginHandler.getPluginId(pluginClass);
    }

    ZomboidPlugin loadPlugin(File pluginJar) {
        try {
            return pluginHandler.loadPlugin(pluginJar);
        }
        catch (Exception e) {
            throw new RuntimeException("Error loading plugin " + pluginJar.getPath(), e);
        }
    }

    PluginHandler getPluginHandler() {
        return pluginHandler;
    }

    void enablePlugin(ZomboidPlugin plugin) {
        try {
            pluginHandler.enablePlugin(plugin);
        }
        catch (Exception e) {
            throw new RuntimeException("Error enabling plugin " + plugin.getPluginId(), e);
        }
    }

    void disablePlugin(ZomboidPlugin plugin){
        try {
            pluginHandler.disablePlugin(plugin);
        }
        catch (Exception e) {
            throw new RuntimeException("Error disabling plugin " + plugin.getPluginId(), e);
        }
    }
}
