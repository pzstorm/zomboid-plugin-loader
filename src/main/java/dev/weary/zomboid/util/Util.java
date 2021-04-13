package dev.weary.zomboid.util;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import dev.weary.zomboid.plugin.ClassNodeTransformer;
import dev.weary.zomboid.plugin.ClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.io.*;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.util.Arrays;
import java.util.List;

public class Util {
    private static final Printer asmPrinter = new Textifier();
    private static final TraceMethodVisitor methodPrinter = new TraceMethodVisitor(asmPrinter);

    public static String getArgAfterLast(String[] programArgs, String argKey) {
        for (int i = programArgs.length - 1; i >= 0; i--) {
            if (programArgs[i].equals(argKey)) {
                return i + 1 < programArgs.length ? programArgs[i + 1] : "";
            }
        }

        return null;
    }

    public static String printNode(AbstractInsnNode insnNode) {
        insnNode.accept(methodPrinter);
        StringWriter stringWriter = new StringWriter();
        asmPrinter.print(new PrintWriter(stringWriter));
        asmPrinter.getText().clear();
        return stringWriter.toString();
    }

    public static String byteCodeToMnemonic(byte[] byteCode) {
        StringBuilder stringBuilder = new StringBuilder();
        ClassReader reader = new ClassReader(byteCode);
        ClassNode classNode = new ClassNode();
        reader.accept(classNode,0);

        for (MethodNode methodNode: classNode.methods) {
            stringBuilder.append(String.format("Method %s -> %s:\n", methodNode.name, methodNode.desc));
            for (AbstractInsnNode insnNode: methodNode.instructions) {
                stringBuilder.append(printNode(insnNode));
            }
        }

        return stringBuilder.toString();
    }

    public static void verifyClass(byte[] byteCode) {
        StringWriter stringWriter = new StringWriter();
        CheckClassAdapter.verify(new ClassReader(byteCode), false, new PrintWriter(stringWriter));

        if (stringWriter.toString().length() != 0) {
            try {
                BufferedWriter out = new BufferedWriter(new FileWriter("bytecode-output.txt"));
                out.write(stringWriter.toString());
                out.write(byteCodeToMnemonic(byteCode));
                out.close();
            }
            catch (IOException e) {
                System.out.println("Couldn't write bytecode output: " + e.getMessage());
            }

            System.err.println(stringWriter);
            throw new IllegalStateException("Couldn't verify bytecode!");
        }
    }

    public static String toInternalClassName(String className) {
        return className.replaceAll("\\.", "/");
    }

    public static ClassLoader getCurrentClassLoader() {
        return ClassLoader.getSystemClassLoader();
    }

    public static Class<?> getClassForName(String className) {
        try {
            return Class.forName(className, true, getCurrentClassLoader());
        }
        catch (Exception ignored) {}
        return null;
    }

    public static ClassDefinition readClassDefinition(Class<?> classForName) {
        return new ClassDefinition(classForName, getBytesFromClass(classForName));
    }

    public static ClassDefinition readClassDefinition(String className) {
        return readClassDefinition(getClassForName(className));
    }

    public static void transformClass(Instrumentation inst, String className, ClassNodeTransformer classNodeTransformer) {
        try {
            ClassTransformer classTransformer = new ClassTransformer(className, classNodeTransformer);
            inst.addTransformer(classTransformer, true);
            inst.redefineClasses(classTransformer.getClassDefinition());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String dumpClassMethods(String className, ClassNode classNode, String classMethod) {
        try {
            StringBuilder builder = new StringBuilder();
            builder.append(String.format("Methods %s of class %s:\n\n", classMethod, className));

            for (MethodNode methodNode: classNode.methods) {
                if (!methodNode.name.equals(classMethod)) {
                    continue;
                }

                builder.append(String.format("Method %s -> %s\n", methodNode.name, methodNode.desc));
                for (AbstractInsnNode insnNode: methodNode.instructions) {
                    builder.append(String.format(".... %s", printNode(insnNode)));
                }
            }

            return builder.toString();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String dumpDiff(String className, String originalCode, String modifiedCode, int diffLines) {
        List<String> originalLines = Arrays.asList(originalCode.split("\n"));
        List<String> modifiedLines = Arrays.asList(modifiedCode.split("\n"));

        Patch<String> bytecodePatch = DiffUtils.diff(originalLines, modifiedLines);
        List<String> unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff(className + ".class (original)", className + ".class (transformed)", originalLines, bytecodePatch, diffLines);

        try {
            StringBuilder builder = new StringBuilder();
            for (String diffLine : unifiedDiff) {
                builder.append(diffLine).append("\n");
            }
            return builder.toString();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String dumpClassFile(String className, String classOriginalCode) {
        try {
            StringBuilder builder = new StringBuilder();
            builder.append("Class ").append(className).append(":\n\n");
            builder.append(classOriginalCode);
            return builder.toString();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String generateClassDump(ClassNode classNode) {
        StringBuilder builder = new StringBuilder();
        for (MethodNode methodNode: classNode.methods) {
            builder.append(String.format("Method %s -> %s\n", methodNode.name, methodNode.desc));
            for (AbstractInsnNode insnNode: methodNode.instructions) {
                builder.append(String.format(".... %s", printNode(insnNode)));
            }
        }

        return builder.toString();
    }

    private static byte[] getBytesFromClass(Class<?> clazz) {
        String classFile = clazz.getName().replace('.', '/') + ".class";
        InputStream inputStream = clazz.getClassLoader().getResourceAsStream(classFile);
        return readStreamAndClose(inputStream);
    }

    public static byte[] readStreamAndClose(InputStream inputStream) {
        try {
            return readStream(inputStream);
        }
        finally {
            closeStream(inputStream);
        }
    }

    private static byte[] readStream(InputStream inputStream) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] readBuffer = new byte[16384];
            int bytesRead;

            while ((bytesRead = inputStream.read(readBuffer, 0, readBuffer.length)) > -1) {
                outputStream.write(readBuffer, 0, bytesRead);
            }

            return outputStream.toByteArray();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void closeStream(Closeable stream) {
        if (stream != null) {
            try {
                if (stream instanceof Flushable) {
                    ((Flushable) stream).flush();
                }

                stream.close();
            }
            catch (Exception ignored) {}
        }
    }
}
