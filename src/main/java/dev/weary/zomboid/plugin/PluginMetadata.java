package dev.weary.zomboid.plugin;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class PluginMetadata {
    private final String pluginId;
    private final String mainClass;

    public PluginMetadata(File pluginJar) {
        Attributes mainAttributes = readMainAttributes(pluginJar);
        this.pluginId = mainAttributes.getValue("Zombie-Plugin-Id");
        this.mainClass = mainAttributes.getValue("Zombie-Main-Class");

        if (pluginId == null) {
            throw new RuntimeException("Zombie-Plugin-Id not found in plugin manifest");
        }

        if (mainClass == null) {
            throw new RuntimeException("Zombie-Main-Class not found in plugin manifest");
        }
    }

    private static Attributes readMainAttributes(File pluginJar) {
        JarFile jarFile = null;
        Attributes mainAttributes = null;
        IOException ioException = null;

        try {
            jarFile = new JarFile(pluginJar);
            mainAttributes = jarFile.getManifest().getMainAttributes();
        }
        catch (IOException e) {
            ioException = e;
        }

        try {
            if (jarFile != null) {
                jarFile.close();
            }
        }
        catch (Exception ignored) {}

        if (ioException != null) {
            throw new RuntimeException(ioException);
        }

        return mainAttributes;
    }

    public String getPluginId() {
        return pluginId;
    }

    public String getMainClassName() {
        return mainClass;
    }
}
