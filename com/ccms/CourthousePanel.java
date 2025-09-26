package com.ccms;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class CourthousePanel extends JPanel {
    private final User currentUser;

    // Form controls
    private final JTextField nameField     = new JTextField(24);
    private final JTextField locationField = new JTextField(24);
    private final JComboBox<String> typeCombo =
            new JComboBox<>(new String[]{"High", "District", "Appeals", "Other"});

    private final JButton saveBtn    = new JButton("Save");
    private final JButton updateBtn  = new JButton("Update");
    private final JButton deleteBtn  = new JButton("Delete");
    private final JButton clearBtn   = new JButton("Clear");
    private final JButton refreshBtn = new JButton("Refresh");

    // Table
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"ID", "Name", "Location", "Type", "Created At"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(tableModel);

    // Currently selected record ID (null if none)
    private Long selectedId = null;

    public CourthousePanel(User currentUser) {
        this.currentUser = currentUser;
        setLayout(new BorderLayout(10,10));
        setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        // ---------- Form ----------
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4,4,4,4);

        gc.anchor = GridBagConstraints.EAST;
        gc.gridx=0; gc.gridy=0; form.add(new JLabel("Name:"), gc);
        gc.gridx=1; gc.anchor = GridBagConstraints.WEST; form.add(nameField, gc);

        gc.anchor = GridBagConstraints.EAST;
        gc.gridx=0; gc.gridy=1; form.add(new JLabel("Location:"), gc);
        gc.gridx=1; gc.anchor = GridBagConstraints.WEST; form.add(locationField, gc);

        gc.anchor = GridBagConstraints.EAST;
        gc.gridx=0; gc.gridy=2; form.add(new JLabel("Court Type:"), gc);
        gc.gridx=1; gc.anchor = GridBagConstraints.WEST; form.add(typeCombo, gc);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.add(saveBtn);
        buttons.add(updateBtn);
        buttons.add(deleteBtn);
        buttons.add(clearBtn);
        buttons.add(refreshBtn);
        gc.gridx=1; gc.gridy=3; gc.anchor = GridBagConstraints.WEST; form.add(buttons, gc);

        add(form, BorderLayout.NORTH);

        // ---------- Table ----------
        table.setFillsViewportHeight(true);
        table.setRowHeight(22);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Actions
        saveBtn.addActionListener(e -> save());
        updateBtn.addActionListener(e -> update());
        deleteBtn.addActionListener(e -> deleteSelected());
        clearBtn.addActionListener(e -> clearForm());
        refreshBtn.addActionListener(e -> loadAll());
        table.getSelectionModel().addListSelectionListener(e -> onRowSelected());

        // Initial load
        loadAll();
    }

    private void onRowSelected() {
        if (table.getSelectedRow() < 0) { return; }
        int r = table.getSelectedRow();
        selectedId = (Long) tableModel.getValueAt(r, 0);
        nameField.setText((String) tableModel.getValueAt(r, 1));
        locationField.setText((String) tableModel.getValueAt(r, 2));
        String typ = (String) tableModel.getValueAt(r, 3);
        typeCombo.setSelectedItem(typ != null ? typ : "High");
    }

    private void clearForm() {
        selectedId = null;
        nameField.setText("");
        locationField.setText("");
        typeCombo.setSelectedIndex(0);
        table.clearSelection();
        nameField.requestFocus();
    }

    private void save() {
        String name = nameField.getText().trim();
        String loc  = locationField.getText().trim();
        String typ  = (String) typeCombo.getSelectedItem();

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name is required.", "Validation",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        String sql = "INSERT INTO courthouse(name, location, court_type) VALUES(?,?,?)";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, loc.isEmpty() ? null : loc);
            ps.setString(3, typ);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Saved.");
            clearForm();
            loadAll();
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void update() {
        if (selectedId == null) {
            JOptionPane.showMessageDialog(this, "Select a row first.", "Info",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String name = nameField.getText().trim();
        String loc  = locationField.getText().trim();
        String typ  = (String) typeCombo.getSelectedItem();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name is required.", "Validation",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        String sql = "UPDATE courthouse SET name=?, location=?, court_type=? WHERE courthouse_id=?";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, loc.isEmpty() ? null : loc);
            ps.setString(3, typ);
            ps.setLong(4, selectedId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Updated.");
            clearForm();
            loadAll();
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void deleteSelected() {
        if (selectedId == null) {
            JOptionPane.showMessageDialog(this, "Select a row first.", "Info",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this, "Delete selected courthouse?", "Confirm",
                JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;

        String sql = "DELETE FROM courthouse WHERE courthouse_id=?";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, selectedId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Deleted.");
            clearForm();
            loadAll();
        } catch (SQLException ex) {
            // If FK constraint blocks it (e.g., judges/cases reference it)
            JOptionPane.showMessageDialog(this,
                    "Cannot delete: it's referenced by other records.\n" + ex.getMessage(),
                    "Delete failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadAll() {
        tableModel.setRowCount(0);
        String sql = "SELECT courthouse_id, name, location, court_type, created_at " +
                     "FROM courthouse ORDER BY courthouse_id DESC";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getLong("courthouse_id"));
                row.add(rs.getString("name"));
                row.add(rs.getString("location"));
                row.add(rs.getString("court_type"));
                row.add(rs.getString("created_at"));
                tableModel.addRow(row);
            }
        } catch (SQLException ex) { showError(ex); }
    }

    private void showError(Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(this, "DB error: " + ex.getMessage(), "Error",
                JOptionPane.ERROR_MESSAGE);
    }
}
