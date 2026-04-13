package main;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class ClientGUI {
    private JFrame frame;
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton;
    private JButton sendFileButton;
    private JButton downloadFileButton;
    private JButton callButton;
    private DefaultListModel<String> usersModel;
    private JList<String> usersList;


    private String promptForUserIfNoneSelected(String actionName) {
        String current = getSelectedUser();
        if (current != null && !current.trim().isEmpty()) {
            return current;
        }

        if (usersModel.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "No online users available.", actionName, JOptionPane.INFORMATION_MESSAGE);
            return null;
        }

        Object choice = JOptionPane.showInputDialog(
                frame,
                "Select a user:",
                actionName,
                JOptionPane.QUESTION_MESSAGE,
                null,
                usersModel.toArray(),
                usersModel.getSize() > 0 ? usersModel.getElementAt(0) : null
        );

        if (choice == null) {
            return null;
        }

        String selected = choice.toString();
        usersList.setSelectedValue(selected, true);
        return selected;
    }

    public ClientGUI(Consumer<String> sendMessageAction, Runnable sendFileAction, Runnable downloadFileAction) {
        frame = new JFrame("Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 500);
        frame.setLayout(new BorderLayout());

        // chat area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(chatArea);

        // users panel
        usersModel = new DefaultListModel<>();
        usersList = new JList<>(usersModel);
        JScrollPane usersScroll = new JScrollPane(usersList);
        usersScroll.setPreferredSize(new Dimension(150,0));

        // bottom input panel (make it obvious where to type)
        JPanel bottomPanel = new JPanel(new BorderLayout(6, 6));
        JPanel inputWrapper = new JPanel(new BorderLayout(6, 6));

        JLabel inputHint = new JLabel("Message:");
        inputHint.setFont(inputHint.getFont().deriveFont(Font.BOLD));

        inputField = new JTextField();
        inputField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(120, 120, 120)),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));

        sendButton = new JButton("Send");

        inputWrapper.add(inputHint, BorderLayout.WEST);
        inputWrapper.add(inputField, BorderLayout.CENTER);
        inputWrapper.add(sendButton, BorderLayout.EAST);

        JLabel commandsHint = new JLabel("Tip: double-click a user to call. Or type /call @user, /sendfile @user <path>");
        commandsHint.setFont(commandsHint.getFont().deriveFont(Font.PLAIN, 11f));

        bottomPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        bottomPanel.add(commandsHint, BorderLayout.NORTH);
        bottomPanel.add(inputWrapper, BorderLayout.CENTER);

        // top buttons
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        sendFileButton =  new JButton("Send File");
        downloadFileButton = new JButton("Download File");
        callButton = new JButton("Video Call");
        topPanel.add(sendFileButton);
        topPanel.add(downloadFileButton);
        topPanel.add(callButton);

        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(usersScroll, BorderLayout.WEST);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);

        // Put some helpful text in the input box and focus it.
        inputField.setToolTipText("Type a message, or commands like /call @user or /sendfile @user C:/path/file");

        // actions
        sendButton.addActionListener(evt -> {
            String text = inputField.getText().trim();
            if (!text.isEmpty()) {
                sendMessageAction.accept(text);
                inputField.setText("");
                inputField.requestFocusInWindow();
            }
        });

        inputField.addActionListener(evt -> sendButton.doClick());

        // Quick-start call: prompts for user if none selected.
        callButton.addActionListener(evt -> {
            String user = promptForUserIfNoneSelected("Video Call");
            if (user == null || user.trim().isEmpty()) {
                return;
            }
            sendMessageAction.accept("/call @" + user);
            inputField.requestFocusInWindow();
        });

        // Double-click a username to call quickly.
        usersList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String user = getSelectedUser();
                    if (user != null && !user.trim().isEmpty()) {
                        sendMessageAction.accept("/call @" + user);
                        inputField.requestFocusInWindow();
                    }
                }
            }
        });

        // Send File: prompt for user if none selected.
        sendFileButton.addActionListener(evt -> {
            String user = promptForUserIfNoneSelected("Send File");
            if (user == null || user.trim().isEmpty()) {
                return;
            }
            sendFileAction.run();
        });

        // Keep Download button behavior the same.
        downloadFileButton.addActionListener(evt -> downloadFileAction.run());

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                System.exit(0);
            }
        });

        frame.setVisible(true);

        // Focus the input field after the window shows.
        SwingUtilities.invokeLater(() -> inputField.requestFocusInWindow());
    }

    public void appendMessage(String message){
        SwingUtilities.invokeLater(()->chatArea.append(message + "\n"));
    }

    public void setOnlineUsers(java.util.List<String> users) {
        SwingUtilities.invokeLater(()-> {
            usersModel.clear();
            for (String user : users) {
                usersModel.addElement(user);
            }
        });
    }

    public String getSelectedUser(){
        return usersList.getSelectedValue();
    }

    public JFrame getFrame() {
        return frame;
    }
}
