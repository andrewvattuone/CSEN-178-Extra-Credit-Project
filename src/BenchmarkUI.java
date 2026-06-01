import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class BenchmarkUI extends JFrame {

    private JTextField systemIDField;
    private JTextField systemNameField;
    private JTextField vendorField;
    private JTextArea resultsArea;

    public BenchmarkUI() {
        setTitle("AI Hardware Benchmark Platform");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel(new GridLayout(3, 2));

        inputPanel.add(new JLabel("System ID"));
        systemIDField = new JTextField();
        inputPanel.add(systemIDField);

        inputPanel.add(new JLabel("System Name"));
        systemNameField = new JTextField();
        inputPanel.add(systemNameField);

        inputPanel.add(new JLabel("Vendor"));
        vendorField = new JTextField();
        inputPanel.add(vendorField);

        add(inputPanel, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel();

        JButton addButton = new JButton("Add Hardware");
        JButton updateButton = new JButton("Update Hardware");
        JButton deleteButton = new JButton("Delete Hardware");
        JButton query1Button = new JButton("Run Query 1");

        buttonPanel.add(addButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(query1Button);

        add(buttonPanel, BorderLayout.SOUTH);

        resultsArea = new JTextArea();
        resultsArea.setEditable(false);
        resultsArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        add(new JScrollPane(resultsArea), BorderLayout.CENTER);

        addButton.addActionListener(e -> addHardware());
        updateButton.addActionListener(e -> updateHardware());
        deleteButton.addActionListener(e -> deleteHardware());
        query1Button.addActionListener(e -> runQuery1());

        setVisible(true);
    }

    private void addHardware() {
        String sql = """
            INSERT INTO Hardware_System
            (system_id, system_name, vendor)
            VALUES (?, ?, ?)
            """;

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, systemIDField.getText());
            ps.setString(2, systemNameField.getText());
            ps.setString(3, vendorField.getText());

            ps.executeUpdate();
            resultsArea.setText("Hardware system added successfully.\n\n");
            showHardwareSystems();

        } catch (SQLException ex) {
            resultsArea.setText("Error adding hardware:\n" + ex.getMessage());
        }
    }

    private void updateHardware() {
        String sql = """
            UPDATE Hardware_System
            SET system_name = ?,
                vendor = ?
            WHERE system_id = ?
            """;

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, systemNameField.getText());
            ps.setString(2, vendorField.getText());
            ps.setString(3, systemIDField.getText());

            int rows = ps.executeUpdate();
            resultsArea.setText(rows + " hardware system(s) updated.\n\n");
            showHardwareSystems();

        } catch (SQLException ex) {
            resultsArea.setText("Error updating hardware:\n" + ex.getMessage());
        }
    }

    private void deleteHardware() {
        String sql = """
            DELETE FROM Hardware_System
            WHERE system_id = ?
            """;

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, systemIDField.getText());

            int rows = ps.executeUpdate();
            resultsArea.setText(rows + " hardware system(s) deleted.\n\n");
            showHardwareSystems();

        } catch (SQLException ex) {
            resultsArea.setText("Error deleting hardware:\n" + ex.getMessage());
        }
    }

    private void showHardwareSystems() {
        String sql = """
            SELECT system_id, system_name, vendor
            FROM Hardware_System
            ORDER BY system_id
            """;

        try (
                Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)
        ) {
            resultsArea.append("Current Hardware Systems\n");
            resultsArea.append("------------------------\n");

            while (rs.next()) {
                resultsArea.append(
                        rs.getString("system_id") + " | " +
                        rs.getString("system_name") + " | " +
                        rs.getString("vendor") + "\n"
                );
            }

        } catch (SQLException ex) {
            resultsArea.append("\nError showing hardware systems:\n" + ex.getMessage());
        }
    }

    private void runQuery1() {
        String sql = """
            SELECT HS.system_id,
                   HS.system_name,
                   AVG(RM.metric_value) AS avg_latency
            FROM Hardware_System HS,
                 Hardware_Configuration HC,
                 Experiment_Run ER,
                 Benchmark_Result BR,
                 Result_Metric RM
            WHERE HS.system_id = HC.system_id
              AND HC.config_id = ER.config_id
              AND HC.system_id = ER.system_id
              AND ER.run_id = BR.run_id
              AND BR.result_id = RM.result_id
              AND RM.metric_name = 'Latency'
            GROUP BY HS.system_id, HS.system_name
            """;

        resultsArea.setText("");

        try (
                Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)
        ) {
            resultsArea.append("Average Experimental Latency by Hardware System\n");
            resultsArea.append("------------------------------------------------\n");

            while (rs.next()) {
                resultsArea.append(
                        rs.getString("system_id") + " | " +
                        rs.getString("system_name") + " | " +
                        rs.getDouble("avg_latency") + "\n"
                );
            }

        } catch (SQLException ex) {
            resultsArea.setText("Error running Query 1:\n" + ex.getMessage());
        }
    }
}