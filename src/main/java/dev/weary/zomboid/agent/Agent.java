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
