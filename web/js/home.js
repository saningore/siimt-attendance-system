const date = todayIso();

function loadDashboard() {
  document.getElementById("today-label").textContent = showDate(date);
  return api("/api/summary?date=" + encodeURIComponent(date)).then(function (summary) {
    document.getElementById("stat-students").textContent = summary.students;
    document.getElementById("stat-present").textContent = summary.present;
    document.getElementById("stat-absent").textContent = summary.absent;
    document.getElementById("stat-late").textContent = summary.late;

    const line = document.getElementById("rate-line");
    const bar = document.getElementById("rate-bar");
    line.textContent = summary.percent + "% attended today, out of every student on the roll. Late counts as attended.";
    bar.style.width = summary.percent + "%";
    bar.setAttribute("aria-valuenow", String(summary.percent));

    const note = document.getElementById("attention-note");
    const table = document.getElementById("attention-table");
    const body = document.getElementById("attention-rows");
    body.replaceChildren();
    if (summary.students === 0) {
      note.hidden = false;
      note.textContent = "Add students first, then take the register.";
      table.hidden = true;
    } else if (summary.attention.length === 0) {
      note.hidden = false;
      table.hidden = true;
      note.textContent = summary.unmarked === summary.students
        ? "Today's register has not been taken yet. " + summary.unmarked + " students are not marked."
        : "No one is marked absent or late."
          + (summary.unmarked > 0 ? " " + summary.unmarked + " students are still not marked." : "");
    } else {
      note.hidden = summary.unmarked === 0;
      note.textContent = summary.unmarked + " students are still not marked.";
      table.hidden = false;
      summary.attention.forEach(function (row) {
        const tr = document.createElement("tr");
        tr.appendChild(cell(row.fullName));
        tr.appendChild(cell(row.className));
        tr.appendChild(cell(row.admissionNo));
        const statusCell = document.createElement("td");
        statusCell.appendChild(statusBadge(row.status));
        tr.appendChild(statusCell);
        body.appendChild(tr);
      });
    }

    const classes = document.getElementById("class-rows");
    classes.replaceChildren();
    summary.classes.forEach(function (row) {
      const tr = document.createElement("tr");
      tr.appendChild(cell(row.className));
      tr.appendChild(cell(String(row.present)));
      tr.appendChild(cell(String(row.absent)));
      tr.appendChild(cell(String(row.late)));
      tr.appendChild(cell(String(row.unmarked)));
      const action = document.createElement("td");
      const link = document.createElement("a");
      link.className = "btn btn-sm btn-outline-success";
      link.textContent = "Take";
      link.href = "register.html?class=" + encodeURIComponent(row.className) + "&date=" + encodeURIComponent(date);
      action.appendChild(link);
      tr.appendChild(action);
      classes.appendChild(tr);
    });
  });
}

loadDashboard().catch(function (error) {
  document.getElementById("rate-line").textContent = error.message;
});
