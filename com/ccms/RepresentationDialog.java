package com.ccms;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class RepresentationDialog extends JDialog {
    private final long caseId;

    private final JComboBox<ComboItem> personCombo = new JComboBox<>(); // client (should be among case parties)
    private final JComboBox<ComboItem> lawyerCombo = new JComboBox<>();
    private final JComboBox<String> sideCombo = new JComboBox<>(new String[]{"PLAINTIFF","DEFENDANT","NEUTRAL"});
    private final JCheckBox primaryCheck = new JCheckBox("Primary");

    private final JButton addBtn    = new JButton("Add");
    private final JButton updateBtn = new JButton("Update");
    private final JButton deleteBtn = new JButton("Delete");
    private final JButton closeBtn  = new JButton("Close");
    private final JButton refreshBtn= new JButton("Refresh");

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"ID","Person (Client)","Lawyer","Side","Primary?","Added At"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(tableModel);

    private Long selectedId = null; // representation_id

    public RepresentationDialog(Window owner, long caseId) {
        super(owner, "Manage Representation for Case #" + caseId, ModalityType.APPLICATION_MODAL);
        this.caseId = caseId;
        setSize(800, 480);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(8,8));
        ((JComponent)getContentPane()).setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        // top form
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4,4,4,4);
        gc.anchor = GridBagConstraints.EAST;

        gc.gridx=0; gc.gridy=0; form.add(new JLabel("Person (client):"), gc);
        gc.gridx=1; gc.anchor = GridBagConstraints.WEST; form.add(personCombo, gc);

        gc.gridx=0; gc.gridy=1; gc.anchor = GridBagConstraints.EAST; form.add(new JLabel("Lawyer:"), gc);
        gc.gridx=1; gc.anchor = GridBagConstraints.WEST; form.add(lawyerCombo, gc);

        gc.gridx=0; gc.gridy=2; gc.anchor = GridBagConstraints.EAST; form.add(new JLabel("Side:"), gc);
        gc.gridx=1; gc.anchor = GridBagConstraints.WEST; form.add(sideCombo, gc);

        gc.gridx=1; gc.gridy=3; gc.anchor = GridBagConstraints.WEST; form.add(primaryCheck, gc);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.add(addBtn); buttons.add(updateBtn); buttons.add(deleteBtn); buttons.add(refreshBtn); buttons.add(closeBtn);
        gc.gridx=1; gc.gridy=4; gc.anchor = GridBagConstraints.WEST; form.add(buttons, gc);

        add(form, BorderLayout.NORTH);

        // table
        table.setRowHeight(22);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // actions
        addBtn.addActionListener(e -> addRep());
        updateBtn.addActionListener(e -> updateRep());
        deleteBtn.addActionListener(e -> deleteRep());
        refreshBtn.addActionListener(e -> loadReps());
        closeBtn.addActionListener(e -> dispose());
        table.getSelectionModel().addListSelectionListener(e -> onRowSelected());

        loadPersonChoices(); // only people already added to this case (via case_person)
        loadLawyerChoices();
        loadReps();
    }

    private void loadPersonChoices() {
        personCombo.removeAllItems();
        String sql = """
            SELECT p.person_id, p.full_name
            FROM case_person cp
            JOIN person p ON p.person_id = cp.person_id
            WHERE cp.case_id = ?
            GROUP BY p.person_id, p.full_name
            ORDER BY p.full_name ASC
        """;
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, caseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    personCombo.addItem(new ComboItem(rs.getLong("person_id"), rs.getString("full_name")));
                }
            }
        } catch (SQLException ex) { showError(ex); }
    }

    private void loadLawyerChoices() {
        lawyerCombo.removeAllItems();
        String sql = "SELECT lawyer_id, full_name FROM lawyer ORDER BY full_name ASC";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lawyerCombo.addItem(new ComboItem(rs.getLong("lawyer_id"), rs.getString("full_name")));
            }
        } catch (SQLException ex) { showError(ex); }
    }

    private void addRep() {
        ComboItem person = (ComboItem) personCombo.getSelectedItem();
        ComboItem lawyer = (ComboItem) lawyerCombo.getSelectedItem();
        String side = (String) sideCombo.getSelectedItem();
        int primary = primaryCheck.isSelected() ? 1 : 0;

        if (person == null || lawyer == null) {
            JOptionPane.showMessageDialog(this, "Pick both person and lawyer.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String sql = "INSERT INTO representation(case_id, person_id, lawyer_id, side, is_primary) VALUES(?,?,?,?,?)";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, caseId);
            ps.setLong(2, person.id);
            ps.setLong(3, lawyer.id);
            ps.setString(4, side);
            ps.setInt(5, primary);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Added.");
            selectedId = null; table.clearSelection(); loadReps();
        } catch (SQLException ex) {
            if (isUniqueViolation(ex)) {
                JOptionPane.showMessageDialog(this, "This lawyer already represents this person on that side.",
                        "Duplicate", JOptionPane.WARNING_MESSAGE);
            } else showError(ex);
        }
    }

    private void updateRep() {
        if (selectedId == null) {
            JOptionPane.showMessageDialog(this, "Select a row first.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        ComboItem person = (ComboItem) personCombo.getSelectedItem();
        ComboItem lawyer = (ComboItem) lawyerCombo.getSelectedItem();
        String side = (String) sideCombo.getSelectedItem();
        int primary = primaryCheck.isSelected() ? 1 : 0;

        if (person == null || lawyer == null) {
            JOptionPane.showMessageDialog(this, "Pick both person and lawyer.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String sql = "UPDATE representation SET person_id=?, lawyer_id=?, side=?, is_primary=? WHERE representation_id=?";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, person.id);
            ps.setLong(2, lawyer.id);
            ps.setString(3, side);
            ps.setInt(4, primary);
            ps.setLong(5, selectedId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Updated.");
            selectedId = null; table.clearSelection(); loadReps();
        } catch (SQLException ex) {
            if (isUniqueViolation(ex)) {
                JOptionPane.showMessageDialog(this, "Duplicate combination.", "Duplicate", JOptionPane.WARNING_MESSAGE);
            } else showError(ex);
        }
    }

    private void deleteRep() {
        if (selectedId == null) {
            JOptionPane.showMessageDialog(this, "Select a row first.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this, "Remove selected representation?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;

        String sql = "DELETE FROM representation WHERE representation_id=?";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, selectedId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Deleted.");
            selectedId = null; table.clearSelection(); loadReps();
        } catch (SQLException ex) { showError(ex); }
    }

    private void loadReps() {
        tableModel.setRowCount(0);
        String sql = """
            SELECT r.representation_id, r.side, r.is_primary, r.created_at,
                   p.full_name AS person_name, l.full_name AS lawyer_name
            FROM representation r
            JOIN person p ON p.person_id = r.person_id
            JOIN lawyer l ON l.lawyer_id = r.lawyer_id
            WHERE r.case_id = ?
            ORDER BY r.representation_id DESC
        """;
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, caseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Vector<Object> row = new Vector<>();
                    row.add(rs.getLong("representation_id"));
                    row.add(rs.getString("person_name"));
                    row.add(rs.getString("lawyer_name"));
                    row.add(rs.getString("side"));
                    row.add(rs.getInt("is_primary")==1 ? "Yes" : "No");
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
        String lawyerName = (String) tableModel.getValueAt(r, 2);
        String side       = (String) tableModel.getValueAt(r, 3);
        boolean primary   = "Yes".equals(tableModel.getValueAt(r, 4));

        selectComboByLabel(personCombo, personName);
        selectComboByLabel(lawyerCombo, lawyerName);
        sideCombo.setSelectedItem(side);
        primaryCheck.setSelected(primary);
    }

    private void selectComboByLabel(JComboBox<ComboItem> combo, String label) {
        for (int i=0;i<combo.getItemCount();i++) {
            ComboItem it = combo.getItemAt(i);
            if (it.label.equals(label)) { combo.setSelectedIndex(i); return; }
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
