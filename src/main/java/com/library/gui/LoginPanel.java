package com.library.gui;

import com.library.facade.LibraryFacade;
import com.library.security.Session;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Executive Authentication Screen — Clean enterprise layout, role switcher,
 * password toggle, and clear inline validation banners.
 *
 * @author University Central Library — Software Engineering Division
 * @version 2.0.0
 */
public final class LoginPanel extends JPanel {

    private final LibraryFacade facade;
    private final Runnable onSuccess;
    private JTextField userField;
    private JPasswordField passField;
    private JComboBox<String> roleBox;
    private JPanel errPanel;
    private JLabel errLabel;
    private JToggleButton showPassBtn;
    private String token;

    public LoginPanel(LibraryFacade facade, Runnable onSuccess) {
        this.facade = facade;
        this.onSuccess = onSuccess;
        setLayout(new GridBagLayout());
        setOpaque(true);
        build();
    }

    /* ── UI construction ─────────────────────────────────────────── */

    private void build() {
        removeAll();
        setBackground(AppTheme.bg());

        // card container
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                AppTheme.aa(g);
                var g2 = (Graphics2D) g;
                int w = getWidth(), h = getHeight();
                g2.setColor(AppTheme.bgCard());
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, AppTheme.CARD_R + 4, AppTheme.CARD_R + 4));
                g2.setColor(AppTheme.border());
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(.5f, .5f, w - 1, h - 1, AppTheme.CARD_R + 4, AppTheme.CARD_R + 4));
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(420, 580));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(28, 36, 28, 36));

        // top row: theme toggle button
        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        topRow.setOpaque(false);
        topRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        topRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        topRow.add(AppTheme.themeBtn());

        // branding logo — load the actual app icon from resources
        JLabel logo = new JLabel();
        logo.setPreferredSize(new Dimension(56, 56));
        logo.setMaximumSize(new Dimension(56, 56));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        try {
            var iconUrl = getClass().getResource("/images/app-logo.png");
            if (iconUrl != null) {
                ImageIcon raw = new ImageIcon(iconUrl);
                Image scaled = raw.getImage().getScaledInstance(56, 56, Image.SCALE_SMOOTH);
                logo.setIcon(new ImageIcon(scaled));
            }
        } catch (Exception ignored) {}

        JLabel title = lbl("University Central Library", AppTheme.H1, AppTheme.fg());
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel sub = lbl("Enterprise Library Portal", AppTheme.SMALL, AppTheme.fgSecondary());
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        // role selector
        JLabel roleLbl = fieldLabel("Sign in as");
        roleBox = AppTheme.comboBox("Administrator", "Librarian", "Student");
        roleBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        roleBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        // username input
        JLabel userLbl = fieldLabel("Username");
        userField = AppTheme.textField(20);
        userField.putClientProperty("JTextField.placeholderText", "Enter your username");
        userField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        userField.setAlignmentX(Component.LEFT_ALIGNMENT);

        // password input with show/hide toggle
        JLabel passLbl = fieldLabel("Password");
        passField = AppTheme.passwordField(20);
        passField.putClientProperty("JTextField.placeholderText", "Enter your password");
        passField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        passField.setAlignmentX(Component.LEFT_ALIGNMENT);

        // error banner panel
        errPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        errPanel.setBackground(AppTheme.isDark() ? new Color(0x99, 0x1B, 0x1B, 40) : new Color(0xFE, 0xE2, 0xE2));
        errPanel.setBorder(BorderFactory.createLineBorder(AppTheme.isDark() ? new Color(0x99, 0x1B, 0x1B) : new Color(0xFC, 0xA5, 0xA5)));
        errPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        errPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        errPanel.setVisible(false);

        errLabel = new JLabel("");
        errLabel.setFont(AppTheme.SMALL);
        errLabel.setForeground(AppTheme.isDark() ? new Color(0xF8, 0x71, 0x71) : new Color(0x99, 0x1B, 0x1B));
        errPanel.add(errLabel);

        // sign-in button
        JButton signIn = AppTheme.primaryBtn("Sign In");
        signIn.setPreferredSize(new Dimension(348, 42));
        signIn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        signIn.setAlignmentX(Component.LEFT_ALIGNMENT);
        signIn.addActionListener(this::login);
        passField.addActionListener(this::login);
        userField.addActionListener(e -> passField.requestFocusInWindow());

        // version footer
        JLabel ver = lbl("Version 2.0.0 • Enterprise Edition", AppTheme.SMALL, AppTheme.fgMuted());
        ver.setAlignmentX(Component.CENTER_ALIGNMENT);

        // info text for students
        JLabel infoText = lbl("Contact your administrator for login credentials", AppTheme.SMALL, AppTheme.fgMuted());
        infoText.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(topRow);
        card.add(logo);
        card.add(Box.createVerticalStrut(10));
        card.add(title);
        card.add(Box.createVerticalStrut(4));
        card.add(sub);
        card.add(Box.createVerticalStrut(22));
        card.add(roleLbl); card.add(Box.createVerticalStrut(6)); card.add(roleBox);
        card.add(Box.createVerticalStrut(12));
        card.add(userLbl); card.add(Box.createVerticalStrut(6)); card.add(userField);
        card.add(Box.createVerticalStrut(12));
        card.add(passLbl); card.add(Box.createVerticalStrut(6)); card.add(passField);
        card.add(Box.createVerticalStrut(8));
        card.add(errPanel);
        card.add(Box.createVerticalStrut(16));
        card.add(signIn);
        card.add(Box.createVerticalStrut(10));
        card.add(infoText);
        card.add(Box.createVerticalStrut(10));
        card.add(ver);

        // Use explicit GridBagConstraints to center the card
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        add(card, gbc);
        revalidate(); repaint();
    }

    /* ── Helpers ──────────────────────────────────────────────────── */

    private JLabel fieldLabel(String t) {
        JLabel l = new JLabel(t);
        l.setFont(AppTheme.SMALL_B); l.setForeground(AppTheme.fgSecondary());
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JLabel lbl(String t, Font f, Color c) {
        var l = new JLabel(t); l.setFont(f); l.setForeground(c); return l;
    }

    private void login(ActionEvent e) {
        String user = userField.getText().trim();
        String pass = new String(passField.getPassword());
        if (user.isEmpty() || pass.isEmpty()) {
            showError("Please enter both username and password.");
            return;
        }
        try {
            token = facade.auth().login(user, pass);

            // Validate selected role matches the authenticated session role
            String selectedRole = (String) roleBox.getSelectedItem();
            Session session = facade.sessions().require(token);
            String actualRole = session.role().name();
            String expectedRole = switch (selectedRole) {
                case "Administrator" -> "ADMIN";
                case "Librarian"     -> "LIBRARIAN";
                case "Student"       -> "STUDENT";
                default              -> "";
            };
            if (!expectedRole.isEmpty() && !actualRole.equals(expectedRole)) {
                facade.auth().logout(token);
                token = null;
                showError("This account is registered as " + actualRole + ", not " + selectedRole + ".");
                return;
            }

            errPanel.setVisible(false);
            onSuccess.run();
        } catch (Exception ex) {
            showError("Invalid credentials. Please verify username and password.");
            passField.setText(""); passField.requestFocusInWindow();
        }
    }

    private void showError(String msg) {
        errLabel.setText(msg);
        errPanel.setVisible(true);
        revalidate(); repaint();
    }


    /* ── Public API ──────────────────────────────────────────────── */

    public String  getToken()    { return token; }
    public Session getSession()  { return token == null ? null : facade.sessions().require(token); }
    public String  getRole()     { return (String) roleBox.getSelectedItem(); }

    public void reset() {
        userField.setText(""); passField.setText(""); errPanel.setVisible(false);
        userField.requestFocusInWindow(); token = null;
    }

    public void applyTheme() {
        String saved = userField != null ? userField.getText() : "";
        build();
        userField.setText(saved);
    }
}

