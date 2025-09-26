package com.ccms;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class JudgePanel extends JPanel {
    private final User currentUser;

    private final JTextField nameField = new JTextField(24);
    private final JTextField specField = new JTextField(24);
    private final JComboBox<ComboItem> courthouseCombo = new JComboBox<>();

    private final JButton saveBtn    = new JButton("Save");
    private final JButton updateBtn  = new JButton("Update");
    private final JButton deleteBtn  = new JButton("Delete");
    private final JButton clearBtn   = new JButton("Clear");
    private final JButton refreshBtn = new JButton("Refresh");

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"ID","Name","Specialization","Courthouse","Created At"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(tableModel);

    private Long selectedId = null;

    public JudgePanel(User currentUser) {
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
        gc.gridx=0; gc.gridy=1; form.add(new JLabel("Specialization:"), gc);
        gc.gridx=1; gc.anchor = GridBagConstraints.WEST; form.add(specField, gc);

        gc.anchor = GridBagConstraints.EAST;
        gc.gridx=0; gc.gridy=2; form.add(new JLabel("Courthouse:"), gc);
        gc.gridx=1; gc.anchor = GridBagConstraints.WEST; form.add(courthouseCombo, gc);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.add(saveBtn);
        buttons.add(updateBtn);
        buttons.add(deleteBtn);
        buttons.add(clearBtn);
        buttons.add(refreshBtn);
        gc.gridx=1; gc.gridy=3; gc.anchor = GridBagConstraints.WEST; form.add(buttons, gc);

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
        refreshBtn.addActionListener(e -> { loadCourthouses(); loadJudges(); });
        table.getSelectionModel().addListSelectionListener(e -> onRowSelected());

        loadCourthouses();
        loadJudges();
    }

    private void onRowSelected() {
        if (table.getSelectedRow() < 0) return;
        int r = table.getSelectedRow();
        selectedId = (Long) tableModel.getValueAt(r, 0);

        // safer: re-read full record including courthouse_id
        String sql = "SELECT full_name, specialization, courthouse_id FROM judge WHERE judge_id=?";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, selectedId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    nameField.setText(rs.getString("full_name"));
                    specField.setText(rs.getString("specialization"));
                    Long chId = rs.getObject("courthouse_id") == null ? null : rs.getLong("courthouse_id");
                    selectCourthouseById(chId);
                }
            }
        } catch (SQLException ex) { showError(ex); }
    }

    private void selectCourthouseById(Long id) {
        for (int i=0; i<courthouseCombo.getItemCount(); i++) {
            ComboItem it = courthouseCombo.getItemAt(i);
            if ((it.id == null && id == null) || (it.id != null && it.id.equals(id))) {
                courthouseCombo.setSelectedIndex(i);
                return;
            }
        }
        // if not found, pick none
        courthouseCombo.setSelectedIndex(0);
    }

    private void clearForm() {
        selectedId = null;
        nameField.setText("");
        specField.setText("");
        courthouseCombo.setSelectedIndex(0);
        table.clearSelection();
        nameField.requestFocus();
    }

    private void save() {
        String name = nameField.getText().trim();
        String spec = specField.getText().trim();
        ComboItem chosen = (ComboItem) courthouseCombo.getSelectedItem();
        Long courthouseId = (chosen != null) ? chosen.id : null;

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Full name is required.", "Validation",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String sql = "INSERT INTO judge(full_name, specialization, courthouse_id) VALUES(?,?,?)";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, spec.isEmpty() ? null : spec);
            if (courthouseId == null) ps.setNull(3, Types.INTEGER);
            else ps.setLong(3, courthouseId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Saved.");
            clearForm();
            loadJudges();
        } catch (SQLException ex) { showError(ex); }
    }

    private void update() {
        if (selectedId == null) {
            JOptionPane.showMessageDialog(this, "Select a row first.", "Info",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String name = nameField.getText().trim();
        String spec = specField.getText().trim();
        ComboItem chosen = (ComboItem) courthouseCombo.getSelectedItem();
        Long courthouseId = (chosen != null) ? chosen.id : null;
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Full name is required.", "Validation",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        String sql = "UPDATE judge SET full_name=?, specialization=?, courthouse_id=? WHERE judge_id=?";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, spec.isEmpty() ? null : spec);
            if (courthouseId == null) ps.setNull(3, Types.INTEGER);
            else ps.setLong(3, courthouseId);
            ps.setLong(4, selectedId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Updated.");
            clearForm();
            loadJudges();
        } catch (SQLException ex) { showError(ex); }
    }

    private void deleteSelected() {
        if (selectedId == null) {
            JOptionPane.showMessageDialog(this, "Select a row first.", "Info",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this, "Delete selected judge?", "Confirm",
                JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;

        String sql = "DELETE FROM judge WHERE judge_id=?";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, selectedId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Deleted.");
            clearForm();
            loadJudges();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Cannot delete: referenced by other records.\n" + ex.getMessage(),
                    "Delete failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadCourthouses() {
        courthouseCombo.removeAllItems();
        courthouseCombo.addItem(new ComboItem(null, "— (none) —"));
        String sql = "SELECT courthouse_id, name FROM courthouse ORDER BY name ASC";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                courthouseCombo.addItem(new ComboItem(rs.getLong("courthouse_id"),
                                                      rs.getString("name")));
            }
        } catch (SQLException ex) { showError(ex); }
    }

    private void loadJudges() {
        tableModel.setRowCount(0);
        String sql = """
            SELECT j.judge_id, j.full_name, j.specialization, j.created_at,
                   c.name AS courthouse_name
            FROM judge j
            LEFT JOIN courthouse c ON c.courthouse_id = j.courthouse_id
            ORDER BY j.judge_id DESC
        """;
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getLong("judge_id"));
                row.add(rs.getString("full_name"));
                row.add(rs.getString("specialization"));
                row.add(rs.getString("courthouse_name"));
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

    /** Combo item for (id,label) */
    private static class ComboItem {
        final Long id; final String label;
        ComboItem(Long id, String label) { this.id = id; this.label = label; }
        @Override public String toString() { return label; }
    }
}
