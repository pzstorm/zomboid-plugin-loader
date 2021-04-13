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

import dev.weary.zomboid.lua.LuaEvent;

import java.lang.instrument.ClassDefinition;
import java.util.HashSet;
import java.util.Set;

public abstract class ZomboidPlugin {
    String pluginId;
    boolean isEnabled;
    PluginClassLoader classLoader;

    @Override
    protected void finalize() throws Throwable {
        System.out.println("** ZomboidPlugin is finalizing!");
        super.finalize();
    }

    final Set<ClassTransformer> classTransformers = new HashSet<>();
    final Set<ClassDefinition> affectedClasses = new HashSet<>();

    public void onEnable() {}
    public void onDisable() {}
    public void onLuaEvent(LuaEvent luaEvent) {}

    protected final void addTransformer(String className, ClassNodeTransformer classNodeTransformer) {
        ClassTransformer classTransformer = new ClassTransformer(className, classNodeTransformer);
        affectedClasses.add(classTransformer.getClassDefinition());
        classTransformers.add(classTransformer);
    }

    final ClassDefinition[] getClassDefinitions() {
        if (affectedClasses.size() == 0) {
            return null;
        }

        return affectedClasses.stream().distinct().toArray(ClassDefinition[]::new);
    }

    final Class<?>[] getAffectedClasses() {
        if (affectedClasses.size() == 0) {
            return null;
        }

        return affectedClasses.stream().map(ClassDefinition::getDefinitionClass).distinct().toArray(Class[]::new);
    }

    final Set<ClassTransformer> getClassTransformers() {
        return classTransformers;
    }

    public final String getPluginId() {
        return pluginId;
    }

    public final boolean isEnabled() {
        return isEnabled;
    }
}
