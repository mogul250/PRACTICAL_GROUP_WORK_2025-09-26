package com.ccms;

import javax.swing.*;
import java.awt.*;

public class AdminDashboardFrame extends JFrame {
    private final User currentUser;
    private final JTabbedPane tabs = new JTabbedPane();

    public AdminDashboardFrame(User user) {
        super("CCMS - Admin Dashboard");
        this.currentUser = user;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Top bar
        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        top.add(new JLabel("Logged in as: " + currentUser.getFullName() + "  [" + currentUser.getRole() + "]"),
                BorderLayout.WEST);
        add(top, BorderLayout.NORTH);

        // Tabs (each tab is a JPanel)
        tabs.addTab("Courthouse", new CourthousePanel(currentUser));
        tabs.addTab("Judge",      new JudgePanel(currentUser));
        tabs.addTab("Lawyer",     new LawyerPanel(currentUser));
        tabs.addTab("Person",     new PersonPanel(currentUser));
        tabs.addTab("Case",    new CasePanel(currentUser));
        tabs.addTab("Hearing", new HearingPanel(currentUser));


        add(tabs, BorderLayout.CENTER);
    }

    /** Simple placeholder until we wire the other panels */
    static class PlaceholderPanel extends JPanel {
        public PlaceholderPanel(String text) {
            setLayout(new BorderLayout());
            JLabel label = new JLabel(text, SwingConstants.CENTER);
            label.setFont(label.getFont().deriveFont(Font.PLAIN, 16f));
            add(label, BorderLayout.CENTER);
            setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
        }
    }
}
