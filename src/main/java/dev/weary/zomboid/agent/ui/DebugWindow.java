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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS;

public class DebugWindow extends JFrame {
    static {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception ignored) {}
    }

    private JTextArea outputArea;
    private JTextField inputField;
    private JScrollPane outputScroll;
    private Consumer<String> inputHandler;
    private boolean firstAppend = true;

    public void addMessage(String format, Object... args) {
        String text = String.format(format, args);

        if (!firstAppend) {
            text = "\n" + text;
        }
        else {
            firstAppend = false;
        }

        outputArea.append(text);
    }

    public DebugWindow(Consumer<String> inputHandler) {
        this("Console", inputHandler);
    }

    public DebugWindow(String title, Consumer<String> inputHandler) {
        super(title);

        this.inputHandler = inputHandler;

        outputArea = new JTextArea();
        outputArea.setEditable(false);

        outputScroll = new JScrollPane(outputArea);
        outputScroll.setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS);

        inputField = new JTextField();

        this.setLayout(new BorderLayout());
        this.add(outputScroll, BorderLayout.CENTER);
        this.add(inputField, BorderLayout.SOUTH);

        inputField.addActionListener(e -> {
            String text = inputField.getText();
            inputHandler.accept(text);
            inputField.setText("");
        });

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(400, 300);
        this.setVisible(true);
    }
}
