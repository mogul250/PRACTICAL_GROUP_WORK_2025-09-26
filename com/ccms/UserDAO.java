package com.ccms;

import java.sql.*;

public class UserDAO {

    public User findByEmail(String email) {
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT user_id, full_name, email, role, active FROM user_account WHERE email=? LIMIT 1")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User u = new User();
                    u.setUserId(rs.getLong("user_id"));
                    u.setFullName(rs.getString("full_name"));
                    u.setEmail(rs.getString("email"));
                    u.setRole(rs.getString("role"));
                    u.setActive(rs.getInt("active") == 1);
                    return u;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /** Returns user if credentials valid, else null */
    public User login(String email, String password) {
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT user_id, full_name, email, role, active, password_hash FROM user_account WHERE email=? LIMIT 1")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String hash = rs.getString("password_hash");
                    boolean ok = PasswordUtil.matches(password, hash);
                    boolean active = rs.getInt("active") == 1;
                    if (ok && active) {
                        User u = new User();
                        u.setUserId(rs.getLong("user_id"));
                        u.setFullName(rs.getString("full_name"));
                        u.setEmail(rs.getString("email"));
                        u.setRole(rs.getString("role"));
                        u.setActive(true);
                        return u;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
