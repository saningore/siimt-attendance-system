document.getElementById("day").value = todayIso();

function loadRecords() {
  const className = document.getElementById("class-name").value;
  const date = document.getElementById("day").value;
  const button = document.getElementById("apply");
  button.disabled = true;
  const reportUrl = "/api/report" + (className ? "?className=" + encodeURIComponent(className) : "");
  const dayUrl = "/api/register?date=" + encodeURIComponent(date)
    + (className ? "&className=" + encodeURIComponent(className) : "");
  return Promise.all([api(reportUrl), api(dayUrl)]).then(function (result) {
    drawReport(result[0]);
    drawDay(result[1], date);
    hideAlert("alert");
  }).catch(function (error) {
    showAlert("alert", error.message, "danger");
  }).finally(function () {
    button.disabled = false;
  });
}

function drawReport(rows) {
  const body = document.getElementById("report-rows");
  body.replaceChildren();
  if (rows.length === 0) {
    const tr = document.createElement("tr");
    const empty = cell("No students in this class.");
    empty.colSpan = 7;
    tr.appendChild(empty);
    body.appendChild(tr);
    return;
  }
  rows.forEach(function (student) {
    const tr = document.createElement("tr");
    if (student.marked > 0 && student.percent < 75) {
      tr.className = "low-row";
    }
    tr.appendChild(cell(student.admissionNo));
    tr.appendChild(cell(student.fullName));
    tr.appendChild(cell(student.className));
    tr.appendChild(cell(String(student.present)));
    tr.appendChild(cell(String(student.absent)));
    tr.appendChild(cell(String(student.late)));
    const percent = document.createElement("td");
    percent.textContent = student.marked === 0 ? "—" : student.percent + "%";
    if (student.marked > 0 && student.percent < 75) {
      percent.className = "fw-semibold";
    }
    tr.appendChild(percent);
    body.appendChild(tr);
  });
}

function drawDay(rows, date) {
  document.getElementById("day-title").textContent = "Marks on " + showDate(date);
  const body = document.getElementById("day-rows");
  body.replaceChildren();
  if (rows.length === 0) {
    const tr = document.createElement("tr");
    const empty = cell("No students in this class.");
    empty.colSpan = 4;
    tr.appendChild(empty);
    body.appendChild(tr);
    return;
  }
  rows.forEach(function (student) {
    const tr = document.createElement("tr");
    tr.appendChild(cell(student.admissionNo));
    tr.appendChild(cell(student.fullName));
    tr.appendChild(cell(student.className));
    const status = document.createElement("td");
    status.appendChild(statusBadge(student.status));
    tr.appendChild(status);
    body.appendChild(tr);
  });
}

document.getElementById("filters").addEventListener("submit", function (event) {
  event.preventDefault();
  loadRecords();
});

loadRecords();
