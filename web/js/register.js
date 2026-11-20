let roll = [];

const params = new URLSearchParams(window.location.search);
document.getElementById("date").value = params.get("date") || todayIso();
if (params.get("class")) {
  document.getElementById("class-name").value = params.get("class");
}

function paintRow(row, status) {
  row.querySelectorAll("button[data-status]").forEach(function (button) {
    const on = button.dataset.status === status;
    const color = button.dataset.status === "Present" ? "success"
      : button.dataset.status === "Absent" ? "danger"
      : "warning";
    button.className = "btn btn-sm " + (on ? "btn-" : "btn-outline-") + color;
  });
}

function statusButton(label, status) {
  const button = document.createElement("button");
  button.type = "button";
  button.dataset.status = status;
  button.textContent = label;
  return button;
}

function drawRoll() {
  const body = document.getElementById("roll-rows");
  body.replaceChildren();
  roll.forEach(function (student) {
    const tr = document.createElement("tr");
    tr.dataset.id = String(student.id);
    tr.appendChild(cell(student.admissionNo));
    tr.appendChild(cell(student.fullName));
    tr.appendChild(cell(student.className));
    const choice = document.createElement("td");
    const group = document.createElement("div");
    group.className = "status-group";
    group.appendChild(statusButton("Present", "Present"));
    group.appendChild(statusButton("Absent", "Absent"));
    group.appendChild(statusButton("Late", "Late"));
    choice.appendChild(group);
    tr.appendChild(choice);
    paintRow(tr, student.status);
    body.appendChild(tr);
  });
  tally();
}

function tally() {
  let present = 0;
  let absent = 0;
  let late = 0;
  let unmarked = 0;
  roll.forEach(function (student) {
    if (student.status === "Present") present += 1;
    else if (student.status === "Absent") absent += 1;
    else if (student.status === "Late") late += 1;
    else unmarked += 1;
  });
  document.getElementById("tally").textContent =
    present + " present, " + absent + " absent, " + late + " late, " + unmarked + " not marked";
}

function loadRoll() {
  const className = document.getElementById("class-name").value;
  const date = document.getElementById("date").value;
  const button = document.getElementById("load-class");
  button.disabled = true;
  return api("/api/register?date=" + encodeURIComponent(date) + "&className=" + encodeURIComponent(className))
    .then(function (rows) {
      roll = rows;
      document.getElementById("roll-card").hidden = false;
      document.getElementById("roll-title").textContent = className + " · " + showDate(date);
      const empty = document.getElementById("roll-empty");
      const table = document.getElementById("roll-table");
      const tools = document.getElementById("roll-tools");
      if (roll.length === 0) {
        empty.hidden = false;
        empty.textContent = "No students in this class. Add them on the Students page.";
        table.hidden = true;
        tools.hidden = true;
        hideAlert("alert");
        return;
      }
      empty.hidden = true;
      table.hidden = false;
      tools.hidden = false;
      drawRoll();
      const savedBefore = roll.some(function (student) { return student.status; });
      if (savedBefore) {
        showAlert("alert", "This register was already saved. Change a mark and save again to correct it.", "success");
      } else {
        hideAlert("alert");
      }
    })
    .catch(function (error) {
      showAlert("alert", error.message, "danger");
    })
    .finally(function () {
      button.disabled = false;
    });
}

document.getElementById("filter").addEventListener("submit", function (event) {
  event.preventDefault();
  loadRoll();
});

document.getElementById("mark-all").addEventListener("click", function () {
  roll.forEach(function (student) { student.status = "Present"; });
  drawRoll();
});

document.getElementById("roll-rows").addEventListener("click", function (event) {
  const button = event.target.closest("button[data-status]");
  if (!button) {
    return;
  }
  const row = button.closest("tr");
  const id = Number(row.dataset.id);
  const student = roll.find(function (item) { return item.id === id; });
  if (!student) {
    return;
  }
  student.status = button.dataset.status;
  paintRow(row, student.status);
  tally();
});

document.getElementById("save-register").addEventListener("click", function () {
  if (roll.length === 0) {
    return;
  }
  const missing = roll.some(function (student) { return !student.status; });
  if (missing) {
    showAlert("alert", "Mark every student. Use Mark all present, then change the exceptions.", "warning");
    return;
  }
  const button = document.getElementById("save-register");
  button.disabled = true;
  const date = document.getElementById("date").value;
  sendJson("/api/register", {
    date: date,
    marks: roll.map(function (student) {
      return { studentId: student.id, status: student.status };
    })
  }, "POST").then(function (result) {
    let message = "Register saved for " + document.getElementById("class-name").value + " on " + showDate(date) + ".";
    if (result.emailed > 0) {
      message += " " + result.emailed + (result.emailed === 1 ? " email sent." : " emails sent.");
    }
    if (result.mailNote) {
      message += " " + result.mailNote;
    }
    showAlert("alert", message, "success");
  }).catch(function (error) {
    showAlert("alert", error.message, "danger");
  }).finally(function () {
    button.disabled = false;
  });
});

if (params.get("class")) {
  loadRoll();
}
