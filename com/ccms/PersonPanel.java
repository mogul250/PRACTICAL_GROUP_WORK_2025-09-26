package com.ccms;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class PersonPanel extends JPanel {
    private final User currentUser;

    private final JTextField nameField  = new JTextField(24);
    private final JTextField emailField = new JTextField(24);
    private final JTextField phoneField = new JTextField(24);
    private final JCheckBox  orgCheck   = new JCheckBox("Is Organization?");

    private final JButton saveBtn    = new JButton("Save");
    private final JButton updateBtn  = new JButton("Update");
    private final JButton deleteBtn  = new JButton("Delete");
    private final JButton clearBtn   = new JButton("Clear");
    private final JButton refreshBtn = new JButton("Refresh");

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"ID","Name","Email","Phone","Org?","Created At"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(tableModel);

    private Long selectedId = null;

    public PersonPanel(User currentUser) {
        this.currentUser = currentUser;
        setLayout(new BorderLayout(10,10));
        setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        // form
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4,4,4,4);

        gc.anchor = GridBagConstraints.EAST;
        gc.gridx=0; gc.gridy=0; form.add(new JLabel("Full Name:"), gc);
        gc.gridx=1; gc.anchor = GridBagConstraints.WEST; form.add(nameField, gc);

        gc.anchor = GridBagConstraints.EAST;
        gc.gridx=0; gc.gridy=1; form.add(new JLabel("Email:"), gc);
        gc.gridx=1; gc.anchor = GridBagConstraints.WEST; form.add(emailField, gc);

        gc.anchor = GridBagConstraints.EAST;
        gc.gridx=0; gc.gridy=2; form.add(new JLabel("Phone:"), gc);
        gc.gridx=1; gc.anchor = GridBagConstraints.WEST; form.add(phoneField, gc);

        gc.gridx=1; gc.gridy=3; gc.anchor = GridBagConstraints.WEST;
        form.add(orgCheck, gc);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.add(saveBtn);
        buttons.add(updateBtn);
        buttons.add(deleteBtn);
        buttons.add(clearBtn);
        buttons.add(refreshBtn);
        gc.gridx=1; gc.gridy=4; gc.anchor = GridBagConstraints.WEST; form.add(buttons, gc);

        add(form, BorderLayout.NORTH);

        // table
        table.setFillsViewportHeight(true);
        table.setRowHeight(22);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // actions
        saveBtn.addActionListener(e -> save());
        updateBtn.addActionListener(e -> update());
        deleteBtn.addActionListener(e -> deleteSelected());
        clearBtn.addActionListener(e -> clearForm());
        refreshBtn.addActionListener(e -> loadAll());
        table.getSelectionModel().addListSelectionListener(e -> onRowSelected());

        loadAll();
    }

    private void onRowSelected() {
        if (table.getSelectedRow() < 0) return;
        int r = table.getSelectedRow();
        selectedId = (Long) tableModel.getValueAt(r, 0);
        nameField.setText((String) tableModel.getValueAt(r, 1));
        emailField.setText((String) tableModel.getValueAt(r, 2));
        phoneField.setText((String) tableModel.getValueAt(r, 3));
        orgCheck.setSelected("Yes".equals(tableModel.getValueAt(r, 4)));
    }

    private void clearForm() {
        selectedId = null;
        nameField.setText("");
        emailField.setText("");
        phoneField.setText("");
        orgCheck.setSelected(false);
        table.clearSelection();
        nameField.requestFocus();
    }

    private void save() {
        String name  = nameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        int isOrg    = orgCheck.isSelected() ? 1 : 0;

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Full name is required.", "Validation",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        String sql = "INSERT INTO person(full_name,email,phone,is_organization) VALUES(?,?,?,?)";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, email.isEmpty() ? null : email);
            ps.setString(3, phone.isEmpty() ? null : phone);
            ps.setInt(4, isOrg);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Saved.");
            clearForm();
            loadAll();
        } catch (SQLException ex) {
            if (isUniqueViolation(ex)) {
                JOptionPane.showMessageDialog(this, "Email already exists. Leave it blank or use another.",
                        "Duplicate email", JOptionPane.WARNING_MESSAGE);
            } else showError(ex);
        }
    }

    private void update() {
        if (selectedId == null) {
            JOptionPane.showMessageDialog(this, "Select a row first.", "Info",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String name  = nameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        int isOrg    = orgCheck.isSelected() ? 1 : 0;

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Full name is required.", "Validation",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        String sql = "UPDATE person SET full_name=?, email=?, phone=?, is_organization=? WHERE person_id=?";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, email.isEmpty() ? null : email);
            ps.setString(3, phone.isEmpty() ? null : phone);
            ps.setInt(4, isOrg);
            ps.setLong(5, selectedId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Updated.");
            clearForm();
            loadAll();
        } catch (SQLException ex) {
            if (isUniqueViolation(ex)) {
                JOptionPane.showMessageDialog(this, "Email already exists.", "Duplicate email",
                        JOptionPane.WARNING_MESSAGE);
            } else showError(ex);
        }
    }

    private void deleteSelected() {
        if (selectedId == null) {
            JOptionPane.showMessageDialog(this, "Select a row first.", "Info",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this, "Delete selected person?", "Confirm",
                JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;

        String sql = "DELETE FROM person WHERE person_id=?";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, selectedId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Deleted.");
            clearForm();
            loadAll();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Cannot delete: referenced by other records.\n" + ex.getMessage(),
                    "Delete failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadAll() {
        tableModel.setRowCount(0);
        String sql = "SELECT person_id, full_name, email, phone, is_organization, created_at " +
                     "FROM person ORDER BY person_id DESC";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getLong("person_id"));
                row.add(rs.getString("full_name"));
                row.add(rs.getString("email"));
                row.add(rs.getString("phone"));
                row.add(rs.getInt("is_organization") == 1 ? "Yes" : "No");
                row.add(rs.getString("created_at"));
                tableModel.addRow(row);
            }
        } catch (SQLException ex) { showError(ex); }
    }

    private boolean isUniqueViolation(SQLException ex) {
        String msg = ex.getMessage();
        return msg != null && msg.toLowerCase().contains("unique");
    }

    private void showError(Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(this, "DB error: " + ex.getMessage(), "Error",
                JOptionPane.ERROR_MESSAGE);
    }
}
