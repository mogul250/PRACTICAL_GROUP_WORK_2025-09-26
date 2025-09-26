package com.ccms;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {
    private final JTextField emailField = new JTextField(20);
    private final JPasswordField passField = new JPasswordField(20);
    private final JButton loginBtn = new JButton("Login");

    public LoginFrame() {
        super("CCMS - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(380, 200);
        setLocationRelativeTo(null); // center
        setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridLayout(3, 2, 8, 8));
        form.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
        form.add(new JLabel("Email:"));
        form.add(emailField);
        form.add(new JLabel("Password:"));
        form.add(passField);
        form.add(new JLabel(""));
        form.add(loginBtn);

        add(form, BorderLayout.CENTER);

        loginBtn.addActionListener(e -> doLogin());
        getRootPane().setDefaultButton(loginBtn); // Enter key triggers login
    }

    private void doLogin() {
        String email = emailField.getText().trim();
        String pass  = new String(passField.getPassword());

        if (email.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter email and password.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        UserDAO dao = new UserDAO();
        User user = dao.login(email, pass);

        if (user == null) {
            JOptionPane.showMessageDialog(this, "Invalid credentials or inactive user.", "Login failed",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(this, "Welcome, " + user.getFullName() + " (" + user.getRole() + ")");
        // open dashboard
        AdminDashboardFrame dash = new AdminDashboardFrame(user);
        dash.setVisible(true);
        this.dispose();
    }
}
