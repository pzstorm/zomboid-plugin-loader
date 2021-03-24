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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class UI {
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
