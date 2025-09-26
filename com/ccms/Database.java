package com.ccms;

import java.sql.*;

public class Database {
     private static final String DB_URL = "jdbc:mysql://localhost:3306/court_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    public static void init() {
        try (Connection con = getConnection();
             Statement st = con.createStatement()) {

            // Create user table
            st.execute("""
                CREATE TABLE IF NOT EXISTS user_account (
                  user_id       INTEGER PRIMARY KEY AUTO_INCREMENT,
                  full_name     TEXT NOT NULL,
                  email         TEXT NOT NULL UNIQUE,
                  password_hash TEXT NOT NULL,
                  role          TEXT NOT NULL CHECK(role IN ('ADMIN','CLERK','JUDGE','LAWYER','READONLY')),
                  active        INTEGER NOT NULL DEFAULT 1,
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                );
            """);

            // Bootstrap an ADMIN if none exists
            if (!adminExists(con)) {
                String defaultEmail = "admin@ccms.com";
                String defaultPass  = "123456"; // change after first login
                String hash = PasswordUtil.hash(defaultPass);

                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO user_account(full_name,email,password_hash,role,active) VALUES(?,?,?,?,1)")) {
                    ps.setString(1, "System Administrator");
                    ps.setString(2, defaultEmail);
                    ps.setString(3, hash);
                    ps.setString(4, "ADMIN");
                    ps.executeUpdate();
                    System.out.println("[BOOTSTRAP] Admin created: " + defaultEmail + " / " + defaultPass);
                }
            } else {
                System.out.println("[BOOTSTRAP] Admin already exists. Skipping.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("DB init failed: " + e.getMessage());
        }
    }

    private static boolean adminExists(Connection con) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT 1 FROM user_account WHERE role='ADMIN' AND active=1 LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            return rs.next();
        }
    }
}
