// TerribleGame.java - A poorly coded game for educational purposes

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;
import java.sql.*;

public class TerribleGame extends JFrame implements KeyListener, ActionListener {

    public static final int W = 800;
    public static final int H = 600;
    private static String usr = null;
    private static Connection db = null;
    private static final String DB_URL = "jdbc:sqlite:terrible_game_data.db";

    private GamePanel gp;
    private javax.swing.Timer tm;

    public int px;
    public int py;
    public int pw = 30;
    public int ph = 15;
    public int s = 5;

    public ArrayList<int[]> bt;
    public ArrayList<int[]> gt;
    public ArrayList<int[]> b;

    public int sc = 0;
    public int lv = 3;
    public boolean go = false;
    public boolean p = false;
    public boolean run = false;

    private boolean l = false;
    private boolean r = false;
    private boolean u = false;
    private boolean d = false;
    private boolean f = false;

    private Random rg = new Random();

    private JTextField uf;
    private JPasswordField pf;
    private JButton lb;
    private JButton rb;
    private JLabel sl;
    private JPanel lp;

    public TerribleGame() {
        super("The Terrible Space Game");

        dbSetup();
        dbInit();

        setSize(W, H);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        lp = new JPanel();
        lp.setLayout(null);
        lp.setBackground(Color.DARK_GRAY);

        JLabel ul = new JLabel("Username:");
        ul.setBounds(300, 150, 80, 25);
        ul.setForeground(Color.WHITE);
        lp.add(ul);

        uf = new JTextField();
        uf.setBounds(400, 150, 160, 25);
        lp.add(uf);

        JLabel pl = new JLabel("Password:");
        pl.setBounds(300, 190, 80, 25);
        pl.setForeground(Color.WHITE);
        lp.add(pl);

        pf = new JPasswordField();
        pf.setBounds(400, 190, 160, 25);
        lp.add(pf);

        lb = new JButton("Login");
        lb.setBounds(300, 230, 120, 30);
        lb.addActionListener(this);
        lp.add(lb);

        rb = new JButton("Register");
        rb.setBounds(440, 230, 120, 30);
        rb.addActionListener(this);
        lp.add(rb);

        sl = new JLabel("");
        sl.setBounds(300, 270, 260, 25);
        sl.setForeground(Color.RED);
        sl.setHorizontalAlignment(SwingConstants.CENTER);
        lp.add(sl);

        JTextArea hs = new JTextArea();
        hs.setBounds(50, 350, W - 100, 200);
        hs.setEditable(false);
        hs.setForeground(Color.CYAN);
        hs.setBackground(Color.BLACK);
        hs.setFont(new Font("Monospaced", Font.PLAIN, 12));
        lp.add(hs);
        showHS(hs);

        gp = new GamePanel();
        gp.setPreferredSize(new Dimension(W, H));
        gp.setBackground(Color.BLACK);
        gp.setVisible(false);

        add(lp, BorderLayout.CENTER);

        addKeyListener(this);
        setFocusable(true);
        requestFocusInWindow();

        setLocationRelativeTo(null);
        setVisible(true);

        tm = new javax.swing.Timer(16, this);
    }

    private void dbSetup() {
        try {
            Class.forName("org.sqlite.JDBC");
            if (db == null || db.isClosed()) {
                db = DriverManager.getConnection(DB_URL);
                System.out.println("Database connection established.");
            }
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC Driver not found! Make sure the JAR is in the classpath.");
            JOptionPane.showMessageDialog(this, "Critical Error: SQLite JDBC Driver not found.\nPlease check application setup.", "Database Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Could not connect to the database: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void dbInit() {
         if (db == null) {
             System.err.println("Cannot initialize DB tables, connection is null.");
             return;
         }
        String sql1 = "CREATE TABLE IF NOT EXISTS users (" +
                                    " id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                    " username TEXT UNIQUE NOT NULL," +
                                    " password TEXT NOT NULL" +
                                    ");";
        String sql2 = "CREATE TABLE IF NOT EXISTS high_scores (" +
                                      " id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                      " username TEXT NOT NULL," +
                                      " score INTEGER NOT NULL," +
                                      " timestamp DATETIME DEFAULT CURRENT_TIMESTAMP" +
                                      ");";

        try (Statement st = db.createStatement()) {
            st.execute(sql1);
            st.execute(sql2);
            System.out.println("Database tables checked/created.");
        } catch (SQLException e) {
            System.err.println("Error creating database tables: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean reg(String u, String p) {
        if (u == null || u.trim().isEmpty() || p == null || p.isEmpty()) {
            sl.setText("Username and password cannot be empty.");
            return false;
        }
        String sql = "INSERT INTO users (username, password) VALUES ('" + u + "', '" + p + "')";

        try (Statement st = db.createStatement()) {
            String chk = "SELECT id FROM users WHERE username = '" + u + "'";
            ResultSet rs = st.executeQuery(chk);
            if (rs.next()) {
                sl.setText("Username already exists.");
                rs.close();
                return false;
            }
            rs.close();

            int r = st.executeUpdate(sql);
            if (r > 0) {
                sl.setText("Registration successful!");
                return true;
            } else {
                sl.setText("Registration failed (database error).");
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Registration error: " + e.getMessage());
            sl.setText("Registration failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private boolean login(String u, String p) {
         if (u == null || u.trim().isEmpty() || p == null || p.isEmpty()) {
            sl.setText("Username and password cannot be empty.");
            return false;
        }
        String sql = "SELECT password FROM users WHERE username = '" + u + "'";

        try (Statement st = db.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            if (rs.next()) {
                String sp = rs.getString("password");
                if (sp.equals(p)) {
                    return true;
                } else {
                    sl.setText("Incorrect password.");
                    return false;
                }
            } else {
                sl.setText("Username not found.");
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Login error: " + e.getMessage());
            sl.setText("Login failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void saveScore(String u, int s) {
        if (u == null || s <= 0) {
             System.err.println("Invalid data for saving score (User: " + u + ", Score: " + s + ")");
             return;
        }
        String sql = "INSERT INTO high_scores (username, score) VALUES (?, ?)";

        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setString(1, u);
            ps.setInt(2, s);
            ps.executeUpdate();
            System.out.println("Score " + s + " for user " + u + " saved.");
        } catch (SQLException e) {
            System.err.println("Error saving score: " + e.getMessage());
            e.printStackTrace();
        } catch (NullPointerException e) {
            System.err.println("Database connection is likely null. Cannot save score.");
             e.printStackTrace();
        }
    }

     private void showHS(JTextArea ta) {
        String sql = "SELECT username, score FROM high_scores ORDER BY score DESC LIMIT 10";
        StringBuilder st = new StringBuilder("--- High Scores ---\n");

        try (Statement s = db.createStatement();
             ResultSet rs = s.executeQuery(sql)) {

            int r = 1;
            while (rs.next()) {
                String u = rs.getString("username");
                int sc = rs.getInt("score");
                st.append(String.format("%d. %-15s : %d\n", r++, u, sc));
            }
            if (r == 1) {
                 st.append("No scores recorded yet.\n");
            }
        } catch (SQLException e) {
            System.err.println("Error fetching high scores: " + e.getMessage());
            st.append("Error loading scores.\n");
            e.printStackTrace();
        } catch (NullPointerException e) {
            System.err.println("Database connection is likely null. Cannot fetch scores.");
            st.append("Database connection error.\n");
            e.printStackTrace();
        }
        ta.setText(st.toString());
    }

    private void init() {
        px = W / 2 - pw / 2;
        py = H - 50 - ph;
        sc = 0;
        lv = 3;
        go = false;
        p = false;
        run = true;

        bt = new ArrayList<>();
        gt = new ArrayList<>();
        b = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            spawnBad();
        }
         for (int i = 0; i < 3; i++) {
            spawnGood();
        }

        remove(lp);
        add(gp, BorderLayout.CENTER);
        gp.setVisible(true);
        revalidate();
        repaint();

        gp.requestFocusInWindow();
        requestFocus();

        tm.start();
    }

    private void update() {
        if (go || p || !run) {
            return;
        }

        if (l) {
            px -= s;
        }
        if (r) {
            px += s;
        }
         if (u) {
            py -= s;
        }
        if (d) {
            py += s;
        }

        if (px < 0) {
            px = 0;
        }
        if (px > W - pw) {
            px = W - pw;
        }
         if (py < 0) {
            py = 0;
        }
        if (py > H - ph - 30) {
            py = H - ph - 30;
        }

        if (f) {
              int bw = 5;
              int bh = 10;
              int bs = 8;
              int[] nb = {px + pw / 2 - bw / 2, py - bh, bw, bh, bs};
              b.add(nb);
              f = false;
        }

        ArrayList<int[]> br = new ArrayList<>();
        for (int i = 0; i < b.size(); i++) {
            int[] b2 = b.get(i);
            b2[1] -= b2[4];
            if (b2[1] + b2[3] < 0) {
                br.add(b2);
            }
        }
        b.removeAll(br);

        ArrayList<int[]> btr = new ArrayList<>();
        Rectangle pr = new Rectangle(px, py, pw, ph);

        for (int i = 0; i < bt.size(); i++) {
            int[] th = bt.get(i);
            th[1] += th[4];

            Rectangle tr = new Rectangle(th[0], th[1], th[2], th[3]);
            if (pr.intersects(tr)) {
                lv--;
                btr.add(th);
                if (lv <= 0) {
                    go = true;
                    tm.stop();
                     end();
                }
                continue;
            }

              ArrayList<int[]> bh = new ArrayList<>();
              boolean h = false;
              for (int[] bl : b) {
                  Rectangle br2 = new Rectangle(bl[0], bl[1], bl[2], bl[3]);
                  if (br2.intersects(tr)) {
                      btr.add(th);
                      bh.add(bl);
                      sc += 10;
                      h = true;
                      break;
                  }
              }
              b.removeAll(bh);

            if (!h && th[1] > H) {
                btr.add(th);
            }
        }
        bt.removeAll(btr);

          ArrayList<int[]> gtr = new ArrayList<>();
         for (int i = 0; i < gt.size(); i++) {
             int[] th = gt.get(i);
             th[1] += th[4];

             Rectangle tr = new Rectangle(th[0], th[1], th[2], th[3]);
             if (pr.intersects(tr)) {
                 sc += 50;
                 gtr.add(th);
             } else if (th[1] > H) {
                 gtr.add(th);
             }
         }
         gt.removeAll(gtr);

        if (rg.nextInt(100) < 5) {
             if (bt.size() < 15) {
                spawnBad();
             }
        }
         if (rg.nextInt(100) < 2) {
             if (gt.size() < 5) {
                 spawnGood();
             }
        }
    }

     private void spawnBad() {
         int w = 20 + rg.nextInt(30);
         int h = w;
         int x = rg.nextInt(W - w);
         int y = -h;
         int s = 2 + rg.nextInt(3);
         bt.add(new int[]{x, y, w, h, s});
     }

     private void spawnGood() {
         int w = 15;
         int h = 15;
         int x = rg.nextInt(W - w);
         int y = -h;
         int s = 3 + rg.nextInt(2);
         gt.add(new int[]{x, y, w, h, s});
     }

      private void end() {
        run = false;
        System.out.println("Game Over! Final Score: " + sc);
        if (usr != null) {
             saveScore(usr, sc);
        } else {
            System.out.println("Score not saved - user not logged in.");
        }
     }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();

        if (src == tm) {
            update();
            gp.repaint();
        } else if (src == lb) {
            String u = uf.getText();
            String p = new String(pf.getPassword());
            if (login(u, p)) {
                sl.setText("Login Successful!");
                usr = u;
                init();
            } else {
                pf.setText("");
            }
        } else if (src == rb) {
              String u = uf.getText();
              String p = new String(pf.getPassword());
              if (reg(u, p)) {
                  uf.setText("");
                  pf.setText("");
              } else {
                   pf.setText("");
              }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        if (k == KeyEvent.VK_LEFT || k == KeyEvent.VK_A) {
            l = true;
        }
        if (k == KeyEvent.VK_RIGHT || k == KeyEvent.VK_D) {
            r = true;
        }
        if (k == KeyEvent.VK_UP || k == KeyEvent.VK_W) {
            u = true;
        }
        if (k == KeyEvent.VK_DOWN || k == KeyEvent.VK_S) {
            d = true;
        }
         if (k == KeyEvent.VK_SPACE || k == KeyEvent.VK_CONTROL) {
            if (run && !go && !p) {
                 f = true;
            }
        }
        if (k == KeyEvent.VK_P) {
             if (run && !go) {
                 p = !p;
                 if (p) {
                     tm.stop();
                 } else {
                     tm.start();
                 }
                 gp.repaint();
             }
         }
        if (k == KeyEvent.VK_ESCAPE) {
             System.out.println("Escape pressed, exiting.");
             try {
                 if (db != null && !db.isClosed()) {
                     db.close();
                     System.out.println("Database connection closed.");
                 }
             } catch (SQLException ex) {
                 ex.printStackTrace();
             }
             System.exit(0);
        }
        if (go && k == KeyEvent.VK_R) {
             System.out.println("Restart requested (Not implemented fully - requires relaunch or better state management)");
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int k = e.getKeyCode();
        if (k == KeyEvent.VK_LEFT || k == KeyEvent.VK_A) {
            l = false;
        }
        if (k == KeyEvent.VK_RIGHT || k == KeyEvent.VK_D) {
            r = false;
        }
         if (k == KeyEvent.VK_UP || k == KeyEvent.VK_W) {
            u = false;
        }
        if (k == KeyEvent.VK_DOWN || k == KeyEvent.VK_S) {
            d = false;
        }
    }

    private class GamePanel extends JPanel {

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2d.setColor(Color.BLACK);
            g2d.fillRect(0, 0, W, H);

            if (!run && !go) {
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 20));
                g2d.drawString("Waiting to start...", W/2 - 100, H/2);
                return;
            }

            g2d.setColor(Color.CYAN);
            g2d.fillRect(px, py, pw, ph);

            g2d.setColor(Color.YELLOW);
            try {
                ArrayList<int[]> cb = new ArrayList<>(b);
                for (int[] b : cb) {
                    g2d.fillRect(b[0], b[1], b[2], b[3]);
                }
            } catch (Exception e) { }

            g2d.setColor(Color.RED);
             try {
                 ArrayList<int[]> cbt = new ArrayList<>(bt);
                 for (int[] t : cbt) {
                     g2d.fillRect(t[0], t[1], t[2], t[3]);
                 }
            } catch (Exception e) { }

            g2d.setColor(Color.GREEN);
             try {
                 ArrayList<int[]> cgt = new ArrayList<>(gt);
                for (int[] t : cgt) {
                    g2d.fillOval(t[0], t[1], t[2], t[3]);
                }
            } catch (Exception e) { }

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Consolas", Font.BOLD, 16));

            g2d.drawString("Score: " + sc, 10, 20);

            g2d.drawString("Lives: " + lv, W - 100, 20);

            if (go) {
                g2d.setColor(Color.YELLOW);
                g2d.setFont(new Font("Arial", Font.BOLD, 48));
                String m = "GAME OVER";
                FontMetrics fm = g2d.getFontMetrics();
                int mw = fm.stringWidth(m);
                g2d.drawString(m, (W - mw) / 2, H / 2 - 50);

                g2d.setFont(new Font("Arial", Font.BOLD, 24));
                String sm = "Final Score: " + sc;
                int sw = fm.stringWidth(sm);
                fm = g2d.getFontMetrics();
                sw = fm.stringWidth(sm);
                g2d.drawString(sm, (W - sw) / 2, H / 2);

                g2d.setFont(new Font("Arial", Font.PLAIN, 16));
                String rm = "(Press ESC to exit)";
                int rw = fm.stringWidth(rm);
                 fm = g2d.getFontMetrics();
                 rw = fm.stringWidth(rm);
                g2d.drawString(rm, (W - rw) / 2, H / 2 + 50);
            }

             if (p && !go) {
                 g2d.setColor(new Color(0, 0, 0, 150));
                 g2d.fillRect(0, 0, W, H);

                 g2d.setColor(Color.WHITE);
                 g2d.setFont(new Font("Arial", Font.BOLD, 48));
                 String m = "PAUSED";
                 FontMetrics fm = g2d.getFontMetrics();
                 int mw = fm.stringWidth(m);
                 g2d.drawString(m, (W - mw) / 2, H / 2);

                 g2d.setFont(new Font("Arial", Font.PLAIN, 16));
                 String rm = "(Press 'P' to resume)";
                 int rw = fm.stringWidth(rm);
                 fm = g2d.getFontMetrics();
                 rw = fm.stringWidth(rm);
                 g2d.drawString(rm, (W - rw) / 2, H / 2 + 50);
             }
        }
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                TerribleGame tg = new TerribleGame();
            }
        });

          Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (db != null && !db.isClosed()) {
                    System.out.println("Shutdown hook closing database connection.");
                    db.close();
                }
            } catch (SQLException e) {
                System.err.println("Error closing database connection during shutdown: " + e.getMessage());
                e.printStackTrace();
            }
        }));
    }

}