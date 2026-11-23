const apps = [
  ["浏览器", "browser", "liulanqi", "常用工具"],
  ["图库", "photos", "tuku", "影音媒体"],
  ["相机", "camera", "xiangji", "影音媒体"],
  ["备忘录", "notes", "beiwanglu", "效率办公"],
  ["日历", "calendar", "rili", "效率办公"],
  ["文件管理", "files", "wenjianguanli", "效率办公"],
  ["设置", "settings", "shezhi", "系统服务"],
  ["设备信息", "android", "shebeixinxi", "系统服务"],
  ["查找设备", "find", "chazhaoshebei", "系统服务"],
  ["电子邮件", "mail", "dianziyoujian", "效率办公"],
  ["飞书", "feishu", "feishu", "效率办公"],
  ["换机克隆", "clone", "huanjikelong", "系统服务"],
  ["系统更新", "update", "xitonggengxin", "系统服务"],
  ["天气", "weather", "tianqi", "常用工具"],
  ["股市", "stock", "gushi", "常用工具"],
  ["应用文件夹", "folder", "yingyongwenjianjia", "常用工具"],
  ["主题", "theme", "zhuti", "系统服务"],
  ["云空间", "cloud", "yunkongjian", "系统服务"],
  ["视频", "video", "shipin", "影音媒体"],
  ["音乐", "music", "yinyue", "影音媒体"],
  ["应用市场", "market", "yingyongshichang", "系统服务"],
  ["计算器", "calculator", "jisuanqi", "常用工具"],
].map(([name, icon, pinyin, category]) => ({ name, icon, pinyin, category }));

const sortedApps = [...apps].sort((a, b) => a.pinyin.localeCompare(b.pinyin));
const recentApps = ["browser", "files", "notes", "photos", "weather", "settings"]
  .map((icon) => apps.find((app) => app.icon === icon));
const categories = ["全部应用", "效率办公", "影音媒体", "常用工具", "系统服务"];
const ranges = [["A", "H"], ["I", "S"], ["T", "Z"]];

const params = new URLSearchParams(location.search);
const screen = document.querySelector(".screen");
const frame = document.querySelector(".frame");
const stage = document.querySelector(".stage");
const controls = document.querySelector(".preview-controls");
const drawer = document.querySelector(".drawer-body");
const search = document.querySelector(".search input");
const modeButtons = [...controls.querySelectorAll("[data-mode]")];
const directionButtons = [...controls.querySelectorAll("[data-direction]")];

let mode = modeButtons.some((button) => button.dataset.mode === params.get("mode"))
  ? params.get("mode") : "index";
let direction = params.get("direction") === "portrait" ? "portrait" : "landscape";
let category = categories[0];

document.body.classList.toggle("capture", params.get("capture") === "1");

function grid(items, className = "") {
  return `<div class="app-grid ${className}">${items.map(({ name, icon, pinyin }) => `
    <div class="app" data-initial="${pinyin[0].toUpperCase()}" title="${name}">
      <img src="assets/${icon}.png" alt="" draggable="false">
      <span class="app-name">${name}</span>
    </div>`).join("")}</div>`;
}

function heading(label, count, className = "") {
  const total = count === undefined ? "" : `<small>${count} 个应用</small>`;
  return `<h2 class="section-title ${className}"><span>${label}</span>${total}</h2>`;
}

function indexLayout() {
  const sections = ranges.map(([first, last]) => {
    const items = sortedApps.filter(({ pinyin }) => {
      const initial = pinyin[0].toUpperCase();
      return initial >= first && initial <= last;
    });
    return `<section class="index-section">${heading(`${first}–${last}`)}${grid(items)}</section>`;
  }).join("");
  const initials = [...new Set(sortedApps.map(({ pinyin }) => pinyin[0].toUpperCase()))];
  const buttons = initials.map((initial) =>
    `<button type="button" data-initial="${initial}" aria-label="跳到 ${initial}">${initial}</button>`
  ).join("");
  return `<div class="collection index-collection"><div class="scroll">${sections}</div></div>
    <nav class="alphabet" aria-label="拼音首字母索引">${buttons}</nav>`;
}

function recentLayout() {
  return `<div class="collection recent-collection"><div class="scroll">
    <section class="recent-block">${heading("最近打开")}${grid(recentApps, "recent-grid")}</section>
    ${heading("应用列表", apps.length, "all-heading")}${grid(sortedApps)}
    </div></div>`;
}

function categoryLayout() {
  const buttons = categories.map((name) =>
    `<button type="button" data-category="${name}" aria-pressed="${name === category}">
      <i aria-hidden="true"></i>${name}
    </button>`
  ).join("");
  const items = category === categories[0] ? sortedApps : sortedApps.filter((app) => app.category === category);
  return `<nav class="category-nav" aria-label="应用分类">${buttons}</nav>
    <div class="collection category-collection"><div class="scroll">
      ${heading(category === categories[0] ? "应用列表" : category, items.length)}${grid(items)}
    </div></div>`;
}

function renderDrawer() {
  const query = search.value.trim().toLowerCase();
  drawer.className = "drawer-body";
  if (query) {
    const results = sortedApps.filter(({ name, pinyin }) => name.includes(query) || pinyin.includes(query));
    const content = results.length ? grid(results) : '<p class="empty">没有找到匹配的应用</p>';
    drawer.innerHTML = `<div class="collection"><div class="scroll">
      ${heading("搜索结果", results.length)}${content}
    </div></div>`;
    return;
  }

  if (mode === "index") {
    drawer.classList.add("index-layout");
    drawer.innerHTML = indexLayout();
    drawer.querySelector(".alphabet button")?.setAttribute("aria-current", "true");
  } else if (mode === "recent") {
    drawer.innerHTML = recentLayout();
  } else {
    drawer.classList.add("category-layout");
    drawer.innerHTML = categoryLayout();
  }
}

function fitPreview() {
  const [width, height] = direction === "portrait" ? [600, 1000] : [1000, 600];
  const padding = getComputedStyle(stage);
  const availableWidth = stage.clientWidth - parseFloat(padding.paddingLeft) - parseFloat(padding.paddingRight);
  const scale = Math.min(1, availableWidth / width);
  frame.style.width = `${width * scale}px`;
  frame.style.height = `${height * scale}px`;
  screen.style.transform = `scale(${scale})`;
}

function updatePreview() {
  screen.dataset.mode = mode;
  screen.dataset.direction = direction;
  modeButtons.forEach((button) => button.setAttribute("aria-pressed", button.dataset.mode === mode));
  directionButtons.forEach((button) => button.setAttribute("aria-pressed", button.dataset.direction === direction));
  renderDrawer();
  fitPreview();
}

modeButtons.forEach((button) => button.addEventListener("click", () => {
  if (mode === button.dataset.mode) return;
  mode = button.dataset.mode;
  category = categories[0];
  search.value = "";
  updatePreview();
}));
directionButtons.forEach((button) => button.addEventListener("click", () => {
  direction = button.dataset.direction;
  updatePreview();
}));
search.addEventListener("input", renderDrawer);
drawer.addEventListener("click", (event) => {
  const button = event.target.closest("button");
  if (!button) return;

  if (button.dataset.category) {
    category = button.dataset.category;
    renderDrawer();
    [...drawer.querySelectorAll(".category-nav button")]
      .find((item) => item.dataset.category === category)?.focus({ preventScroll: true });
    return;
  }

  if (button.dataset.initial) {
    const first = drawer.querySelector(`.app[data-initial="${button.dataset.initial}"]`);
    const scroll = drawer.querySelector(".scroll");
    const top = first.offsetTop - scroll.offsetTop;
    scroll.scrollTo({ top });
    drawer.querySelector(".alphabet [aria-current]")?.removeAttribute("aria-current");
    button.setAttribute("aria-current", "true");
  }
});
window.addEventListener("resize", fitPreview);
updatePreview();
