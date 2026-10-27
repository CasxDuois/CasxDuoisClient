// =============================================================================
// CasxDuoisMC 1.0 — Application Controller
// =============================================================================

const API_BASE = window.location.origin;

let accounts = [];

// DOM Elements
const txtUsername = document.getElementById('txt-username');
const btnAddNick = document.getElementById('btn-add-nick');
const btnCreateBots = document.getElementById('btn-create-bots');
const sliderRam = document.getElementById('slider-ram');
const lblRamVal = document.getElementById('lbl-ram-val');
const chkAutoClose = document.getElementById('chk-auto-close');
const btnPlay = document.getElementById('btn-play');
const selectMode = document.getElementById('select-mode');
const selectAccounts = document.getElementById('select-accounts');
const btnDelAccount = document.getElementById('btn-del-account');
const btnOpenFolder = document.getElementById('btn-open-folder');
const btnLaunchAll = document.getElementById('btn-launch-all');
const lblStatus = document.getElementById('lbl-status');
const btnMin = document.getElementById('btn-min');
const btnClose = document.getElementById('btn-close');

// 1. Initial Load
document.addEventListener('DOMContentLoaded', () => {
  loadAccounts();
  bindEvents();
});

function bindEvents() {
  // RAM Slider live update
  sliderRam.addEventListener('input', () => {
    lblRamVal.textContent = `${sliderRam.value} MB`;
  });

  // Enter to add account
  txtUsername.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') {
      doAddNick();
    }
  });

  // Add Button
  if (btnAddNick) btnAddNick.addEventListener('click', doAddNick);

  // 5 Bots Button (if present)
  if (btnCreateBots) btnCreateBots.addEventListener('click', doCreateBots);

  // Select account dropdown changed
  if (selectAccounts) {
    selectAccounts.addEventListener('change', () => {
      if (selectAccounts.value) {
        txtUsername.value = selectAccounts.value;
      }
    });
  }

  // Delete account button
  if (btnDelAccount) btnDelAccount.addEventListener('click', doDeleteAccount);

  // Mode selection change
  if (selectMode) {
    selectMode.addEventListener('change', () => {
      const playText = btnPlay.querySelector('.play-text');
      if (playText) playText.textContent = 'Chơi ngay';
    });
  }

  // Play button
  if (btnPlay) btnPlay.addEventListener('click', doPlay);

  // Launch all accounts button (if present)
  if (btnLaunchAll) btnLaunchAll.addEventListener('click', doLaunchAll);

  // Open game folder button
  if (btnOpenFolder) {
    btnOpenFolder.addEventListener('click', () => {
      fetch(`${API_BASE}/api/open-folder`).catch(() => {});
    });
  }

  // Window Controls
  btnMin.addEventListener('click', () => {
    fetch(`${API_BASE}/api/window/minimize`).catch(() => {});
  });

  btnClose.addEventListener('click', () => {
    fetch(`${API_BASE}/api/window/close`).catch(() => {});
    window.close();
  });
}

// 2. Accounts API
async function loadAccounts() {
  try {
    const res = await fetch(`${API_BASE}/api/accounts`);
    const data = await res.json();
    accounts = data.accounts || ['CasxDuoisPlayer'];
    renderAccounts();
  } catch {
    accounts = ['CasxDuoisPlayer'];
    renderAccounts();
  }
}

function renderAccounts() {
  selectAccounts.innerHTML = '';
  if (accounts.length === 0) {
    selectAccounts.innerHTML = '<option value="">(Chưa có tài khoản)</option>';
    return;
  }

  accounts.forEach((acc) => {
    const opt = document.createElement('option');
    opt.value = acc;
    opt.textContent = `👤 ${acc}`;
    selectAccounts.appendChild(opt);
  });

  if (!txtUsername.value || !accounts.includes(txtUsername.value)) {
    txtUsername.value = accounts[0];
  }
  selectAccounts.value = txtUsername.value;
}

async function doAddNick() {
  const name = txtUsername.Text ? txtUsername.Text.trim() : txtUsername.value.trim();
  if (!name) {
    setStatus('Vui lòng nhập tên tài khoản!', 'danger');
    return;
  }

  if (!accounts.includes(name)) {
    accounts.push(name);
    await saveAccounts();
    renderAccounts();
    setStatus(`Đã thêm tài khoản: ${name}`, 'success');
  }
}

async function doCreateBots() {
  const seed = Math.floor(Math.random() * 90) + 10;
  for (let i = 1; i <= 5; i++) {
    const botName = `BotAFK_${seed}_${String(i).padStart(2, '0')}`;
    if (!accounts.includes(botName)) {
      accounts.push(botName);
    }
  }
  await saveAccounts();
  renderAccounts();
  setStatus('Đã tạo nhanh 5 tài khoản bot farm!', 'cyan');
}

async function doDeleteAccount() {
  const selected = selectAccounts.value;
  if (!selected) return;

  accounts = accounts.filter((a) => a !== selected);
  await saveAccounts();
  renderAccounts();
  setStatus(`Đã xóa tài khoản: ${selected}`, 'danger');
}

async function saveAccounts() {
  try {
    await fetch(`${API_BASE}/api/accounts`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ accounts }),
    });
  } catch (err) {
    console.error('Error saving accounts', err);
  }
}

// 3. Game Launch Actions
async function doPlay() {
  const nick = txtUsername.value.trim();
  if (!nick) {
    setStatus('Vui lòng nhập tên tài khoản trước khi chơi!', 'danger');
    return;
  }

  if (!accounts.includes(nick)) {
    accounts.push(nick);
    await saveAccounts();
    renderAccounts();
  }

  const ram = parseInt(sliderRam.value, 10) || 1024;
  const autoClose = chkAutoClose.checked;
  const mode = selectMode ? parseInt(selectMode.value, 10) : 0;
  const modeName = mode === 0 ? 'Mixin x Forge' : 'OptiFine';

  setStatus(`Đang mở Minecraft 1.12.2 (${modeName}) cho ${nick}...`, 'purple');
  btnPlay.disabled = true;
  btnPlay.style.opacity = '0.7';

  try {
    const res = await fetch(`${API_BASE}/api/launch`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: nick,
        ram: ram,
        mode: mode,
        autoClose: autoClose,
      }),
    });

    const data = await res.json();
    if (data.success) {
      setStatus(`✔ Đã mở game thành công [PID: ${data.pid}] (${modeName})!`, 'success');
      if (autoClose) {
        setTimeout(() => window.close(), 600);
      }
    } else {
      setStatus(`Lỗi: ${data.error}`, 'danger');
      btnPlay.disabled = false;
      btnPlay.style.opacity = '1';
    }
  } catch (err) {
    setStatus(`Lỗi kết nối khởi chạy: ${err.message}`, 'danger');
    btnPlay.disabled = false;
    btnPlay.style.opacity = '1';
  }
}

async function doLaunchAll() {
  if (accounts.length === 0) {
    setStatus('Chưa có tài khoản nào trong danh sách!', 'danger');
    return;
  }

  const ram = parseInt(sliderRam.value, 10) || 1024;
  const autoClose = chkAutoClose.checked;
  const mode = selectMode ? parseInt(selectMode.value, 10) : 0;
  const modeName = mode === 0 ? 'Mixin x Forge' : 'OptiFine';

  setStatus(`Đang mở ${accounts.length} tab game (${modeName}) cùng lúc...`, 'purple');

  try {
    const res = await fetch(`${API_BASE}/api/launch-all`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        accounts: accounts,
        ram: ram,
        mode: mode,
        autoClose: autoClose,
      }),
    });

    const data = await res.json();
    if (data.success) {
      setStatus(`✔ Đã mở toàn bộ ${accounts.length} tab thành công (${modeName})!`, 'success');
      if (autoClose) {
        setTimeout(() => window.close(), 1000);
      }
    }
  } catch (err) {
    setStatus(`Lỗi: ${err.message}`, 'danger');
  }
}

function setStatus(msg, type = 'success') {
  lblStatus.textContent = msg;
  if (type === 'danger') {
    lblStatus.style.color = '#ef4444';
  } else if (type === 'cyan' || type === 'silver') {
    lblStatus.style.color = '#cbd5e1';
  } else if (type === 'purple' || type === 'white') {
    lblStatus.style.color = '#ffffff';
  } else {
    lblStatus.style.color = '#22c55e';
  }
}

// Guide Modal Controller
const btnGuide = document.getElementById('btn-guide');
const guideModal = document.getElementById('guide-modal');
const btnCloseGuide = document.getElementById('btn-close-guide');

if (btnGuide && guideModal && btnCloseGuide) {
  btnGuide.addEventListener('click', () => {
    guideModal.style.display = 'flex';
  });

  btnCloseGuide.addEventListener('click', () => {
    guideModal.style.display = 'none';
  });

  guideModal.addEventListener('click', (e) => {
    if (e.target === guideModal) {
      guideModal.style.display = 'none';
    }
  });

  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && guideModal.style.display === 'flex') {
      guideModal.style.display = 'none';
    }
  });
}

