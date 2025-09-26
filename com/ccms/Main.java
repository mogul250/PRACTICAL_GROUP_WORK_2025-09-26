package com.ccms;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        // 1) Init DB (create tables + bootstrap admin if missing)
        Database.init();

        // 2) Show Login window (on the Swing UI thread)
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
