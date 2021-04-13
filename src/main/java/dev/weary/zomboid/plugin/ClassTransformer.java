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

import dev.weary.zomboid.util.Util;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;

import static dev.weary.zomboid.util.Util.*;

public class ClassTransformer implements ClassFileTransformer {
    private final String className;
    private final ClassNodeTransformer classNodeTransformer;
    private final ClassDefinition classDefinition;

    public ClassTransformer(String className, ClassNodeTransformer classNodeTransformer) {
        this.className = toInternalClassName(className);
        this.classNodeTransformer = classNodeTransformer;
        this.classDefinition = readClassDefinition(className);
    }

    public ClassDefinition getClassDefinition() {
        return this.classDefinition;
    }

    @Override
    public byte[] transform(ClassLoader classLoader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] byteCode) {
        if (this.className.equals(className)) {
            try {

                // Read the class to be transformed
                ClassNode classNode = new ClassNode();
                ClassReader classReader = new ClassReader(byteCode);
                classReader.accept(classNode, 0);

                // Is the transformClass method annotated?
                Transformer transformOptions = getTransformOptions();
                if (transformOptions == null) {
                    throw new IllegalStateException("transformClass method must be annotated with @Transformer");
                }

                String dumpOriginal = "", dumpModified;
                if (transformOptions.generateDiff() || transformOptions.dumpClass()) {
                    dumpOriginal = generateClassDump(classNode);
                }

                if (!transformOptions.dumpMethod().isEmpty()) {
                    System.out.println(dumpClassMethods(className, classNode, transformOptions.dumpMethod()));
                }

                this.classNodeTransformer.transformClass(classNode);

                if (transformOptions.generateDiff()) {
                    dumpModified = generateClassDump(classNode);
                    System.out.println("Generating diff...");
                    System.out.println(dumpDiff(className, dumpOriginal, dumpModified, 4));
                }

                if (transformOptions.dumpClass()) {
                    dumpClassFile(className, dumpOriginal);
                }

                // Write the class
                ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
                classNode.accept(classWriter);

                byte[] newByteCode = classWriter.toByteArray();
                Util.verifyClass(newByteCode);

                return newByteCode;
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        return byteCode;
    }

    private Transformer getTransformOptions() {
        try {
            Method transformMethod = this.classNodeTransformer.getClass().getDeclaredMethod("transformClass", ClassNode.class);
            return transformMethod.getAnnotation(Transformer.class);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
