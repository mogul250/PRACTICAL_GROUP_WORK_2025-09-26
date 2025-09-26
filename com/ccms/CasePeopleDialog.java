package com.ccms;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class CasePeopleDialog extends JDialog {
    private final long caseId;

    private final JComboBox<ComboItem> personCombo = new JComboBox<>();
    private final JComboBox<String> roleCombo = new JComboBox<>(new String[]{
            "PLAINTIFF","DEFENDANT","WITNESS","INTERESTED_PARTY","OTHER"
    });

    private final JButton addBtn    = new JButton("Add");
    private final JButton updateBtn = new JButton("Update");
    private final JButton deleteBtn = new JButton("Delete");
    private final JButton closeBtn  = new JButton("Close");
    private final JButton refreshBtn= new JButton("Refresh");

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"ID","Person","Role","Added At"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(tableModel);

    private Long selectedId = null; // case_person_id

    public CasePeopleDialog(Window owner, long caseId) {
        super(owner, "Manage Parties for Case #" + caseId, ModalityType.APPLICATION_MODAL);
        this.caseId = caseId;
        setSize(700, 450);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(8,8));
        ((JComponent)getContentPane()).setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        // Top form
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4,4,4,4);
        gc.anchor = GridBagConstraints.EAST;

        gc.gridx=0; gc.gridy=0; form.add(new JLabel("Person:"), gc);
        gc.gridx=1; gc.anchor = GridBagConstraints.WEST; form.add(personCombo, gc);

        gc.gridx=0; gc.gridy=1; gc.anchor = GridBagConstraints.EAST; form.add(new JLabel("Role:"), gc);
        gc.gridx=1; gc.anchor = GridBagConstraints.WEST; form.add(roleCombo, gc);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.add(addBtn); buttons.add(updateBtn); buttons.add(deleteBtn); buttons.add(refreshBtn); buttons.add(closeBtn);
        gc.gridx=1; gc.gridy=2; gc.anchor = GridBagConstraints.WEST; form.add(buttons, gc);

        add(form, BorderLayout.NORTH);

        // Table
        table.setRowHeight(22);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // actions
        addBtn.addActionListener(e -> addParty());
        updateBtn.addActionListener(e -> updateParty());
        deleteBtn.addActionListener(e -> deleteParty());
        refreshBtn.addActionListener(e -> loadParties());
        closeBtn.addActionListener(e -> dispose());
        table.getSelectionModel().addListSelectionListener(e -> onRowSelected());

        loadPeopleChoices();
        loadParties();
    }

    private void loadPeopleChoices() {
        personCombo.removeAllItems();
        String sql = "SELECT person_id, full_name FROM person ORDER BY full_name ASC";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                personCombo.addItem(new ComboItem(rs.getLong("person_id"), rs.getString("full_name")));
            }
        } catch (SQLException ex) { showError(ex); }
    }

    private void addParty() {
        ComboItem p = (ComboItem) personCombo.getSelectedItem();
        String role = (String) roleCombo.getSelectedItem();
        if (p == null) { JOptionPane.showMessageDialog(this, "Pick a person.", "Validation", JOptionPane.WARNING_MESSAGE); return; }

        String sql = "INSERT INTO case_person(case_id, person_id, role) VALUES(?,?,?)";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, caseId);
            ps.setLong(2, p.id);
            ps.setString(3, role);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Added.");
            selectedId = null;
            table.clearSelection();
            loadParties();
        } catch (SQLException ex) {
            if (isUniqueViolation(ex)) {
                JOptionPane.showMessageDialog(this, "This person with this role is already in the case.",
                        "Duplicate", JOptionPane.WARNING_MESSAGE);
            } else showError(ex);
        }
    }

    private void updateParty() {
        if (selectedId == null) { JOptionPane.showMessageDialog(this, "Select a row first.", "Info", JOptionPane.INFORMATION_MESSAGE); return; }
        ComboItem p = (ComboItem) personCombo.getSelectedItem();
        String role = (String) roleCombo.getSelectedItem();
        if (p == null) { JOptionPane.showMessageDialog(this, "Pick a person.", "Validation", JOptionPane.WARNING_MESSAGE); return; }

        String sql = "UPDATE case_person SET person_id=?, role=? WHERE case_person_id=?";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, p.id);
            ps.setString(2, role);
            ps.setLong(3, selectedId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Updated.");
            selectedId = null; table.clearSelection(); loadParties();
        } catch (SQLException ex) {
            if (isUniqueViolation(ex)) {
                JOptionPane.showMessageDialog(this, "Duplicate combination.", "Duplicate", JOptionPane.WARNING_MESSAGE);
            } else showError(ex);
        }
    }

    private void deleteParty() {
        if (selectedId == null) { JOptionPane.showMessageDialog(this, "Select a row first.", "Info", JOptionPane.INFORMATION_MESSAGE); return; }
        int ok = JOptionPane.showConfirmDialog(this, "Remove selected party from this case?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;

        String sql = "DELETE FROM case_person WHERE case_person_id=?";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, selectedId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Removed.");
            selectedId = null; table.clearSelection(); loadParties();
        } catch (SQLException ex) { showError(ex); }
    }

    private void loadParties() {
        tableModel.setRowCount(0);
        String sql = """
            SELECT cp.case_person_id, p.full_name, cp.role, cp.created_at
            FROM case_person cp
            JOIN person p ON p.person_id = cp.person_id
            WHERE cp.case_id = ?
            ORDER BY cp.case_person_id DESC
        """;
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, caseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Vector<Object> row = new Vector<>();
                    row.add(rs.getLong("case_person_id"));
                    row.add(rs.getString("full_name"));
                    row.add(rs.getString("role"));
                    row.add(rs.getString("created_at"));
                    tableModel.addRow(row);
                }
            }
        } catch (SQLException ex) { showError(ex); }
    }

    private void onRowSelected() {
        if (table.getSelectedRow() < 0) return;
        int r = table.getSelectedRow();
        selectedId = (Long) tableModel.getValueAt(r, 0);
        String personName = (String) tableModel.getValueAt(r, 1);
        String role = (String) tableModel.getValueAt(r, 2);

        // select combo items
        selectPersonByLabel(personName);
        roleCombo.setSelectedItem(role);
    }

    private void selectPersonByLabel(String label) {
        for (int i=0;i<personCombo.getItemCount();i++) {
            ComboItem it = personCombo.getItemAt(i);
            if (it.label.equals(label)) { personCombo.setSelectedIndex(i); return; }
        }
    }

    private boolean isUniqueViolation(SQLException ex) {
        String m = ex.getMessage();
        return m != null && m.toLowerCase().contains("unique");
    }

    private void showError(Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(this, "DB error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }

    private static class ComboItem {
        final Long id; final String label;
        ComboItem(Long id, String label) { this.id=id; this.label=label; }
        @Override public String toString() { return label; }
    }
}
