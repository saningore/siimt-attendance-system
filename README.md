# ClassRoll

A school attendance register for the Web Technologies 1 practical. One class, one date, three marks: Present, Absent, or Late.

Each technology has one job:

| Technology | Job in this project |
| --- | --- |
| HTML | The four pages |
| CSS and Bootstrap | Layout, tables, forms, and buttons |
| JavaScript | `fetch` the data and update the page |
| Java | Receive those requests and run SQL |
| MySQL | Store students and one mark per student per day |

The JDK includes a small web server, so the practical does not need Tomcat. Open the site from that server. Double-clicking an HTML file will not work, because the browser would not be able to call the Java program.

## Run it before the lesson

1. Open `database/schema.sql` in MySQL Workbench and execute the whole file (lightning bolt).
2. `config.properties` is already set for user `root` and password `root`. If Workbench uses a different password, change the `db.password` line. No quotes.
3. Double-click `run.bat`.
4. Open http://localhost:8080

Leave the black window open during the lesson. Run `schema.sql` again any time you want the original 12 students back.

Sample story already in the database: Neema Laurent and Daniel Kimaro are under 75%. Peter Massawe is on 75%. Today has not been taken yet.

## 30-minute lesson

| Minutes | Open this | What to show |
| --- | --- | --- |
| 0–5 | Today page, then `database/schema.sql` | Four numbers are zero because today is not saved. The `students` and `attendance` tables. The unique key `one_mark_per_day`. |
| 5–12 | `web/students.html`, `web/js/students.js`, `src/attendance/StudentsApi.java` | Add a student. The browser `POST`s JSON. Java uses a `PreparedStatement` and `?` placeholders. |
| 12–22 | `web/register.html`, `web/js/register.js`, `RegisterApi.save`, `Mail.java` | Load Form 1. Mark all present. Set one Absent and one Late. Save. Each student is emailed their mark. Save again and point at `ON DUPLICATE KEY UPDATE`: the same day is corrected, not duplicated. |
| 22–27 | Today page, then Records | The class you saved moves off zero. Records uses saved days only. Late counts as attended. Under 75% is highlighted. |
| 27–30 | Students page | Delete the student you added. `ON DELETE CASCADE` removes their marks with them. |

## Files to teach from

```
database/schema.sql          MySQL tables and sample students
config.properties            host, user, password
src/attendance/Server.java   starts the site on port 8080
src/attendance/Db.java       JDBC connection
src/attendance/StudentsApi.java
src/attendance/RegisterApi.java
src/attendance/Mail.java     the email sent after a save
web/index.html               today
web/students.html            class list
web/register.html            take attendance
web/records.html             percentages and one day
web/js/                      fetch and the page updates
web/css/app.css              the school colours on top of Bootstrap
```

`lib/` holds the MySQL driver, a small JSON library, and the mail library. `web/vendor/` holds Bootstrap so the lab still works if the room has no internet. Students do not need to read those files.

## Email

Put the sender login in `config.properties`:

```
mail.host=smtp.gmail.com
mail.port=587
mail.user=your.address@gmail.com
mail.password=the-app-password
```

Change a student's email on the Students page to a real address, then save that class. The student receives: "You were marked Present in Form 1 on 14 November 2026." For Gmail, `mail.password` is an App Password, not the normal account password. Leave `mail.user` empty and the register still saves, with a note that email was skipped.

## What was left out on purpose

Login, teacher roles, PDF reports, SMS, and biometric clocks show up in larger attendance systems. They are the next project, after this register is clear.
