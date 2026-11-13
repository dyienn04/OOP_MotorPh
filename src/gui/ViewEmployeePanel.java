package gui;

import service.AttendanceEntryDto;
import service.AuthenticatedUser;
import service.EmployeeDetailsDto;
import service.EmployeeService;
import service.PayrollResultDto;
import service.PayrollService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

public class ViewEmployeePanel extends JFrame {

    private static final Font UI_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font HEADER_FONT = new Font("Segoe UI", Font.BOLD, 16);

    private static final Color GRADIENT_START = new Color(255, 204, 229);
    private static final Color GRADIENT_END = new Color(255, 229, 180);

    private final String employeeId;
    private final EmployeeService employeeService;
    private final PayrollService payrollService;
    private final AuthenticatedUser currentUser;

    private final JComboBox<MonthYearItem> monthBox = new JComboBox<>();
    private final JTextPane reportPane = new JTextPane();

    private EmployeeDetailsDto employeeDetails;

    public ViewEmployeePanel(
            String employeeId,
            EmployeeService employeeService,
            PayrollService payrollService,
            AuthenticatedUser currentUser
    ) {
        this.employeeId = safe(employeeId);
        this.employeeService = Objects.requireNonNull(employeeService, "employeeService is required");
        this.payrollService = Objects.requireNonNull(payrollService, "payrollService is required");
        this.currentUser = currentUser;
        this.employeeDetails = employeeService.findDetailsById(this.employeeId);

        setupFrame();
        buildUI();
    }

    private void setupFrame() {
        setTitle("Employee Details");
        setSize(1100, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    private void buildUI() {
        if (employeeDetails == null) {
            add(buildMessagePanel("Employee record not found."), BorderLayout.CENTER);
            return;
        }

        if (!canViewTargetEmployee()) {
            add(buildMessagePanel("You do not have permission to view this employee record."), BorderLayout.CENTER);
            return;
        }

        JPanel leftPanel = buildLeftDetailsPanel();
        JScrollPane leftScroll = new JScrollPane(leftPanel);
        leftScroll.setBorder(null);
        leftScroll.getVerticalScrollBar().setUI(createScrollBarUI());

        JPanel rightPanel = buildRightPayrollPanel();

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftScroll, rightPanel);
        splitPane.setResizeWeight(0.45);
        splitPane.setDividerLocation(0.45);
        splitPane.setOneTouchExpandable(true);

        add(splitPane, BorderLayout.CENTER);
    }

    private JPanel buildMessagePanel(String message) {
        JPanel panel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setPaint(new GradientPaint(0, 0, GRADIENT_START, 0, getHeight(), GRADIENT_END));
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        panel.setBorder(new EmptyBorder(40, 40, 40, 40));

        JLabel label = new JLabel(message);
        label.setFont(new Font("Segoe UI", Font.BOLD, 16));
        panel.add(label);
        return panel;
    }

    private JPanel buildLeftDetailsPanel() {
        JPanel leftPanel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setPaint(new GradientPaint(0, 0, GRADIENT_START, 0, getHeight(), GRADIENT_END));
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };

        leftPanel.setOpaque(false);
        leftPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.gridy = 0;

        List<String[]> sections = new ArrayList<>();
        sections.add(new String[]{"Personal Information", "Employee No.", "Last Name", "First Name", "Birthday", "Address", "Phone Number"});
        sections.add(new String[]{"Government Identifications", "SSS No.", "PhilHealth No.", "TIN No.", "PAG-IBIG No."});
        sections.add(new String[]{"Job Information", "Status", "Position", "Immediate Supervisor"});

        if (canViewCompensationDetails()) {
            sections.add(new String[]{"Compensation & Benefits", "Basic Salary", "Rice Subsidy", "Phone Allowance", "Clothing Allowance", "Gross Semi-monthly Rate", "Hourly Rate"});
        }

        for (String[] section : sections) {
            JLabel sectionTitle = new JLabel(section[0]);
            sectionTitle.setFont(HEADER_FONT);
            sectionTitle.setForeground(new Color(70, 70, 70));

            gbc.gridx = 0;
            gbc.gridwidth = 2;
            leftPanel.add(sectionTitle, gbc);
            gbc.gridy++;
            gbc.gridwidth = 1;

            for (int i = 1; i < section.length; i++) {
                gbc.gridx = 0;
                JLabel label = new JLabel(section[i] + ":");
                label.setFont(UI_FONT);
                leftPanel.add(label, gbc);

                gbc.gridx = 1;

                JTextArea dataField = new JTextArea(getFieldValue(section[i]));
                dataField.setWrapStyleWord(true);
                dataField.setLineWrap(true);
                dataField.setEditable(false);
                dataField.setOpaque(false);
                dataField.setFont(UI_FONT);
                dataField.setBorder(null);

                leftPanel.add(dataField, gbc);
                gbc.gridy++;
            }

            gbc.gridy++;
        }

        return leftPanel;
    }

    private JPanel buildRightPayrollPanel() {
        JPanel rightPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setPaint(new GradientPaint(0, 0, GRADIENT_START, 0, getHeight(), GRADIENT_END));
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };

        rightPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        reportPane.setContentType("text/html");
        reportPane.setEditable(false);
        reportPane.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        reportPane.setBackground(UIManager.getColor("Panel.background"));

        if (!canViewPayrollForTarget()) {
            reportPane.setText(buildPermissionRestrictedPayrollHtml());
            rightPanel.add(new JScrollPane(reportPane), BorderLayout.CENTER);
            return rightPanel;
        }

        configureMonthBox();
        loadAvailableMonths();

        JButton computeButton = new JButton("Compute");
        computeButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        computeButton.setBackground(Color.BLACK);
        computeButton.setForeground(Color.WHITE);
        computeButton.setFocusPainted(false);
        computeButton.setBorder(BorderFactory.createEmptyBorder());
        computeButton.setPreferredSize(new Dimension(130, 40));
        computeButton.addActionListener(e -> computeSelectedMonth());

        reportPane.setText(buildEmptyReportHtml());

        JScrollPane reportScroll = new JScrollPane(reportPane);
        reportScroll.setBorder(null);
        reportScroll.getVerticalScrollBar().setUI(createScrollBarUI());

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setOpaque(false);
        topPanel.add(monthBox);
        topPanel.add(Box.createHorizontalStrut(10));
        topPanel.add(computeButton);

        rightPanel.add(topPanel, BorderLayout.NORTH);
        rightPanel.add(reportScroll, BorderLayout.CENTER);

        return rightPanel;
    }

    private boolean canViewTargetEmployee() {
        return currentUser != null
                && currentUser.getRole() != null
                && currentUser.getRole().canViewEmployeeRecord(employeeId, safe(currentUser.getEmployeeNumber()));
    }

    private boolean canViewCompensationDetails() {
        return currentUser != null
                && currentUser.getRole() != null
                && (currentUser.getRole().canEditPayrollInfo() || isSelfView());
    }

    private boolean canViewPayrollForTarget() {
        return currentUser != null
                && currentUser.getRole() != null
                && currentUser.getRole().canViewPayrollFor(employeeId, safe(currentUser.getEmployeeNumber()));
    }

    private boolean isSelfView() {
        return safe(employeeId).equals(safe(currentUser == null ? null : currentUser.getEmployeeNumber()));
    }

    private void configureMonthBox() {
        monthBox.setFont(UI_FONT);

        monthBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus
            ) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                c.setFont(UI_FONT);

                if (value instanceof MonthYearItem item && item.isPlaceholder()) {
                    c.setForeground(Color.GRAY);
                    setText("Select Month");
                } else {
                    c.setForeground(Color.BLACK);
                }

                return c;
            }
        });
    }

    private void loadAvailableMonths() {
        monthBox.removeAllItems();
        monthBox.addItem(MonthYearItem.placeholder());

        List<YearMonth> availableMonths = employeeService.findAvailablePayrollMonths(employeeId);

        SortedSet<MonthYearItem> items = new TreeSet<>();
        for (YearMonth ym : availableMonths) {
            if (ym != null) {
                items.add(new MonthYearItem(ym.getMonth(), ym.getYear()));
            }
        }

        for (MonthYearItem item : items) {
            monthBox.addItem(item);
        }
    }

    private void computeSelectedMonth() {
        Object selected = monthBox.getSelectedItem();

        if (!(selected instanceof MonthYearItem item) || item.isPlaceholder()) {
            JOptionPane.showMessageDialog(this, "Please select a valid month.");
            return;
        }

        try {
            PayrollResultDto result = payrollService.computeForMonthDto(employeeId, item.year(), item.month().getValue());
            reportPane.setText(buildPayrollHtml(result, item));
            reportPane.setCaretPosition(0);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Calculation error: " + ex.getMessage(),
                    "Payroll Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private String buildPayrollHtml(PayrollResultDto result, MonthYearItem item) {
        String monthName = item.month().getDisplayName(TextStyle.FULL, Locale.ENGLISH);

        double sss = safeDouble(result.getSss());
        double philHealth = safeDouble(result.getPhilHealth());
        double pagIbig = safeDouble(result.getPagIbig());
        double tax = safeDouble(result.getWithholdingTax());
        double totalDeductions = sss + philHealth + pagIbig + tax;

        return String.format("""
                <html>
                <body style='font-family:Segoe UI; font-size:12px; padding:8px;'>
                    <h2 style='margin-bottom:4px;'>Payroll Report</h2>
                    <div style='margin-bottom:12px; color:#555;'>%s %d</div>

                    <table style='border-collapse:collapse; width:100%%; margin-bottom:16px;'>
                        <tr><td><b>Employee #</b></td><td>%s</td></tr>
                        <tr><td><b>Attendance Days Processed</b></td><td>%d</td></tr>
                        <tr><td><b>Total Hours Worked</b></td><td>%s</td></tr>
                        <tr><td><b>Overtime Hours</b></td><td>%s</td></tr>
                        <tr><td><b>Late Minutes</b></td><td>%d</td></tr>
                    </table>

                    <h3 style='margin:10px 0 6px 0;'>Payroll Breakdown</h3>
                    <table style='border-collapse:collapse; width:100%%; margin-bottom:16px;'>
                        <tr><td><b>Basic Pay</b></td><td>₱%,.2f</td></tr>
                        <tr><td><b>Allowances</b></td><td>₱%,.2f</td></tr>
                        <tr><td><b>Overtime Pay</b></td><td>₱%,.2f</td></tr>
                        <tr><td><b>Late Deduction</b></td><td>-₱%,.2f</td></tr>
                        <tr><td><b>Gross Pay</b></td><td>₱%,.2f</td></tr>
                        <tr><td><b>SSS</b></td><td>₱%,.2f</td></tr>
                        <tr><td><b>PhilHealth</b></td><td>₱%,.2f</td></tr>
                        <tr><td><b>Pag-IBIG</b></td><td>₱%,.2f</td></tr>
                        <tr><td><b>Withholding Tax</b></td><td>₱%,.2f</td></tr>
                        <tr><td><b>Total Deductions</b></td><td>₱%,.2f</td></tr>
                        <tr><td><b>Net Pay</b></td><td><span style='color:green; font-weight:bold;'>₱%,.2f</span></td></tr>
                    </table>

                    <h3 style='margin:10px 0 6px 0;'>Attendance Basis</h3>
                    <div style='margin-bottom:8px; color:#555;'>Payroll is computed from the recorded time-in and time-out entries below.</div>
                    %s
                </body>
                </html>
                """,
                monthName,
                item.year(),
                escapeHtml(result.getEmployeeId()),
                result.getAttendanceDaysProcessed(),
                escapeHtml(result.getHoursWorked()),
                escapeHtml(result.getOvertimeHours()),
                result.getLateMinutes(),
                safeDouble(result.getBasicPay()),
                safeDouble(result.getAllowances()),
                safeDouble(result.getOvertimePay()),
                safeDouble(result.getLateDeduction()),
                safeDouble(result.getGrossPay()),
                sss,
                philHealth,
                pagIbig,
                tax,
                totalDeductions,
                safeDouble(result.getNetPay()),
                buildAttendanceTableHtml(result.getAttendanceEntries())
        );
    }

    private String buildAttendanceTableHtml(List<AttendanceEntryDto> entries) {
        if (entries == null || entries.isEmpty()) {
            return "<div style='color:#666;'>No attendance entries were found for this payroll period.</div>";
        }

        StringBuilder html = new StringBuilder();
        html.append("<table style='border-collapse:collapse; width:100%;'>")
                .append("<tr style='background:#f0f0f0;'>")
                .append("<th style='border:1px solid #ccc; padding:6px;'>Date</th>")
                .append("<th style='border:1px solid #ccc; padding:6px;'>Time In</th>")
                .append("<th style='border:1px solid #ccc; padding:6px;'>Time Out</th>")
                .append("<th style='border:1px solid #ccc; padding:6px;'>Worked Hours</th>")
                .append("<th style='border:1px solid #ccc; padding:6px;'>Late (min)</th>")
                .append("<th style='border:1px solid #ccc; padding:6px;'>OT (min)</th>")
                .append("</tr>");

        for (AttendanceEntryDto entry : entries) {
            html.append("<tr>")
                    .append(td(entry.getDate()))
                    .append(td(entry.getTimeIn()))
                    .append(td(entry.getTimeOut()))
                    .append(td(entry.getWorkedHours()))
                    .append(td(String.valueOf(entry.getLateMinutes())))
                    .append(td(String.valueOf(entry.getOvertimeMinutes())))
                    .append("</tr>");
        }

        html.append("</table>");
        return html.toString();
    }

    private String td(String value) {
        return "<td style='border:1px solid #ccc; padding:6px;'>" + escapeHtml(value) + "</td>";
    }

    private String buildEmptyReportHtml() {
        return """
                <html>
                <body style='font-family:Segoe UI; font-size:12px; padding:10px; color:#666;'>
                    Select a payroll month, then click <b>Compute</b> to view the attendance-backed payroll report.
                </body>
                </html>
                """;
    }

    private String buildPermissionRestrictedPayrollHtml() {
        return """
                <html>
                <body style='font-family:Segoe UI; font-size:12px; padding:10px; color:#666;'>
                    Payroll details are restricted for your role.
                </body>
                </html>
                """;
    }

    private String getFieldValue(String fieldLabel) {
        if (employeeDetails == null) {
            return "";
        }

        return switch (fieldLabel) {
            case "Employee No." -> safe(employeeDetails.getEmployeeId());
            case "Last Name" -> safe(employeeDetails.getLastName());
            case "First Name" -> safe(employeeDetails.getFirstName());
            case "Birthday" -> safe(employeeDetails.getBirthday());
            case "Address" -> safe(employeeDetails.getAddress());
            case "Phone Number" -> safe(employeeDetails.getPhoneNumber());
            case "SSS No." -> safe(employeeDetails.getSssNumber());
            case "PhilHealth No." -> safe(employeeDetails.getPhilHealthNumber());
            case "TIN No." -> safe(employeeDetails.getTinNumber());
            case "PAG-IBIG No." -> safe(employeeDetails.getPagIbigNumber());
            case "Status" -> safe(employeeDetails.getStatus());
            case "Position" -> safe(employeeDetails.getPosition());
            case "Immediate Supervisor" -> safe(employeeDetails.getImmediateSupervisor());
            case "Basic Salary" -> safe(employeeDetails.getBasicSalary());
            case "Rice Subsidy" -> safe(employeeDetails.getRiceSubsidy());
            case "Phone Allowance" -> safe(employeeDetails.getPhoneAllowance());
            case "Clothing Allowance" -> safe(employeeDetails.getClothingAllowance());
            case "Gross Semi-monthly Rate" -> safe(employeeDetails.getGrossSemiMonthlyRate());
            case "Hourly Rate" -> safe(employeeDetails.getHourlyRate());
            default -> "";
        };
    }

    private static BasicScrollBarUI createScrollBarUI() {
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

    private String safe(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    private double safeDouble(Object value) {
        if (value == null) {
            return 0.0;
        }

        try {
            return Double.parseDouble(value.toString());
        } catch (Exception ex) {
            return 0.0;
        }
    }

    private String escapeHtml(String value) {
        String text = safe(value);
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private record MonthYearItem(Month month, int year) implements Comparable<MonthYearItem> {

        static MonthYearItem placeholder() {
            return new MonthYearItem(null, 0);
        }

        boolean isPlaceholder() {
            return month == null || year == 0;
        }

        @Override
        public String toString() {
            if (isPlaceholder()) {
                return "Select Month";
            }
            return month.getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + year;
        }

        @Override
        public int compareTo(MonthYearItem other) {
            if (this.isPlaceholder()) {
                return -1;
            }
            if (other.isPlaceholder()) {
                return 1;
            }

            int yearCompare = Integer.compare(this.year, other.year);
            if (yearCompare != 0) {
                return yearCompare;
            }

            return Integer.compare(this.month.getValue(), other.month.getValue());
        }
    }
}
