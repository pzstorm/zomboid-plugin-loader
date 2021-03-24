package dev.weary.zomboid.util.asm;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static dev.weary.zomboid.util.Util.printNode;

public class AsmUtil {
    public static String toInternalClassName(String className) {
        return className.replaceAll("\\.", "/");
    }

    public static boolean isDescriptor(String descriptor) {
        return descriptor.length() == 1 || (descriptor.startsWith("L") && descriptor.endsWith(";"));
    }

    public static String toDescriptor(String className) {
        return isDescriptor(className) ? className : "L" + toInternalClassName(className) + ";";
    }

    public static String toDescriptor(Class<?> classType) {
        return Type.getDescriptor(classType);
    }

    public static String toMethodDescriptor(Class<?> returnType, Class<?>... parameterTypes) {
        return "(" + Arrays.stream(parameterTypes).map(AsmUtil::toDescriptor).collect(Collectors.joining("")) + ")" + toDescriptor(returnType);
    }

    public static String toMethodDescriptor(String returnType, String... parameterTypes) {
        return "(" + Arrays.stream(parameterTypes).map(AsmUtil::toDescriptor).collect(Collectors.joining("")) + ")" + toDescriptor(returnType);
    }

    public static boolean isLabelOrLineNumber(AbstractInsnNode insnNode) {
        return insnNode.getType() == AbstractInsnNode.LABEL || insnNode.getType() == AbstractInsnNode.LINE;
    }

    public static InsnList getInsnPatternFromStart(MethodNode methodNode, InsnList insnList) {
        return getInsnPattern(findFirstInstruction(methodNode), insnList);
    }

    public static InsnList getInsnPattern(AbstractInsnNode firstNode, InsnList insnList) {
        InsnList insnPattern = findInsnList(firstNode, insnList);
        if (insnPattern.size() == 0) {
            throw new AsmException("Couldn't find instruction pattern!");
        }

        return insnPattern;
    }

    public static MethodNode getClassMethodNode(ClassNode classNode, String methodName, Class<?> returnType, Class<?>... parameterTypes) {
       return getClassMethodNode(classNode, methodName, toMethodDescriptor(returnType, parameterTypes));
    }

    public static MethodNode getClassMethodNode(ClassNode classNode, String methodName, String methodDesc) {
        MethodNode methodNode = findMethodNodeOfClass(classNode, methodName, methodDesc);
        if (methodNode == null) {
            throw new AsmException("Couldn't find method '" + methodName + "' with signature '" + methodDesc + "' in class '" + classNode.name + "'");
        }

        return methodNode;
    }

    public static MethodNode findMethodNodeOfClass(ClassNode classNode, String methodName, String methodDesc) {
        for (MethodNode methodNode: classNode.methods) {
            if (methodNode.name.equals(methodName) && (methodNode.desc == null || methodNode.desc.equals(methodDesc))) {
                return methodNode;
            }
        }

        return null;
    }

    public static AbstractInsnNode findInstructionPredicate(AbstractInsnNode firstInsnToCheck, Predicate<AbstractInsnNode> insnPredicate, boolean reverseDirection) {
        AbstractInsnNode currentInsn = firstInsnToCheck;

        while (currentInsn != null) {
            if (insnPredicate.test(currentInsn)) {
                return currentInsn;
            }

            currentInsn = (reverseDirection ? currentInsn.getPrevious() : currentInsn.getNext());
        }

        return null;
    }

    public static InsnList findInsnList(AbstractInsnNode haystackStart, InsnList needle) {
        int needleStartOpcode = needle.getFirst().getOpcode();
        AbstractInsnNode checkAgainstStart = findInstructionWithOpcode(haystackStart, needleStartOpcode);
        while (checkAgainstStart != null) {
            InsnList found = checkForPatternAt(needle, checkAgainstStart);
            if (found.getFirst() != null) {
                return found;
            }

            checkAgainstStart = findNextInstructionWithOpcode(checkAgainstStart, needleStartOpcode);
        }

        return new InsnList();
    }

    public static AbstractInsnNode findNextInstructionWithOpcode(AbstractInsnNode insnNode, int opcode) {
        return findInstructionWithOpcode(insnNode.getNext(), opcode);
    }

    public static AbstractInsnNode findInstructionWithOpcode(AbstractInsnNode firstInsnToCheck, int opcode) {
        return findInstructionWithOpcode(firstInsnToCheck, opcode, false);
    }

    public static AbstractInsnNode findInstructionWithOpcode(AbstractInsnNode firstInsnToCheck, int opcode, boolean reverseDirection) {
        return findInstructionPredicate(firstInsnToCheck, insnNode -> insnNode.getOpcode() == opcode, reverseDirection);
    }

    public static boolean instructionsMatch(AbstractInsnNode first, AbstractInsnNode second) {
        return AsmInsnComparator.areInsnsEqual(first, second);
    }

    @Deprecated
    public static InsnList matchInsnList(AbstractInsnNode firstNode, InsnList insnList) {
        AbstractInsnNode currentNode = firstNode;
        AbstractInsnNode matchingNode = insnList.getFirst();
        InsnList matchedList = new InsnList();

        while (matchingNode != null) {
            if (instructionsMatch(currentNode, matchingNode)) {
                matchedList.add(currentNode);
                matchingNode = matchingNode.getNext();
                currentNode = currentNode.getNext();

                if (matchingNode == null) {
                    return matchedList;
                }

                continue;
            }

            return null;
        }

        return null;
    }

    public static InsnList checkForPatternAt(InsnList checkFor, AbstractInsnNode checkAgainst) {
        InsnList foundInsnList = new InsnList();
        boolean firstNeedleFound = false;
        AbstractInsnNode lookFor = checkFor.getFirst();

        while (lookFor != null) {
            if (checkAgainst == null) {
                return new InsnList();
            }

            if (isLabelOrLineNumber(lookFor)) {
                lookFor = lookFor.getNext();
                continue;
            }

            if (isLabelOrLineNumber(checkAgainst)) {
                if (firstNeedleFound) {
                    foundInsnList.add(checkAgainst);
                }

                checkAgainst = checkAgainst.getNext();
                continue;
            }

            if (!instructionsMatch(lookFor, checkAgainst)) {
                return new InsnList();
            }

            foundInsnList.add(checkAgainst);
            lookFor = lookFor.getNext();
            checkAgainst = checkAgainst.getNext();
            firstNeedleFound = true;
        }

        return foundInsnList;
    }

    public static AbstractInsnNode findInstructionPredicate(AbstractInsnNode firstInsnToCheck, Predicate<AbstractInsnNode> insnPredicate) {
        return findInstructionPredicate(firstInsnToCheck, insnPredicate, false);
    }

    public static AbstractInsnNode findInstruction(AbstractInsnNode firstInsnToCheck, boolean reverseDirection) {
        return findInstructionPredicate(firstInsnToCheck, insnNode -> !isLabelOrLineNumber(insnNode), reverseDirection);
    }

    public static AbstractInsnNode findInstruction(AbstractInsnNode firstInsnToCheck) {
        return findInstruction(firstInsnToCheck, false);
    }

    public static AbstractInsnNode findFirstInstruction(MethodNode methodNode) {
        return findInstruction(methodNode.instructions.getFirst());
    }

    public static AbstractInsnNode findLastInstruction(MethodNode methodNode) {
        return findInstruction(methodNode.instructions.getLast(), true);
    }

    public static InsnList makeInsnList(Consumer<InsnList> insnListConsumer) {
        InsnList insnList = new InsnList();
        insnListConsumer.accept(insnList);
        return insnList;
    }

    public static void dumpMethod(MethodNode methodNode) {
        if (methodNode == null) {
            System.out.println("Method is null");
            return;
        }

        try {
            System.out.printf("Method %s -> %s\n", methodNode.name, methodNode.desc);
            for (AbstractInsnNode insnNode: methodNode.instructions) {
                System.out.printf(".... %s", printNode(insnNode));
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

}
