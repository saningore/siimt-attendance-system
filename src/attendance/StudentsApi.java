package attendance;

import com.sun.net.httpserver.HttpExchange;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

// Create, list, update, and delete students.
public class StudentsApi {
    private static final String[] CLASSES = {"Form 1", "Form 2", "Form 3", "Form 4"};

    static boolean allowedClass(String className) {
        for (int i = 0; i < CLASSES.length; i++) {
            if (CLASSES[i].equals(className)) {
                return true;
            }
        }
        return false;
    }

    static void list(HttpExchange ex) throws Exception {
        JSONArray rows = new JSONArray();
        String sql = "SELECT id, admission_no, full_name, email, class_name, gender "
                + "FROM students ORDER BY class_name, full_name";
        try (Connection con = Db.open();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.put(toJson(rs));
            }
        }
        Api.json(ex, 200, rows.toString());
    }

    static void create(HttpExchange ex) throws Exception {
        StudentInput input = readInput(Api.body(ex));
        String sql = "INSERT INTO students (admission_no, full_name, email, class_name, gender) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = Db.open();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, input);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                Api.json(ex, 201, toJson(keys.getInt(1), input).toString());
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                Api.json(ex, 409, Api.errorBody("That admission number is already used."));
                return;
            }
            throw e;
        }
    }

    static void update(HttpExchange ex, int id) throws Exception {
        StudentInput input = readInput(Api.body(ex));
        String sql = "UPDATE students SET admission_no = ?, full_name = ?, email = ?, class_name = ?, gender = ? WHERE id = ?";
        try (Connection con = Db.open(); PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, input);
            ps.setInt(6, id);
            int changed = ps.executeUpdate();
            if (changed == 0) {
                Api.json(ex, 404, Api.errorBody("Student not found."));
                return;
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                Api.json(ex, 409, Api.errorBody("That admission number is already used."));
                return;
            }
            throw e;
        }
        Api.json(ex, 200, toJson(id, input).toString());
    }

    static void delete(HttpExchange ex, int id) throws Exception {
        String sql = "DELETE FROM students WHERE id = ?";
        try (Connection con = Db.open(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            int changed = ps.executeUpdate();
            if (changed == 0) {
                Api.json(ex, 404, Api.errorBody("Student not found."));
                return;
            }
        }
        JSONObject ok = new JSONObject();
        ok.put("deleted", id);
        Api.json(ex, 200, ok.toString());
    }

    private static void bind(PreparedStatement ps, StudentInput input) throws SQLException {
        ps.setString(1, input.admissionNo);
        ps.setString(2, input.fullName);
        ps.setString(3, input.email);
        ps.setString(4, input.className);
        ps.setString(5, input.gender);
    }

    private static StudentInput readInput(JSONObject body) {
        String admissionNo = Api.text(body, "admissionNo");
        String fullName = Api.text(body, "fullName");
        String email = Api.text(body, "email");
        String className = Api.text(body, "className");
        String gender = Api.text(body, "gender");
        if (admissionNo.isEmpty() || fullName.isEmpty() || email.isEmpty() || className.isEmpty() || gender.isEmpty()) {
            throw new IllegalArgumentException("Admission number, name, email, class, and gender are required.");
        }
        if (admissionNo.length() > 20 || fullName.length() > 100 || email.length() > 100) {
            throw new IllegalArgumentException("Admission number, name, or email is too long.");
        }
        int at = email.indexOf('@');
        if (at < 1 || email.indexOf('.', at) < 0 || email.indexOf(' ') >= 0) {
            throw new IllegalArgumentException("Enter an email address like name@school.com.");
        }
        if (!allowedClass(className)) {
            throw new IllegalArgumentException("Choose Form 1, Form 2, Form 3, or Form 4.");
        }
        if (!gender.equals("Female") && !gender.equals("Male")) {
            throw new IllegalArgumentException("Gender must be Female or Male.");
        }
        return new StudentInput(admissionNo, fullName, email, className, gender);
    }

    private static JSONObject toJson(ResultSet rs) throws SQLException {
        JSONObject row = new JSONObject();
        row.put("id", rs.getInt("id"));
        row.put("admissionNo", rs.getString("admission_no"));
        row.put("fullName", rs.getString("full_name"));
        row.put("email", rs.getString("email"));
        row.put("className", rs.getString("class_name"));
        row.put("gender", rs.getString("gender"));
        return row;
    }

    private static JSONObject toJson(int id, StudentInput input) {
        JSONObject row = new JSONObject();
        row.put("id", id);
        row.put("admissionNo", input.admissionNo);
        row.put("fullName", input.fullName);
        row.put("email", input.email);
        row.put("className", input.className);
        row.put("gender", input.gender);
        return row;
    }

    private static final class StudentInput {
        final String admissionNo;
        final String fullName;
        final String email;
        final String className;
        final String gender;

        StudentInput(String admissionNo, String fullName, String email, String className, String gender) {
            this.admissionNo = admissionNo;
            this.fullName = fullName;
            this.email = email;
            this.className = className;
            this.gender = gender;
        }
    }
}
