package attendance;

import com.sun.net.httpserver.HttpExchange;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

// Today's summary, the class register, and the attendance percentages.
public class RegisterApi {
    static void summary(HttpExchange ex) throws Exception {
        String date = readDate(Api.query(ex).get("date"));
        JSONObject out = new JSONObject();
        out.put("date", date);

        String totals = "SELECT COUNT(*) AS students, "
                + "COALESCE(SUM(a.status = 'Present'), 0) AS present, "
                + "COALESCE(SUM(a.status = 'Absent'), 0) AS absent, "
                + "COALESCE(SUM(a.status = 'Late'), 0) AS late "
                + "FROM students s "
                + "LEFT JOIN attendance a ON a.student_id = s.id AND a.attendance_date = ?";
        int students;
        int present;
        int absent;
        int late;
        try (Connection con = Db.open(); PreparedStatement ps = con.prepareStatement(totals)) {
            ps.setString(1, date);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                students = rs.getInt("students");
                present = rs.getInt("present");
                absent = rs.getInt("absent");
                late = rs.getInt("late");
            }
        }
        // Students with no mark yet count as not attended, so the office can see an unfinished morning.
        int percent = students == 0 ? 0 : (int) Math.round((present + late) * 100.0 / students);
        out.put("students", students);
        out.put("present", present);
        out.put("absent", absent);
        out.put("late", late);
        out.put("unmarked", students - present - absent - late);
        out.put("percent", percent);

        JSONArray attention = new JSONArray();
        String followUp = "SELECT s.full_name, s.class_name, s.admission_no, a.status "
                + "FROM attendance a JOIN students s ON s.id = a.student_id "
                + "WHERE a.attendance_date = ? AND a.status IN ('Absent', 'Late') "
                + "ORDER BY a.status, s.class_name, s.full_name";
        try (Connection con = Db.open(); PreparedStatement ps = con.prepareStatement(followUp)) {
            ps.setString(1, date);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JSONObject row = new JSONObject();
                    row.put("fullName", rs.getString("full_name"));
                    row.put("className", rs.getString("class_name"));
                    row.put("admissionNo", rs.getString("admission_no"));
                    row.put("status", rs.getString("status"));
                    attention.put(row);
                }
            }
        }
        out.put("attention", attention);

        JSONArray classes = new JSONArray();
        String byClass = "SELECT s.class_name, COUNT(*) AS students, "
                + "COALESCE(SUM(a.status = 'Present'), 0) AS present, "
                + "COALESCE(SUM(a.status = 'Absent'), 0) AS absent, "
                + "COALESCE(SUM(a.status = 'Late'), 0) AS late "
                + "FROM students s "
                + "LEFT JOIN attendance a ON a.student_id = s.id AND a.attendance_date = ? "
                + "GROUP BY s.class_name ORDER BY s.class_name";
        try (Connection con = Db.open(); PreparedStatement ps = con.prepareStatement(byClass)) {
            ps.setString(1, date);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int count = rs.getInt("students");
                    int classPresent = rs.getInt("present");
                    int classAbsent = rs.getInt("absent");
                    int classLate = rs.getInt("late");
                    JSONObject row = new JSONObject();
                    row.put("className", rs.getString("class_name"));
                    row.put("students", count);
                    row.put("present", classPresent);
                    row.put("absent", classAbsent);
                    row.put("late", classLate);
                    row.put("unmarked", count - classPresent - classAbsent - classLate);
                    classes.put(row);
                }
            }
        }
        out.put("classes", classes);
        Api.json(ex, 200, out.toString());
    }

    static void load(HttpExchange ex) throws Exception {
        Map<String, String> query = Api.query(ex);
        String date = readDate(query.get("date"));
        String className = classFilter(query.get("className"));
        JSONArray rows = new JSONArray();
        String sql = "SELECT s.id, s.admission_no, s.full_name, s.class_name, a.status "
                + "FROM students s "
                + "LEFT JOIN attendance a ON a.student_id = s.id AND a.attendance_date = ? "
                + "WHERE (? = '' OR s.class_name = ?) "
                + "ORDER BY s.class_name, s.full_name";
        try (Connection con = Db.open(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, date);
            ps.setString(2, className);
            ps.setString(3, className);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JSONObject row = new JSONObject();
                    row.put("id", rs.getInt("id"));
                    row.put("admissionNo", rs.getString("admission_no"));
                    row.put("fullName", rs.getString("full_name"));
                    row.put("className", rs.getString("class_name"));
                    String status = rs.getString("status");
                    if (status == null) {
                        row.put("status", JSONObject.NULL);
                    } else {
                        row.put("status", status);
                    }
                    rows.put(row);
                }
            }
        }
        Api.json(ex, 200, rows.toString());
    }

    static void save(HttpExchange ex) throws Exception {
        JSONObject body = Api.body(ex);
        String date = readDate(Api.text(body, "date"));
        JSONArray marks = body.optJSONArray("marks");
        if (marks == null || marks.length() == 0) {
            throw new IllegalArgumentException("Mark at least one student.");
        }
        int[] ids = new int[marks.length()];
        String[] statuses = new String[marks.length()];
        for (int i = 0; i < marks.length(); i++) {
            JSONObject mark;
            try {
                mark = marks.getJSONObject(i);
            } catch (JSONException e) {
                throw new IllegalArgumentException("Each mark must include a student and a status.");
            }
            ids[i] = mark.optInt("studentId", 0);
            statuses[i] = mark.optString("status", "").trim();
            if (ids[i] < 1) {
                throw new IllegalArgumentException("Each mark needs a student.");
            }
            checkStatus(statuses[i]);
        }

        // one_mark_per_day makes a second save update the existing row.
        String sql = "INSERT INTO attendance (student_id, attendance_date, status) VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE status = VALUES(status)";
        try (Connection con = Db.open()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                for (int i = 0; i < ids.length; i++) {
                    ps.setInt(1, ids[i]);
                    ps.setString(2, date);
                    ps.setString(3, statuses[i]);
                    ps.addBatch();
                }
                ps.executeBatch();
                con.commit();
            } catch (Exception e) {
                con.rollback();
                SQLException sqlError = sqlCause(e);
                if (sqlError != null && sqlError.getErrorCode() == 1452) {
                    Api.json(ex, 400, Api.errorBody("One of the students no longer exists. Load the class again."));
                    return;
                }
                throw e;
            }
        }
        Mail.Result mail;
        try {
            mail = Mail.sendMarks(date, ids, statuses);
        } catch (Exception e) {
            e.printStackTrace();
            String reason = e.getMessage() == null ? "The mail server could not be reached." : e.getMessage();
            mail = new Mail.Result(0, "Attendance was saved. Email failed: " + reason);
        }
        JSONObject ok = new JSONObject();
        ok.put("saved", ids.length);
        ok.put("date", date);
        ok.put("emailed", mail.sent);
        ok.put("mailNote", mail.note);
        Api.json(ex, 200, ok.toString());
    }

    static void report(HttpExchange ex) throws Exception {
        String className = classFilter(Api.query(ex).get("className"));
        JSONArray rows = new JSONArray();
        String sql = "SELECT s.id, s.admission_no, s.full_name, s.class_name, "
                + "COALESCE(SUM(a.status = 'Present'), 0) AS present_days, "
                + "COALESCE(SUM(a.status = 'Absent'), 0) AS absent_days, "
                + "COALESCE(SUM(a.status = 'Late'), 0) AS late_days, "
                + "COUNT(a.id) AS marked_days "
                + "FROM students s LEFT JOIN attendance a ON a.student_id = s.id "
                + "WHERE (? = '' OR s.class_name = ?) "
                + "GROUP BY s.id, s.admission_no, s.full_name, s.class_name "
                + "ORDER BY s.class_name, s.full_name";
        try (Connection con = Db.open(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, className);
            ps.setString(2, className);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int present = rs.getInt("present_days");
                    int absent = rs.getInt("absent_days");
                    int late = rs.getInt("late_days");
                    int marked = rs.getInt("marked_days");
                    // Late still means the student attended. Only Absent lowers the percentage.
                    int percent = marked == 0 ? 0 : (int) Math.round((present + late) * 100.0 / marked);
                    JSONObject row = new JSONObject();
                    row.put("id", rs.getInt("id"));
                    row.put("admissionNo", rs.getString("admission_no"));
                    row.put("fullName", rs.getString("full_name"));
                    row.put("className", rs.getString("class_name"));
                    row.put("present", present);
                    row.put("absent", absent);
                    row.put("late", late);
                    row.put("marked", marked);
                    row.put("percent", percent);
                    rows.put(row);
                }
            }
        }
        Api.json(ex, 200, rows.toString());
    }

    private static String readDate(String date) {
        if (date == null || date.trim().isEmpty()) {
            throw new IllegalArgumentException("Choose a date.");
        }
        date = date.trim();
        try {
            LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Use a date like 2026-11-14.");
        }
        return date;
    }

    private static String classFilter(String value) {
        if (value == null) {
            return "";
        }
        value = value.trim();
        if (value.isEmpty() || value.equals("All")) {
            return "";
        }
        if (!StudentsApi.allowedClass(value)) {
            throw new IllegalArgumentException("Choose Form 1, Form 2, Form 3, or Form 4.");
        }
        return value;
    }

    private static void checkStatus(String status) {
        if (!status.equals("Present") && !status.equals("Absent") && !status.equals("Late")) {
            throw new IllegalArgumentException("Status must be Present, Absent, or Late.");
        }
    }

    private static SQLException sqlCause(Throwable error) {
        while (error != null) {
            if (error instanceof SQLException) {
                return (SQLException) error;
            }
            error = error.getCause();
        }
        return null;
    }
}
