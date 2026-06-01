import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleServer {
    private static final int PORT = 8080;
    private static final Path ROOT = Path.of("..").toAbsolutePath().normalize();

    private static final List<String> TABLES = List.of(
            "User", "Project", "AI_Model", "Workload", "Hardware_System",
            "Hardware_Configuration", "Software_Environment", "Optimization_Method",
            "Experiment_Run", "Benchmark_Result", "Result_Metric", "Works_On",
            "Experiment_Run_Optimization"
    );

    private static final Map<Integer, String> SAVED_QUERIES = Map.ofEntries(
            Map.entry(1, """
                    SELECT HS.system_id, HS.system_name, AVG(RM.metric_value) AS avg_latency
                    FROM Hardware_System HS, Hardware_Configuration HC, Experiment_Run ER, Benchmark_Result BR, Result_Metric RM
                    WHERE HS.system_id = HC.system_id
                      AND HC.config_id = ER.config_id
                      AND HC.system_id = ER.system_id
                      AND ER.run_id = BR.run_id
                      AND BR.result_id = RM.result_id
                      AND RM.metric_name = 'Latency'
                    GROUP BY HS.system_id, HS.system_name
                    """),
            Map.entry(2, """
                    SELECT ER.run_id, ER.run_name, RM.metric_value AS latency, RM.unit
                    FROM Experiment_Run ER, Benchmark_Result BR, Result_Metric RM
                    WHERE ER.run_id = BR.run_id
                      AND BR.result_id = RM.result_id
                      AND RM.metric_name = 'Latency'
                      AND RM.metric_value < (SELECT AVG(RM2.metric_value)
                                             FROM Result_Metric RM2
                                             WHERE RM2.metric_name = 'Latency')
                    """),
            Map.entry(3, """
                    SELECT ER.run_id, ER.run_name
                    FROM Experiment_Run ER
                    WHERE NOT EXISTS (SELECT OM.optimization_id
                                      FROM Optimization_Method OM
                                      WHERE NOT EXISTS (SELECT ERO.optimization_id
                                                        FROM Experiment_Run_Optimization ERO
                                                        WHERE ERO.run_id = ER.run_id
                                                          AND ERO.optimization_id = OM.optimization_id))
                    """),
            Map.entry(4, """
                    SELECT HS.system_id, HS.system_name, AVG(RM.metric_value) AS avg_power_consumption
                    FROM Hardware_System HS, Hardware_Configuration HC, Experiment_Run ER, Benchmark_Result BR, Result_Metric RM
                    WHERE HS.system_id = HC.system_id
                      AND HC.config_id = ER.config_id
                      AND HC.system_id = ER.system_id
                      AND ER.run_id = BR.run_id
                      AND BR.result_id = RM.result_id
                      AND RM.metric_name = 'Power Consumption'
                    GROUP BY HS.system_id, HS.system_name
                    ORDER BY avg_power_consumption ASC
                    LIMIT 3
                    """),
            Map.entry(5, """
                    SELECT HS.system_id, HS.system_name, COUNT(ER.run_id) AS num_experiments
                    FROM Hardware_System HS, Hardware_Configuration HC, Experiment_Run ER
                    WHERE HS.system_id = HC.system_id
                      AND HC.config_id = ER.config_id
                      AND HC.system_id = ER.system_id
                    GROUP BY HS.system_id, HS.system_name
                    HAVING COUNT(ER.run_id) = (
                        SELECT MAX(run_count)
                        FROM (
                            SELECT COUNT(ER2.run_id) AS run_count
                            FROM Hardware_System HS2, Hardware_Configuration HC2, Experiment_Run ER2
                            WHERE HS2.system_id = HC2.system_id
                              AND HC2.config_id = ER2.config_id
                              AND HC2.system_id = ER2.system_id
                            GROUP BY HS2.system_id
                        ) AS Counts
                    )
                    """),
            Map.entry(6, """
                    SELECT HS.system_id, HS.system_name, HC.config_id, HC.config_name,
                           SE.environment_id, SE.os_name, SE.framework,
                           AVG(RM.metric_value) AS avg_latency
                    FROM Hardware_System HS, Hardware_Configuration HC, Experiment_Run ER,
                         Software_Environment SE, Benchmark_Result BR, Result_Metric RM
                    WHERE HS.system_id = HC.system_id
                      AND HC.config_id = ER.config_id
                      AND HS.system_id = ER.system_id
                      AND ER.environment_id = SE.environment_id
                      AND ER.run_id = BR.run_id
                      AND BR.result_id = RM.result_id
                      AND RM.metric_name = 'Latency'
                    GROUP BY HS.system_id, HS.system_name, HC.config_id, HC.config_name,
                             SE.environment_id, SE.os_name, SE.framework
                    HAVING AVG(RM.metric_value) = (
                        SELECT MIN(avg_latency)
                        FROM (
                            SELECT AVG(RM2.metric_value) AS avg_latency
                            FROM Hardware_System HS2, Hardware_Configuration HC2, Experiment_Run ER2,
                                 Benchmark_Result BR2, Result_Metric RM2, Software_Environment SE2
                            WHERE HS2.system_id = HC2.system_id
                              AND HC2.config_id = ER2.config_id
                              AND HC2.system_id = ER2.system_id
                              AND ER2.environment_id = SE2.environment_id
                              AND ER2.run_id = BR2.run_id
                              AND BR2.result_id = RM2.result_id
                              AND RM2.metric_name = 'Latency'
                            GROUP BY HS2.system_id, HC2.config_id, SE2.environment_id
                        ) AS SetupAverages
                    )
                    """),
            Map.entry(7, """
                    SELECT DISTINCT U.user_id, U.first_name, U.last_name
                    FROM User U, Works_On WO, Project P, Experiment_Run ER, AI_Model AIM
                    WHERE U.user_id = WO.user_id
                      AND P.project_id = ER.project_id
                      AND ER.model_id = AIM.model_id
                      AND WO.project_id = P.project_id
                      AND AIM.model_family = 'Transformer'
                    """)
    );

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/api/tables", SimpleServer::handleTables);
        server.createContext("/api/schema", SimpleServer::handleSchema);
        server.createContext("/api/rows", SimpleServer::handleRows);
        server.createContext("/api/row", SimpleServer::handleRowWrite);
        server.createContext("/api/query", SimpleServer::handleSavedQuery);
        server.createContext("/api/custom-query", SimpleServer::handleCustomQuery);
        server.createContext("/", SimpleServer::handleStatic);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        System.out.println("AI Hardware Benchmark site running at http://localhost:" + PORT);
        System.out.println("Serving files from " + ROOT);
    }

    private static void handleTables(HttpExchange ex) throws IOException {
        try {
            requireMethod(ex, "GET");
            sendJson(ex, 200, "{\"tables\":" + toJsonArray(TABLES) + "}");
        } catch (Exception e) {
            sendError(ex, e);
        }
    }

    private static void handleSchema(HttpExchange ex) throws IOException {
        try {
            requireMethod(ex, "GET");
            String table = requireTable(queryParams(ex).get("table"));
            sendJson(ex, 200, schemaJson(table));
        } catch (Exception e) {
            sendError(ex, e);
        }
    }

    private static void handleRows(HttpExchange ex) throws IOException {
        try {
            requireMethod(ex, "GET");
            Map<String, String> q = queryParams(ex);
            String table = requireTable(q.get("table"));
            int limit = Math.min(parseInt(q.getOrDefault("limit", "200"), 200), 500);
            String sql = "SELECT * FROM " + ident(table) + " LIMIT " + limit;
            try (Connection conn = DBConnection.getConnection(); Statement stmt = conn.createStatement()) {
                sendJson(ex, 200, resultSetJson(stmt.executeQuery(sql)));
            }
        } catch (Exception e) {
            sendError(ex, e);
        }
    }

    private static void handleRowWrite(HttpExchange ex) throws IOException {
        try {
            Map<String, String> q = queryParams(ex);
            String table = requireTable(q.get("table"));
            Map<String, String> body = formParams(ex);
            if ("POST".equals(ex.getRequestMethod())) {
                insertRow(table, body);
                sendJson(ex, 200, "{\"ok\":true,\"message\":\"Row added\"}");
            } else if ("PUT".equals(ex.getRequestMethod())) {
                updateRow(table, body);
                sendJson(ex, 200, "{\"ok\":true,\"message\":\"Row updated\"}");
            } else if ("DELETE".equals(ex.getRequestMethod())) {
                deleteRow(table, body);
                sendJson(ex, 200, "{\"ok\":true,\"message\":\"Row deleted\"}");
            } else {
                throw new IllegalArgumentException("Unsupported method: " + ex.getRequestMethod());
            }
        } catch (Exception e) {
            sendError(ex, e);
        }
    }

    private static void handleSavedQuery(HttpExchange ex) throws IOException {
        try {
            requireMethod(ex, "GET");
            int id = parseInt(queryParams(ex).get("id"), 0);
            String sql = SAVED_QUERIES.get(id);
            if (sql == null) throw new IllegalArgumentException("Unknown query id: " + id);
            try (Connection conn = DBConnection.getConnection(); Statement stmt = conn.createStatement()) {
                sendJson(ex, 200, resultSetJson(stmt.executeQuery(sql)));
            }
        } catch (Exception e) {
            sendError(ex, e);
        }
    }

    private static void handleCustomQuery(HttpExchange ex) throws IOException {
        try {
            requireMethod(ex, "POST");
            String sql = normalizeSql(formParams(ex).getOrDefault("sql", ""));
            if (!isReadOnlyQuery(sql)) {
                throw new IllegalArgumentException("Custom query runner only accepts SELECT, WITH, SHOW, DESCRIBE, and EXPLAIN statements. Use the table editor for inserts, updates, and deletes.");
            }
            try (Connection conn = DBConnection.getConnection(); Statement stmt = conn.createStatement()) {
                sendJson(ex, 200, resultSetJson(stmt.executeQuery(sql)));
            }
        } catch (Exception e) {
            sendError(ex, e);
        }
    }

    private static void handleStatic(HttpExchange ex) throws IOException {
        try {
            String rawPath = ex.getRequestURI().getPath();
            Path file = rawPath.equals("/") ? ROOT.resolve("index.html") : ROOT.resolve(rawPath.substring(1)).normalize();
            if (!file.startsWith(ROOT) || !Files.exists(file) || Files.isDirectory(file)) {
                sendText(ex, 404, "Not found", "text/plain");
                return;
            }
            String contentType = rawPath.endsWith(".html") ? "text/html" : rawPath.endsWith(".css") ? "text/css" : "text/plain";
            byte[] data = Files.readAllBytes(file);
            ex.getResponseHeaders().set("Content-Type", contentType + "; charset=utf-8");
            ex.sendResponseHeaders(200, data.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(data);
            }
        } catch (Exception e) {
            sendError(ex, e);
        }
    }

    private static String schemaJson(String table) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            Set<String> primaryKeys = new HashSet<>();
            try (ResultSet pk = meta.getPrimaryKeys(null, null, table)) {
                while (pk.next()) primaryKeys.add(pk.getString("COLUMN_NAME"));
            }

            Map<String, List<String>> enumValues = enumValues(conn, table);
            StringBuilder columns = new StringBuilder("[");
            try (ResultSet cols = meta.getColumns(null, null, table, null)) {
                boolean first = true;
                while (cols.next()) {
                    if (!first) columns.append(',');
                    first = false;
                    String name = cols.getString("COLUMN_NAME");
                    columns.append("{\"name\":").append(json(name))
                            .append(",\"type\":").append(json(cols.getString("TYPE_NAME")))
                            .append(",\"size\":").append(cols.getInt("COLUMN_SIZE"))
                            .append(",\"nullable\":").append(cols.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls)
                            .append(",\"primaryKey\":").append(primaryKeys.contains(name))
                            .append(",\"enumValues\":").append(toJsonArray(enumValues.getOrDefault(name, List.of())))
                            .append('}');
                }
            }
            columns.append(']');
            return "{\"table\":" + json(table) + ",\"primaryKeys\":" + toJsonArray(new ArrayList<>(primaryKeys)) + ",\"columns\":" + columns + "}";
        }
    }

    private static Map<String, List<String>> enumValues(Connection conn, String table) throws SQLException {
        Map<String, List<String>> values = new HashMap<>();
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SHOW COLUMNS FROM " + ident(table))) {
            Pattern enumPattern = Pattern.compile("enum\\((.*)\\)", Pattern.CASE_INSENSITIVE);
            while (rs.next()) {
                Matcher m = enumPattern.matcher(rs.getString("Type"));
                if (m.matches()) {
                    List<String> opts = new ArrayList<>();
                    Matcher quoted = Pattern.compile("'((?:[^']|'')*)'").matcher(m.group(1));
                    while (quoted.find()) opts.add(quoted.group(1).replace("''", "'"));
                    values.put(rs.getString("Field"), opts);
                }
            }
        }
        return values;
    }

    private static void insertRow(String table, Map<String, String> body) throws SQLException {
        List<String> columns = columnNames(table);
        List<String> provided = columns.stream().filter(body::containsKey).toList();
        if (provided.isEmpty()) throw new IllegalArgumentException("No values submitted.");
        String placeholders = String.join(",", Collections.nCopies(provided.size(), "?"));
        String sql = "INSERT INTO " + ident(table) + " (" + joinIdents(provided) + ") VALUES (" + placeholders + ")";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            bindValues(ps, provided, body);
            ps.executeUpdate();
        }
    }

    private static void updateRow(String table, Map<String, String> body) throws SQLException {
        List<String> pks = primaryKeys(table);
        if (pks.isEmpty()) throw new IllegalArgumentException("Table has no primary key.");
        List<String> columns = columnNames(table);
        List<String> editable = columns.stream().filter(c -> !pks.contains(c) && body.containsKey(c)).toList();
        if (editable.isEmpty()) throw new IllegalArgumentException("No non-key values submitted.");
        for (String pk : pks) if (!body.containsKey("pk_" + pk)) throw new IllegalArgumentException("Missing primary key: " + pk);
        String sql = "UPDATE " + ident(table) + " SET " + joinAssignments(editable) + " WHERE " + joinPkWhere(pks);
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            int i = bindValues(ps, editable, body);
            for (String pk : pks) setParam(ps, i++, body.get("pk_" + pk));
            ps.executeUpdate();
        }
    }

    private static void deleteRow(String table, Map<String, String> body) throws SQLException {
        List<String> pks = primaryKeys(table);
        if (pks.isEmpty()) throw new IllegalArgumentException("Table has no primary key.");
        for (String pk : pks) if (!body.containsKey("pk_" + pk)) throw new IllegalArgumentException("Missing primary key: " + pk);
        String sql = "DELETE FROM " + ident(table) + " WHERE " + joinPkWhere(pks);
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            int i = 1;
            for (String pk : pks) setParam(ps, i++, body.get("pk_" + pk));
            ps.executeUpdate();
        }
    }

    private static List<String> columnNames(String table) throws SQLException {
        List<String> cols = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection(); ResultSet rs = conn.getMetaData().getColumns(null, null, table, null)) {
            while (rs.next()) cols.add(rs.getString("COLUMN_NAME"));
        }
        return cols;
    }

    private static List<String> primaryKeys(String table) throws SQLException {
        List<String> keys = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection(); ResultSet rs = conn.getMetaData().getPrimaryKeys(null, null, table)) {
            while (rs.next()) keys.add(rs.getString("COLUMN_NAME"));
        }
        return keys;
    }

    private static int bindValues(PreparedStatement ps, List<String> columns, Map<String, String> body) throws SQLException {
        int i = 1;
        for (String col : columns) setParam(ps, i++, body.get(col));
        return i;
    }

    private static void setParam(PreparedStatement ps, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) ps.setNull(index, Types.NULL);
        else ps.setString(index, value);
    }

    private static String resultSetJson(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int count = meta.getColumnCount();
        StringBuilder cols = new StringBuilder("[");
        for (int i = 1; i <= count; i++) {
            if (i > 1) cols.append(',');
            cols.append(json(meta.getColumnLabel(i)));
        }
        cols.append(']');

        StringBuilder rows = new StringBuilder("[");
        boolean firstRow = true;
        while (rs.next()) {
            if (!firstRow) rows.append(',');
            firstRow = false;
            rows.append('{');
            for (int i = 1; i <= count; i++) {
                if (i > 1) rows.append(',');
                String key = meta.getColumnLabel(i);
                Object value = rs.getObject(i);
                rows.append(json(key)).append(':').append(jsonValue(value));
            }
            rows.append('}');
        }
        rows.append(']');
        return "{\"columns\":" + cols + ",\"rows\":" + rows + "}";
    }

    private static String normalizeSql(String sql) {
        String normalized = sql == null ? "" : sql.trim();
        while (normalized.endsWith(";")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }
        return normalized;
    }

    private static boolean isReadOnlyQuery(String sql) {
        String lower = sql.toLowerCase(Locale.ROOT);
        if (sql.isBlank() || lower.contains(";")) return false;
        return lower.startsWith("select ") || lower.startsWith("with ") || lower.startsWith("show ")
                || lower.startsWith("describe ") || lower.startsWith("desc ") || lower.startsWith("explain ");
    }

    private static String requireTable(String table) {
        if (table == null || !TABLES.contains(table)) throw new IllegalArgumentException("Unknown table: " + table);
        return table;
    }

    private static void requireMethod(HttpExchange ex, String method) {
        if (!method.equals(ex.getRequestMethod())) throw new IllegalArgumentException("Expected " + method + " request.");
    }

    private static Map<String, String> queryParams(HttpExchange ex) {
        return parseParams(ex.getRequestURI().getRawQuery());
    }

    private static Map<String, String> formParams(HttpExchange ex) throws IOException {
        try (InputStream in = ex.getRequestBody()) {
            return parseParams(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private static Map<String, String> parseParams(String raw) {
        Map<String, String> params = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) return params;
        for (String pair : raw.split("&")) {
            int idx = pair.indexOf('=');
            String key = idx >= 0 ? pair.substring(0, idx) : pair;
            String val = idx >= 0 ? pair.substring(idx + 1) : "";
            params.put(urlDecode(key), urlDecode(val));
        }
        return params;
    }

    private static String urlDecode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }

    private static int parseInt(String s, int fallback) {
        try { return Integer.parseInt(s); } catch (Exception e) { return fallback; }
    }

    private static String ident(String name) {
        return "`" + name.replace("`", "``") + "`";
    }

    private static String joinIdents(List<String> cols) {
        return String.join(",", cols.stream().map(SimpleServer::ident).toList());
    }

    private static String joinAssignments(List<String> cols) {
        return String.join(",", cols.stream().map(c -> ident(c) + "=?").toList());
    }

    private static String joinPkWhere(List<String> pks) {
        return String.join(" AND ", pks.stream().map(c -> ident(c) + "=?").toList());
    }

    private static void sendJson(HttpExchange ex, int status, String body) throws IOException {
        sendText(ex, status, body, "application/json");
    }

    private static void sendError(HttpExchange ex, Exception e) throws IOException {
        e.printStackTrace();
        sendJson(ex, 400, "{\"ok\":false,\"error\":" + json(e.getMessage()) + "}");
    }

    private static void sendText(HttpExchange ex, int status, String body, String contentType) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        Headers headers = ex.getResponseHeaders();
        headers.set("Content-Type", contentType + "; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String jsonValue(Object value) {
        if (value == null) return "null";
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof Timestamp ts) return json(ts.toLocalDateTime().toString());
        if (value instanceof java.sql.Date || value instanceof Time || value instanceof LocalDateTime) return json(value.toString());
        if (value instanceof BigDecimal bd) return bd.stripTrailingZeros().toPlainString();
        return json(value.toString());
    }

    private static String json(String s) {
        if (s == null) return "null";
        StringBuilder out = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> out.append(c);
            }
        }
        return out.append('"').toString();
    }

    private static String toJsonArray(List<String> values) {
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) out.append(',');
            out.append(json(values.get(i)));
        }
        return out.append(']').toString();
    }
}



