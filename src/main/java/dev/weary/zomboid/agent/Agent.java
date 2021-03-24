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
import dev.weary.zomboid.plugin.ZomboidPlugin;

import java.io.File;
import java.lang.instrument.Instrumentation;

public class Agent {
    public static PluginManager pluginManager;

    // Static load using the -javaagent parameter (before the game starts)
    public static void premain(String args, Instrumentation inst) {
        start(args, inst);
    }

    // Dynamic load using the Attach API (after the game starts)
    public static void agentmain(String args, Instrumentation inst) {
        start(args, inst);
    }

    private static void start(String args, Instrumentation inst) {
        pluginManager = new PluginManager(inst);

        UI.showWindow();
        UI.addMessage("Agent loaded");

        File root = new File("C:\\Work\\pz-modding\\sample-plugin");
        UI.addMessage("Loading plugins from %s", root.getAbsolutePath());
        UI.addMessage("Type in a plugin name:");

        //UI.handleInput(input -> {
            String input = "plugin.jar";
            UI.addMessage("Loading plugin %s...", input);
            File pluginJar = new File(root, input);
            ZomboidPlugin zomboidPlugin;

            try {
                zomboidPlugin = pluginManager.loadPlugin(pluginJar);
            }
            catch (Exception e) {
                UI.addMessage("Can't load plugin: %s", e.getMessage());
                e.printStackTrace();
                return;
            }

            UI.addMessage("Enabling plugin %s...", input);
            try {
                pluginManager.enablePlugin(zomboidPlugin);
            }
            catch (Exception e) {
                UI.addMessage("Can't enable plugin: %s", e.getMessage());
                e.printStackTrace();
            }
        //});
    }
}
