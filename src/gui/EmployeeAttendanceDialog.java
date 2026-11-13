package gui;

import service.AttendanceEntryDto;
import service.EmployeeService;
import service.PayrollResultDto;
import service.PayrollService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

public class EmployeeAttendanceDialog extends JDialog {

    private static final Color GRADIENT_START = new Color(255, 204, 229);
    private static final Color GRADIENT_END = new Color(255, 229, 180);
    private static final Font UI_FONT = new Font("Segoe UI", Font.PLAIN, 14);

    private final String employeeId;
    private final EmployeeService employeeService;
    private final PayrollService payrollService;

    private final JComboBox<MonthYearItem> monthBox = new JComboBox<>();
    private final JButton showButton = new JButton("Show Attendance");
    private final JLabel summaryLabel = new JLabel("Select a month to view your attendance record.");
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Date", "Time In", "Time Out", "Worked Hours", "Late (min)", "OT (min)"},
            0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    public EmployeeAttendanceDialog(
            Frame owner,
            String employeeId,
            EmployeeService employeeService,
            PayrollService payrollService
    ) {
        super(owner, "My Attendance Record", true);
        this.employeeId = safe(employeeId);
        this.employeeService = Objects.requireNonNull(employeeService, "employeeService is required");
        this.payrollService = Objects.requireNonNull(payrollService, "payrollService is required");

        setSize(900, 580);
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

        JLabel titleLabel = new JLabel("Attendance Record");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));

        JLabel subtitleLabel = new JLabel("Recorded time-in and time-out entries by month.");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitleLabel.setForeground(new Color(90, 90, 90));

        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.add(titleLabel);
        titlePanel.add(Box.createVerticalStrut(4));
        titlePanel.add(subtitleLabel);

        configureMonthBox();
        stylePrimaryButton(showButton, 165, 38);
        showButton.addActionListener(e -> loadSelectedAttendance());

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filterPanel.setOpaque(false);
        filterPanel.add(monthBox);
        filterPanel.add(showButton);

        summaryLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        summaryLabel.setForeground(new Color(70, 70, 70));

        JPanel northPanel = new JPanel(new BorderLayout(0, 12));
        northPanel.setOpaque(false);
        northPanel.add(titlePanel, BorderLayout.NORTH);
        northPanel.add(filterPanel, BorderLayout.CENTER);
        northPanel.add(summaryLabel, BorderLayout.SOUTH);

        JTable table = new JTable(tableModel);
        table.setRowHeight(28);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getVerticalScrollBar().setUI(createScrollBarUI());

        content.add(northPanel, BorderLayout.NORTH);
        content.add(scrollPane, BorderLayout.CENTER);
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
            summaryLabel.setText("No attendance records are available yet.");
            return;
        }

        for (MonthYearItem item : items) {
            monthBox.addItem(item);
        }

        monthBox.setSelectedIndex(monthBox.getItemCount() - 1);
        loadSelectedAttendance();
    }

    private void loadSelectedAttendance() {
        clearTable();

        Object selectedItem = monthBox.getSelectedItem();
        if (!(selectedItem instanceof MonthYearItem item)) {
            summaryLabel.setText("Please select a valid month.");
            return;
        }

        try {
            PayrollResultDto result = payrollService.computeForMonthDto(employeeId, item.year(), item.month().getValue());
            populateTable(result.getAttendanceEntries());
            summaryLabel.setText(buildSummaryText(item, result));
        } catch (Exception ex) {
            summaryLabel.setText("Unable to load attendance: " + ex.getMessage());
        }
    }

    private void populateTable(List<AttendanceEntryDto> entries) {
        if (entries == null) {
            return;
        }

        for (AttendanceEntryDto entry : entries) {
            tableModel.addRow(new Object[]{
                    entry.getDate(),
                    entry.getTimeIn(),
                    entry.getTimeOut(),
                    entry.getWorkedHours(),
                    entry.getLateMinutes(),
                    entry.getOvertimeMinutes()
            });
        }
    }

    private void clearTable() {
        tableModel.setRowCount(0);
    }

    private String buildSummaryText(MonthYearItem item, PayrollResultDto result) {
        int totalLateMinutes = 0;
        int totalOvertimeMinutes = 0;

        for (AttendanceEntryDto entry : result.getAttendanceEntries()) {
            totalLateMinutes += Math.max(entry.getLateMinutes(), 0);
            totalOvertimeMinutes += Math.max(entry.getOvertimeMinutes(), 0);
        }

        return String.format(
                "%s %d  •  Days Recorded: %d  •  Total Worked Hours: %s  •  Total Late: %d min  •  Total OT: %s hr",
                item.month().getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                item.year(),
                result.getAttendanceDaysProcessed(),
                safe(result.getHoursWorked()),
                totalLateMinutes,
                formatHoursFromMinutes(totalOvertimeMinutes)
        );
    }

    private String formatHoursFromMinutes(int minutes) {
        BigDecimal hours = BigDecimal.valueOf(Math.max(minutes, 0))
                .divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP);
        return hours.toPlainString();
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

    private String safe(String value) {
        return value == null ? "" : value.trim();
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
