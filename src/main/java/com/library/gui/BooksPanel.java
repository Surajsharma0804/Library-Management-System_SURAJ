package com.library.gui;

import com.library.enums.UserRole;
import com.library.facade.LibraryFacade;
import com.library.model.Book;
import com.library.model.Reservation;
import com.library.model.Student;
import com.library.security.Session;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

/**
 * Executive Book Catalogue Management Panel — Instant search filtering,
 * cover image thumbnails, Dewey Decimal prefix filter, status filter,
 * rounded status pill badges, and structured modal dialogs.
 *
 * @author University Central Library — Software Engineering Division
 * @version 2.0.0
 */
public final class BooksPanel extends JPanel {

    private final LibraryFacade facade;
    private JTable table;
    private DefaultTableModel model;
    private JTextField searchField;
    private JTextField deweyField;
    private JComboBox<String> statusCombo;
    private JButton addBtn;
    private JButton editBtn;
    private JButton removeBtn;
    private JButton borrowBtn;
    private JButton reserveBtn;
    private Session session;

    // Columns: Cover, ID, Title, Author, ISBN, Category, Available, Total, Status
    private static final String[] COLS = {
        "Cover", "ID", "Title", "Author", "ISBN", "Category", "Available", "Total", "Status"
    };
    private static final int COL_COVER  = 0;
    private static final int COL_STATUS = 8;

    public BooksPanel(LibraryFacade facade) {
        this.facade = facade;
        setBackground(AppTheme.bg());
        setLayout(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        build();
    }

    private void build() {
        removeAll();

        JPanel hdr = new JPanel(new BorderLayout(16, 0));
        hdr.setOpaque(false);

        JPanel title = new JPanel();
        title.setOpaque(false);
        title.setLayout(new BoxLayout(title, BoxLayout.Y_AXIS));
        title.add(AppTheme.heading("Book Catalogue"));
        title.add(Box.createVerticalStrut(4));
        title.add(AppTheme.label2("Browse and manage the repository collection"));

        // Filter bar: text search | Dewey prefix | Status | Add button
        searchField = AppTheme.textField(18);
        searchField.putClientProperty("JTextField.placeholderText", "Search title, author, ISBN, category…");
        searchField.setPreferredSize(new Dimension(250, 38));
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { filter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { filter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filter(); }
        });

        deweyField = AppTheme.textField(10);
        deweyField.putClientProperty("JTextField.placeholderText", "Dewey prefix…");
        deweyField.setPreferredSize(new Dimension(120, 38));
        deweyField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { filter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { filter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filter(); }
        });

        statusCombo = new JComboBox<>(new String[]{"All", "Available", "Borrowed", "Reserved"});
        statusCombo.setPreferredSize(new Dimension(120, 38));
        statusCombo.setFont(AppTheme.BODY);
        statusCombo.addActionListener(e -> filter());

        addBtn = AppTheme.primaryBtn("+ Add Book");
        addBtn.setPreferredSize(new Dimension(120, 38));
        addBtn.addActionListener(e -> addBook());

        editBtn = AppTheme.secondaryBtn("Edit Book");
        editBtn.setPreferredSize(new Dimension(110, 38));
        editBtn.addActionListener(e -> editBook());

        removeBtn = AppTheme.dangerBtn("Remove");
        removeBtn.setPreferredSize(new Dimension(100, 38));
        removeBtn.addActionListener(e -> removeBook());

        borrowBtn = AppTheme.primaryBtn("Borrow");
        borrowBtn.setPreferredSize(new Dimension(100, 38));
        borrowBtn.addActionListener(e -> borrowBook());

        reserveBtn = AppTheme.secondaryBtn("Reserve");
        reserveBtn.setPreferredSize(new Dimension(100, 38));
        reserveBtn.addActionListener(e -> reserveBook());

        // Two-row layout: title on top, actions below
        JPanel hdr2 = new JPanel(new BorderLayout(0, 10));
        hdr2.setOpaque(false);
        hdr2.add(title, BorderLayout.NORTH);

        JPanel acts = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        acts.setOpaque(false);
        acts.add(searchField);
        acts.add(deweyField);
        acts.add(statusCombo);
        acts.add(borrowBtn);
        acts.add(reserveBtn);
        acts.add(editBtn);
        acts.add(removeBtn);
        acts.add(addBtn);
        hdr2.add(acts, BorderLayout.SOUTH);

        hdr = hdr2;

        model = new DefaultTableModel(COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) {
                return c == COL_COVER ? ImageIcon.class : Object.class;
            }
        };

        table = new JTable(model) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        AppTheme.styleTable(table);

        // Cover image column renderer
        table.getColumnModel().getColumn(COL_COVER).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable tbl, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel lbl = new JLabel();
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                lbl.setOpaque(true);
                lbl.setBackground(isSelected
                        ? new Color(AppTheme.ACCENT.getRed(), AppTheme.ACCENT.getGreen(),
                                    AppTheme.ACCENT.getBlue(), 40)
                        : (row % 2 == 0 ? AppTheme.bgCard() : AppTheme.tableAlt()));
                if (value instanceof ImageIcon icon) {
                    lbl.setIcon(icon);
                } else {
                    // Grey placeholder
                    lbl.setIcon(greyPlaceholder());
                }
                return lbl;
            }
        });
        table.getColumnModel().getColumn(COL_COVER).setPreferredWidth(48);
        table.getColumnModel().getColumn(COL_COVER).setMaxWidth(52);
        table.setRowHeight(58);

        // Status column pill renderer
        table.getColumnModel().getColumn(COL_STATUS).setCellRenderer((tbl, val, isSelected, hasFocus, row, col) -> {
            String statusStr = val != null ? val.toString() : "UNKNOWN";
            JPanel pill = AppTheme.createStatusPill(statusStr);
            if (isSelected) {
                pill.setOpaque(true);
                pill.setBackground(tbl.getSelectionBackground());
            } else {
                pill.setOpaque(false);
            }
            return pill;
        });

        // Right-click context menu — built dynamically in refresh() based on role
        table.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { showPopup(e); }
            @Override public void mouseReleased(MouseEvent e) { showPopup(e); }
            private void showPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        table.setRowSelectionInterval(row, row);
                        JPopupMenu popup = new JPopupMenu();
                        boolean isStudent = session != null && session.role() == UserRole.STUDENT;
                        if (isStudent) {
                            JMenuItem borrowItem = new JMenuItem("Borrow This Book");
                            borrowItem.addActionListener(ev -> borrowBook());
                            JMenuItem reserveItem = new JMenuItem("Reserve This Book");
                            reserveItem.addActionListener(ev -> reserveBook());
                            popup.add(borrowItem);
                            popup.add(reserveItem);
                        } else {
                            JMenuItem editItem = new JMenuItem("Edit Book");
                            editItem.addActionListener(ev -> editBook());
                            JMenuItem removeItem = new JMenuItem("Remove Book");
                            removeItem.addActionListener(ev -> removeBook());
                            popup.add(editItem);
                            popup.addSeparator();
                            popup.add(removeItem);
                        }
                        popup.show(table, e.getX(), e.getY());
                    }
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

        boolean isStudent = session != null && session.role() == UserRole.STUDENT;
        // Students: show Borrow/Reserve, hide Add/Edit/Remove
        addBtn.setVisible(!isStudent);
        editBtn.setVisible(!isStudent);
        removeBtn.setVisible(!isStudent);
        borrowBtn.setVisible(isStudent);
        reserveBtn.setVisible(isStudent);

        load(facade.bookRepo().findAll());
    }

    private void load(List<Book> books) {
        model.setRowCount(0);
        for (Book b : books) {
            ImageIcon cover = loadCover(b.getCoverImagePath());
            model.addRow(new Object[]{
                    cover,
                    b.getId(),
                    b.getTitle(),
                    b.getAuthor(),
                    b.getIsbn(),
                    b.getCategory() != null ? b.getCategory() : "-",
                    b.getAvailableQuantity(),
                    b.getTotalQuantity(),
                    b.getStatus().name()
            });
        }
    }

    private ImageIcon loadCover(String path) {
        if (path != null && !path.isBlank()) {
            try {
                File f = new File(path);
                if (f.exists() && f.isFile()) {
                    BufferedImage raw = ImageIO.read(f);
                    if (raw != null) {
                        Image scaled = raw.getScaledInstance(40, 55, Image.SCALE_SMOOTH);
                        return new ImageIcon(scaled);
                    }
                }
            } catch (Exception ignored) {}
        }
        return null; // renderer shows grey placeholder for null
    }

    private static ImageIcon greyPlaceholderIcon;
    private static boolean placeholderDarkMode;

    private ImageIcon greyPlaceholder() {
        if (greyPlaceholderIcon == null || placeholderDarkMode != AppTheme.isDark()) {
            placeholderDarkMode = AppTheme.isDark();
            BufferedImage img = new BufferedImage(40, 55, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img.createGraphics();
            g2.setColor(AppTheme.border());
            g2.fillRect(0, 0, 40, 55);
            g2.setColor(AppTheme.fgMuted());
            g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 9));
            g2.drawString("No", 13, 25);
            g2.drawString("Cover", 7, 36);
            g2.dispose();
            greyPlaceholderIcon = new ImageIcon(img);
        }
        return greyPlaceholderIcon;
    }

    private void filter() {
        String q      = searchField.getText().trim().toLowerCase();
        String dewey  = deweyField.getText().trim();
        String status = (String) statusCombo.getSelectedItem();
        boolean allStatus = "All".equals(status);

        List<Book> all = facade.bookRepo().findAll();
        load(all.stream().filter(b -> {
            // Text search
            boolean textOk = q.isEmpty()
                    || b.getTitle().toLowerCase().contains(q)
                    || b.getAuthor().toLowerCase().contains(q)
                    || (b.getIsbn() != null && b.getIsbn().toLowerCase().contains(q))
                    || (b.getCategory() != null && b.getCategory().toLowerCase().contains(q));

            // Dewey prefix filter
            boolean deweyOk = dewey.isEmpty()
                    || (b.getDeweyDecimal() != null && b.getDeweyDecimal().startsWith(dewey));

            // Status filter
            boolean statusOk = allStatus || b.getStatus().name().equalsIgnoreCase(status);

            return textOk && deweyOk && statusOk;
        }).toList());
    }

    private void addBook() {
        if (session == null) return;

        JPanel f = new JPanel(new GridLayout(0, 2, 12, 12));
        f.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JTextField ti = AppTheme.textField(15);
        JTextField au = AppTheme.textField(15);
        JTextField is = AppTheme.textField(15);
        JTextField pu = AppTheme.textField(15);
        JTextField ca = AppTheme.textField(15);
        JTextField qt = AppTheme.textField(15);
        qt.setText("1");

        f.add(lbl("Title:"));          f.add(ti);
        f.add(lbl("Author:"));         f.add(au);
        f.add(lbl("ISBN:"));           f.add(is);
        f.add(lbl("Publisher:"));      f.add(pu);
        f.add(lbl("Category:"));       f.add(ca);
        f.add(lbl("Total Quantity:")); f.add(qt);

        if (JOptionPane.showConfirmDialog(this, f, "Add New Book Record",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                int quantity;
                try {
                    quantity = Integer.parseInt(qt.getText().trim());
                    if (quantity <= 0) throw new NumberFormatException("non-positive");
                } catch (NumberFormatException nfe) {
                    AppTheme.error(this, "Total Quantity must be a positive whole number.");
                    return;
                }
                Book b = facade.factory().createBook(is.getText().trim(), ti.getText().trim(),
                        au.getText().trim(), quantity);
                b.setPublisher(pu.getText().trim());
                b.setCategory(ca.getText().trim());
                facade.bookRepo().save(b);
                refresh(session);
                AppTheme.success(this, "Book successfully added to catalogue!\nBook ID: " + b.getId());
            } catch (Exception ex) {
                AppTheme.error(this, ex.getMessage());
            }
        }
    }

    private void editBook() {
        if (session == null) return;
        int row = table.getSelectedRow();
        if (row < 0) { AppTheme.error(this, "Please select a book first."); return; }

        String bookId = (String) model.getValueAt(row, 1);
        Book book = facade.books().findById(bookId);
        if (book == null) { AppTheme.error(this, "Book not found."); return; }

        JPanel f = new JPanel(new GridLayout(0, 2, 12, 12));
        f.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JTextField ti = AppTheme.textField(15); ti.setText(book.getTitle());
        JTextField au = AppTheme.textField(15); au.setText(book.getAuthor());
        JTextField is = AppTheme.textField(15); is.setText(book.getIsbn() != null ? book.getIsbn() : "");
        JTextField pu = AppTheme.textField(15); pu.setText(book.getPublisher() != null ? book.getPublisher() : "");
        JTextField ca = AppTheme.textField(15); ca.setText(book.getCategory() != null ? book.getCategory() : "");

        f.add(lbl("Title:"));     f.add(ti);
        f.add(lbl("Author:"));    f.add(au);
        f.add(lbl("ISBN:"));      f.add(is);
        f.add(lbl("Publisher:")); f.add(pu);
        f.add(lbl("Category:")); f.add(ca);

        if (JOptionPane.showConfirmDialog(this, f, "Edit Book — " + bookId,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                book.setTitle(ti.getText().trim());
                book.setAuthor(au.getText().trim());
                book.setIsbn(is.getText().trim());
                book.setPublisher(pu.getText().trim());
                book.setCategory(ca.getText().trim());
                facade.books().updateBook(session, book);
                refresh(session);
                AppTheme.success(this, "Book updated successfully.");
            } catch (Exception ex) {
                AppTheme.error(this, ex.getMessage());
            }
        }
    }

    private void removeBook() {
        if (session == null) return;
        int row = table.getSelectedRow();
        if (row < 0) { AppTheme.error(this, "Please select a book first."); return; }

        String bookId = (String) model.getValueAt(row, 1);
        String title = (String) model.getValueAt(row, 2);

        if (JOptionPane.showConfirmDialog(this,
                "Remove book \"" + title + "\" (" + bookId + ")?\nThis action cannot be undone.",
                "Confirm Remove", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
        try {
            facade.books().deleteBook(session, bookId);
            refresh(session);
            AppTheme.success(this, "Book removed from catalogue.");
        } catch (Exception ex) {
            AppTheme.error(this, ex.getMessage());
        }
    }

    private void borrowBook() {
        if (session == null) return;
        int row = table.getSelectedRow();
        if (row < 0) { AppTheme.error(this, "Please select a book to borrow."); return; }

        String bookId = (String) model.getValueAt(row, 1);
        String bookTitle = (String) model.getValueAt(row, 2);
        Student student = facade.userRepo().findStudentByUsername(session.username());
        if (student == null) { AppTheme.error(this, "Student profile not found."); return; }

        if (JOptionPane.showConfirmDialog(this,
                "Borrow \"" + bookTitle + "\"?",
                "Confirm Borrow", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.YES_OPTION) return;
        try {
            facade.borrows().issueBook(session, bookId, student.getRegistrationNumber());
            refresh(session);
            AppTheme.success(this, "Book \"" + bookTitle + "\" borrowed successfully!");
        } catch (Exception ex) {
            AppTheme.error(this, ex.getMessage());
        }
    }

    private void reserveBook() {
        if (session == null) return;
        int row = table.getSelectedRow();
        if (row < 0) { AppTheme.error(this, "Please select a book to reserve."); return; }

        String bookId = (String) model.getValueAt(row, 1);
        String bookTitle = (String) model.getValueAt(row, 2);
        Student student = facade.userRepo().findStudentByUsername(session.username());
        if (student == null) { AppTheme.error(this, "Student profile not found."); return; }

        if (JOptionPane.showConfirmDialog(this,
                "Reserve \"" + bookTitle + "\"?",
                "Confirm Reserve", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.YES_OPTION) return;
        try {
            Reservation r = facade.reservations().reserve(session, bookId, student.getRegistrationNumber());
            refresh(session);
            AppTheme.success(this, "Reserved! Queue position: #" + r.getQueuePosition());
        } catch (Exception ex) {
            AppTheme.error(this, ex.getMessage());
        }
    }

    private JLabel lbl(String t) {
        var l = new JLabel(t);
        l.setFont(AppTheme.BODY_B);
        l.setForeground(AppTheme.fg());
        return l;
    }
}
