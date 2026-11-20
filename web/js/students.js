let students = [];
let editingId = null;

function draw() {
  const query = document.getElementById("search").value.trim().toLowerCase();
  const body = document.getElementById("student-rows");
  body.replaceChildren();
  const shown = students.filter(function (student) {
    return student.fullName.toLowerCase().indexOf(query) >= 0
      || student.email.toLowerCase().indexOf(query) >= 0
      || student.admissionNo.toLowerCase().indexOf(query) >= 0
      || student.className.toLowerCase().indexOf(query) >= 0;
  });
  document.getElementById("student-count").textContent = shown.length + " shown, " + students.length + " on the roll";
  if (shown.length === 0) {
    const tr = document.createElement("tr");
    const empty = cell("No students to show.");
    empty.colSpan = 6;
    tr.appendChild(empty);
    body.appendChild(tr);
    return;
  }
  shown.forEach(function (student) {
    const tr = document.createElement("tr");
    tr.dataset.id = String(student.id);
    tr.appendChild(cell(student.admissionNo));
    tr.appendChild(cell(student.fullName));
    tr.appendChild(cell(student.email));
    tr.appendChild(cell(student.className));
    tr.appendChild(cell(student.gender));
    const actions = document.createElement("td");
    actions.className = "text-nowrap";
    actions.appendChild(actionButton("edit", "Edit", "btn btn-sm btn-outline-secondary me-1"));
    actions.appendChild(actionButton("delete", "Delete", "btn btn-sm btn-outline-danger"));
    tr.appendChild(actions);
    body.appendChild(tr);
  });
}

function actionButton(action, label, className) {
  const button = document.createElement("button");
  button.type = "button";
  button.dataset.action = action;
  button.className = className;
  button.textContent = label;
  return button;
}

function resetForm() {
  editingId = null;
  document.getElementById("student-form").reset();
  document.getElementById("form-title").textContent = "Add a student";
  document.getElementById("save-student").textContent = "Save student";
  document.getElementById("cancel-edit").hidden = true;
}

function formData() {
  return {
    admissionNo: document.getElementById("admission-no").value.trim(),
    fullName: document.getElementById("full-name").value.trim(),
    email: document.getElementById("email").value.trim(),
    className: document.getElementById("class-name").value,
    gender: document.getElementById("gender").value
  };
}

function startEdit(student) {
  editingId = student.id;
  document.getElementById("admission-no").value = student.admissionNo;
  document.getElementById("full-name").value = student.fullName;
  document.getElementById("email").value = student.email;
  document.getElementById("class-name").value = student.className;
  document.getElementById("gender").value = student.gender;
  document.getElementById("form-title").textContent = "Edit student";
  document.getElementById("save-student").textContent = "Update student";
  document.getElementById("cancel-edit").hidden = false;
  document.getElementById("student-form").scrollIntoView({ behavior: "smooth", block: "start" });
}

function loadStudents() {
  return api("/api/students").then(function (rows) {
    students = rows;
    draw();
  });
}

document.getElementById("search").addEventListener("input", draw);

document.getElementById("cancel-edit").addEventListener("click", function () {
  resetForm();
  hideAlert("alert");
});

document.getElementById("student-form").addEventListener("submit", function (event) {
  event.preventDefault();
  const button = document.getElementById("save-student");
  button.disabled = true;
  const payload = formData();
  const request = editingId
    ? sendJson("/api/students/" + editingId, payload, "PUT")
    : sendJson("/api/students", payload, "POST");
  request.then(function () {
    showAlert("alert", editingId ? "Student updated." : "Student added.", "success");
    resetForm();
    return loadStudents();
  }).catch(function (error) {
    showAlert("alert", error.message, "danger");
  }).finally(function () {
    button.disabled = false;
  });
});

document.getElementById("student-rows").addEventListener("click", function (event) {
  const button = event.target.closest("button");
  if (!button) {
    return;
  }
  const id = Number(button.closest("tr").dataset.id);
  const student = students.find(function (item) { return item.id === id; });
  if (!student) {
    return;
  }
  if (button.dataset.action === "edit") {
    startEdit(student);
  }
  if (button.dataset.action === "delete") {
    if (!window.confirm("Remove " + student.fullName + " and their attendance?")) {
      return;
    }
    api("/api/students/" + student.id, { method: "DELETE" }).then(function () {
      if (editingId === student.id) {
        resetForm();
      }
      showAlert("alert", student.fullName + " was removed.", "success");
      return loadStudents();
    }).catch(function (error) {
      showAlert("alert", error.message, "danger");
    });
  }
});

loadStudents().catch(function (error) {
  showAlert("alert", error.message, "danger");
});
