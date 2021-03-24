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
