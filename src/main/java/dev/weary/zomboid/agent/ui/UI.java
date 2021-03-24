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
