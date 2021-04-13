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
package dev.weary.zomboid.lua;

import dev.weary.zomboid.plugin.PluginHandler;
import dev.weary.zomboid.plugin.ZomboidPlugin;
import se.krka.kahlua.vm.LuaClosure;
import zombie.Lua.Event;

import java.lang.instrument.Instrumentation;
import java.util.Arrays;
import java.util.stream.Collectors;

import static dev.weary.zomboid.util.Util.transformClass;

public class EventDispatcher {
    private static PluginHandler pluginHandler;

    public EventDispatcher(Instrumentation instrumentation, PluginHandler pluginHandler) {
        EventDispatcher.pluginHandler = pluginHandler;
        transformClass(instrumentation, "zombie.Lua.Event", new PatchEvent());
    }

    public static Object[] cloneEventArgs(Object[] args) {
        if (args == null) {
            return null;
        }

        return args.clone();
    }

    public static boolean shouldTriggerEvent(LuaClosure callback, Event event, Object[] args) {
        LuaEvent luaEvent = new LuaEvent(event, callback, args);
        for (ZomboidPlugin plugin: pluginHandler.getLoadedPlugins()) {
            plugin.onLuaEvent(luaEvent);
        }

        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                args[i] = luaEvent.getArgument(i);
            }
        }

        return !luaEvent.isCancelled();
    }
}
