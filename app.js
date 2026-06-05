const STORAGE_KEY = "position-risk-widget:stepper-v1";
const MARGIN_RATE = 0.02;
const RISK_RATE = 0.01;
const STOP_MIN = 0.5;
const STOP_MAX = 15;
const STOP_STEP = 0.1;

const defaults = {
  accountTotal: "100",
  stopLoss: "4.8",
};

const els = {
  accountTotal: document.getElementById("accountTotal"),
  decreaseStop: document.getElementById("decreaseStop"),
  increaseStop: document.getElementById("increaseStop"),
  stopLossReadout: document.getElementById("stopLossReadout"),
  primaryResult: document.getElementById("primaryResult"),
  theoryLine: document.getElementById("theoryLine"),
  marginResult: document.getElementById("marginResult"),
  offlineStatus: document.getElementById("offlineStatus"),
};

let state = loadState();

function loadState() {
  try {
    const saved = JSON.parse(localStorage.getItem(STORAGE_KEY) || "{}");
    return { ...defaults, ...saved };
  } catch {
    return { ...defaults };
  }
}

function saveState() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
}

function clamp(value, min, max) {
  return Math.min(Math.max(value, min), max);
}

function parsePositive(value) {
  const number = Number.parseFloat(value);
  return Number.isFinite(number) && number > 0 ? number : null;
}

function normalizeStopLoss(value) {
  const parsed = parsePositive(value);
  const safeValue = parsed ?? Number.parseFloat(defaults.stopLoss);
  return clamp(Math.round(safeValue * 10) / 10, STOP_MIN, STOP_MAX).toFixed(1);
}

function formatMoney(value) {
  return `${value.toFixed(2)}U`;
}

function formatPercent(value) {
  return `${value.toFixed(1)}%`;
}

function formatLeverage(value) {
  return value.toFixed(2);
}

function calculatePosition(input) {
  const accountTotal = parsePositive(input.accountTotal);
  const stopLoss = parsePositive(input.stopLoss);

  if (!accountTotal) {
    return { ok: false, message: "請輸入大於 0 的總資金" };
  }

  if (!stopLoss) {
    return { ok: false, message: "請輸入大於 0 的止損距離" };
  }

  const margin = accountTotal * MARGIN_RATE;
  const targetLoss = accountTotal * RISK_RATE;
  const theoreticalLeverage = targetLoss / (margin * (stopLoss / 100));
  const suggestedLeverage = Math.floor(theoreticalLeverage + Number.EPSILON);

  return {
    ok: true,
    margin,
    stopLoss,
    theoreticalLeverage,
    suggestedLeverage,
  };
}

function syncInputsFromState() {
  els.accountTotal.value = state.accountTotal;
  state.stopLoss = normalizeStopLoss(state.stopLoss);
}

function syncStateFromInputs() {
  state.accountTotal = els.accountTotal.value;
}

function renderInvalid(message) {
  els.primaryResult.textContent = "未完成";
  els.primaryResult.classList.add("warning");
  els.theoryLine.textContent = message;
  els.marginResult.textContent = "--";
}

function render() {
  syncStateFromInputs();
  saveState();

  const stopLossNumber = Number.parseFloat(state.stopLoss);
  els.stopLossReadout.textContent = Number.isFinite(stopLossNumber) ? formatPercent(stopLossNumber) : "--";
  els.decreaseStop.disabled = stopLossNumber <= STOP_MIN;
  els.increaseStop.disabled = stopLossNumber >= STOP_MAX;

  const result = calculatePosition(state);

  if (!result.ok) {
    renderInvalid(result.message);
    return;
  }

  els.primaryResult.classList.remove("warning");
  els.marginResult.textContent = formatMoney(result.margin);

  if (result.suggestedLeverage < 1) {
    els.primaryResult.textContent = "低於1x";
    els.primaryResult.classList.add("warning");
    els.theoryLine.textContent = `理論 ${formatLeverage(result.theoreticalLeverage)}x`;
    return;
  }

  els.primaryResult.textContent = `${result.suggestedLeverage}x`;
  els.theoryLine.textContent = `理論 ${formatLeverage(result.theoreticalLeverage)}x → 設 ${result.suggestedLeverage}x`;
}

function adjustStopLoss(delta) {
  const current = Number.parseFloat(normalizeStopLoss(state.stopLoss));
  state.stopLoss = normalizeStopLoss(current + delta);
  render();
}

function bindRepeatingButton(button, delta) {
  let repeatTimer = null;
  let repeatDelay = null;

  function stopRepeating() {
    window.clearTimeout(repeatDelay);
    window.clearInterval(repeatTimer);
    repeatDelay = null;
    repeatTimer = null;
  }

  button.addEventListener("click", () => adjustStopLoss(delta));
  button.addEventListener("pointerdown", () => {
    stopRepeating();
    repeatDelay = window.setTimeout(() => {
      repeatTimer = window.setInterval(() => adjustStopLoss(delta), 90);
    }, 360);
  });
  button.addEventListener("pointerup", stopRepeating);
  button.addEventListener("pointerleave", stopRepeating);
  button.addEventListener("pointercancel", stopRepeating);
}

function updateNetworkStatus() {
  els.offlineStatus.textContent = navigator.onLine ? "本機" : "離線";
}

els.accountTotal.addEventListener("input", render);
bindRepeatingButton(els.decreaseStop, -STOP_STEP);
bindRepeatingButton(els.increaseStop, STOP_STEP);
window.addEventListener("online", updateNetworkStatus);
window.addEventListener("offline", updateNetworkStatus);

syncInputsFromState();
updateNetworkStatus();
render();

if ("serviceWorker" in navigator) {
  window.addEventListener("load", () => {
    navigator.serviceWorker.register("sw.js").catch((error) => {
      console.warn("Service worker registration failed", error);
    });
  });
}

window.calculateRiskPosition = calculatePosition;
