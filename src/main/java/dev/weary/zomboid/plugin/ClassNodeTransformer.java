package dev.weary.zomboid.plugin;


import org.objectweb.asm.tree.ClassNode;

@FunctionalInterface
public interface ClassNodeTransformer {

    void transformClass(ClassNode classNode);
}
