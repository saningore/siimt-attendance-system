-- ClassRoll database for the Web Technologies 1 practical.
-- Open this file in MySQL Workbench and click the lightning bolt.
-- Running it again deletes the practice data and starts fresh.

DROP DATABASE IF EXISTS school_attendance;
CREATE DATABASE school_attendance CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE school_attendance;

-- One row per learner. admission_no is the school number, so it cannot repeat.
CREATE TABLE students (
  id INT PRIMARY KEY AUTO_INCREMENT,
  admission_no VARCHAR(20) NOT NULL UNIQUE,
  full_name VARCHAR(100) NOT NULL,
  email VARCHAR(100) NOT NULL,
  class_name ENUM('Form 1', 'Form 2', 'Form 3', 'Form 4') NOT NULL,
  gender ENUM('Female', 'Male') NOT NULL
);

-- One mark per student per date. Saving the same day again updates the mark.
CREATE TABLE attendance (
  id INT PRIMARY KEY AUTO_INCREMENT,
  student_id INT NOT NULL,
  attendance_date DATE NOT NULL,
  status ENUM('Present', 'Absent', 'Late') NOT NULL,
  UNIQUE KEY one_mark_per_day (student_id, attendance_date),
  KEY by_date (attendance_date),
  CONSTRAINT fk_attendance_student
    FOREIGN KEY (student_id) REFERENCES students (id)
    ON DELETE CASCADE
);

INSERT INTO students (admission_no, full_name, email, class_name, gender) VALUES
('STU001', 'Amina Juma', 'amina.juma@school.test', 'Form 1', 'Female'),
('STU002', 'Joseph Mwangi', 'joseph.mwangi@school.test', 'Form 1', 'Male'),
('STU003', 'Neema Laurent', 'neema.laurent@school.test', 'Form 1', 'Female'),
('STU004', 'Grace Otieno', 'grace.otieno@school.test', 'Form 2', 'Female'),
('STU005', 'Daniel Kimaro', 'daniel.kimaro@school.test', 'Form 2', 'Male'),
('STU006', 'Brian Ochieng', 'brian.ochieng@school.test', 'Form 2', 'Male'),
('STU007', 'Fatma Ally', 'fatma.ally@school.test', 'Form 3', 'Female'),
('STU008', 'Peter Massawe', 'peter.massawe@school.test', 'Form 3', 'Male'),
('STU009', 'Lucy Wanjiku', 'lucy.wanjiku@school.test', 'Form 3', 'Female'),
('STU010', 'Hassan Omar', 'hassan.omar@school.test', 'Form 4', 'Male'),
('STU011', 'Mercy Chebet', 'mercy.chebet@school.test', 'Form 4', 'Female'),
('STU012', 'Emmanuel Ngowi', 'emmanuel.ngowi@school.test', 'Form 4', 'Male');

-- Four saved school days. Today is left empty so the class can take it live.
-- Neema (STU003) and Daniel (STU005) finish under 75%. Peter (STU008) sits on 75%.
INSERT INTO attendance (student_id, attendance_date, status)
SELECT s.id, d.attendance_date,
  CASE
    WHEN d.n = 1 AND s.admission_no = 'STU003' THEN 'Absent'
    WHEN d.n = 1 AND s.admission_no = 'STU007' THEN 'Late'
    WHEN d.n = 2 AND s.admission_no IN ('STU003', 'STU005') THEN 'Absent'
    WHEN d.n = 2 AND s.admission_no = 'STU010' THEN 'Late'
    WHEN d.n = 3 AND s.admission_no = 'STU012' THEN 'Late'
    WHEN d.n = 4 AND s.admission_no IN ('STU003', 'STU005', 'STU008') THEN 'Absent'
    ELSE 'Present'
  END
FROM students s
JOIN (
  SELECT 1 AS n, CURDATE() - INTERVAL 1 DAY AS attendance_date
  UNION ALL SELECT 2, CURDATE() - INTERVAL 2 DAY
  UNION ALL SELECT 3, CURDATE() - INTERVAL 3 DAY
  UNION ALL SELECT 4, CURDATE() - INTERVAL 4 DAY
) d;
