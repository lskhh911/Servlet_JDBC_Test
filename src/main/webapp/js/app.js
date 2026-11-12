"use strict";

/**
 * 接口地址使用相对路径，所以 WAR 部署在 /my-app 或 ROOT 下都能用。
 * 后端接口见 com.example.web.StudentServlet。
 */
const API_URL = "api/students";

let students = [];
let isLoading = true;
let loadError = "";
let keyword = "";
let editingId = null;
let saving = false;
let toastTimer = null;
let lastFocusedElement = null;

const studentTbody = document.getElementById("studentTbody");
const tableWrap = document.getElementById("tableWrap");
const emptyState = document.getElementById("emptyState");
const emptyTitle = document.getElementById("emptyTitle");
const emptyText = document.getElementById("emptyText");
const recordCount = document.getElementById("recordCount");
const searchInput = document.getElementById("searchInput");
const openAddBtn = document.getElementById("openAddBtn");
const modal = document.getElementById("studentModal");
const modalTitle = document.getElementById("modalTitle");
const closeModalBtn = document.getElementById("closeModalBtn");
const cancelBtn = document.getElementById("cancelBtn");
const studentForm = document.getElementById("studentForm");
const studentIdInput = document.getElementById("studentId");
const nameInput = document.getElementById("nameInput");
const gradeInput = document.getElementById("gradeInput");
const genderInput = document.getElementById("genderInput");
const scoreInput = document.getElementById("scoreInput");
const formError = document.getElementById("formError");
const saveBtn = document.getElementById("saveBtn");
const toast = document.getElementById("toast");

function escapeHtml(value) {
  return String(value).replace(/[&<>"']/g, character => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': "&quot;",
    "'": "&#39;"
  })[character]);
}

function errorMessage(error) {
  if (error instanceof TypeError) {
    return "无法连接后端，请确认 Tomcat 已启动，并且通过 http 地址访问页面。";
  }
  return error.message || "请求失败，请稍后重试。";
}

async function request(url, options = {}) {
  const config = {
    ...options,
    headers: {
      Accept: "application/json",
      ...(options.headers || {})
    }
  };

  const response = await fetch(url, config);
  let data = null;

  if (response.status !== 204) {
    const contentType = response.headers.get("content-type") || "";
    if (contentType.includes("application/json")) {
      data = await response.json();
    }
  }

  if (!response.ok) {
    throw new Error((data && data.message) || `请求失败（HTTP ${response.status}）`);
  }

  return data;
}

async function loadStudents() {
  isLoading = true;
  loadError = "";
  renderStudents();

  if (window.location.protocol === "file:") {
    isLoading = false;
    loadError = "不能直接双击打开 HTML。请先启动 Tomcat，再访问 http://localhost:8080/my-app/。";
    renderStudents();
    return;
  }

  try {
    const data = await request(API_URL);
    students = Array.isArray(data) ? data : [];
  } catch (error) {
    students = [];
    loadError = errorMessage(error);
    showToast(loadError, "error");
  } finally {
    isLoading = false;
    renderStudents();
  }
}

function getFilteredStudents() {
  const text = keyword.trim().toLowerCase();
  if (!text) {
    return students;
  }
  return students.filter(student =>
    String(student.name).toLowerCase().includes(text) ||
    String(student.grade).includes(text)
  );
}

function renderStudents() {
  if (isLoading) {
    tableWrap.hidden = false;
    emptyState.hidden = true;
    recordCount.textContent = "正在加载...";
    studentTbody.innerHTML = `
      <tr>
        <td class="loading-cell" colspan="5">
          <span class="spinner"></span>正在从数据库读取学生信息...
        </td>
      </tr>
    `;
    return;
  }

  if (loadError) {
    tableWrap.hidden = true;
    emptyState.hidden = false;
    emptyTitle.textContent = "数据加载失败";
    emptyText.textContent = loadError;
    recordCount.textContent = "加载失败";
    return;
  }

  const filteredStudents = getFilteredStudents();

  studentTbody.innerHTML = filteredStudents.map(student => {
    const isMale = Number(student.gender) === 1;
    const genderText = isMale ? "男" : "女";
    const genderClass = isMale ? "" : " female";
    const score = Number(student.score);
    const lowScoreClass = score < 60 ? " low" : "";

    return `
      <tr>
        <td>
          <div class="student">
            <span class="avatar${genderClass}">${escapeHtml(String(student.name).slice(0, 1))}</span>
            <span class="student-name">${escapeHtml(student.name)}</span>
          </div>
        </td>
        <td>${escapeHtml(student.grade)} 年级</td>
        <td><span class="gender${genderClass}">${genderText}</span></td>
        <td><span class="score-pill${lowScoreClass}">${score} 分</span></td>
        <td>
          <div class="actions">
            <button class="link-btn" type="button" data-action="edit" data-id="${student.id}">编辑</button>
            <button class="link-btn danger" type="button" data-action="delete" data-id="${student.id}">删除</button>
          </div>
        </td>
      </tr>
    `;
  }).join("");

  tableWrap.hidden = filteredStudents.length === 0;
  emptyState.hidden = filteredStudents.length !== 0;

  if (filteredStudents.length === 0) {
    emptyTitle.textContent = "没有找到学生";
    emptyText.textContent = "换个关键词试试，或者点击右上角新增一位。";
  }

  recordCount.textContent = keyword.trim()
    ? `找到 ${filteredStudents.length} 名学生`
    : `共 ${students.length} 名学生`;
}

function showToast(message, type = "success") {
  window.clearTimeout(toastTimer);
  toast.textContent = message;
  toast.className = `toast show${type === "error" ? " error" : ""}`;
  toastTimer = window.setTimeout(() => {
    toast.className = "toast";
  }, 2600);
}

function openModal(student) {
  lastFocusedElement = document.activeElement;
  formError.textContent = "";

  if (student) {
    editingId = student.id;
    modalTitle.textContent = "编辑学生";
    studentIdInput.value = student.id;
    nameInput.value = student.name;
    gradeInput.value = String(student.grade);
    genderInput.value = String(student.gender);
    scoreInput.value = student.score;
  } else {
    editingId = null;
    modalTitle.textContent = "新增学生";
    studentForm.reset();
    studentIdInput.value = "";
    gradeInput.value = "1";
    genderInput.value = "1";
    scoreInput.value = "";
  }

  modal.classList.add("open");
  modal.setAttribute("aria-hidden", "false");
  document.body.style.overflow = "hidden";
  window.setTimeout(() => nameInput.focus(), 30);
}

function closeModal(force = false) {
  if (saving && !force) {
    return;
  }
  modal.classList.remove("open");
  modal.setAttribute("aria-hidden", "true");
  document.body.style.overflow = "";
  editingId = null;

  if (lastFocusedElement instanceof HTMLElement) {
    lastFocusedElement.focus();
  }
}

function setSaving(value) {
  saving = value;
  saveBtn.disabled = value;
  saveBtn.textContent = value ? "保存中..." : "保存";
}

async function handleSubmit(event) {
  event.preventDefault();

  if (saving) {
    return;
  }

  const name = nameInput.value.trim();
  const grade = Number(gradeInput.value);
  const gender = Number(genderInput.value);
  const scoreText = scoreInput.value.trim();
  const score = Number(scoreText);

  if (!name) {
    formError.textContent = "请输入学生姓名。";
    nameInput.focus();
    return;
  }

  if (name.length > 50) {
    formError.textContent = "姓名不能超过 50 个字符。";
    nameInput.focus();
    return;
  }

  if (!scoreText || !Number.isInteger(score) || score < 0 || score > 100) {
    formError.textContent = "成绩必须是 0 到 100 之间的整数。";
    scoreInput.focus();
    return;
  }

  const isEditing = editingId !== null;
  const payload = { name, grade, gender, score };
  formError.textContent = "";
  setSaving(true);

  try {
    await request(isEditing ? `${API_URL}/${editingId}` : API_URL, {
      method: isEditing ? "PUT" : "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });

    closeModal(true);
    await loadStudents();
    if (!loadError) {
      showToast(isEditing ? "学生信息已更新" : "已新增一名学生");
    }
  } catch (error) {
    const message = errorMessage(error);
    formError.textContent = message;
    showToast(message, "error");
  } finally {
    setSaving(false);
  }
}

function findStudent(id) {
  return students.find(student => student.id === id);
}

async function deleteStudent(student) {
  if (!window.confirm(`确定要删除「${student.name}」吗？此操作会同步删除数据库记录。`)) {
    return;
  }

  try {
    await request(`${API_URL}/${student.id}`, { method: "DELETE" });
    await loadStudents();
    if (!loadError) {
      showToast("学生已删除");
    }
  } catch (error) {
    showToast(errorMessage(error), "error");
  }
}

openAddBtn.addEventListener("click", () => openModal(null));

closeModalBtn.addEventListener("click", closeModal);
cancelBtn.addEventListener("click", closeModal);
studentForm.addEventListener("submit", handleSubmit);

document.querySelector(".modal-mask").addEventListener("click", closeModal);

document.addEventListener("keydown", event => {
  if (event.key === "Escape" && modal.classList.contains("open")) {
    closeModal();
  }
});

studentTbody.addEventListener("click", event => {
  const button = event.target.closest("button[data-action]");
  if (!button) {
    return;
  }

  const id = Number(button.dataset.id);
  const student = findStudent(id);

  if (!student) {
    return;
  }

  if (button.dataset.action === "edit") {
    openModal(student);
  }

  if (button.dataset.action === "delete") {
    deleteStudent(student);
  }
});

searchInput.addEventListener("input", event => {
  keyword = event.target.value;
  renderStudents();
});

loadStudents();
