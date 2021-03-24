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
