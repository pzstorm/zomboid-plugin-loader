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
