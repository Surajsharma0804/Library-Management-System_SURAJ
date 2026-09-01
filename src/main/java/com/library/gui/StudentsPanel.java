package com.library.gui;

import com.library.controller.StudentController;
import com.library.facade.LibraryFacade;
import com.library.media.IDCardGenerator;
import com.library.model.Student;
import com.library.security.Permissions;
import com.library.security.Session;
import com.library.service.StudentImportService;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.List;

/**
 * Executive Student Registry Panel â€” Manage student records, membership status,
 * instant search, and registration workflows.
 *
 * @author University Central Library â€” Software Engineering Division
 * @version 2.0.0
 */
public final class StudentsPanel extends JPanel {

    private final LibraryFacade facade;
    private final StudentController ctrl;
    private JTable table;
    private DefaultTableModel model;
    private JTextField searchField;
    private Session session;

    private static final String[] COLS = {"Reg No.", "Name", "Department", "Course", "Sem", "Status", "Borrows", "Fine Balance"};

    public StudentsPanel(LibraryFacade facade) {
        this.facade = facade;
        this.ctrl = new StudentController(facade);
        setBackground(AppTheme.bg());
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        build();
    }

    private void build() {
        removeAll();

        // â”€â”€ Header â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        JPanel title = new JPanel();
        title.setOpaque(false);
        title.setLayout(new BoxLayout(title, BoxLayout.Y_AXIS));
        title.add(AppTheme.heading("Student Records"));
        title.add(Box.createVerticalStrut(4));
        title.add(AppTheme.label2("Directory of registered library members and active standing"));

        // â”€â”€ Action bar â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        searchField = AppTheme.textField(22);
        searchField.putClientProperty("JTextField.placeholderText", "Search by name, reg number, department...");
        searchField.setPreferredSize(new Dimension(280, 36));
        searchField.setMaximumSize(new Dimension(300, 36));
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { filter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { filter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filter(); }
        });

        JButton suspendBtn = AppTheme.secondaryBtn("Suspend");
        suspendBtn.setPreferredSize(new Dimension(100, 36));
        suspendBtn.addActionListener(e -> suspendStudent());

        JButton activateBtn = AppTheme.secondaryBtn("Activate");
        activateBtn.setPreferredSize(new Dimension(100, 36));
        activateBtn.addActionListener(e -> activateStudent());

        JButton removeBtn = AppTheme.dangerBtn("Remove");
        removeBtn.setPreferredSize(new Dimension(100, 36));
        removeBtn.addActionListener(e -> removeStudent());

        JButton genCardBtn = AppTheme.secondaryBtn("Generate Card");
        genCardBtn.setPreferredSize(new Dimension(130, 36));
        genCardBtn.addActionListener(e -> generateCard());

        JButton importCsvBtn = AppTheme.secondaryBtn("Import CSV");
        importCsvBtn.setPreferredSize(new Dimension(110, 36));
        importCsvBtn.addActionListener(e -> importCsv());

        JButton addBtn = AppTheme.primaryBtn("+ Add Student");
        addBtn.setPreferredSize(new Dimension(130, 36));
        addBtn.addActionListener(e -> addStudent());

        JPanel hdr = new JPanel(new BorderLayout(0, 10));
        hdr.setOpaque(false);
        hdr.add(title, BorderLayout.NORTH);

        JPanel acts = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        acts.setOpaque(false);
        acts.add(searchField);
        acts.add(suspendBtn);
        acts.add(activateBtn);
        acts.add(removeBtn);
        acts.add(genCardBtn);
        acts.add(importCsvBtn);
        acts.add(addBtn);
        hdr.add(acts, BorderLayout.SOUTH);

        // â”€â”€ Table â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        model = new DefaultTableModel(COLS, 0);
        table = new JTable(model) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        AppTheme.styleTable(table);

        // Status column pill renderer
        table.getColumnModel().getColumn(5).setCellRenderer((tbl, val, isSelected, hasFocus, row, col) -> {
            String statusStr = val != null ? val.toString() : "UNKNOWN";
            JPanel pill = AppTheme.createStatusPill(statusStr);
            if (isSelected) { pill.setOpaque(true); pill.setBackground(tbl.getSelectionBackground()); }
            else { pill.setOpaque(false); }
            return pill;
        });

        // Right-click context menu
        JPopupMenu popup = new JPopupMenu();
        JMenuItem viewItem = new JMenuItem("View Details");
        viewItem.addActionListener(e -> viewStudentDetails());
        JMenuItem suspendItem = new JMenuItem("Suspend Student");
        suspendItem.addActionListener(e -> suspendStudent());
        JMenuItem activateItem = new JMenuItem("Activate Student");
        activateItem.addActionListener(e -> activateStudent());
        JMenuItem resetPwItem = new JMenuItem("Reset Password");
        resetPwItem.addActionListener(e -> resetPassword());
        JMenuItem removeItem = new JMenuItem("Remove Student");
        removeItem.addActionListener(e -> removeStudent());
        popup.add(viewItem);
        popup.addSeparator();
        popup.add(suspendItem);
        popup.add(activateItem);
        popup.add(resetPwItem);
        popup.addSeparator();
        popup.add(removeItem);

        table.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { showPopup(e); }
            @Override public void mouseReleased(MouseEvent e) { showPopup(e); }
            private void showPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0) { table.setRowSelectionInterval(row, row); popup.show(table, e.getX(), e.getY()); }
                }
            }
        });

        JPanel tbl = new JPanel(new BorderLayout());
        tbl.setOpaque(false);
        tbl.add(AppTheme.scroll(table), BorderLayout.CENTER);

        add(hdr, BorderLayout.NORTH);
        add(tbl, BorderLayout.CENTER);
    }

    public void refresh(Session s) {
        this.session = s;
        setBackground(AppTheme.bg());
        load(facade.userRepo().findAllStudents());
    }

    private void load(List<Student> list) {
        model.setRowCount(0);
        for (Student s : list) {
            model.addRow(new Object[]{
                    s.getRegistrationNumber(),
                    s.getFirstName() + " " + s.getLastName(),
                    s.getDepartment() != null ? s.getDepartment() : "-",
                    s.getCourse() != null ? s.getCourse() : "-",
                    s.getSemester(),
                    s.getMembershipStatus().name(),
                    s.getCurrentBorrowCount(),
                    String.format("â‚¹%.2f", s.getFineBalancePaise() / 100.0)
            });
        }
    }

    private void filter() {
        String q = searchField.getText().trim().toLowerCase();
        List<Student> all = facade.userRepo().findAllStudents();
        if (q.isEmpty()) { load(all); return; }
        load(all.stream().filter(s ->
                (s.getFirstName() + " " + s.getLastName()).toLowerCase().contains(q)
                || s.getRegistrationNumber().toLowerCase().contains(q)
                || (s.getDepartment() != null && s.getDepartment().toLowerCase().contains(q))
        ).toList());
    }

    /* â”€â”€ Student Actions â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */

    private String getSelectedRegNo() {
        int row = table.getSelectedRow();
        if (row < 0) { AppTheme.error(this, "Please select a student first."); return null; }
        return (String) model.getValueAt(row, 0);
    }

    private void viewStudentDetails() {
        String regNo = getSelectedRegNo();
        if (regNo == null) return;
        Student s = facade.userRepo().findStudentByRegistrationNumber(regNo);
        if (s == null) { AppTheme.error(this, "Student not found."); return; }

        String details = String.format(
                "Registration No:  %s\nUsername:         %s\nName:             %s %s\n" +
                "Email:            %s\nPhone:            %s\nDepartment:       %s\n" +
                "Course:           %s\nSemester:         %d\nSection:          %s\n" +
                "Status:           %s\nLibrary Card:     %s\nActive Borrows:   %d / %d\n" +
                "Fine Balance:     â‚¹%.2f",
                s.getRegistrationNumber(), s.getUsername(), s.getFirstName(), s.getLastName(),
                s.getEmail() != null ? s.getEmail() : "-", s.getPhone() != null ? s.getPhone() : "-",
                s.getDepartment() != null ? s.getDepartment() : "-", s.getCourse() != null ? s.getCourse() : "-",
                s.getSemester(), s.getSection() != null ? s.getSection() : "-",
                s.getMembershipStatus().name(), s.getLibraryCardNumber() != null ? s.getLibraryCardNumber() : "-",
                s.getCurrentBorrowCount(), s.getBorrowLimit(), s.getFineBalancePaise() / 100.0);

        JTextArea area = new JTextArea(details);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setEditable(false); area.setBackground(AppTheme.bgCard()); area.setForeground(AppTheme.fg());
        JOptionPane.showMessageDialog(this, new JScrollPane(area), "Student Details â€” " + regNo,
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void suspendStudent() {
        if (session == null) return;
        String regNo = getSelectedRegNo();
        if (regNo == null) return;
        if (JOptionPane.showConfirmDialog(this, "Suspend student " + regNo + "?",
                "Confirm Suspend", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
        try { ctrl.suspend(session, regNo); refresh(session); AppTheme.success(this, "Student " + regNo + " suspended."); }
        catch (Exception ex) { AppTheme.error(this, ex.getMessage()); }
    }

    private void activateStudent() {
        if (session == null) return;
        String regNo = getSelectedRegNo();
        if (regNo == null) return;
        try { ctrl.activate(session, regNo); refresh(session); AppTheme.success(this, "Student " + regNo + " activated."); }
        catch (Exception ex) { AppTheme.error(this, ex.getMessage()); }
    }

    private void removeStudent() {
        if (session == null) return;
        String regNo = getSelectedRegNo();
        if (regNo == null) return;
        if (JOptionPane.showConfirmDialog(this, "Permanently remove student " + regNo + "?\nThis cannot be undone.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
        try { ctrl.delete(session, regNo); refresh(session); AppTheme.success(this, "Student " + regNo + " removed."); }
        catch (Exception ex) { AppTheme.error(this, ex.getMessage()); }
    }

    private void resetPassword() {
        if (session == null) return;
        String regNo = getSelectedRegNo();
        if (regNo == null) return;
        String newPw = JOptionPane.showInputDialog(this, "Enter new password for " + regNo + ":",
                "Reset Password", JOptionPane.PLAIN_MESSAGE);
        if (newPw == null || newPw.trim().isEmpty()) return;
        try { ctrl.resetPassword(session, regNo, newPw.trim()); AppTheme.success(this, "Password reset for " + regNo + "."); }
        catch (Exception ex) { AppTheme.error(this, ex.getMessage()); }
    }

    private void generateCard() {
        if (session == null) return;
        try { facade.rbac().require(session, Permissions.STUDENT_GENERATE_CARD); }
        catch (Exception ex) { AppTheme.error(this, ex.getMessage()); return; }

        int row = table.getSelectedRow();
        if (row < 0) { AppTheme.error(this, "Please select a student first."); return; }
        String regNo = (String) model.getValueAt(row, 0);
        Student student = facade.userRepo().findStudentByRegistrationNumber(regNo);
        if (student == null) { AppTheme.error(this, "Student record not found."); return; }

        new SwingWorker<Path, Void>() {
            @Override protected Path doInBackground() throws Exception {
                return new IDCardGenerator().generateAndSave(student, facade.config().get());
            }
            @Override protected void done() {
                try { JOptionPane.showMessageDialog(StudentsPanel.this, "ID Card saved to:\n" + get().toAbsolutePath(),
                        "ID Card Generated", JOptionPane.INFORMATION_MESSAGE); }
                catch (Exception ex) { AppTheme.error(StudentsPanel.this, "Failed: " + ex.getMessage()); }
            }
        }.execute();
    }

    private void importCsv() {
        if (session == null) return;
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select CSV File");
        chooser.setFileFilter(new FileNameExtensionFilter("CSV Files (*.csv)", "csv"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        java.io.File file = chooser.getSelectedFile();
        new SwingWorker<StudentImportService.ImportResult, Void>() {
            @Override protected StudentImportService.ImportResult doInBackground() throws Exception {
                return facade.studentImport().importFromCsv(session, file.toPath());
            }
            @Override protected void done() {
                try {
                    StudentImportService.ImportResult result = get();
                    StringBuilder msg = new StringBuilder();
                    msg.append("Imported: ").append(result.imported()).append(" students\n");
                    msg.append("Skipped: ").append(result.skipped()).append(" rows");
                    if (!result.skipReasons().isEmpty()) {
                        msg.append("\n\nSkip reasons:\n");
                        result.skipReasons().forEach(r -> msg.append("  â€¢ ").append(r).append("\n"));
                    }
                    JOptionPane.showMessageDialog(StudentsPanel.this, msg.toString(),
                            "CSV Import Complete", JOptionPane.INFORMATION_MESSAGE);
                    if (result.imported() > 0) refresh(session);
                } catch (Exception ex) { AppTheme.error(StudentsPanel.this, "Import failed: " + ex.getMessage()); }
            }
        }.execute();
    }

    private void addStudent() {
        if (session == null) return;

        JPanel f = new JPanel(new GridLayout(0, 2, 12, 12));
        f.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JTextField fn = AppTheme.textField(15);
        JTextField ln = AppTheme.textField(15);
        JTextField em = AppTheme.textField(15);
        JTextField ph = AppTheme.textField(15);
        JTextField dp = AppTheme.textField(15);
        JTextField co = AppTheme.textField(15);
        JTextField sm = AppTheme.textField(15); sm.setText("1");
        JTextField sc = AppTheme.textField(15);

        f.add(lbl("First Name:")); f.add(fn);
        f.add(lbl("Last Name:"));  f.add(ln);
        f.add(lbl("Email:"));      f.add(em);
        f.add(lbl("Phone:"));      f.add(ph);
        f.add(lbl("Department:")); f.add(dp);
        f.add(lbl("Course:"));     f.add(co);
        f.add(lbl("Semester:"));   f.add(sm);
        f.add(lbl("Section:"));    f.add(sc);

        if (JOptionPane.showConfirmDialog(this, f, "Register New Student Record",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                Student s = ctrl.register(session, fn.getText().trim(), ln.getText().trim(),
                        em.getText().trim(), ph.getText().trim(), dp.getText().trim(),
                        co.getText().trim(), Integer.parseInt(sm.getText().trim()), sc.getText().trim());

                // Show credentials FIRST â€” before refresh so dialog isn't hidden
                JPanel credPanel = new JPanel(new GridLayout(0, 2, 12, 8));
                credPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

                credPanel.add(lbl("Username:"));
                JTextField uF = new JTextField(s.getUsername()); uF.setEditable(false); uF.setFont(AppTheme.BODY_B);
                credPanel.add(uF);

                credPanel.add(lbl("Registration No:"));
                JTextField rF = new JTextField(s.getRegistrationNumber()); rF.setEditable(false); rF.setFont(AppTheme.BODY_B);
                credPanel.add(rF);

                credPanel.add(lbl("Library Card:"));
                JTextField cF = new JTextField(s.getLibraryCardNumber() != null ? s.getLibraryCardNumber() : "-");
                cF.setEditable(false); cF.setFont(AppTheme.BODY_B);
                credPanel.add(cF);

                credPanel.add(lbl("Default Password:"));
                JTextField pF = new JTextField("changeme123"); pF.setEditable(false); pF.setFont(AppTheme.BODY_B);
                pF.setForeground(new Color(0xCC, 0x55, 0x00));
                credPanel.add(pF);

                JOptionPane.showMessageDialog(this, credPanel,
                        "Registration Successful â€” Share Credentials", JOptionPane.INFORMATION_MESSAGE);

                refresh(session);
            } catch (Exception ex) {
                AppTheme.error(this, ex.getMessage());
            }
        }
    }

    private JLabel lbl(String s) {
        var l = new JLabel(s);
        l.setFont(AppTheme.BODY_B);
        l.setForeground(AppTheme.fg());
        return l;
    }
}

