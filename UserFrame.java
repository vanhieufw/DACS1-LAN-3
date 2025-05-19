package com.movie.ui;

import com.movie.bus.MovieBUS;
import com.movie.bus.RoomBUS;
import com.movie.bus.TicketBUS;
import com.movie.model.BookingHistory;
import com.movie.model.Movie;
import com.movie.model.Room;
import com.movie.network.ThreadManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class UserFrame extends JFrame {
    private final int customerID;
    private final MovieBUS movieBUS = new MovieBUS();
    private final RoomBUS roomBUS = new RoomBUS();
    private final TicketBUS ticketBUS = new TicketBUS();
    private JPanel contentPanel;
    private CardLayout cardLayout;
    private JLabel timeLabel;
    private final AtomicBoolean running = new AtomicBoolean(true); // Control clock thread
    private JPanel movieListPanel; // For refresh functionality

    public UserFrame(int customerID) {
        this.customerID = customerID;
        initUI();
        startClock();
    }

    private void initUI() {
        setTitle("Hệ thống bán vé xem phim - Người dùng");
        setSize(1000, 700);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        // Stop clock thread when closing
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                running.set(false);
            }
        });

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Sidebar
        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(200, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(40, 40, 40));

        JButton moviesButton = new JButton("Danh sách phim");
        JButton historyButton = new JButton("Lịch sử đặt vé");
        JButton logoutButton = new JButton("Đăng xuất");

        styleButton(moviesButton);
        styleButton(historyButton);
        styleButton(logoutButton);

        moviesButton.addActionListener(e -> showPanel("Movies"));
        historyButton.addActionListener(e -> showPanel("History"));
        logoutButton.addActionListener(e -> {
            running.set(false);
            dispose();
            SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
        });

        sidebar.add(Box.createVerticalStrut(30));
        sidebar.add(moviesButton);
        sidebar.add(Box.createVerticalStrut(15));
        sidebar.add(historyButton);
        sidebar.add(Box.createVerticalStrut(15));
        sidebar.add(logoutButton);

        // Content area
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.add(createMoviesPanel(), "Movies");
        contentPanel.add(createHistoryPanel(), "History");

        mainPanel.add(sidebar, BorderLayout.WEST);
        mainPanel.add(contentPanel, BorderLayout.CENTER);

        add(mainPanel);
        setVisible(true);
    }

    private void startClock() {
        timeLabel = new JLabel();
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        timeLabel.setForeground(Color.BLACK);
        JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        timePanel.add(timeLabel);
        add(timePanel, BorderLayout.NORTH);

        new Thread(() -> {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            while (running.get()) {
                timeLabel.setText(sdf.format(new java.util.Date()));
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Clock thread interrupted: " + e.getMessage());
                }
            }
        }).start();
    }

    private void styleButton(JButton button) {
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        button.setBackground(new Color(60, 60, 60));
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(80, 80, 80), 1),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent evt) {
                button.setBackground(new Color(80, 80, 80));
            }

            @Override
            public void mouseExited(MouseEvent evt) {
                button.setBackground(new Color(60, 60, 60));
            }
        });
    }

    private void showPanel(String panelName) {
        cardLayout.show(contentPanel, panelName);
        if (panelName.equals("Movies")) {
            loadMovies(movieListPanel); // Refresh movie list when switching to Movies panel
        }
    }

    private JPanel createMoviesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(245, 245, 245));
        JLabel titleLabel = new JLabel("Danh sách phim", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        panel.add(titleLabel, BorderLayout.NORTH);

        movieListPanel = new JPanel();
        movieListPanel.setLayout(new BoxLayout(movieListPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(movieListPanel);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Add refresh button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refreshButton = new JButton("Làm mới");
        refreshButton.addActionListener(e -> loadMovies(movieListPanel));
        styleButton(refreshButton);
        buttonPanel.add(refreshButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        loadMovies(movieListPanel);

        return panel;
    }

    private void loadMovies(JPanel movieListPanel) {
        new SwingWorker<List<Movie>, Void>() {
            @Override
            protected List<Movie> doInBackground() throws SQLException {
                return movieBUS.getAllMovies();
            }

            @Override
            protected void done() {
                try {
                    List<Movie> movies = get();
                    movieListPanel.removeAll();
                    if (movies.isEmpty()) {
                        JLabel noMoviesLabel = new JLabel("Hiện không có phim nào.", SwingConstants.CENTER);
                        noMoviesLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
                        movieListPanel.add(noMoviesLabel);
                    } else {
                        for (Movie movie : movies) {
                            JPanel moviePanel = createMoviePanel(movie);
                            movieListPanel.add(moviePanel);
                        }
                    }
                    movieListPanel.revalidate();
                    movieListPanel.repaint();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(UserFrame.this, "Không thể tải danh sách phim: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private JPanel createMoviePanel(Movie movie) {
        JPanel moviePanel = new JPanel(new BorderLayout());
        moviePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        moviePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        JLabel posterLabel = new JLabel();
        if (movie.getPoster() != null && !movie.getPoster().isEmpty()) {
            try {
                ImageIcon icon = new ImageIcon(movie.getPoster());
                if (icon.getIconWidth() > 0) { // Validate image
                    posterLabel.setIcon(new ImageIcon(icon.getImage().getScaledInstance(200, 140, Image.SCALE_SMOOTH)));
                }
            } catch (Exception e) {
                System.err.println("Error loading poster for " + movie.getTitle() + ": " + e.getMessage());
            }
        }
        posterLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        moviePanel.add(posterLabel, BorderLayout.WEST);

        JPanel infoPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        JLabel titleLabel = new JLabel(movie.getTitle());
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        infoPanel.add(titleLabel, gbc);

        JTextArea descriptionArea = new JTextArea(movie.getDescription());
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setEditable(false);
        descriptionArea.setBackground(infoPanel.getBackground());
        descriptionArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        descriptionArea.setPreferredSize(new Dimension(600, 60));
        gbc.gridy = 1;
        infoPanel.add(descriptionArea, gbc);

        JLabel roomLabel = new JLabel("Phòng chiếu: Đang tải...");
        gbc.gridy = 2;
        infoPanel.add(roomLabel, gbc);

        JLabel priceLabel = new JLabel("Giá vé: Đang tải...");
        gbc.gridy = 3;
        infoPanel.add(priceLabel, gbc);

        JButton bookButton = new JButton("Đặt vé");
        styleButton(bookButton);
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        infoPanel.add(bookButton, gbc);

        moviePanel.add(infoPanel, BorderLayout.CENTER);

        // Load room and price info asynchronously
        ThreadManager.execute(() -> {
            try {
                List<Room> rooms = roomBUS.getAllRooms();
                Room movieRoom = rooms.stream()
                        .filter(r -> r.getMovieTitle() != null && r.getMovieTitle().equals(movie.getTitle()))
                        .findFirst()
                        .orElse(null);
                SwingUtilities.invokeLater(() -> {
                    if (movieRoom != null) {
                        roomLabel.setText("Phòng chiếu: " + movieRoom.getRoomName());
                        priceLabel.setText("Giá vé: " + String.format("%,.0f VND", movieRoom.getPrice()));
                        bookButton.setEnabled(movieRoom.getStatus().equals("Đang chiếu") || movieRoom.getStatus().equals("Chuẩn bị chiếu"));
                    } else {
                        roomLabel.setText("Phòng chiếu: Không có");
                        priceLabel.setText("Giá vé: Không có");
                        bookButton.setEnabled(false);
                    }
                });
            } catch (SQLException ex) {
                SwingUtilities.invokeLater(() -> {
                    roomLabel.setText("Phòng chiếu: Lỗi tải dữ liệu");
                    priceLabel.setText("Giá vé: Lỗi tải dữ liệu");
                    bookButton.setEnabled(false);
                });
            }
        });

        bookButton.addActionListener(e -> {
            try {
                Room movieRoom = roomBUS.getAllRooms().stream()
                        .filter(r -> r.getMovieTitle() != null && r.getMovieTitle().equals(movie.getTitle()))
                        .findFirst()
                        .orElse(null);
                if (movieRoom != null) {
                    new BookingFrame(customerID, movieRoom.getRoomID(), movie.getMovieID()).setVisible(true);
                } else {
                    JOptionPane.showMessageDialog(this, "Phim này hiện không có phòng chiếu.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Không thể mở giao diện đặt vé: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        return moviePanel;
    }

    private JPanel createHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(245, 245, 245));
        JLabel titleLabel = new JLabel("Lịch sử đặt vé", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        panel.add(titleLabel, BorderLayout.NORTH);

        JPanel historyPanel = new JPanel();
        historyPanel.setLayout(new BoxLayout(historyPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(historyPanel);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Add sorting and refresh options
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JComboBox<String> sortCombo = new JComboBox<>(new String[]{"Mới nhất", "Cũ nhất", "Giá cao", "Giá thấp"});
        JButton refreshButton = new JButton("Làm mới");
        styleButton(refreshButton);
        controlPanel.add(new JLabel("Sắp xếp:"));
        controlPanel.add(sortCombo);
        controlPanel.add(refreshButton);
        panel.add(controlPanel, BorderLayout.SOUTH);

        loadBookingHistory(historyPanel, sortCombo.getSelectedItem().toString());

        sortCombo.addActionListener(e -> loadBookingHistory(historyPanel, sortCombo.getSelectedItem().toString()));
        refreshButton.addActionListener(e -> loadBookingHistory(historyPanel, sortCombo.getSelectedItem().toString()));

        return panel;
    }

    private void loadBookingHistory(JPanel historyPanel, String sortOption) {
        new SwingWorker<List<BookingHistory>, Void>() {
            @Override
            protected List<BookingHistory> doInBackground() throws SQLException {
                List<BookingHistory> historyList = ticketBUS.getBookingHistory(customerID);
                // Sort based on selection
                switch (sortOption) {
                    case "Mới nhất":
                        historyList.sort((a, b) -> b.getBookingDate().compareTo(a.getBookingDate()));
                        break;
                    case "Cũ nhất":
                        historyList.sort((a, b) -> a.getBookingDate().compareTo(b.getBookingDate()));
                        break;
                    case "Giá cao":
                        historyList.sort((a, b) -> Double.compare(b.getPrice(), a.getPrice()));
                        break;
                    case "Giá thấp":
                        historyList.sort((a, b) -> Double.compare(a.getPrice(), b.getPrice()));
                        break;
                }
                return historyList;
            }

            @Override
            protected void done() {
                try {
                    List<BookingHistory> historyList = get();
                    historyPanel.removeAll();
                    if (historyList.isEmpty()) {
                        JLabel noHistoryLabel = new JLabel("Bạn chưa có lịch sử đặt vé.", SwingConstants.CENTER);
                        noHistoryLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
                        historyPanel.add(noHistoryLabel);
                    } else {
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                        for (BookingHistory history : historyList) {
                            JPanel historyItem = new JPanel(new BorderLayout());
                            historyItem.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                            historyItem.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

                            String bookingInfo = String.format("Phim: %s - Phòng: %s - Ghế: %s - Giá: %,d VND - Ngày đặt: %s",
                                    history.getMovieTitle(), history.getRoomName(), history.getSeatNumber(),
                                    (int) history.getPrice(), sdf.format(history.getBookingDate()));
                            JLabel infoLabel = new JLabel(bookingInfo);
                            infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                            historyItem.add(infoLabel, BorderLayout.CENTER);

                            historyPanel.add(historyItem);
                        }
                    }
                    historyPanel.revalidate();
                    historyPanel.repaint();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(UserFrame.this, "Không thể tải lịch sử đặt vé: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
}