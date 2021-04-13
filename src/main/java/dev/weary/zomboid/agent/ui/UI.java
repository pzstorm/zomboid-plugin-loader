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
package dev.weary.zomboid.agent.ui;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import dev.weary.zomboid.util.awt.AwtUtil;

public class UI {
    static {
        AwtUtil.setNativeLookAndFeel();
        AwtUtil.fixWindowsDialogIcons();
    }

    private static DebugWindow debugWindow;

    public static void showWindow() {
        try {
            System.setProperty("java.awt.headless", "false");
            SwingUtilities.invokeAndWait(() -> debugWindow = new DebugWindow(UI::onInputReceived));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int showYesNoDialog(String title, String message, String... choices) {
        return JOptionPane.showOptionDialog(null, message, title, JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, choices, choices[0]);
    }

    public static void showErrorDialog(String title, String message) {
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
    }

    private static final List<Consumer<String>> inputHandlers = new ArrayList<>();

    public static void handleInput(Consumer<String> inputHandler) {
        inputHandlers.add(inputHandler);
    }

    public static void addMessage(String format, Object... args) {
        SwingUtilities.invokeLater(() -> debugWindow.addMessage(format, args));
    }

    private static void onInputReceived(String input) {
        inputHandlers.forEach(inputHandler -> inputHandler.accept(input));
    }
}
