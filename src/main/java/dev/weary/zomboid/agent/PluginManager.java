/*
 * Zomboid Plugin Loader - Java modding tool for Project Zomboid
 * Copyright (C) 2021 00c1
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
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
