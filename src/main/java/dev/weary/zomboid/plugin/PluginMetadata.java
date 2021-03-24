package dev.weary.zomboid.plugin;

import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class PluginMetadata {
    private final String pluginId;
    private final String mainClass;

    public PluginMetadata(Attributes mainAttributes) {
        this.pluginId = mainAttributes.getValue("Zombie-Plugin-Id");
        this.mainClass = mainAttributes.getValue("Zombie-Main-Class");

        if (pluginId == null) {
            throw new RuntimeException("Zombie-Plugin-Id not found in plugin manifest");
        }

        if (mainClass == null) {
            throw new RuntimeException("Zombie-Main-Class not found in plugin manifest");
        }
    }

    public String getPluginId() {
        return pluginId;
    }

    public String getMainClassName() {
        return mainClass;
    }
}
