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
package dev.weary.zomboid.util.asm;

import org.objectweb.asm.tree.*;

public class AsmInsnComparator {
    private static final int INT_WILDCARD = -1;
    private static final String WILDCARD = "*";

    public static int compare(AbstractInsnNode a, AbstractInsnNode b) {
        return areInsnsEqual(a, b) ? 0 : 1;
    }

    public static boolean areInsnsEqual(AbstractInsnNode a, AbstractInsnNode b) {
        if (a == b) {
            return true;
        }

        if (a == null || b == null) {
            return false;
        }

        if (a.equals(b)) {
            return true;
        }

        if (a.getOpcode() != b.getOpcode()) {
            return false;
        }

        switch (a.getType()) {
            case AbstractInsnNode.VAR_INSN:
                return areVarInsnsEqual((VarInsnNode) a, (VarInsnNode) b);
            case AbstractInsnNode.TYPE_INSN:
                return areTypeInsnsEqual((TypeInsnNode) a, (TypeInsnNode) b);
            case AbstractInsnNode.FIELD_INSN:
                return areFieldInsnsEqual((FieldInsnNode) a, (FieldInsnNode) b);
            case AbstractInsnNode.METHOD_INSN:
                return areMethodInsnsEqual((MethodInsnNode) a, (MethodInsnNode) b);
            case AbstractInsnNode.LDC_INSN:
                return areLdcInsnsEqual((LdcInsnNode) a, (LdcInsnNode) b);
            case AbstractInsnNode.IINC_INSN:
                return areIincInsnsEqual((IincInsnNode) a, (IincInsnNode) b);
            case AbstractInsnNode.INT_INSN:
                return areIntInsnsEqual((IntInsnNode) a, (IntInsnNode) b);
            default:
                return true;
        }
    }

    public static boolean areVarInsnsEqual(VarInsnNode a, VarInsnNode b) {
        return intValuesMatch(a.var, b.var);
    }

    public static boolean areTypeInsnsEqual(TypeInsnNode a, TypeInsnNode b) {
        return valuesMatch(a.desc, b.desc);
    }

    public static boolean areFieldInsnsEqual(FieldInsnNode a, FieldInsnNode b) {
        return valuesMatch(a.owner, b.owner) && valuesMatch(a.name, b.name) && valuesMatch(a.desc, b.desc);
    }

    public static boolean areMethodInsnsEqual(MethodInsnNode a, MethodInsnNode b) {
        return valuesMatch(a.owner, b.owner) && valuesMatch(a.name, b.name) && valuesMatch(a.desc, b.desc);
    }

    public static boolean areIntInsnsEqual(IntInsnNode a, IntInsnNode b) {
        return intValuesMatch(a.operand, b.operand);
    }

    public static boolean areIincInsnsEqual(IincInsnNode a, IincInsnNode b) {
        return intValuesMatch(a.var, b.var) && intValuesMatch(a.incr, b.incr);
    }

    public static boolean areLdcInsnsEqual(LdcInsnNode a, LdcInsnNode b) {
        return valuesMatch(a.cst, b.cst);
    }

    public static boolean intValuesMatch(int a, int b) {
        return a == b || a == INT_WILDCARD || b == INT_WILDCARD;
    }

    public static boolean valuesMatch(Object a, Object b) {
        return a == b || a.equals(b) || a == WILDCARD || b == WILDCARD;
    }
}
