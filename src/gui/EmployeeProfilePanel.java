package gui;

import service.AuthenticatedUser;
import service.EmployeeDetailsDto;
import service.EmployeeService;
import service.PayrollService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EmployeeProfilePanel extends JPanel {

    private static final Color GRADIENT_START = new Color(255, 204, 229);
    private static final Color GRADIENT_END = new Color(255, 229, 180);
    private static final Color CARD_BACKGROUND = new Color(255, 255, 255, 235);
    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 24);
    private static final Font SUBTITLE_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font SECTION_FONT = new Font("Segoe UI", Font.BOLD, 18);
    private static final Font LABEL_FONT = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font VALUE_FONT = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Color SECTION_COLOR = new Color(36, 36, 36);
    private static final Color LABEL_COLOR = new Color(62, 62, 62);
    private static final Color VALUE_COLOR = new Color(28, 28, 28);

    private final EmployeeService employeeService;
    private final PayrollService payrollService;
    private final AuthenticatedUser currentUser;
    private final String employeeId;

    private EmployeeDetailsDto employeeDetails;

    public EmployeeProfilePanel(
            EmployeeService employeeService,
            PayrollService payrollService,
            AuthenticatedUser currentUser
    ) {
        this.employeeService = Objects.requireNonNull(employeeService, "employeeService is required");
        this.payrollService = Objects.requireNonNull(payrollService, "payrollService is required");
        this.currentUser = currentUser;
        this.employeeId = safe(currentUser == null ? null : currentUser.getEmployeeNumber());
        this.employeeDetails = employeeService.findDetailsById(employeeId);

        setLayout(new BorderLayout());
        setOpaque(false);

        buildUI();
    }

    private void buildUI() {
        removeAll();

        if (employeeId.isBlank()) {
            add(buildMessagePanel("Unable to load your profile because no employee number is linked to this account."), BorderLayout.CENTER);
            return;
        }

        if (employeeDetails == null) {
            add(buildMessagePanel("Employee profile not found."), BorderLayout.CENTER);
            return;
        }

        JPanel content = new JPanel(new BorderLayout(0, 20)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setPaint(new GradientPaint(0, 0, GRADIENT_START, 0, getHeight(), GRADIENT_END));
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.dispose();
            }
        };
        content.setBorder(new EmptyBorder(24, 32, 24, 32));

        content.add(buildHeaderPanel(), BorderLayout.NORTH);
        content.add(buildScrollableDetailsPanel(), BorderLayout.CENTER);
        content.add(buildActionPanel(), BorderLayout.SOUTH);

        add(content, BorderLayout.CENTER);
    }

    private JPanel buildMessagePanel(String message) {
        JPanel panel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setPaint(new GradientPaint(0, 0, GRADIENT_START, 0, getHeight(), GRADIENT_END));
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.dispose();
            }
        };
        panel.setBorder(new EmptyBorder(40, 40, 40, 40));

        JLabel label = new JLabel(message);
        label.setFont(new Font("Segoe UI", Font.BOLD, 16));
        panel.add(label);
        return panel;
    }

    private JPanel buildHeaderPanel() {
        JPanel headerPanel = new JPanel();
        headerPanel.setOpaque(false);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("My Profile");
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitleLabel = new JLabel(buildSubtitle());
        subtitleLabel.setFont(SUBTITLE_FONT);
        subtitleLabel.setForeground(new Color(80, 80, 80));
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        headerPanel.add(titleLabel);
        headerPanel.add(Box.createVerticalStrut(6));
        headerPanel.add(subtitleLabel);

        return headerPanel;
    }

    private JScrollPane buildScrollableDetailsPanel() {
        JPanel detailsPanel = new JPanel();
        detailsPanel.setOpaque(false);
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
        detailsPanel.setBorder(new EmptyBorder(4, 0, 4, 0));

        detailsPanel.add(buildSectionPanel(
                "Personal Information",
                List.of(
                        new DetailItem("Employee No.", safe(employeeDetails.getEmployeeId())),
                        new DetailItem("Last Name", safe(employeeDetails.getLastName())),
                        new DetailItem("First Name", safe(employeeDetails.getFirstName())),
                        new DetailItem("Birthday", safe(employeeDetails.getBirthday())),
                        new DetailItem("Address", safe(employeeDetails.getAddress())),
                        new DetailItem("Phone Number", safe(employeeDetails.getPhoneNumber()))
                )
        ));
        detailsPanel.add(Box.createVerticalStrut(16));

        detailsPanel.add(buildSectionPanel(
                "Government Identifications",
                List.of(
                        new DetailItem("SSS No.", safe(employeeDetails.getSssNumber())),
                        new DetailItem("PhilHealth No.", safe(employeeDetails.getPhilHealthNumber())),
                        new DetailItem("TIN No.", safe(employeeDetails.getTinNumber())),
                        new DetailItem("PAG-IBIG No.", safe(employeeDetails.getPagIbigNumber()))
                )
        ));
        detailsPanel.add(Box.createVerticalStrut(16));

        detailsPanel.add(buildSectionPanel(
                "Job Information",
                List.of(
                        new DetailItem("Status", safe(employeeDetails.getStatus())),
                        new DetailItem("Position", safe(employeeDetails.getPosition())),
                        new DetailItem("Immediate Supervisor", safe(employeeDetails.getImmediateSupervisor()))
                )
        ));
        detailsPanel.add(Box.createVerticalStrut(16));

        detailsPanel.add(buildSectionPanel(
                "Compensation & Benefits",
                List.of(
                        new DetailItem("Basic Salary", formatMoney(employeeDetails.getBasicSalary())),
                        new DetailItem("Rice Subsidy", formatMoney(employeeDetails.getRiceSubsidy())),
                        new DetailItem("Phone Allowance", formatMoney(employeeDetails.getPhoneAllowance())),
                        new DetailItem("Clothing Allowance", formatMoney(employeeDetails.getClothingAllowance())),
                        new DetailItem("Gross Semi-monthly Rate", formatMoney(employeeDetails.getGrossSemiMonthlyRate())),
                        new DetailItem("Hourly Rate", formatMoney(employeeDetails.getHourlyRate()))
                )
        ));

        JScrollPane scrollPane = new JScrollPane(detailsPanel);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getVerticalScrollBar().setUI(createScrollBarUI());
        return scrollPane;
    }

    private JPanel buildSectionPanel(String title, List<DetailItem> items) {
        JPanel sectionPanel = new JPanel(new BorderLayout(0, 16));
        sectionPanel.setBackground(CARD_BACKGROUND);
        sectionPanel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(255, 255, 255, 185), 1, true),
                new EmptyBorder(22, 24, 22, 24)
        ));
        sectionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        sectionPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JPanel headerPanel = new JPanel();
        headerPanel.setOpaque(false);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

        JLabel sectionLabel = new JLabel(title);
        sectionLabel.setFont(SECTION_FONT);
        sectionLabel.setForeground(SECTION_COLOR);
        sectionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JSeparator separator = new JSeparator();
        separator.setForeground(new Color(230, 230, 230));
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        separator.setAlignmentX(Component.LEFT_ALIGNMENT);

        headerPanel.add(sectionLabel);
        headerPanel.add(Box.createVerticalStrut(10));
        headerPanel.add(separator);
        sectionPanel.add(headerPanel, BorderLayout.NORTH);

        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.insets = new Insets(6, 0, 10, 20);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        for (DetailItem item : items) {
            gbc.gridx = 0;
            gbc.weightx = 0.28;

            JLabel label = new JLabel(item.label() + ":");
            label.setFont(LABEL_FONT);
            label.setForeground(LABEL_COLOR);
            fieldsPanel.add(label, gbc);

            gbc.gridx = 1;
            gbc.weightx = 0.72;
            gbc.insets = new Insets(6, 0, 10, 0);

            JLabel valueLabel = new JLabel(toHtml(item.value()));
            valueLabel.setFont(VALUE_FONT);
            valueLabel.setForeground(VALUE_COLOR);
            valueLabel.setVerticalAlignment(SwingConstants.TOP);
            fieldsPanel.add(valueLabel, gbc);

            gbc.gridy++;
            gbc.insets = new Insets(6, 0, 10, 20);
        }

        sectionPanel.add(fieldsPanel, BorderLayout.CENTER);
        return sectionPanel;
    }

    private JPanel buildActionPanel() {
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        actionPanel.setOpaque(false);

        JButton payrollButton = new JButton("Show Payroll");
        stylePrimaryButton(payrollButton, 150, 40);
        payrollButton.addActionListener(e -> openPayrollDialog());

        JButton attendanceButton = new JButton("Show Attendance Record");
        styleSecondaryButton(attendanceButton, 210, 40);
        attendanceButton.addActionListener(e -> openAttendanceDialog());

        actionPanel.add(payrollButton);
        actionPanel.add(attendanceButton);
        return actionPanel;
    }

    private void openPayrollDialog() {
        Window window = SwingUtilities.getWindowAncestor(this);
        Frame owner = window instanceof Frame frame ? frame : null;
        new EmployeePayrollDialog(owner, employeeId, employeeService, payrollService).setVisible(true);
    }

    private void openAttendanceDialog() {
        Window window = SwingUtilities.getWindowAncestor(this);
        Frame owner = window instanceof Frame frame ? frame : null;
        new EmployeeAttendanceDialog(owner, employeeId, employeeService, payrollService).setVisible(true);
    }

    private void stylePrimaryButton(JButton button, int width, int height) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBackground(Color.BLACK);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setPreferredSize(new Dimension(width, height));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void styleSecondaryButton(JButton button, int width, int height) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBackground(new Color(70, 70, 70));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setPreferredSize(new Dimension(width, height));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private String toHtml(String value) {
        String safeValue = safe(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br>");
        if (safeValue.isBlank()) {
            safeValue = "-";
        }
        return "<html><div style='width: 520px; line-height: 1.45; padding-top: 1px;'>" + safeValue + "</div></html>";
    }

    private String buildSubtitle() {
        List<String> values = new ArrayList<>();
        if (!safe(employeeDetails.getPosition()).isBlank()) {
            values.add(employeeDetails.getPosition().trim());
        }
        if (!safe(employeeDetails.getStatus()).isBlank()) {
            values.add(employeeDetails.getStatus().trim());
        }
        if (!safe(employeeDetails.getEmployeeId()).isBlank()) {
            values.add("Employee #" + employeeDetails.getEmployeeId().trim());
        }
        return values.isEmpty() ? "Your employee information" : String.join("  •  ", values);
    }

    private String formatMoney(String value) {
        String text = safe(value);
        if (text.isBlank() || "0".equals(text) || "0.00".equals(text)) {
            return text.isBlank() ? "-" : "₱" + text;
        }

        try {
            return "₱" + String.format("%,.2f", Double.parseDouble(text));
        } catch (NumberFormatException ex) {
            return text;
        }
    }

    private BasicScrollBarUI createScrollBarUI() {
        return new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                thumbColor = Color.WHITE;
                trackColor = GRADIENT_END;
            }

            @Override
            protected Dimension getMinimumThumbSize() {
                return new Dimension(8, 30);
            }
        };
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private record DetailItem(String label, String value) {
    }
}
