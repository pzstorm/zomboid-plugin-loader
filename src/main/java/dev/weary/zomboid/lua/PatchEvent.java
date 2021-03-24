package dev.weary.zomboid.lua;

import dev.weary.zomboid.plugin.ClassNodeTransformer;
import dev.weary.zomboid.plugin.Transformer;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import se.krka.kahlua.integration.LuaCaller;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaThread;
import se.krka.kahlua.vm.LuaClosure;
import zombie.Lua.Event;

import java.util.ArrayList;

import static dev.weary.zomboid.util.asm.AsmUtil.*;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

public class PatchEvent implements ClassNodeTransformer {
    private static final String protectedCallVoid = getMethodDescriptor(VOID_TYPE, Type.getType(KahluaThread.class), Type.getType(Object.class), Type.getType(Object[].class));
    private static final String shouldTriggerEvent = getMethodDescriptor(BOOLEAN_TYPE, Type.getType(LuaClosure.class), Type.getType(Event.class), Type.getType(Object[].class));
    private static final String cloneEventArgs = getMethodDescriptor(Type.getType(Object[].class), Type.getType(Object[].class));

    @Transformer(generateDiff = true)
    public void transformClass(ClassNode classNode) {
        MethodNode methodTrigger = getClassMethodNode(classNode, "trigger", boolean.class, KahluaTable.class, LuaCaller.class, Object[].class);

        InsnList listCallbackLoop = getInsnPatternFromStart(methodTrigger, makeInsnList(insnList -> {
            insnList.add(new VarInsnNode(ILOAD, 4));
            insnList.add(new VarInsnNode(ALOAD, 0));
            insnList.add(new FieldInsnNode(GETFIELD, getInternalName(Event.class), "callbacks", getDescriptor(ArrayList.class)));
            insnList.add(new MethodInsnNode(INVOKEVIRTUAL, getInternalName(ArrayList.class), "size", getMethodDescriptor(INT_TYPE)));
        }));

        InsnList listCallClosure = getInsnPatternFromStart(methodTrigger, makeInsnList(insnList -> {
            insnList.add(new VarInsnNode(ALOAD, 3));
            insnList.add(new MethodInsnNode(INVOKEVIRTUAL, getInternalName(LuaCaller.class), "protectedCallVoid", protectedCallVoid));
        }));

        LabelNode skipEventTrigger = (LabelNode) listCallClosure.getLast().getNext();
        methodTrigger.instructions.insert(skipEventTrigger, new FrameNode(F_SAME, 0, null, 0, null));

        LabelNode cloneVarStart = new LabelNode();
        InsnList listCloneArgs = makeInsnList(insnList -> {
            insnList.add(new VarInsnNode(ALOAD, 3));
            insnList.add(new MethodInsnNode(INVOKESTATIC, getInternalName(EventDispatcher.class), "cloneEventArgs", cloneEventArgs));
            insnList.add(new VarInsnNode(ASTORE, 5));
            insnList.add(cloneVarStart);
        });

        LocalVariableNode argsClone = new LocalVariableNode("args", getInternalName(Object[].class), null, cloneVarStart, skipEventTrigger, 5);
        methodTrigger.localVariables.add(argsClone);

        VarInsnNode loadArgs = (VarInsnNode) listCallClosure.getFirst();
        loadArgs.var = 5;

        InsnList listCheckShouldTrigger = makeInsnList(insnList -> {
            insnList.add(new VarInsnNode(ALOAD, 0));
            insnList.add(new FieldInsnNode(GETFIELD, getInternalName(Event.class), "callbacks", getDescriptor(ArrayList.class)));
            insnList.add(new VarInsnNode(ILOAD, 4));
            insnList.add(new MethodInsnNode(INVOKEVIRTUAL, getInternalName(ArrayList.class), "get", getMethodDescriptor(Type.getType(Object.class), INT_TYPE)));
            insnList.add(new TypeInsnNode(CHECKCAST, getInternalName(LuaClosure.class)));
            insnList.add(new VarInsnNode(ALOAD, 0));
            insnList.add(new VarInsnNode(ALOAD, 5));
            insnList.add(new MethodInsnNode(INVOKESTATIC, getInternalName(EventDispatcher.class), "shouldTriggerEvent", shouldTriggerEvent));
            insnList.add(new JumpInsnNode(IFEQ, skipEventTrigger));
        });

        LabelNode labelTryCatch = (LabelNode) listCallbackLoop.getLast().getNext().getNext();
        methodTrigger.instructions.insert(labelTryCatch, listCheckShouldTrigger);
        methodTrigger.instructions.insert(labelTryCatch, listCloneArgs);
    }
}