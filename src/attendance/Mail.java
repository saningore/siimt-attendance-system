package attendance;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Properties;

// After a register is saved, email each student the mark they received.
public class Mail {
    static Result sendMarks(String date, int[] ids, String[] statuses) throws Exception {
        Properties file = new Properties();
        try (InputStreamReader reader = new InputStreamReader(
                new FileInputStream("config.properties"), StandardCharsets.UTF_8)) {
            file.load(reader);
        }
        String user = file.getProperty("mail.user", "").trim();
        String password = file.getProperty("mail.password", "");
        String from = file.getProperty("mail.from", "").trim();
        if (from.isEmpty()) {
            from = user;
        }
        if (user.isEmpty() || password.isEmpty()) {
            return new Result(0, "Add mail.user and mail.password in config.properties to email the class.");
        }

        String host = file.getProperty("mail.host", "smtp.gmail.com").trim();
        String port = file.getProperty("mail.port", "587").trim();
        Properties smtp = new Properties();
        smtp.put("mail.smtp.host", host);
        smtp.put("mail.smtp.port", port);
        smtp.put("mail.smtp.auth", "true");
        smtp.put("mail.smtp.connectiontimeout", "8000");
        smtp.put("mail.smtp.timeout", "8000");
        smtp.put("mail.smtp.writetimeout", "8000");
        if ("465".equals(port)) {
            smtp.put("mail.smtp.ssl.enable", "true");
        } else {
            smtp.put("mail.smtp.starttls.enable", "true");
        }
        final String login = user;
        final String secret = password;
        Session session = Session.getInstance(smtp, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(login, secret);
            }
        });

        String when = LocalDate.parse(date).format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH));
        int sent = 0;
        int failed = 0;
        String firstError = "";
        String sql = "SELECT full_name, class_name, email FROM students WHERE id = ?";
        try (Connection con = Db.open(); PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < ids.length; i++) {
                ps.setInt(1, ids[i]);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next() || rs.getString("email") == null || rs.getString("email").trim().isEmpty()) {
                        failed++;
                        if (firstError.isEmpty()) {
                            firstError = "A student has no email address.";
                        }
                        continue;
                    }
                    String name = rs.getString("full_name");
                    String className = rs.getString("class_name");
                    String email = rs.getString("email").trim();
                    String body = "Hello " + name + ",\n\n"
                            + "You were marked " + statuses[i] + " in " + className + " on " + when + ".\n\n"
                            + "ClassRoll";
                    try {
                        MimeMessage message = new MimeMessage(session);
                        message.setFrom(new InternetAddress(from));
                        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
                        message.setSubject("Attendance for " + when);
                        message.setText(body);
                        Transport.send(message);
                        sent++;
                    } catch (Exception e) {
                        failed++;
                        if (firstError.isEmpty()) {
                            firstError = e.getMessage() == null ? "The mail server refused the message." : e.getMessage();
                        }
                    }
                }
            }
        }
        String note = "";
        if (failed > 0) {
            note = failed + (failed == 1 ? " email could not be sent. " : " emails could not be sent. ") + firstError;
        }
        return new Result(sent, note);
    }

    static final class Result {
        final int sent;
        final String note;

        Result(int sent, String note) {
            this.sent = sent;
            this.note = note == null ? "" : note;
        }
    }
}
