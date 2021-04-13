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

    public final ClassDefinition[] getClassDefinitions() {
        if (affectedClasses.size() == 0) {
            return null;
        }

        return affectedClasses.stream().distinct().toArray(ClassDefinition[]::new);
    }

    public final Class<?>[] getAffectedClasses() {
        if (affectedClasses.size() == 0) {
            return null;
        }

        return affectedClasses.stream().map(ClassDefinition::getDefinitionClass).distinct().toArray(Class[]::new);
    }

    public final Set<ClassTransformer> getClassTransformers() {
        return classTransformers;
    }

    public final String getPluginId() {
        return pluginId;
    }

    public final boolean isEnabled() {
        return isEnabled;
    }
}
