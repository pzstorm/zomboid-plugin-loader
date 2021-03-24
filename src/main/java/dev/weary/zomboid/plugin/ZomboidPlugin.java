package dev.weary.zomboid.plugin;

import dev.weary.zomboid.lua.LuaEvent;

import java.lang.instrument.ClassDefinition;
import java.util.HashSet;
import java.util.Set;

public abstract class ZomboidPlugin {
    String pluginId;
    boolean isEnabled;

    private final Set<ClassTransformer> classTransformers = new HashSet<>();
    private final Set<ClassDefinition> affectedClasses = new HashSet<>();

    public void onEnable() {}
    public void onDisable() {}
    public void onLuaEvent(LuaEvent luaEvent) {}

    protected final void addTransformer(String className, ClassNodeTransformer classNodeTransformer) {
        ClassTransformer classTransformer = new ClassTransformer(className, classNodeTransformer);
        affectedClasses.add(classTransformer.getClassDefinition());
        classTransformers.add(classTransformer);
    }

    public final Set<ClassDefinition> getAffectedClasses() {
        return affectedClasses;
    }

    public final Set<ClassTransformer> getClassTransformers() {
        return classTransformers;
    }

    public final String getPluginId() {
        return pluginId;
    }
}
