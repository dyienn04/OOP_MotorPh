package gui;

import service.AttendanceEntryDto;
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
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

public class EmployeePayrollDialog extends JDialog {

    private static final Color GRADIENT_START = new Color(255, 204, 229);
    private static final Color GRADIENT_END = new Color(255, 229, 180);
    private static final Font UI_FONT = new Font("Segoe UI", Font.PLAIN, 14);

    private final String employeeId;
    private final EmployeeService employeeService;
    private final PayrollService payrollService;

    private final JComboBox<MonthYearItem> monthBox = new JComboBox<>();
    private final JButton showButton = new JButton("Show Payroll");
    private final JTextPane reportPane = new JTextPane();

    public EmployeePayrollDialog(
            Frame owner,
            String employeeId,
            EmployeeService employeeService,
            PayrollService payrollService
    ) {
        super(owner, "My Payroll", true);
        this.employeeId = safe(employeeId);
        this.employeeService = Objects.requireNonNull(employeeService, "employeeService is required");
        this.payrollService = Objects.requireNonNull(payrollService, "payrollService is required");

        setSize(920, 620);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        buildUI();
        loadAvailableMonths();
    }

    private void buildUI() {
        JPanel content = new JPanel(new BorderLayout(0, 16)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setPaint(new GradientPaint(0, 0, GRADIENT_START, 0, getHeight(), GRADIENT_END));
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.dispose();
            }
        };
        content.setBorder(new EmptyBorder(20, 20, 20, 20));
        setContentPane(content);

        JLabel titleLabel = new JLabel("Payroll Details");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));

        JLabel subtitleLabel = new JLabel("Select a month to view the attendance-backed payroll computation.");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitleLabel.setForeground(new Color(90, 90, 90));

        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.add(titleLabel);
        titlePanel.add(Box.createVerticalStrut(4));
        titlePanel.add(subtitleLabel);

        configureMonthBox();
        stylePrimaryButton(showButton, 150, 38);
        showButton.addActionListener(e -> loadSelectedPayroll());

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filterPanel.setOpaque(false);
        filterPanel.add(monthBox);
        filterPanel.add(showButton);

        JPanel northPanel = new JPanel(new BorderLayout(0, 12));
        northPanel.setOpaque(false);
        northPanel.add(titlePanel, BorderLayout.NORTH);
        northPanel.add(filterPanel, BorderLayout.CENTER);

        reportPane.setContentType("text/html");
        reportPane.setEditable(false);
        reportPane.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        reportPane.setText(buildInfoHtml("Choose a payroll month to display the report."));

        JScrollPane reportScroll = new JScrollPane(reportPane);
        reportScroll.setBorder(null);
        reportScroll.getVerticalScrollBar().setUnitIncrement(16);
        reportScroll.getVerticalScrollBar().setUI(createScrollBarUI());

        content.add(northPanel, BorderLayout.NORTH);
        content.add(reportScroll, BorderLayout.CENTER);
    }

    private void configureMonthBox() {
        monthBox.setFont(UI_FONT);
        monthBox.setPreferredSize(new Dimension(220, 36));
        monthBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus
            ) {
                Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                component.setFont(UI_FONT);
                return component;
            }
        });
    }

    private void loadAvailableMonths() {
        monthBox.removeAllItems();

        List<YearMonth> availableMonths = employeeService.findAvailablePayrollMonths(employeeId);
        SortedSet<MonthYearItem> items = new TreeSet<>();
        for (YearMonth month : availableMonths) {
            if (month != null) {
                items.add(new MonthYearItem(month.getMonth(), month.getYear()));
            }
        }

        if (items.isEmpty()) {
            showButton.setEnabled(false);
            reportPane.setText(buildInfoHtml("No payroll periods are available because there are no attendance records yet."));
            return;
        }

        for (MonthYearItem item : items) {
            monthBox.addItem(item);
        }

        monthBox.setSelectedIndex(monthBox.getItemCount() - 1);
        loadSelectedPayroll();
    }

    private void loadSelectedPayroll() {
        Object selectedItem = monthBox.getSelectedItem();
        if (!(selectedItem instanceof MonthYearItem item)) {
            reportPane.setText(buildInfoHtml("Please select a valid month."));
            return;
        }

        try {
            PayrollResultDto result = payrollService.computeForMonthDto(employeeId, item.year(), item.month().getValue());
            reportPane.setText(buildPayrollHtml(result, item));
            reportPane.setCaretPosition(0);
        } catch (Exception ex) {
            reportPane.setText(buildErrorHtml(ex.getMessage()));
            reportPane.setCaretPosition(0);
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

    private String buildInfoHtml(String message) {
        return """
                <html>
                <body style='font-family:Segoe UI; font-size:12px; padding:10px; color:#666;'>
                """ + escapeHtml(message) + """
                </body>
                </html>
                """;
    }

    private String buildErrorHtml(String message) {
        return """
                <html>
                <body style='font-family:Segoe UI; font-size:12px; padding:10px; color:#a80000;'>
                    Unable to load payroll details.<br><br>
                    """ + escapeHtml(message) + """
                </body>
                </html>
                """;
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

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String escapeHtml(String value) {
        return safe(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private record MonthYearItem(Month month, int year) implements Comparable<MonthYearItem> {

        @Override
        public String toString() {
            return month.getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + year;
        }

        @Override
        public int compareTo(MonthYearItem other) {
            int yearCompare = Integer.compare(this.year, other.year);
            if (yearCompare != 0) {
                return yearCompare;
            }
            return Integer.compare(this.month.getValue(), other.month.getValue());
        }
    }
}
