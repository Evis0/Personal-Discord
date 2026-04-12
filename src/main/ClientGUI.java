package main;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.function.Consumer;

public class ClientGUI {
    private JFrame frame;
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton;
    private JButton sendFileButton;
    private JButton downloadFileButton;
    private DefaultListModel<String> usersModel;
    private JList<String> usersList;


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

        // bottom input panel
        JPanel bottomPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        sendButton = new JButton("Send");
        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        // top buttons
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        sendFileButton =  new JButton("Send File");
        downloadFileButton = new JButton("Download File");
        topPanel.add(sendFileButton);
        topPanel.add(downloadFileButton);

        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(usersScroll, BorderLayout.WEST);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);

        // actions
        sendButton.addActionListener(e ->{
            String text = inputField.getText().trim();
            if (!text.isEmpty()) {
                sendMessageAction.accept(text);
                inputField.setText("");
            }
        });

        inputField.addActionListener(e -> sendButton.doClick());
        sendFileButton.addActionListener(e -> sendFileAction.run());
        downloadFileButton.addActionListener(e -> downloadFileAction.run());

        frame.setVisible(true);
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
