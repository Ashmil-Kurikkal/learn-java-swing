import java.awt.*;
import java.sql.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;

public class BookInventoryApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}

// --- Database Utility ---
class DBUtil {
    private static final String URL = "jdbc:mysql://localhost:3306/book_inventory";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL Driver not found", e);
        }
    }
}

// --- Model classes ---
class Book {
    private int id;
    private String name, author, genre;
    private double price;
    private int quantity;

    public Book(int id, String name, String author, String genre, double price, int quantity) {
        this.id = id; this.name = name; this.author = author;
        this.genre = genre; this.price = price; this.quantity = quantity;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getAuthor() { return author; }
    public String getGenre() { return genre; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int q) { quantity = q; }
}

class User {
    private int id;
    private String username, password, role;

    public User(int id, String u, String p, String r) {
        this.id = id; username = u; password = p; role = r;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getRole() { return role; }
}

class CartItem {
    private int id;
    private Book book;
    private int qty;

    public CartItem(int id, Book book, int qty) {
        this.id = id; this.book = book; this.qty = qty;
    }

    public int getId() { return id; }
    public Book getBook() { return book; }
    public int getQuantity() { return qty; }
    public void setQuantity(int q) { qty = q; }
    public double getLineTotal() { return book.getPrice() * qty; }
}

class Order {
    private int id;
    private int userId;
    private double total;
    private java.util.List<OrderItem> items;

    public Order(int id, int userId, double total, java.util.List<OrderItem> items) {
        this.id = id; this.userId = userId; this.total = total; this.items = items;
    }

    public int getId() { return id; }
    public int getUserId() { return userId; }
    public double getTotal() { return total; }
    public java.util.List<OrderItem> getItems() { return items; }
}

class OrderItem {
    private String bookName;
    private int quantity;
    private double price;

    public OrderItem(String bookName, int quantity, double price) {
        this.bookName = bookName; this.quantity = quantity; this.price = price;
    }

    public String getBookName() { return bookName; }
    public int getQuantity() { return quantity; }
    public double getPrice() { return price; }
}

// --- Service classes ---

class UserService {
    public boolean register(String u, String p) {
        if ("admin".equalsIgnoreCase(u)) return false;
        try (Connection conn = DBUtil.getConnection()) {
            String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, 'user')";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, u);
            stmt.setString(2, p);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public User authenticate(String u, String p) {
        try (Connection conn = DBUtil.getConnection()) {
            String sql = "SELECT * FROM users WHERE username=? AND password=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, u);
            stmt.setString(2, p);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new User(rs.getInt("id"), rs.getString("username"),
                        rs.getString("password"), rs.getString("role"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}

class InventoryService {
    public java.util.List<Book> list() {
        java.util.List<Book> books = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection()) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM books");

            while (rs.next()) {
                books.add(new Book(rs.getInt("id"), rs.getString("name"),
                        rs.getString("author"), rs.getString("genre"),
                        rs.getDouble("price"), rs.getInt("quantity")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return books;
    }

    public java.util.List<String> getAllGenres() {
        Set<String> genres = new LinkedHashSet<>();
        try (Connection conn = DBUtil.getConnection()) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT DISTINCT genre FROM books");
            while (rs.next()) {
                genres.add(rs.getString("genre"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new ArrayList<>(genres);
    }

    public Book get(int id) {
        try (Connection conn = DBUtil.getConnection()) {
            String sql = "SELECT * FROM books WHERE id=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new Book(rs.getInt("id"), rs.getString("name"),
                        rs.getString("author"), rs.getString("genre"),
                        rs.getDouble("price"), rs.getInt("quantity"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void update(Book b) {
        try (Connection conn = DBUtil.getConnection()) {
            String sql = "UPDATE books SET name=?, author=?, genre=?, price=?, quantity=? WHERE id=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, b.getName());
            stmt.setString(2, b.getAuthor());
            stmt.setString(3, b.getGenre());
            stmt.setDouble(4, b.getPrice());
            stmt.setInt(5, b.getQuantity());
            stmt.setInt(6, b.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void remove(int id) {
        try (Connection conn = DBUtil.getConnection()) {
            String sql = "DELETE FROM books WHERE id=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Book addBook(Book b) {
        try (Connection conn = DBUtil.getConnection()) {
            String sql = "INSERT INTO books (name, author, genre, price, quantity) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, b.getName());
            stmt.setString(2, b.getAuthor());
            stmt.setString(3, b.getGenre());
            stmt.setDouble(4, b.getPrice());
            stmt.setInt(5, b.getQuantity());
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return new Book(rs.getInt(1), b.getName(), b.getAuthor(),
                        b.getGenre(), b.getPrice(), b.getQuantity());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}

class CartService {
    public void addToCart(int userId, Book book, int qty) {
        try (Connection conn = DBUtil.getConnection()) {
            String checkSql = "SELECT id FROM cart WHERE user_id=? AND book_id=?";
            PreparedStatement stmt = conn.prepareStatement(checkSql);
            stmt.setInt(1, userId);
            stmt.setInt(2, book.getId());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int cartId = rs.getInt("id");
                String updateSql = "UPDATE cart SET quantity=quantity+? WHERE id=?";
                PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                updateStmt.setInt(1, qty);
                updateStmt.setInt(2, cartId);
                updateStmt.executeUpdate();
            } else {
                String insertSql = "INSERT INTO cart (user_id, book_id, quantity) VALUES (?, ?, ?)";
                PreparedStatement insertStmt = conn.prepareStatement(insertSql);
                insertStmt.setInt(1, userId);
                insertStmt.setInt(2, book.getId());
                insertStmt.setInt(3, qty);
                insertStmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public java.util.List<CartItem> items(int userId) {
        java.util.List<CartItem> items = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection()) {
            String sql = "SELECT c.id, c.quantity, b.id as book_id, b.name, b.author, b.genre, b.price, b.quantity as stock " +
                    "FROM cart c JOIN books b ON c.book_id = b.id WHERE c.user_id=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Book book = new Book(rs.getInt("book_id"), rs.getString("name"),
                        rs.getString("author"), rs.getString("genre"),
                        rs.getDouble("price"), rs.getInt("stock"));
                items.add(new CartItem(rs.getInt("id"), book, rs.getInt("quantity")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    public void setQuantity(int cartId, int qty) {
        try (Connection conn = DBUtil.getConnection()) {
            String sql = "UPDATE cart SET quantity=? WHERE id=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, qty);
            stmt.setInt(2, cartId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeFromCart(int cartId) {
        try (Connection conn = DBUtil.getConnection()) {
            String sql = "DELETE FROM cart WHERE id=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, cartId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void clear(int userId) {
        try (Connection conn = DBUtil.getConnection()) {
            String sql = "DELETE FROM cart WHERE user_id=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

class OrderService {
    public void addOrder(int userId, java.util.List<CartItem> items) throws SQLException {
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                double total = 0;
                for (CartItem ci : items) {
                    total += ci.getLineTotal();
                }

                String orderSql = "INSERT INTO orders (user_id, total) VALUES (?, ?)";
                PreparedStatement orderStmt = conn.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS);
                orderStmt.setInt(1, userId);
                orderStmt.setDouble(2, total);
                orderStmt.executeUpdate();

                ResultSet rs = orderStmt.getGeneratedKeys();
                int orderId = 0;
                if (rs.next()) {
                    orderId = rs.getInt(1);
                }

                String itemSql = "INSERT INTO order_items (order_id, book_id, quantity, price) VALUES (?, ?, ?, ?)";
                PreparedStatement itemStmt = conn.prepareStatement(itemSql);
                String updateBookSql = "UPDATE books SET quantity = quantity - ? WHERE id = ?";
                PreparedStatement updateBookStmt = conn.prepareStatement(updateBookSql);

                for (CartItem ci : items) {
                    itemStmt.setInt(1, orderId);
                    itemStmt.setInt(2, ci.getBook().getId());
                    itemStmt.setInt(3, ci.getQuantity());
                    itemStmt.setDouble(4, ci.getBook().getPrice());
                    itemStmt.addBatch();

                    updateBookStmt.setInt(1, ci.getQuantity());
                    updateBookStmt.setInt(2, ci.getBook().getId());
                    updateBookStmt.addBatch();
                }
                itemStmt.executeBatch();
                updateBookStmt.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public java.util.List<Order> getOrders(int userId) {
        java.util.List<Order> orders = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection()) {
            String sql = "SELECT * FROM orders WHERE user_id=? ORDER BY order_date DESC";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int orderId = rs.getInt("id");
                double total = rs.getDouble("total");

                String itemSql = "SELECT oi.quantity, oi.price, b.name FROM order_items oi " +
                        "JOIN books b ON oi.book_id = b.id WHERE oi.order_id=?";
                PreparedStatement itemStmt = conn.prepareStatement(itemSql);
                itemStmt.setInt(1, orderId);
                ResultSet itemRs = itemStmt.executeQuery();

                java.util.List<OrderItem> items = new ArrayList<>();
                while (itemRs.next()) {
                    items.add(new OrderItem(itemRs.getString("name"),
                            itemRs.getInt("quantity"), itemRs.getDouble("price")));
                }
                orders.add(new Order(orderId, userId, total, items));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return orders;
    }
}

// --- UI classes ---
class LoginFrame extends JFrame {
    private UserService userService = new UserService();
    private JTextField usernameField = new JTextField();
    private JPasswordField passwordField = new JPasswordField();

    public LoginFrame() {
        setTitle("Login / Signup");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(350, 180);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        panel.add(new JLabel("Username:"));
        panel.add(usernameField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);

        JButton loginBtn = new JButton("Login");
        JButton signupBtn = new JButton("Sign Up");
        panel.add(loginBtn);
        panel.add(signupBtn);

        add(panel);

        loginBtn.addActionListener(e -> doLogin());
        signupBtn.addActionListener(e -> doSignup());
    }

    private void doLogin() {
        String u = usernameField.getText().trim();
        String p = new String(passwordField.getPassword());
        User user = userService.authenticate(u, p);
        if (user != null) {
            new MainFrame(user).setVisible(true);
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Incorrect username or password!",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doSignup() {
        String u = usernameField.getText().trim();
        String p = new String(passwordField.getPassword());

        if (u.isEmpty() || p.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username and password cannot be empty.");
            return;
        }

        if (userService.register(u, p))
            JOptionPane.showMessageDialog(this, "Signup successful. You can login now.");
        else
            JOptionPane.showMessageDialog(this, "Username already exists or invalid.");
    }
}

class MainFrame extends JFrame {
    private User currentUser;
    private InventoryService inventory = new InventoryService();
    private CartService cartService = new CartService();
    private OrderService orderService = new OrderService();

    private JTable booksTable, cartTable, ordersTable, adminTable;
    private DefaultTableModel booksModel, cartModel, orderModel, adminModel;

    private JComboBox<String> genreCombo;
    private JTextField searchField;

    public MainFrame(User user) {
        this.currentUser = user;
        setTitle("Book Inventory | User: " + user.getUsername() +
                " [" + user.getRole().toUpperCase() + "]");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();

        String[] booksCols = {"ID", "Name", "Author", "Genre", "Price", "Stock"};
        booksModel = new DefaultTableModel(booksCols, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        booksTable = new JTable(booksModel);
        JScrollPane booksPane = new JScrollPane(booksTable);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        if ("user".equalsIgnoreCase(user.getRole())) {
            genreCombo = new JComboBox<>();
            genreCombo.addItem("All");
            for (String genre : inventory.getAllGenres()) {
                genreCombo.addItem(genre);
            }

            searchField = new JTextField(15);
            JButton filterBtn = new JButton("Filter");
            filterBtn.addActionListener(e -> filterBooks());

            filterPanel.add(new JLabel("Genre:"));
            filterPanel.add(genreCombo);
            filterPanel.add(new JLabel("Search:"));
            filterPanel.add(searchField);
            filterPanel.add(filterBtn);
        }

        JPanel booksBtnPanel = new JPanel();
        JButton refreshBooksBtn = new JButton("Refresh");
        booksBtnPanel.add(refreshBooksBtn);

        if ("user".equalsIgnoreCase(user.getRole())) {
            JButton addToCartBtn = new JButton("Add To Cart");
            booksBtnPanel.add(addToCartBtn);
            addToCartBtn.addActionListener(e -> addBookToCart());
        }

        refreshBooksBtn.addActionListener(e -> {
            if ("user".equalsIgnoreCase(user.getRole())) {
                updateGenreCombo();
                filterBooks();
            } else {
                refreshBooks();
            }
        });

        JPanel booksPanel = new JPanel(new BorderLayout());
        if ("user".equalsIgnoreCase(user.getRole())) {
            booksPanel.add(filterPanel, BorderLayout.NORTH);
        }
        booksPanel.add(booksPane, BorderLayout.CENTER);
        booksPanel.add(booksBtnPanel, BorderLayout.SOUTH);

        String[] cartCols = {"Cart ID", "Book ID", "Name", "Qty", "Price", "Total"};
        cartModel = new DefaultTableModel(cartCols, 0);
        cartTable = new JTable(cartModel);
        JScrollPane cartPane = new JScrollPane(cartTable);
        JPanel cartBtnPanel = new JPanel();
        JButton updateQtyBtn = new JButton("Update Qty");
        JButton removeBtn = new JButton("Remove");
        JButton orderBtn = new JButton("Place Order");
        cartBtnPanel.add(updateQtyBtn);
        cartBtnPanel.add(removeBtn);
        cartBtnPanel.add(orderBtn);

        JPanel cartPanel = new JPanel(new BorderLayout());
        cartPanel.add(cartPane, BorderLayout.CENTER);
        cartPanel.add(cartBtnPanel, BorderLayout.SOUTH);

        updateQtyBtn.addActionListener(e -> updateCartQty());
        removeBtn.addActionListener(e -> removeCartItem());
        orderBtn.addActionListener(e -> placeOrder());

        String[] orderCols = {"Order ID", "Book", "Qty", "Price", "Total"};
        orderModel = new DefaultTableModel(orderCols, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        ordersTable = new JTable(orderModel);
        JScrollPane ordersPane = new JScrollPane(ordersTable);
        JPanel ordersPanel = new JPanel(new BorderLayout());
        ordersPanel.add(ordersPane, BorderLayout.CENTER);

        JPanel adminPanel = null;
        if ("admin".equalsIgnoreCase(user.getRole())) {
            String[] adminCols = {"ID", "Name", "Author", "Genre", "Price", "Stock"};
            adminModel = new DefaultTableModel(adminCols, 0) {
                public boolean isCellEditable(int r, int c) {
                    return false;
                }
            };
            adminTable = new JTable(adminModel);
            JScrollPane adminPane = new JScrollPane(adminTable);

            JPanel adminBtnPanel = new JPanel();
            JButton addBookBtn = new JButton("Add Book");
            JButton editBookBtn = new JButton("Edit Book");
            JButton delBookBtn = new JButton("Delete Book");
            JButton refreshAdminBtn = new JButton("Refresh");

            adminBtnPanel.add(refreshAdminBtn);
            adminBtnPanel.add(addBookBtn);
            adminBtnPanel.add(editBookBtn);
            adminBtnPanel.add(delBookBtn);

            addBookBtn.addActionListener(e -> showBookDialog(false));
            editBookBtn.addActionListener(e -> showBookDialog(true));
            delBookBtn.addActionListener(e -> removeBook());
            refreshAdminBtn.addActionListener(e -> refreshAdmin());

            adminPanel = new JPanel(new BorderLayout());
            adminPanel.add(adminPane, BorderLayout.CENTER);
            adminPanel.add(adminBtnPanel, BorderLayout.SOUTH);
        }

        tabs.add("Books", booksPanel);

        if ("user".equalsIgnoreCase(user.getRole())) {
            tabs.add("Cart", cartPanel);
            tabs.add("Orders", ordersPanel);
        }

        if ("admin".equalsIgnoreCase(user.getRole())) {
            tabs.add("Admin Panel", adminPanel);
        }

        add(tabs, BorderLayout.CENTER);

        if ("user".equalsIgnoreCase(user.getRole())) {
            filterBooks();
            refreshCart();
            refreshOrders();
        } else {
            refreshBooks();
        }

        if ("admin".equalsIgnoreCase(user.getRole())) {
            refreshAdmin();
        }
    }

    private void updateGenreCombo() {
        genreCombo.removeAllItems();
        genreCombo.addItem("All");
        for (String genre : inventory.getAllGenres()) {
            genreCombo.addItem(genre);
        }
    }

    private void filterBooks() {
        String selectedGenre = (String) genreCombo.getSelectedItem();
        String search = searchField.getText().trim().toLowerCase();

        booksModel.setRowCount(0);
        for (Book b : inventory.list()) {
            boolean genreMatches = "All".equals(selectedGenre) ||
                    b.getGenre().equalsIgnoreCase(selectedGenre);
            boolean searchMatches = search.isEmpty()
                    || b.getName().toLowerCase().contains(search)
                    || b.getAuthor().toLowerCase().contains(search);

            if (genreMatches && searchMatches) {
                booksModel.addRow(new Object[]{b.getId(), b.getName(), b.getAuthor(),
                        b.getGenre(), b.getPrice(), b.getQuantity()});
            }
        }
    }

    private void refreshBooks() {
        booksModel.setRowCount(0);
        for (Book b : inventory.list())
            booksModel.addRow(new Object[]{b.getId(), b.getName(), b.getAuthor(),
                    b.getGenre(), b.getPrice(), b.getQuantity()});
    }

    private void refreshAdmin() {
        adminModel.setRowCount(0);
        for (Book b : inventory.list())
            adminModel.addRow(new Object[]{b.getId(), b.getName(), b.getAuthor(),
                    b.getGenre(), b.getPrice(), b.getQuantity()});
    }

    private void refreshCart() {
        cartModel.setRowCount(0);
        for (CartItem ci : cartService.items(currentUser.getId()))
            cartModel.addRow(new Object[]{ci.getId(), ci.getBook().getId(),
                    ci.getBook().getName(), ci.getQuantity(),
                    ci.getBook().getPrice(), ci.getLineTotal()});
    }

    private void refreshOrders() {
        orderModel.setRowCount(0);
        for (Order order : orderService.getOrders(currentUser.getId())) {
            for (OrderItem item : order.getItems()) {
                orderModel.addRow(new Object[]{order.getId(), item.getBookName(),
                        item.getQuantity(), item.getPrice(),
                        item.getQuantity() * item.getPrice()});
            }
        }
    }

    private void showBookDialog(boolean edit) {
        JTextField nameF = new JTextField(), authorF = new JTextField(), genreF = new JTextField(),
                priceF = new JTextField(), qtyF = new JTextField();

        int row = adminTable.getSelectedRow();
        if (edit && row == -1) {
            JOptionPane.showMessageDialog(this, "Select book to edit.");
            return;
        }
        if (edit) {
            nameF.setText(adminModel.getValueAt(row, 1).toString());
            authorF.setText(adminModel.getValueAt(row, 2).toString());
            genreF.setText(adminModel.getValueAt(row, 3).toString());
            priceF.setText(adminModel.getValueAt(row, 4).toString());
            qtyF.setText(adminModel.getValueAt(row, 5).toString());
        }
        JPanel jp = new JPanel(new GridLayout(0, 2));
        jp.add(new JLabel("Name:"));
        jp.add(nameF);
        jp.add(new JLabel("Author:"));
        jp.add(authorF);
        jp.add(new JLabel("Genre:"));
        jp.add(genreF);
        jp.add(new JLabel("Price:"));
        jp.add(priceF);
        jp.add(new JLabel("Quantity:"));
        jp.add(qtyF);
        int r = JOptionPane.showConfirmDialog(this, jp, (edit ? "Edit" : "Add") + " Book",
                JOptionPane.OK_CANCEL_OPTION);
        if (r == JOptionPane.OK_OPTION) {
            try {
                Book nb = new Book(edit ? (int) adminModel.getValueAt(row, 0) : 0,
                        nameF.getText().trim(), authorF.getText().trim(), genreF.getText().trim(),
                        Double.parseDouble(priceF.getText().trim()),
                        Integer.parseInt(qtyF.getText().trim()));
                if (edit) inventory.update(nb);
                else inventory.addBook(nb);
                refreshAdmin();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid input.");
            }
        }
    }

    private void removeBook() {
        int row = adminTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select book to delete.");
            return;
        }
        int id = (int) adminModel.getValueAt(row, 0);
        inventory.remove(id);
        refreshAdmin();
    }

    private void addBookToCart() {
        int row = booksTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a book.");
            return;
        }
        Book b = inventory.get((int) booksModel.getValueAt(row, 0));
        if (b.getQuantity() < 1) {
            JOptionPane.showMessageDialog(this, "Out of stock!");
            return;
        }
        String input = JOptionPane.showInputDialog(this, "Quantity:", 1);
        try {
            int qty = Integer.parseInt(input);
            if (qty > 0 && qty <= b.getQuantity()) {
                cartService.addToCart(currentUser.getId(), b, qty);
                refreshCart();
                JOptionPane.showMessageDialog(this, "Added to cart: " + b.getName());
            } else
                JOptionPane.showMessageDialog(this, "Invalid quantity");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid input.");
        }
    }

    private void updateCartQty() {
        int row = cartTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a cart row.");
            return;
        }
        int cartId = (int) cartModel.getValueAt(row, 0);
        String input = JOptionPane.showInputDialog(this, "New Quantity:",
                cartModel.getValueAt(row, 3));
        try {
            int qty = Integer.parseInt(input);
            if (qty > 0) {
                cartService.setQuantity(cartId, qty);
                refreshCart();
            } else
                JOptionPane.showMessageDialog(this, "Invalid quantity.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid input.");
        }
    }

    private void removeCartItem() {
        int row = cartTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a cart row.");
            return;
        }
        int cartId = (int) cartModel.getValueAt(row, 0);
        cartService.removeFromCart(cartId);
        refreshCart();
    }

    private void placeOrder() {
        java.util.List<CartItem> items = cartService.items(currentUser.getId());
        if (items.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cart is empty.");
            return;
        }

        for (CartItem item : items) {
            if (item.getQuantity() > item.getBook().getQuantity()) {
                JOptionPane.showMessageDialog(this, "Not enough stock for '" + item.getBook().getName() + "'.\n" +
                        "Available: " + item.getBook().getQuantity() + ", In Cart: " + item.getQuantity(),
                        "Stock Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        double total = 0;
        for (CartItem ci : items) total += ci.getLineTotal();
        int opt = JOptionPane.showConfirmDialog(this, "Total: ₹" + total + "\nPlace order?",
                "Cart", JOptionPane.YES_NO_OPTION);
        if (opt != JOptionPane.YES_OPTION) return;

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                orderService.addOrder(currentUser.getId(), items);
                cartService.clear(currentUser.getId());
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    refreshCart();
                    filterBooks();
                    refreshOrders();
                    JOptionPane.showMessageDialog(MainFrame.this, "Order placed successfully!");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(MainFrame.this, "Error placing order: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }
}