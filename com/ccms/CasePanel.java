package com.ccms;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Vector;

public class CasePanel extends JPanel {
    private final User currentUser;

    // form controls
    private final JTextField caseNumberField = new JTextField(20);
    private final JTextField titleField      = new JTextField(28);
    private final JComboBox<String> statusCombo =
            new JComboBox<>(new String[]{"FILED","ACTIVE","STAYED","CLOSED","APPEALED"});
    private final JTextField filedDateField  = new JTextField(12); // YYYY-MM-DD
    private final JComboBox<ComboItem> courthouseCombo = new JComboBox<>();
    private final JComboBox<ComboItem> judgeCombo      = new JComboBox<>();

    private final JButton saveBtn    = new JButton("Save");
    private final JButton updateBtn  = new JButton("Update");
    private final JButton deleteBtn  = new JButton("Delete");
    private final JButton clearBtn   = new JButton("Clear");
    private final JButton refreshBtn = new JButton("Refresh");
    private final JButton partiesBtn = new JButton("Manage Parties");
    private final JButton reprBtn    = new JButton("Manage Representation");

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"ID","Case #","Title","Status","Filed","Courthouse","Lead Judge","Created"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(tableModel);

    private Long selectedId = null;
    private static final DateTimeFormatter D = DateTimeFormatter.ISO_LOCAL_DATE;

    public CasePanel(User currentUser) {
        this.currentUser = currentUser;
        setLayout(new BorderLayout(10,10));
        setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        // form
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4,4,4,4);

        int y=0;
        gc.anchor = GridBagConstraints.EAST; gc.gridx=0; gc.gridy=y; form.add(new JLabel("Case #:"), gc);
        gc.anchor = GridBagConstraints.WEST; gc.gridx=1; form.add(caseNumberField, gc); y++;

        gc.anchor = GridBagConstraints.EAST; gc.gridx=0; gc.gridy=y; form.add(new JLabel("Title:"), gc);
        gc.anchor = GridBagConstraints.WEST; gc.gridx=1; form.add(titleField, gc); y++;

        gc.anchor = GridBagConstraints.EAST; gc.gridx=0; gc.gridy=y; form.add(new JLabel("Status:"), gc);
        gc.anchor = GridBagConstraints.WEST; gc.gridx=1; form.add(statusCombo, gc); y++;

        gc.anchor = GridBagConstraints.EAST; gc.gridx=0; gc.gridy=y; form.add(new JLabel("Filed (YYYY-MM-DD):"), gc);
        gc.anchor = GridBagConstraints.WEST; gc.gridx=1; form.add(filedDateField, gc); y++;

        gc.anchor = GridBagConstraints.EAST; gc.gridx=0; gc.gridy=y; form.add(new JLabel("Courthouse:"), gc);
        gc.anchor = GridBagConstraints.WEST; gc.gridx=1; form.add(courthouseCombo, gc); y++;

        gc.anchor = GridBagConstraints.EAST; gc.gridx=0; gc.gridy=y; form.add(new JLabel("Lead Judge:"), gc);
        gc.anchor = GridBagConstraints.WEST; gc.gridx=1; form.add(judgeCombo, gc); y++;

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.add(saveBtn); buttons.add(updateBtn); buttons.add(deleteBtn); buttons.add(clearBtn); buttons.add(refreshBtn);
        buttons.add(partiesBtn);
        buttons.add(reprBtn);

        gc.anchor = GridBagConstraints.WEST; gc.gridx=1; gc.gridy=y; form.add(buttons, gc);

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
        partiesBtn.addActionListener(e -> openPartiesDialog());
        reprBtn.addActionListener(e -> openReprDialog());


        loadCourthouses();
        loadJudges();
        loadAll();
    }
    private void openPartiesDialog() {
    if (selectedId == null) {
        JOptionPane.showMessageDialog(this, "Select a case first.", "Info", JOptionPane.INFORMATION_MESSAGE);
        return;
    }
    new CasePeopleDialog(SwingUtilities.getWindowAncestor(this), selectedId).setVisible(true);
    loadAll(); // refresh table after dialog closes
    }

    private void openReprDialog() {
        if (selectedId == null) {
            JOptionPane.showMessageDialog(this, "Select a case first.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        new RepresentationDialog(SwingUtilities.getWindowAncestor(this), selectedId).setVisible(true);
        loadAll();
    }

    private void loadCourthouses() {
        courthouseCombo.removeAllItems();
        String sql = "SELECT courthouse_id, name FROM courthouse ORDER BY name ASC";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                courthouseCombo.addItem(new ComboItem(rs.getLong("courthouse_id"), rs.getString("name")));
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
        String num = caseNumberField.getText().trim();
        String tit = titleField.getText().trim();
        String sts = (String) statusCombo.getSelectedItem();
        String fd  = filedDateField.getText().trim();
        if (num.isEmpty() || tit.isEmpty() || fd.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Case #, Title, Filed date are required.", "Validation",
                    JOptionPane.WARNING_MESSAGE); return;
        }
        // validate date
        try { LocalDate.parse(fd, D); } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Filed date must be YYYY-MM-DD.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        ComboItem ch = (ComboItem) courthouseCombo.getSelectedItem();
        if (ch == null) { JOptionPane.showMessageDialog(this, "Pick a courthouse.", "Validation", JOptionPane.WARNING_MESSAGE); return; }
        Long courthouseId = ch.id;

        ComboItem j = (ComboItem) judgeCombo.getSelectedItem();
        Long judgeId = (j == null ? null : j.id);

        String sql = "INSERT INTO court_case(case_number,title,status,filed_date,courthouse_id,assigned_judge_id) " +
                     "VALUES(?,?,?,?,?,?)";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, num);
            ps.setString(2, tit);
            ps.setString(3, sts);
            ps.setString(4, fd);
            ps.setLong(5, courthouseId);
            if (judgeId == null) ps.setNull(6, Types.INTEGER); else ps.setLong(6, judgeId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Saved.");
            clearForm(); loadAll();
        } catch (SQLException ex) {
            if (isUniqueViolation(ex)) {
                JOptionPane.showMessageDialog(this, "Case number already exists.", "Duplicate", JOptionPane.WARNING_MESSAGE);
            } else showError(ex);
        }
    }
    private void clearForm() {
        selectedId = null;
        caseNumberField.setText("");
        titleField.setText("");
        statusCombo.setSelectedIndex(0);
        // default to today
        filedDateField.setText(java.time.LocalDate.now().toString());
        if (courthouseCombo.getItemCount() > 0) courthouseCombo.setSelectedIndex(0);
        if (judgeCombo.getItemCount() > 0) judgeCombo.setSelectedIndex(0);
        table.clearSelection();
        caseNumberField.requestFocus();
    }

    private void update() {
        if (selectedId == null) {
            JOptionPane.showMessageDialog(this, "Select a row first.", "Info", JOptionPane.INFORMATION_MESSAGE); return;
        }
        String num = caseNumberField.getText().trim();
        String tit = titleField.getText().trim();
        String sts = (String) statusCombo.getSelectedItem();
        String fd  = filedDateField.getText().trim();
        if (num.isEmpty() || tit.isEmpty() || fd.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Case #, Title, Filed date are required.", "Validation",
                    JOptionPane.WARNING_MESSAGE); return;
        }
        try { LocalDate.parse(fd, D); } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Filed date must be YYYY-MM-DD.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        ComboItem ch = (ComboItem) courthouseCombo.getSelectedItem();
        if (ch == null) { JOptionPane.showMessageDialog(this, "Pick a courthouse.", "Validation", JOptionPane.WARNING_MESSAGE); return; }
        Long courthouseId = ch.id;

        ComboItem j = (ComboItem) judgeCombo.getSelectedItem();
        Long judgeId = (j == null ? null : j.id);

        String sql = "UPDATE court_case SET case_number=?, title=?, status=?, filed_date=?, courthouse_id=?, assigned_judge_id=? " +
                     "WHERE case_id=?";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, num);
            ps.setString(2, tit);
            ps.setString(3, sts);
            ps.setString(4, fd);
            ps.setLong(5, courthouseId);
            if (judgeId == null) ps.setNull(6, Types.INTEGER); else ps.setLong(6, judgeId);
            ps.setLong(7, selectedId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Updated.");
            clearForm(); loadAll();
        } catch (SQLException ex) {
            if (isUniqueViolation(ex)) {
                JOptionPane.showMessageDialog(this, "Case number already exists.", "Duplicate", JOptionPane.WARNING_MESSAGE);
            } else showError(ex);
        }
    }

    private void deleteSelected() {
        if (selectedId == null) {
            JOptionPane.showMessageDialog(this, "Select a row first.", "Info", JOptionPane.INFORMATION_MESSAGE); return;
        }
        int ok = JOptionPane.showConfirmDialog(this, "Delete selected case?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;

        String sql = "DELETE FROM court_case WHERE case_id=?";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, selectedId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Deleted.");
            clearForm(); loadAll();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Cannot delete: it may have hearings or links.\n" + ex.getMessage(),
                    "Delete failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadAll() {
        tableModel.setRowCount(0);
        String sql = """
            SELECT k.case_id, k.case_number, k.title, k.status, k.filed_date, k.created_at,
                   c.name AS courthouse_name,
                   j.full_name AS lead_judge
            FROM court_case k
            JOIN courthouse c ON c.courthouse_id = k.courthouse_id
            LEFT JOIN judge j ON j.judge_id = k.assigned_judge_id
            ORDER BY k.case_id DESC
        """;
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getLong("case_id"));
                row.add(rs.getString("case_number"));
                row.add(rs.getString("title"));
                row.add(rs.getString("status"));
                row.add(rs.getString("filed_date"));
                row.add(rs.getString("courthouse_name"));
                row.add(rs.getString("lead_judge"));
                row.add(rs.getString("created_at"));
                tableModel.addRow(row);
            }
        } catch (SQLException ex) { showError(ex); }
    }

    private void onRowSelected() {
        if (table.getSelectedRow() < 0) return;
        int r = table.getSelectedRow();
        selectedId = (Long) tableModel.getValueAt(r, 0);

        // re-read full record for FK ids
        String sql = "SELECT case_number,title,status,filed_date,courthouse_id,assigned_judge_id FROM court_case WHERE case_id=?";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, selectedId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    caseNumberField.setText(rs.getString("case_number"));
                    titleField.setText(rs.getString("title"));
                    statusCombo.setSelectedItem(rs.getString("status"));
                    filedDateField.setText(rs.getString("filed_date"));
                    selectComboById(courthouseCombo, rs.getObject("courthouse_id") == null ? null : rs.getLong("courthouse_id"));
                    selectComboById(judgeCombo, rs.getObject("assigned_judge_id") == null ? null : rs.getLong("assigned_judge_id"));
                }
            }
        } catch (SQLException ex) { showError(ex); }
    }

    private void selectComboById(JComboBox<ComboItem> combo, Long id) {
        for (int i=0;i<combo.getItemCount();i++) {
            ComboItem it = combo.getItemAt(i);
            if ((it.id==null && id==null) || (it.id!=null && it.id.equals(id))) { combo.setSelectedIndex(i); return; }
        }
        if (combo.getItemCount()>0) combo.setSelectedIndex(0);
    }

    private boolean isUniqueViolation(SQLException ex) {
        String m = ex.getMessage();
        return m != null && m.toLowerCase().contains("unique");
    }
    private void showError(Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(this, "DB error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }

    /** tiny (id,label) holder */
    private static class ComboItem {
        final Long id; final String label;
        ComboItem(Long id, String label) { this.id=id; this.label=label; }
        @Override public String toString() { return label; }
    }
}
