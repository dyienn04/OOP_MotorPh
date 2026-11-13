package gui;

import model.Role;
import service.AuthService;
import service.AuthenticatedUser;
import service.EmployeeService;
import service.LeaveService;
import service.PayrollService;
import service.UserAccountService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

public class DashboardPanel extends JFrame {

    private static final Color SIDEBAR_COLOR = new Color(255, 255, 255);
    private static final Color SIDEBAR_BORDER = new Color(235, 225, 230);
    private static final Color BRAND_COLOR = new Color(45, 45, 45);
    private static final Color MUTED_TEXT = new Color(108, 108, 108);
    private static final Color NAV_HOVER_COLOR = new Color(248, 241, 246);
    private static final Color NAV_ACTIVE_BG = new Color(36, 36, 36);
    private static final Color NAV_ACTIVE_FG = Color.WHITE;
    private static final Color NAV_DEFAULT_FG = new Color(55, 55, 55);
    private static final Color GRADIENT_START = new Color(255, 204, 229);
    private static final Color GRADIENT_END = new Color(255, 229, 180);

    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 22);
    private static final Font SUBTITLE_FONT = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font SECTION_FONT = new Font("Segoe UI", Font.BOLD, 12);
    private static final Font NAV_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font NAV_ACTIVE_FONT = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font PROFILE_NAME_FONT = new Font("Segoe UI", Font.BOLD, 16);
    private static final Font PROFILE_ROLE_FONT = new Font("Segoe UI", Font.PLAIN, 13);

    private static final String CARD_MY_PROFILE = "MyProfile";
    private static final String CARD_EMPLOYEE = "Employee";
    private static final String CARD_LEAVE_REQUEST = "LeaveRequest";
    private static final String CARD_LEAVE_APPROVALS = "LeaveApprovals";
    private static final String CARD_LEAVE_HISTORY = "LeaveHistory";
    private static final String CARD_USER_ACCOUNTS = "UserAccounts";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel = new GradientPanel();
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();

    private final AuthenticatedUser currentUser;
    private final AuthService authService;
    private final EmployeeService employeeService;
    private final UserAccountService userAccountService;
    private final LeaveService leaveService;
    private final PayrollService payrollService;

    private final JPanel myProfilePanel;
    private final JPanel employeePanel;
    private final LeaveRequestPanel leaveRequestPanel;
    private final LeaveApprovalPanel leaveApprovalPanel;
    private final EmployeeLeaveHistoryPanel leaveHistoryPanel;
    private final UserAccountsPanel userAccountsPanel;

    public DashboardPanel(
            AuthenticatedUser currentUser,
            AuthService authService,
            EmployeeService employeeService,
            UserAccountService userAccountService,
            LeaveService leaveService,
            PayrollService payrollService
    ) {
        this.currentUser = currentUser;
        this.authService = authService;
        this.employeeService = employeeService;
        this.userAccountService = userAccountService;
        this.leaveService = leaveService;
        this.payrollService = payrollService;

        this.myProfilePanel = new EmployeeProfilePanel(employeeService, payrollService, currentUser);
        this.employeePanel = new EmployeeManagementPanel(employeeService, payrollService, currentUser);
        this.leaveRequestPanel = new LeaveRequestPanel(leaveService, currentUser);
        this.leaveApprovalPanel = new LeaveApprovalPanel(leaveService, currentUser);
        this.leaveHistoryPanel = new EmployeeLeaveHistoryPanel(leaveService, currentUser);
        this.userAccountsPanel = new UserAccountsPanel(userAccountService, employeeService);

        setupFrame();
        buildUI();
    }

    private void setupFrame() {
        setTitle("MotorPH Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1120, 690);
        setMinimumSize(new Dimension(920, 560));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
    }

    private void buildUI() {
        Role role = getCurrentRole();

        JPanel sidebar = buildSidebar(role);
        configureContentPanel(role);

        add(sidebar, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);

        showDefaultPage();
    }

    private JPanel buildSidebar(Role role) {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(SIDEBAR_COLOR);
        sidebar.setPreferredSize(new Dimension(300, getHeight()));
        sidebar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, SIDEBAR_BORDER),
                new EmptyBorder(22, 18, 22, 18)
        ));

        JPanel topPanel = new JPanel();
        topPanel.setOpaque(false);
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.add(buildAppHeader());
        topPanel.add(Box.createVerticalStrut(20));
        topPanel.add(buildProfileCard());
        topPanel.add(Box.createVerticalStrut(18));
        topPanel.add(buildNavigationPanel(role));

        sidebar.add(topPanel, BorderLayout.NORTH);
        sidebar.add(buildLogoutButton(), BorderLayout.SOUTH);
        return sidebar;
    }

    private JPanel buildAppHeader() {
        JPanel headerPanel = new JPanel();
        headerPanel.setOpaque(false);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titleLabel = new JLabel("MotorPH HRIS");
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(BRAND_COLOR);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Employee self-service and administration");
        subtitleLabel.setFont(SUBTITLE_FONT);
        subtitleLabel.setForeground(MUTED_TEXT);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        headerPanel.add(titleLabel);
        headerPanel.add(Box.createVerticalStrut(4));
        headerPanel.add(subtitleLabel);
        return headerPanel;
    }

    private JPanel buildProfileCard() {
        JPanel profileCard = new JPanel(new BorderLayout(14, 0));
        profileCard.setBackground(new Color(252, 248, 250));
        profileCard.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(236, 227, 232), 1, true),
                new EmptyBorder(14, 14, 14, 14)
        ));
        profileCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        profileCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        JLabel profileIcon = new JLabel(loadImageIcon("/assets/userprofile.png", 42, 42));
        profileCard.add(profileIcon, BorderLayout.WEST);

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

        JLabel nameLabel = new JLabel(safe(currentUser == null ? null : currentUser.getFirstName(), "(Unknown User)"));
        nameLabel.setFont(PROFILE_NAME_FONT);
        nameLabel.setForeground(BRAND_COLOR);

        JLabel roleLabel = new JLabel(buildRoleLabel());
        roleLabel.setFont(PROFILE_ROLE_FONT);
        roleLabel.setForeground(MUTED_TEXT);

        JLabel helperLabel = new JLabel("Signed in");
        helperLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        helperLabel.setForeground(new Color(150, 150, 150));

        textPanel.add(nameLabel);
        textPanel.add(Box.createVerticalStrut(4));
        textPanel.add(roleLabel);
        textPanel.add(Box.createVerticalStrut(6));
        textPanel.add(helperLabel);

        profileCard.add(textPanel, BorderLayout.CENTER);
        return profileCard;
    }

    private JPanel buildNavigationPanel(Role role) {
        JPanel navPanel = new JPanel();
        navPanel.setOpaque(false);
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS));
        navPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        navPanel.add(createSectionLabel("WORKSPACE"));
        navPanel.add(Box.createVerticalStrut(8));
        navPanel.add(createNavigationButton("My Profile", "userprofile.png", CARD_MY_PROFILE));
        navPanel.add(Box.createVerticalStrut(6));
        navPanel.add(createNavigationButton("Leave Request", "attendance.png", CARD_LEAVE_REQUEST));
        navPanel.add(Box.createVerticalStrut(6));
        navPanel.add(createNavigationButton("My Leaves", "employee.png", CARD_LEAVE_HISTORY));

        boolean hasAdminItems = (role != null && role.canViewAllEmployees())
                || (role != null && role.canApproveLeaves())
                || (role != null && role.canManageUserAccounts());

        if (hasAdminItems) {
            navPanel.add(Box.createVerticalStrut(20));
            navPanel.add(createSectionLabel("ADMINISTRATION"));
            navPanel.add(Box.createVerticalStrut(8));

            if (role != null && role.canViewAllEmployees()) {
                navPanel.add(createNavigationButton("Employee Directory", "employee.png", CARD_EMPLOYEE));
                navPanel.add(Box.createVerticalStrut(6));
            }
            if (role != null && role.canApproveLeaves()) {
                navPanel.add(createNavigationButton("Leave Approvals", "attendance.png", CARD_LEAVE_APPROVALS));
                navPanel.add(Box.createVerticalStrut(6));
            }
            if (role != null && role.canManageUserAccounts()) {
                navPanel.add(createNavigationButton("User Accounts", "payroll.png", CARD_USER_ACCOUNTS));
                navPanel.add(Box.createVerticalStrut(6));
            }
        }

        return navPanel;
    }

    private JLabel createSectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(SECTION_FONT);
        label.setForeground(new Color(145, 130, 138));
        label.setBorder(new EmptyBorder(0, 10, 0, 0));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JButton buildLogoutButton() {
        JButton button = createBaseNavButton("Log-out", "logout.png");
        button.addActionListener(e -> logout());
        return button;
    }

    private void configureContentPanel(Role role) {
        contentPanel.setLayout(cardLayout);

        contentPanel.add(myProfilePanel, CARD_MY_PROFILE);
        contentPanel.add(leaveRequestPanel, CARD_LEAVE_REQUEST);
        contentPanel.add(leaveHistoryPanel, CARD_LEAVE_HISTORY);

        if (role != null && role.canViewAllEmployees()) {
            contentPanel.add(employeePanel, CARD_EMPLOYEE);
        }
        if (role != null && role.canApproveLeaves()) {
            contentPanel.add(leaveApprovalPanel, CARD_LEAVE_APPROVALS);
        }
        if (role != null && role.canManageUserAccounts()) {
            contentPanel.add(userAccountsPanel, CARD_USER_ACCOUNTS);
        }
    }

    private JButton createNavigationButton(String text, String iconFileName, String cardName) {
        JButton button = createBaseNavButton(text, iconFileName);
        navButtons.put(cardName, button);
        button.addActionListener(e -> showCard(cardName));
        return button;
    }

    private JButton createBaseNavButton(String text, String iconFileName) {
        JButton button = new JButton(text);
        button.setIcon(loadImageIcon("/assets/" + iconFileName, 18, 18));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setIconTextGap(12);
        button.setFont(NAV_FONT);
        button.setForeground(NAV_DEFAULT_FG);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        button.setPreferredSize(new Dimension(240, 44));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(11, 14, 11, 14));
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setBackground(SIDEBAR_COLOR);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (!isButtonSelected(button)) {
                    button.setBackground(NAV_HOVER_COLOR);
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (!isButtonSelected(button)) {
                    button.setBackground(SIDEBAR_COLOR);
                }
            }
        });

        return button;
    }

    private boolean isButtonSelected(JButton button) {
        return NAV_ACTIVE_BG.equals(button.getBackground()) && NAV_ACTIVE_FG.equals(button.getForeground());
    }

    private void showCard(String cardName) {
        cardLayout.show(contentPanel, cardName);
        updateSelectedNavigation(cardName);
    }

    private void updateSelectedNavigation(String selectedCard) {
        for (Map.Entry<String, JButton> entry : navButtons.entrySet()) {
            boolean selected = entry.getKey().equals(selectedCard);
            JButton button = entry.getValue();
            button.setBackground(selected ? NAV_ACTIVE_BG : SIDEBAR_COLOR);
            button.setForeground(selected ? NAV_ACTIVE_FG : NAV_DEFAULT_FG);
            button.setFont(selected ? NAV_ACTIVE_FONT : NAV_FONT);
        }
    }

    private void showDefaultPage() {
        showCard(CARD_MY_PROFILE);
    }

    private void logout() {
        dispose();
        SwingUtilities.invokeLater(() -> new LoginPanel(
                authService,
                employeeService,
                userAccountService,
                leaveService,
                payrollService
        ).setVisible(true));
    }

    private Role getCurrentRole() {
        return currentUser == null ? null : currentUser.getRole();
    }

    private String buildRoleLabel() {
        String position = safe(currentUser == null ? null : currentUser.getPosition(), "");
        Role role = getCurrentRole();
        String roleName = role == null ? "Employee" : role.name();
        return position.isBlank() ? roleName : position + " • " + roleName;
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private ImageIcon loadImageIcon(String path, int width, int height) {
        URL imageUrl = getClass().getResource(path);
        if (imageUrl == null) {
            System.err.println("Image not found: " + path);
            return null;
        }
        ImageIcon icon = new ImageIcon(imageUrl);
        Image scaled = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    private static class GradientPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setPaint(new GradientPaint(0, 0, GRADIENT_START, 0, getHeight(), GRADIENT_END));
            g2d.fillRect(0, 0, getWidth(), getHeight());
            g2d.dispose();
        }
    }
}
