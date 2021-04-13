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

import dev.weary.zomboid.agent.ui.UI;
import dev.weary.zomboid.plugin.GameClassLoader;
import dev.weary.zomboid.plugin.ZomboidPlugin;
import sun.misc.Launcher;
import sun.misc.URLClassPath;
import zombie.gameStates.MainScreenState;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Agent {
    private static PluginManager pluginManager;
    private static AgentOptions agentOptions;

    // Static load using the -javaagent parameter (before the game starts)
    public static void premain(String args, Instrumentation inst) {
        start(args, inst);
    }

    // Dynamic load using the Attach API (after the game starts)
    public static void agentmain(String args, Instrumentation inst) {
        start(args, inst);
    }

    private static void start(String args, Instrumentation inst) {
        if (wasAlreadyInitialized()) {
            int dialogChoice = UI.showYesNoDialog("Loader is already loaded",
                "The Zomboid Plugin Loader was already loaded before.\n" +
                "To load it again, you will have to restart the game.\n" +
                "Would you like to quit Project Zomboid now?",
                "Yes", "No");

            if (dialogChoice == 0) {
                exitWithStatusCode(0);
            }
        }

        agentOptions = AgentOptions.fromArgs(args).handleError(err -> {
            UI.showErrorDialog("Bad launch options", "The options you've used to start the loader are incorrect.\nError: " + err + ".");
            exitWithStatusCode(1);
        });

        pluginManager = new PluginManager(inst);

        // Intercept the default class loader by pretending to be its parent
        //
        // There are multiple class loaders involved:
        // - AppClassLoader (which is ClassLoader.getSystemClassLoader())
        // - ExtClassLoader (loads extensions of core Java classes)
        // - Bootstrap (parent of all class loaders)
        //
        // We want to replace the system class loader with our own
        // so that we can make the game see our plugin classes.
        //
        // However, the game classes are already loaded by the time we're here,
        // so even if the system class loader is replaced, already loaded classes
        // will not know about it (like MainScreenState).
        //
        // To make this happen without relaunching the game, we will replace the parent
        // of the AppClassLoader to be the custom one instead of the ExtClassLoader, so
        // that it will resolve to the parent if the class is not found:
        //
        //   Before: AppClassLoader -> ExtClassLoader -> Bootstrap
        //   After:  AppClassLoader -> GameClassLoader -> ExtClassLoader -> Bootstrap
        //
        // Note that this only works because:
        //
        // 1) There's an intermediary classloader between system and bootstrap,
        //    allowing this kind of parent instance replacement. The bootstrap
        //    classloader is implemented in native code and is represented by null.
        //
        // 2) ClassLoaders use their parent ClassLoader if a class is not found
        try {
            Field fieldParent = ClassLoader.class.getDeclaredField("parent");
            fieldParent.setAccessible(true);

            ClassLoader appClassLoader = ClassLoader.getSystemClassLoader();
            ClassLoader extClassLoader = appClassLoader.getParent();
            ClassLoader gameClassLoader = new GameClassLoader(extClassLoader, pluginManager.getPluginHandler());
            fieldParent.set(appClassLoader, gameClassLoader);

            Method methodDisableLookupCache = URLClassPath.class.getDeclaredMethod("disableAllLookupCaches");
            methodDisableLookupCache.setAccessible(true);
            methodDisableLookupCache.invoke(null);
        }
        catch (Exception e) {
            UI.showErrorDialog("Initialization error", "Cannot become a parent of the system classloader: " + e.getMessage());
            exitWithStatusCode(1);
        }

        UI.showWindow();
        UI.addMessage("Agent loaded from %s\n", agentOptions.thisJar.getPath());
        UI.addMessage("Looking up plugins in %s...", agentOptions.pluginsDir.getPath());
        File[] jarFiles = agentOptions.pluginsDir.listFiles(file -> file.getName().endsWith(".jar"));
        if (jarFiles == null || jarFiles.length == 0) {
            UI.addMessage("Found no plugins in this directory.");
        }
        else {
            UI.addMessage("Found %d plugin%s in this directory:", jarFiles.length, jarFiles.length == 1 ? "" : "s");
            for (File jarFile: jarFiles) {
                UI.addMessage("  %s", jarFile.getName());
            }
        }

        UI.addMessage("\nAvailable commands:");
        UI.addMessage("  gc");
        UI.addMessage("  plugins");
        UI.addMessage("  load <jar>");
        UI.addMessage("  enable <id>");
        UI.addMessage("  disable <id>");

        UI.handleInput(input -> {
            String[] commandArgs = input.split(" ");
            if (commandArgs.length > 2) {
                commandArgs = new String[] {commandArgs[0], String.join(" ", Arrays.copyOfRange(commandArgs, 1, commandArgs.length))};
            }

            String commandType = commandArgs[0];
            switch (commandType) {
                case "gc":
                    System.gc();
                    break;
                case "pl":
                case "plugins": {
                    List<ZomboidPlugin> loadedPlugins = pluginManager.getPluginHandler().getLoadedPlugins();
                    UI.addMessage("Plugins (%d): %s", loadedPlugins.size(),
                            loadedPlugins.stream().map(plugin -> plugin.getPluginId() + (plugin.isEnabled() ? "" : " " +
                                    "(disabled)")).collect(Collectors.joining(", ")));
                    break;
                }
                case "load":
                    if (commandArgs.length != 2) {
                        UI.addMessage("Load: Requires plugin JAR path");
                        return;
                    }

                    String jarPath = commandArgs[1];
                    File jarFile = new File(agentOptions.pluginsDir, jarPath);
                    if (!jarFile.exists()) {
                        UI.addMessage("Load: JAR file %s does not exist", jarFile.getPath());
                        return;
                    }

                    ZomboidPlugin newPlugin;

                    try {
                        newPlugin = pluginManager.loadPlugin(jarFile);
                    }
                    catch (Exception e) {
                        UI.addMessage("Load: Can't load plugin: %s", e.getMessage());
                        e.printStackTrace();
                        return;
                    }

                    UI.addMessage("Load: Plugin %s has been loaded", newPlugin.getPluginId());
                    break;
                case "enable": {
                    if (commandArgs.length != 2) {
                        UI.addMessage("Enable: Requires plugin ID");
                        return;
                    }

                    String pluginId = commandArgs[1];
                    List<ZomboidPlugin> loadedPlugins = pluginManager.getPluginHandler().getLoadedPlugins().stream()
                            .filter(plugin -> plugin.getPluginId().equals(pluginId)).collect(Collectors.toList());

                    if (loadedPlugins.size() == 0) {
                        UI.addMessage("Enable: No plugin with ID %s is loaded", pluginId);
                        return;
                    }

                    for (ZomboidPlugin plugin : loadedPlugins) {
                        if (plugin.isEnabled()) {
                            UI.addMessage("Enable: Plugin with ID %s is already enabled", pluginId);
                            continue;
                        }

                        pluginManager.enablePlugin(plugin);
                        UI.addMessage("Enable: Plugin with ID %s has been enabled", pluginId);
                    }
                    break;
                }
                case "disable": {
                    if (commandArgs.length != 2) {
                        UI.addMessage("Disable: Requires plugin ID");
                        return;
                    }

                    String pluginId = commandArgs[1];
                    List<ZomboidPlugin> loadedPlugins = pluginManager.getPluginHandler().getLoadedPlugins().stream()
                            .filter(plugin -> plugin.getPluginId().equals(pluginId)).collect(Collectors.toList());

                    if (loadedPlugins.size() == 0) {
                        UI.addMessage("Disable: No plugin with ID %s is loaded", pluginId);
                        return;
                    }

                    for (ZomboidPlugin plugin : loadedPlugins) {
                        if (!plugin.isEnabled()) {
                            UI.addMessage("Disable: Plugin with ID %s is already disabled", pluginId);
                            continue;
                        }

                        pluginManager.disablePlugin(plugin);
                        UI.addMessage("Disable: Plugin with ID %s has been disabled", pluginId);
                    }
                    break;
                }
            }
        });
    }

    // Prevent multiple agent injections as the agent can only be injected once
    private static boolean isInitialized = false;
    private static boolean wasAlreadyInitialized() {
        if (isInitialized) {
            return true;
        }

        isInitialized = true;
        return false;
    }

    // Throw an exception first so the injector that loaded this agent
    // can pick up that there was an error, then schedule exit so the
    // application fully stops (you can't re-inject a new version!)
    private static void exitWithStatusCode(int statusCode) {
        Executors.newSingleThreadScheduledExecutor().schedule(() -> System.exit(statusCode), 1, TimeUnit.SECONDS);
        throw new RuntimeException("Attempting to force-quit the game");
    }

    public static PluginManager getPluginManager() {
        return pluginManager;
    }
}
