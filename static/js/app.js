const queryLabels = {
  1: "Average latency by hardware system",
  2: "Runs below average latency",
  3: "Runs using all optimizations",
  4: "Lowest average power systems",
  5: "Hardware with most experiments",
  6: "Lowest-latency setup",
  7: "Users who tested transformers",
};
let tables = [],
  currentTable = "",
  currentSchema = null,
  currentRows = [],
  editingRow = null;
const $ = (id) => document.getElementById(id);
function toast(msg) {
  const el = $("message");
  el.textContent = msg;
  el.classList.add("show");
  setTimeout(() => el.classList.remove("show"), 2600);
}
async function api(path, opts = {}) {
  const res = await fetch(path, opts);
  let data;
  try {
    data = await res.json();
  } catch {
    data = { error: await res.text() };
  }
  if (!res.ok || data.ok === false)
    throw new Error(data.error || "Request failed");
  return data;
}
function params(obj) {
  const p = new URLSearchParams();
  Object.entries(obj).forEach(([k, v]) => p.append(k, v ?? ""));
  return p;
}
async function init() {
  try {
    const data = await api("/api/tables");
    tables = data.tables;
    $("status").textContent = "Connected to http://localhost:8080";
    renderTableNav();
    renderSavedQueries();
    await selectTable(tables[0]);
  } catch (err) {
    $("status").textContent = "Could not connect: " + err.message;
    toast(err.message);
  }
}
async function renderHome() {
  try {
    const rowSets = {};
    await Promise.all(
      tables.map(async (table) => {
        rowSets[table] = (
          await api("/api/rows?table=" + encodeURIComponent(table))
        ).rows;
      }),
    );
    const totalRows = Object.values(rowSets).reduce(
      (sum, rows) => sum + rows.length,
      0,
    );
    const runCount = (rowSets.Experiment_Run || []).length;
    const hardwareCount = (rowSets.Hardware_System || []).length;
    const metricCount = (rowSets.Result_Metric || []).length;
    $("homeSummary").innerHTML =
      `<div class="stat"><div class="stat-label">Tables</div><div class="stat-value">${tables.length}</div><div class="stat-detail">schema entities</div></div><div class="stat"><div class="stat-label">Total rows</div><div class="stat-value">${totalRows}</div><div class="stat-detail">loaded records</div></div><div class="stat"><div class="stat-label">Experiment runs</div><div class="stat-value">${runCount}</div><div class="stat-detail">benchmark trials</div></div><div class="stat"><div class="stat-label">Metrics</div><div class="stat-value">${metricCount}</div><div class="stat-detail">recorded results</div></div>`;
    $("tableOverview").innerHTML = tables
      .map(
        (table) =>
          `<div class="home-row"><strong>${table.replaceAll("_", " ")}</strong><span>${(rowSets[table] || []).length} row(s)</span></div>`,
      )
      .join("");
    const recent = (rowSets.Experiment_Run || [])
      .slice()
      .sort((a, b) =>
        String(b.run_datetime || "").localeCompare(
          String(a.run_datetime || ""),
        ),
      )
      .slice(0, 6);
    $("recentRuns").innerHTML = recent.length
      ? recent
          .map(
            (r) =>
              `<div class="home-row"><strong>${escapeHtml(r.run_name || r.run_id)}</strong><span>${escapeHtml(r.run_status || "")} ${escapeHtml(r.run_datetime || "")}</span></div>`,
          )
          .join("")
      : '<div class="empty">No experiment runs yet.</div>';
  } catch (err) {
    toast("Home summary failed: " + err.message);
  }
}
function renderTableNav() {
  $("tableNav").innerHTML = tables
    .map((t) => `<button data-table="${t}">${t.replaceAll("_", " ")}</button>`)
    .join("");
  $("tableNav")
    .querySelectorAll("button")
    .forEach((b) => (b.onclick = () => selectTable(b.dataset.table)));
}
function renderSavedQueries() {
  $("savedQueryNav").innerHTML = Object.entries(queryLabels)
    .map(
      ([id, label]) =>
        `<button class="btn" data-q="${id}">Query ${id}: ${label}</button>`,
    )
    .join("");
  $("savedQueryNav")
    .querySelectorAll("button")
    .forEach((b) => (b.onclick = () => runSavedQuery(b.dataset.q)));
}
async function selectTable(table) {
  try {
    currentTable = table;
    editingRow = null;
    document
      .querySelectorAll("#tableNav button")
      .forEach((b) => b.classList.toggle("on", b.dataset.table === table));
    $("tableTitle").textContent = table.replaceAll("_", " ");
    $("tableHint").textContent =
      `Editing ${table}. Cascading deletes follow schema.sql foreign keys.`;
    currentSchema = await api("/api/schema?table=" + encodeURIComponent(table));
    await loadRows();
    renderForm();
  } catch (err) {
    toast(err.message);
  }
}
async function loadRows() {
  const data = await api("/api/rows?table=" + encodeURIComponent(currentTable));
  currentRows = data.rows;
  renderRows(data.columns, data.rows);
}
function renderRows(columns, rows) {
  renderTableSummary(columns, rows);
  if (!rows.length) {
    $("rows").innerHTML =
      '<div class="empty">No rows yet. Use the form on the right to add one.</div>';
    return;
  }
  const head =
    "<tr>" +
    columns.map((c) => `<th>${c}</th>`).join("") +
    "<th>Actions</th></tr>";
  const body = rows
    .map(
      (r, i) =>
        "<tr>" +
        columns.map((c) => `<td>${formatCell(r[c])}</td>`).join("") +
        `<td><div class="row-actions"><button class="btn small" onclick="editRow(${i})">Edit</button><button class="btn small danger" onclick="deleteRow(${i})">Delete</button></div></td></tr>`,
    )
    .join("");
  $("rows").innerHTML =
    `<table><thead>${head}</thead><tbody>${body}</tbody></table>`;
}
function renderTableSummary(columns, rows) {
  if (!currentSchema) return;
  const pk = currentSchema.primaryKeys.length
    ? currentSchema.primaryKeys.join(", ")
    : "None";
  const nullable = currentSchema.columns.filter((c) => c.nullable).length;
  const enumCount = currentSchema.columns.filter(
    (c) => c.enumValues.length,
  ).length;
  $("tableSummary").innerHTML =
    `<div class="stat"><div class="stat-label">Rows</div><div class="stat-value">${rows.length}</div><div class="stat-detail">loaded from ${currentTable}</div></div><div class="stat"><div class="stat-label">Columns</div><div class="stat-value">${columns.length}</div><div class="stat-detail">${nullable} nullable field(s)</div></div><div class="stat"><div class="stat-label">Primary key</div><div class="stat-value">${currentSchema.primaryKeys.length}</div><div class="stat-detail">${escapeHtml(pk)}</div></div><div class="stat"><div class="stat-label">Enum fields</div><div class="stat-value">${enumCount}</div><div class="stat-detail">controlled values</div></div>`;
  $("schemaProfile").innerHTML =
    currentSchema.columns
      .slice(0, 8)
      .map(
        (c) =>
          `<div class="schema-row"><strong>${c.name}</strong><span>${c.type}${c.primaryKey ? " / PK" : ""}</span></div>`,
      )
      .join("") +
    (currentSchema.columns.length > 8
      ? `<div class="hint">${currentSchema.columns.length - 8} more field(s)</div>`
      : "");
  const interesting = currentSchema.columns
    .filter(
      (c) =>
        c.primaryKey ||
        /name|status|type|metric|value|created|run|model|system/i.test(c.name),
    )
    .slice(0, 12);
  $("keyFields").innerHTML =
    interesting.map((c) => `<span class="chip">${c.name}</span>`).join("") ||
    '<span class="hint">No key fields detected.</span>';
}
function formatCell(v) {
  return v === null || v === undefined
    ? '<span class="hint">NULL</span>'
    : String(v).replace(
        /[&<>]/g,
        (s) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;" })[s],
      );
}
function renderForm(row = null) {
  $("formTitle").textContent = row ? "Update row" : "Add row";
  $("saveBtn").textContent = row ? "Update row" : "Add row";
  $("fields").innerHTML = currentSchema.columns
    .map((col) => {
      const val = row && row[col.name] != null ? row[col.name] : "";
      const pk = col.primaryKey ? '<span class="pk">primary key</span>' : "";
      const isLong = col.size > 60 || /notes|description/i.test(col.name);
      const input = col.enumValues.length
        ? `<select name="${col.name}"><option value=""></option>${col.enumValues.map((v) => `<option ${String(val) === v ? "selected" : ""}>${v}</option>`).join("")}</select>`
        : isLong
          ? `<textarea name="${col.name}">${escapeHtml(val)}</textarea>`
          : `<input name="${col.name}" value="${escapeHtml(val)}" placeholder="${col.type}${col.nullable ? " / nullable" : ""}">`;
      return `<div class="field ${isLong ? "full" : ""}"><label>${col.name}${pk}</label>${input}</div>`;
    })
    .join("");
}
function escapeHtml(v) {
  return String(v ?? "").replace(
    /[&<>'"]/g,
    (s) =>
      ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[
        s
      ],
  );
}
function editRow(i) {
  editingRow = currentRows[i];
  renderForm(editingRow);
  window.scrollTo({ top: 0, behavior: "smooth" });
}
async function deleteRow(i) {
  try {
    const row = currentRows[i];
    if (!confirm("Delete this row from " + currentTable + "?")) return;
    const body = {};
    currentSchema.primaryKeys.forEach((k) => (body["pk_" + k] = row[k]));
    await api("/api/row?table=" + encodeURIComponent(currentTable), {
      method: "DELETE",
      body: params(body),
    });
    toast("Row deleted");
    editingRow = null;
    await loadRows();
    renderForm();
  } catch (err) {
    toast(err.message);
  }
}
$("rowForm").onsubmit = async (e) => {
  e.preventDefault();
  try {
    const fd = new FormData(e.currentTarget);
    const body = {};
    for (const [k, v] of fd.entries()) body[k] = v;
    let method = "POST";
    if (editingRow) {
      method = "PUT";
      currentSchema.primaryKeys.forEach(
        (k) => (body["pk_" + k] = editingRow[k]),
      );
    }
    await api("/api/row?table=" + encodeURIComponent(currentTable), {
      method,
      body: params(body),
    });
    toast(editingRow ? "Row updated" : "Row added");
    editingRow = null;
    await loadRows();
    renderForm();
  } catch (err) {
    toast(err.message);
  }
};
$("clearBtn").onclick = () => {
  editingRow = null;
  renderForm();
};
$("refreshBtn").onclick = () =>
  loadRows()
    .then(() => toast("Table refreshed"))
    .catch((err) => toast(err.message));
$("exportJsonBtn").onclick = () => exportJson();
$("exportCsvBtn").onclick = () => exportCsv();
$("importJsonBtn").onclick = () => $("importJsonFile").click();
$("importJsonFile").onchange = (e) => importJsonFile(e.target.files[0]);
function exportJson() {
  const payload = {
    table: currentTable,
    exported_at: new Date().toISOString(),
    rows: currentRows,
  };
  download(
    `${currentTable}.json`,
    JSON.stringify(payload, null, 2),
    "application/json",
  );
  toast("JSON exported");
}
function exportCsv() {
  const cols = currentSchema.columns.map((c) => c.name);
  const lines = [
    cols.join(","),
    ...currentRows.map((row) => cols.map((c) => csvCell(row[c])).join(",")),
  ];
  download(`${currentTable}.csv`, lines.join("\n"), "text/csv");
  toast("CSV exported");
}
function csvCell(value) {
  if (value === null || value === undefined) return "";
  const text = String(value);
  return /[",\n\r]/.test(text) ? '"' + text.replaceAll('"', '""') + '"' : text;
}
function download(filename, text, type) {
  const blob = new Blob([text], { type: type + ";charset=utf-8" });
  const a = document.createElement("a");
  a.href = URL.createObjectURL(blob);
  a.download = filename;
  a.click();
  URL.revokeObjectURL(a.href);
}
async function importJsonFile(file) {
  if (!file) return;
  try {
    const text = await file.text();
    const parsed = JSON.parse(text);
    const rows = Array.isArray(parsed) ? parsed : parsed.rows;
    if (!Array.isArray(rows))
      throw new Error(
        "JSON must be an array of rows or an object with a rows array.",
      );
    if (
      parsed.table &&
      parsed.table !== currentTable &&
      !confirm(
        `This file says it is for ${parsed.table}. Import into ${currentTable} anyway?`,
      )
    )
      return;
    let count = 0;
    for (const row of rows) {
      const body = {};
      currentSchema.columns.forEach((col) => {
        if (Object.prototype.hasOwnProperty.call(row, col.name))
          body[col.name] = row[col.name];
      });
      await api("/api/row?table=" + encodeURIComponent(currentTable), {
        method: "POST",
        body: params(body),
      });
      count++;
    }
    await loadRows();
    toast(`Imported ${count} row(s)`);
  } catch (err) {
    toast("Import failed: " + err.message);
  } finally {
    $("importJsonFile").value = "";
  }
}
async function runSavedQuery(id) {
  try {
    $("queryHint").textContent = "Query " + id + ": " + queryLabels[id];
    $("queryResult").innerHTML = '<div class="empty">Running query...</div>';
    const data = await api("/api/query?id=" + id);
    renderQueryResult(data.columns, data.rows);
  } catch (err) {
    renderQueryError(err);
  }
}
$("runCustomBtn").onclick = async () => {
  try {
    const sql = $("customSql").value.trim();
    $("queryHint").textContent = "Running custom query...";
    $("queryResult").innerHTML = '<div class="empty">Running query...</div>';
    const data = await api("/api/custom-query", {
      method: "POST",
      body: params({ sql }),
    });
    $("queryHint").textContent =
      "Custom query returned " + data.rows.length + " row(s).";
    renderQueryResult(data.columns, data.rows);
  } catch (err) {
    renderQueryError(err);
  }
};
function renderQueryError(err) {
  $("queryHint").textContent = "Query failed.";
  $("queryResult").innerHTML =
    `<div class="error">${escapeHtml(err.message)}</div>`;
  toast("Query failed");
}
function renderQueryResult(columns, rows) {
  if (!rows.length) {
    $("queryResult").innerHTML =
      '<div class="empty">Query ran successfully, but returned no rows.</div>';
    return;
  }
  const head = "<tr>" + columns.map((c) => `<th>${c}</th>`).join("") + "</tr>";
  const body = rows
    .map(
      (r) =>
        "<tr>" +
        columns.map((c) => `<td>${formatCell(r[c])}</td>`).join("") +
        "</tr>",
    )
    .join("");
  $("queryResult").innerHTML =
    `<p class="result-title"><span class="badge">${rows.length} row(s)</span></p><div class="table-wrap"><table><thead>${head}</thead><tbody>${body}</tbody></table></div>`;
}
function showView(view) {
  const tableMode = view === "tables";
  document
    .querySelectorAll(".tab")
    .forEach((t) => t.classList.toggle("on", t.dataset.view === view));
  document.querySelector(".side").style.display = tableMode ? "block" : "none";
  $("layout").style.gridTemplateColumns = tableMode ? "250px 1fr" : "1fr";
  $("view-home").style.display = view === "home" ? "block" : "none";
  $("view-tables").style.display = tableMode ? "block" : "none";
  $("view-queries").style.display = view === "queries" ? "block" : "none";
  if (view === "home") renderHome();
}
document
  .querySelectorAll(".tab")
  .forEach((t) => (t.onclick = () => showView(t.dataset.view)));
init();
