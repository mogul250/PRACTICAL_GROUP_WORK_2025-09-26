package com.ccms;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Vector;

public class HearingPanel extends JPanel {
    private final User currentUser;

    private final JComboBox<ComboItem> caseCombo   = new JComboBox<>();
    private final JTextField dateTimeField         = new JTextField(16); // YYYY-MM-DD HH:mm
    private final JTextField roomField             = new JTextField(16);
    private final JComboBox<ComboItem> judgeCombo  = new JComboBox<>();
    private final JTextField purposeField          = new JTextField(24);
    private final JTextField outcomeField          = new JTextField(24);

    private final JButton saveBtn    = new JButton("Save");
    private final JButton updateBtn  = new JButton("Update");
    private final JButton deleteBtn  = new JButton("Delete");
    private final JButton clearBtn   = new JButton("Clear");
    private final JButton refreshBtn = new JButton("Refresh");

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"ID","Case","Scheduled","Room","Judge","Purpose","Outcome","Created"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(tableModel);

    private Long selectedId = null;
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public HearingPanel(User currentUser) {
        this.currentUser = currentUser;
        setLayout(new BorderLayout(10,10));
        setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4,4,4,4);

        int y=0;
        gc.anchor = GridBagConstraints.EAST; gc.gridx=0; gc.gridy=y; form.add(new JLabel("Case:"), gc);
        gc.anchor = GridBagConstraints.WEST; gc.gridx=1; form.add(caseCombo, gc); y++;

        gc.anchor = GridBagConstraints.EAST; gc.gridx=0; gc.gridy=y; form.add(new JLabel("Scheduled (YYYY-MM-DD HH:mm):"), gc);
        gc.anchor = GridBagConstraints.WEST; gc.gridx=1; form.add(dateTimeField, gc); y++;

        gc.anchor = GridBagConstraints.EAST; gc.gridx=0; gc.gridy=y; form.add(new JLabel("Room:"), gc);
        gc.anchor = GridBagConstraints.WEST; gc.gridx=1; form.add(roomField, gc); y++;

        gc.anchor = GridBagConstraints.EAST; gc.gridx=0; gc.gridy=y; form.add(new JLabel("Presiding Judge:"), gc);
        gc.anchor = GridBagConstraints.WEST; gc.gridx=1; form.add(judgeCombo, gc); y++;

        gc.anchor = GridBagConstraints.EAST; gc.gridx=0; gc.gridy=y; form.add(new JLabel("Purpose:"), gc);
        gc.anchor = GridBagConstraints.WEST; gc.gridx=1; form.add(purposeField, gc); y++;

        gc.anchor = GridBagConstraints.EAST; gc.gridx=0; gc.gridy=y; form.add(new JLabel("Outcome:"), gc);
        gc.anchor = GridBagConstraints.WEST; gc.gridx=1; form.add(outcomeField, gc); y++;

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.add(saveBtn); buttons.add(updateBtn); buttons.add(deleteBtn); buttons.add(clearBtn); buttons.add(refreshBtn);
        gc.anchor = GridBagConstraints.WEST; gc.gridx=1; gc.gridy=y; form.add(buttons, gc);

        add(form, BorderLayout.NORTH);

        table.setFillsViewportHeight(true);
        table.setRowHeight(22);
        add(new JScrollPane(table), BorderLayout.CENTER);

        saveBtn.addActionListener(e -> save());
        updateBtn.addActionListener(e -> update());
        deleteBtn.addActionListener(e -> deleteSelected());
        clearBtn.addActionListener(e -> clearForm());
        refreshBtn.addActionListener(e -> loadAll());
        table.getSelectionModel().addListSelectionListener(e -> onRowSelected());

        loadCases();
        loadJudges();
        loadAll();
    }

    private void loadCases() {
        caseCombo.removeAllItems();
        String sql = "SELECT case_id, case_number, title FROM court_case ORDER BY case_id DESC";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String label = rs.getString("case_number") + " — " + rs.getString("title");
                caseCombo.addItem(new ComboItem(rs.getLong("case_id"), label));
            }
        } catch (SQLException ex) { showError(ex); }
    }

    private void loadJudges() {
        judgeCombo.removeAllItems();
        judgeCombo.addItem(new ComboItem(null, "— (none) —"));
        String sql = "SELECT judge_id, full_name FROM judge ORDER BY full_name ASC";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                judgeCombo.addItem(new ComboItem(rs.getLong("judge_id"), rs.getString("full_name")));
            }
        } catch (SQLException ex) { showError(ex); }
    }

    private void save() {
        ComboItem c = (ComboItem) caseCombo.getSelectedItem();
        if (c == null) { JOptionPane.showMessageDialog(this, "Pick a case.", "Validation", JOptionPane.WARNING_MESSAGE); return; }

        String dt = dateTimeField.getText().trim();
        try { LocalDateTime.parse(dt, DT); } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Date/time must be YYYY-MM-DD HH:mm", "Validation", JOptionPane.WARNING_MESSAGE); return;
        }
        String room = roomField.getText().trim();
        ComboItem j = (ComboItem) judgeCombo.getSelectedItem();
        Long judgeId = (j == null ? null : j.id);
        String purpose = purposeField.getText().trim();
        String outcome = outcomeField.getText().trim();

        String sql = "INSERT INTO hearing(case_id,scheduled_at,room,presiding_judge_id,purpose,outcome) VALUES(?,?,?,?,?,?)";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, c.id);
            ps.setString(2, dt);
            ps.setString(3, room.isEmpty() ? null : room);
            if (judgeId == null) ps.setNull(4, Types.INTEGER); else ps.setLong(4, judgeId);
            ps.setString(5, purpose.isEmpty() ? null : purpose);
            ps.setString(6, outcome.isEmpty() ? null : outcome);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Saved.");
            clearForm(); loadAll();
        } catch (SQLException ex) { showError(ex); }
    }

    private void update() {
        if (selectedId == null) { JOptionPane.showMessageDialog(this, "Select a row first.", "Info", JOptionPane.INFORMATION_MESSAGE); return; }
        ComboItem c = (ComboItem) caseCombo.getSelectedItem();
        if (c == null) { JOptionPane.showMessageDialog(this, "Pick a case.", "Validation", JOptionPane.WARNING_MESSAGE); return; }

        String dt = dateTimeField.getText().trim();
        try { LocalDateTime.parse(dt, DT); } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Date/time must be YYYY-MM-DD HH:mm", "Validation", JOptionPane.WARNING_MESSAGE); return;
        }
        String room = roomField.getText().trim();
        ComboItem j = (ComboItem) judgeCombo.getSelectedItem();
        Long judgeId = (j == null ? null : j.id);
        String purpose = purposeField.getText().trim();
        String outcome = outcomeField.getText().trim();

        String sql = "UPDATE hearing SET case_id=?, scheduled_at=?, room=?, presiding_judge_id=?, purpose=?, outcome=? WHERE hearing_id=?";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, c.id);
            ps.setString(2, dt);
            ps.setString(3, room.isEmpty() ? null : room);
            if (judgeId == null) ps.setNull(4, Types.INTEGER); else ps.setLong(4, judgeId);
            ps.setString(5, purpose.isEmpty() ? null : purpose);
            ps.setString(6, outcome.isEmpty() ? null : outcome);
            ps.setLong(7, selectedId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Updated.");
            clearForm(); loadAll();
        } catch (SQLException ex) { showError(ex); }
    }
    private void clearForm() {
        selectedId = null;
        if (caseCombo.getItemCount() > 0) caseCombo.setSelectedIndex(0);
        // default to now (YYYY-MM-DD HH:mm)
        dateTimeField.setText(java.time.LocalDateTime.now().format(DT));
        roomField.setText("");
        if (judgeCombo.getItemCount() > 0) judgeCombo.setSelectedIndex(0);
        purposeField.setText("");
        outcomeField.setText("");
        table.clearSelection();
        caseCombo.requestFocusInWindow();
    }

    private void deleteSelected() {
        if (selectedId == null) { JOptionPane.showMessageDialog(this, "Select a row first.", "Info", JOptionPane.INFORMATION_MESSAGE); return; }
        int ok = JOptionPane.showConfirmDialog(this, "Delete selected hearing?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;

        String sql = "DELETE FROM hearing WHERE hearing_id=?";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, selectedId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Deleted.");
            clearForm(); loadAll();
        } catch (SQLException ex) { showError(ex); }
    }

    private void loadAll() {
        tableModel.setRowCount(0);
        String sql = """
            SELECT h.hearing_id, h.scheduled_at, h.room, h.purpose, h.outcome, h.created_at,
                   k.case_number, k.title,
                   j.full_name AS judge_name
            FROM hearing h
            JOIN court_case k ON k.case_id = h.case_id
            LEFT JOIN judge j ON j.judge_id = h.presiding_judge_id
            ORDER BY h.hearing_id DESC
        """;
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getLong("hearing_id"));
                row.add(rs.getString("case_number") + " — " + rs.getString("title"));
                row.add(rs.getString("scheduled_at"));
                row.add(rs.getString("room"));
                row.add(rs.getString("judge_name"));
                row.add(rs.getString("purpose"));
                row.add(rs.getString("outcome"));
                row.add(rs.getString("created_at"));
                tableModel.addRow(row);
            }
        } catch (SQLException ex) { showError(ex); }
    }

    private void onRowSelected() {
        if (table.getSelectedRow() < 0) return;
        int r = table.getSelectedRow();
        selectedId = (Long) tableModel.getValueAt(r, 0);

        String sql = "SELECT case_id, scheduled_at, room, presiding_judge_id, purpose, outcome FROM hearing WHERE hearing_id=?";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, selectedId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    selectComboById(caseCombo, rs.getLong("case_id"));
                    dateTimeField.setText(rs.getString("scheduled_at"));
                    roomField.setText(rs.getString("room"));
                    selectComboById(judgeCombo, (rs.getObject("presiding_judge_id")==null ? null : rs.getLong("presiding_judge_id")));
                    purposeField.setText(rs.getString("purpose"));
                    outcomeField.setText(rs.getString("outcome"));
                }
            }
        } catch (SQLException ex) { showError(ex); }
    }

    private void selectComboById(JComboBox<ComboItem> combo, Long id) {
        for (int i=0; i<combo.getItemCount(); i++) {
            ComboItem it = combo.getItemAt(i);
            if ((it.id==null && id==null) || (it.id!=null && it.id.equals(id))) { combo.setSelectedIndex(i); return; }
        }
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
