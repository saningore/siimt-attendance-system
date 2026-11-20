// Shared helpers. Each page loads this file before its own script.

function todayIso() {
  const date = new Date();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return date.getFullYear() + "-" + month + "-" + day;
}

function showDate(iso) {
  const parts = iso.split("-");
  const months = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"];
  return Number(parts[2]) + " " + months[Number(parts[1]) - 1] + " " + parts[0];
}

function api(path, options) {
  return fetch(path, options).then(function (response) {
    return response.json().catch(function () {
      return {};
    }).then(function (data) {
      if (!response.ok) {
        throw new Error(data.error || "Request failed");
      }
      return data;
    });
  }).catch(function (error) {
    if (error.message === "Failed to fetch") {
      throw new Error("Cannot reach ClassRoll. Start run.bat, then open http://localhost:8080");
    }
    throw error;
  });
}

function sendJson(path, body, method) {
  return api(path, {
    method: method,
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body)
  });
}

function showAlert(id, message, type) {
  const box = document.getElementById(id);
  box.className = "alert alert-" + type;
  box.textContent = message;
  box.hidden = false;
}

function hideAlert(id) {
  document.getElementById(id).hidden = true;
}

function cell(text) {
  const td = document.createElement("td");
  td.textContent = text;
  return td;
}

function statusBadge(status) {
  const span = document.createElement("span");
  const kind = status === "Present" ? "present"
    : status === "Absent" ? "absent"
    : status === "Late" ? "late"
    : "unmarked";
  span.className = "mark mark-" + kind;
  span.textContent = status || "Not marked";
  return span;
}
