'use strict';
(() => {
  const $ = (s, root = document) => root.querySelector(s);
  const $$ = (s, root = document) => [...root.querySelectorAll(s)];
  const esc = v => String(v).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const storage = {
    get(k, fallback) { try { return JSON.parse(localStorage.getItem('easylauncher.v3.' + k)) ?? fallback; } catch { return fallback; } },
    set(k, v) { try { localStorage.setItem('easylauncher.v3.' + k, JSON.stringify(v)); } catch { /* In-memory fallback. */ } }
  };
  const designs = [
    {id:'cloud',name:'云山',subtitle:'经典图标桌面',layout:'classic',count:14,color:'#75a9c3',desc:'最熟悉的平板主屏。上方天气和日历，下方规整图标，底部保留常用应用。壁纸负责氛围，操作一眼就懂。',tags:['均衡布局','双小组件','常驻栏'],focus:'小组件 + 两排图标',use:'日常使用，容易上手',rotation:'横屏图标 7 列、组件并排；竖屏图标 4 列、组件缩为两张卡片。常驻栏始终在底部。',reflow:'7 列 → 4 列'},
    {id:'breeze',name:'光屿',subtitle:'组件增强桌面',layout:'widgets',count:16,color:'#a8c6bd',desc:'天气、日程、音乐各有一个清楚的位置。原生版分别接入天气服务、授权后的系统日历、授权后的媒体会话；当前 H5 使用本地示例。未授权或无数据时保留快捷入口。',tags:['天气 API','系统日历','媒体会话'],focus:'三张组件 + 紧凑图标',use:'经常查看天气、日程和音乐',rotation:'横屏三张组件并排、图标 8 列；竖屏组件组成一大两小，图标改为 4 列。',reflow:'8 列 → 4 列'},
    {id:'quiet',name:'静界',subtitle:'疏朗大留白',layout:'minimal',count:12,color:'#c4a9b6',desc:'留出完整的壁纸和呼吸感。中间是时间，下面只放常用应用，完整应用库随时可开。减少拥挤，但保留完整的桌面操作。',tags:['居中时钟','少量常用','低信息密度'],focus:'居中时钟 + 疏朗图标',use:'阅读、影音，喜欢干净主屏',rotation:'横屏时钟与两排 6 列图标居中；竖屏保持视觉重心，图标变为 4 列。',reflow:'6 列 → 4 列'},
    {id:'studio',name:'书桌',subtitle:'横屏效率桌面',layout:'work',count:12,color:'#94a9bc',desc:'为横屏办公多留一点工具空间。应用网格旁是日程与最近文件，常用工具靠近左手；切换工作桌面即可优先显示办公应用。',tags:['侧边常驻栏','工作空间','最近文件'],focus:'应用网格 + 右侧信息栏',use:'横屏办公、键盘与触控混用',rotation:'横屏常驻栏在左、信息在右；竖屏常驻栏移到底部，日程与文件并排移到应用上方。',reflow:'左栏 / 6 列 / 右栏 → 底栏 / 4 列'},
    {id:'metro',name:'星格',subtitle:'高密度应用桌面',layout:'dense',count:24,color:'#566681',desc:'应用多，也能放得整齐。小尺寸图标配清楚标签，分类条快速筛选，少翻几页就能找到工具。没有把入口藏进复杂的导航。',tags:['高密度','分类筛选','少翻页'],focus:'分类条 + 8 列图标',use:'安装应用多，偏重查找效率',rotation:'横屏 8 列，竖屏 6 列。分类条、应用顺序和当前页保持，图标整体重排。',reflow:'8 列 → 6 列'},
    {id:'paper',name:'素纸',subtitle:'单色图标桌面',layout:'paper',count:12,color:'#c4c1b4',desc:'浅纸色壁纸、统一线条图标和清楚的名称。仍然是标准应用网格与文件夹，视觉安静，也适合不喜欢彩色图标的人。',tags:['单色图标','柔和对比','日期组件'],focus:'线条图标 + 简洁日期',use:'阅读、记录，喜欢克制的视觉',rotation:'横屏日期在左、天气在右，图标 6 列；竖屏日期居上，图标 4 列，底栏保持单色。',reflow:'6 列 → 4 列'},
    {id:'sunny',name:'轻彩',subtitle:'大图标易触达',layout:'large',count:10,color:'#efc18c',desc:'更大的图标、更清楚的名称、更容易点中的入口。保留天气、文件夹和常驻栏，让家人也能自然使用这台平板。',tags:['大图标','大触点','清楚标签'],focus:'大图标 + 简明天气',use:'偏好清晰、轻松点按',rotation:'横屏 5 列、竖屏 3 列；竖屏增加行数，图标与名称保持较大尺寸，常驻栏减少间隔。',reflow:'5 列 → 3 列'},
    {id:'night',name:'夜航',subtitle:'深色沉浸桌面',layout:'night',count:14,color:'#484d90',desc:'深蓝壁纸配柔和光晕，图标保持完整色彩。时间与音乐控件轻量呈现，适合夜间使用，也兼顾白天的可读性。',tags:['深色壁纸','彩色图标','音乐快捷操作'],focus:'暗色主屏 + 音乐条',use:'影音娱乐，偏好深色桌面',rotation:'横屏时间与音乐并排、图标 7 列；竖屏音乐移到时钟下方，图标 4 列。',reflow:'7 列 → 4 列'},
    {id:'zones',name:'分区',subtitle:'大文件夹桌面',layout:'folders',count:8,color:'#8fafb2',desc:'把工作、创作、生活、影音放进四个大文件夹，封面就能辨认其中的应用。下面保留常用图标，让归类与直达同时成立。',tags:['四个大文件夹','分组直达','常用图标'],focus:'大文件夹 + 常用应用',use:'习惯按用途整理应用',rotation:'横屏四个文件夹并排、图标 8 列；竖屏文件夹变 2×2、图标 4 列。打开的文件夹与输入保持。',reflow:'四组并排 → 2×2'},
    {id:'canvas',name:'自在',subtitle:'混排组件桌面',layout:'canvas',count:12,color:'#c8b99a',desc:'像常用的平板主屏一样，把照片、天气、便签和应用混在一屏。编辑时可以隐藏组件或调整应用顺序，留出属于自己的桌面。',tags:['组件与图标混排','可编辑','生活感'],focus:'左侧组件 + 右侧应用',use:'喜欢在主屏保留照片与便签',rotation:'横屏组件在左、图标 4 列在右；竖屏组件移到上方，下面仍是 4 列应用。组件开关与顺序保持。',reflow:'左右混排 → 上下混排'}
  ];
  const appData = [
    ['wechat','微信','#30b76d','bubble'],['browser','浏览器','#3689eb','compass'],['photos','照片','#ee8275','flower'],['camera','相机','#626d7e','camera'],
    ['notes','备忘录','#ecb940','note'],['calendar','日历','#ed6560','calendar'],['files','文件','#58a5df','folder'],['settings','设置','#7d8a9b','gear'],
    ['music','音乐','#ed6a8c','music'],['video','视频','#9071d6','play'],['books','阅读','#dca758','book'],['maps','地图','#67b898','map'],
    ['mail','邮件','#6c9ce8','mail'],['store','应用市场','#6b83df','bag'],['draw','画板','#bc8ddb','pen'],['clock','时钟','#364658','clock'],
    ['weather','天气','#5aaede','sun'],['calculator','计算器','#7f91a2','calculator'],['recorder','录音机','#c98575','wave'],['docs','文档','#6d99d4','document'],
    ['cloud','云空间','#82b2d7','cloud'],['translate','翻译','#6daaa7','translate'],['download','下载','#69a087','download'],['messages','信息','#6da6c7','message']
  ].map(([id,name,color,glyph]) => ({id,name,color,glyph}));
  const apps = new Map(appData.map(a => [a.id,a]));
  const groups = {work:['mail','calendar','docs','files','notes','calculator'],create:['draw','photos','camera','recorder','notes','cloud'],life:['maps','weather','clock','messages','wechat','translate'],media:['music','video','books','browser','photos','store']};
  const groupNames = {work:'工作',create:'创作',life:'生活',media:'影音'};
  const iconPaths = {
    bubble:'<path d="M35 9c-12 0-22 8-22 18 0 6 3 11 9 15l-3 9 12-5h4c12 0 22-8 22-19S47 9 35 9Z"/><circle cx="27" cy="26" r="1.6" fill="currentColor"/><circle cx="42" cy="26" r="1.6" fill="currentColor"/>',
    compass:'<circle cx="32" cy="32" r="23"/><path d="m42 21-6 16-15 6 6-16Z"/><path d="m27 27 9 10"/>',
    flower:'<g fill="currentColor" stroke="none"><ellipse cx="32" cy="19" rx="8" ry="13" opacity=".95"/><ellipse cx="32" cy="45" rx="8" ry="13" opacity=".7"/><ellipse cx="19" cy="32" rx="13" ry="8" opacity=".85"/><ellipse cx="45" cy="32" rx="13" ry="8" opacity=".65"/><ellipse cx="23" cy="23" rx="8" ry="12" transform="rotate(-45 23 23)" opacity=".8"/><ellipse cx="41" cy="41" rx="8" ry="12" transform="rotate(-45 41 41)" opacity=".9"/><ellipse cx="41" cy="23" rx="8" ry="12" transform="rotate(45 41 23)" opacity=".8"/><ellipse cx="23" cy="41" rx="8" ry="12" transform="rotate(45 23 41)" opacity=".75"/></g><circle cx="32" cy="32" r="6"/>',
    camera:'<path d="M11 20h12l4-6h12l4 6h10v29H11Z"/><circle cx="32" cy="34" r="10"/><path d="M45 25h3"/>',
    note:'<rect x="15" y="10" width="35" height="45" rx="4"/><path d="M23 24h19M23 33h19M23 42h12M23 8v6M42 8v6"/>',
    calendar:'<rect x="12" y="14" width="40" height="40" rx="5"/><path d="M12 25h40M23 10v9M41 10v9"/><text x="32" y="45" text-anchor="middle" fill="currentColor" stroke="none" font-size="21" font-family="Arial">11</text>',
    folder:'<path d="M9 18h19l5 7h23v27H9Z"/><path d="M9 25h47"/>',
    gear:'<path d="m28 9 8 0 2 7 6 3 7-2 4 7-5 5v7l5 5-4 7-7-2-6 3-2 7h-8l-2-7-6-3-7 2-4-7 5-5v-7l-5-5 4-7 7 2 6-3Z"/><circle cx="32" cy="33" r="10"/>',
    music:'<path d="M26 43V18l23-5v25M26 24l23-5"/><ellipse cx="19" cy="45" rx="7" ry="5"/><ellipse cx="42" cy="40" rx="7" ry="5"/>',
    play:'<rect x="9" y="13" width="46" height="38" rx="9"/><path d="m27 23 15 9-15 9Z" fill="currentColor" stroke="none"/>',
    book:'<path d="M9 15c10-3 17-1 23 3 7-4 14-6 23-3v36c-10-3-17-1-23 3-6-4-13-6-23-3ZM32 18v36"/>',
    map:'<path d="m10 17 15-6 14 7 15-6v36l-15 6-14-7-15 6ZM25 11v36M39 18v36"/>',
    mail:'<rect x="9" y="15" width="46" height="35" rx="5"/><path d="m11 19 21 16 21-16M11 47l15-14M53 47 38 33"/>',
    bag:'<path d="M14 22h36l3 31H11ZM23 26V18a9 9 0 0 1 18 0v8"/>',
    pen:'<path d="m16 46 3-12L43 10l11 11-24 24-14 5ZM36 17l11 11M19 34l11 11"/>',
    clock:'<circle cx="32" cy="32" r="23"/><path d="M32 15v18l12 7"/>',
    sun:'<circle cx="32" cy="32" r="12"/><path d="M32 5v7M32 52v7M5 32h7M52 32h7M13 13l5 5M46 46l5 5M13 51l5-5M46 18l5-5"/>',
    calculator:'<rect x="15" y="8" width="34" height="48" rx="5"/><path d="M22 17h20v10H22ZM23 36h3M37 36h3M23 46h3M37 46h3"/>',
    wave:'<path d="M11 27v10M20 18v28M29 9v46M38 19v26M47 25v14M55 29v6"/>',
    document:'<path d="M17 8h22l11 12v36H17ZM39 8v13h11M25 32h17M25 40h17M25 48h10"/>',
    cloud:'<path d="M19 48a12 12 0 1 1-1-24 16 16 0 0 1 30-3 14 14 0 0 1-1 27Z"/>',
    translate:'<path d="M9 17h30M24 9v8M15 18c1 14 9 22 23 27M33 18c-2 14-10 22-23 28M36 52l10-25 10 25M40 43h12"/>',
    download:'<path d="M32 9v31M21 29l11 12 11-12M12 42v12h40V42"/>',
    message:'<path d="M10 12h44v33H29L15 55V45h-5Z"/><path d="M20 23h24M20 33h17"/>'
  };
  const glyph = id => `<svg viewBox="0 0 64 64" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">${iconPaths[apps.get(id)?.glyph || 'folder']}</svg>`;
  const mark = id => `<span class="app-icon" style="--icon:${apps.get(id).color}">${glyph(id)}</span>`;
  const initialOrder = ['wechat','browser','photos','camera','notes','calendar','folder:work','maps','video','books','music','files','folder:media','settings',...appData.map(a=>a.id)].filter((id,i,a)=>a.indexOf(id)===i);
  const ids = designs.map(d=>d.id);
  const fresh = () => ({page:0,order:[...initialOrder],edit:false,widgets:true,category:'all',space:'personal',playing:false,track:0});
  const saved = storage.get('shortlist',[]);
  const requestedDesign = new URLSearchParams(location.search).get('design');
  const initialDesign = ids.includes(requestedDesign) ? requestedDesign : storage.get('current','sunny');
  const state = {design:ids.includes(initialDesign)?initialDesign:'sunny',direction:'landscape',effective:'landscape',shortlist:Array.isArray(saved)?saved.filter(x=>ids.includes(x)):[],selected:ids.includes(storage.get('selected','sunny'))?storage.get('selected','sunny'):'sunny',filter:'all',immersive:false,overlay:null,opener:null,note:storage.get('note','周末去走走，拍一些喜欢的风景。\n记得带上相机和好心情。'),data:Object.fromEntries(ids.map(id=>[id,fresh()]))};
  const D = () => state.data[state.design];
  const C = () => designs.find(d=>d.id===state.design);
  const number = id => String(ids.indexOf(id)+1).padStart(2,'0');
  const tracks = ['山间来信','海风慢慢','晴日散步'];
  const nameOf = id => id.startsWith('folder:')?groupNames[id.split(':')[1]]:apps.get(id).name;
  function folderIcon(key) { return `<span class="folder-icon">${groups[key].slice(0,4).map(id=>mark(id)).join('')}</span>`; }
  function iconCell(id, editable = false) {
    const isFolder=id.startsWith('folder:'),key=isFolder?id.split(':')[1]:null;
    return `<div class="icon-cell" ${editable?'data-sort="'+esc(id)+'" draggable="'+D().edit+'"':''}><button class="app-button" ${isFolder?'data-folder="'+key+'"':'data-app="'+id+'"'} aria-label="打开${nameOf(id)}${isFolder?'文件夹':''}">${isFolder?folderIcon(key):mark(id)}<span class="app-name">${nameOf(id)}</span></button>${editable&&D().edit?`<button class="pin" data-pin="${esc(id)}" aria-label="置顶${nameOf(id)}">↑</button>`:''}</div>`;
  }
  function appOrder() { return D().category==='all'?D().order:D().order.filter(id=>groups[D().category]?.includes(id)); }
  function pageCount() { return Math.max(1,Math.ceil(appOrder().length/C().count)); }
  function grid() { const d=D();d.page=Math.min(d.page,pageCount()-1);return `<div class="app-grid" aria-label="桌面应用">${appOrder().slice(d.page*C().count,(d.page+1)*C().count).map(id=>iconCell(id,true)).join('')}</div>`; }
  function searchButton() { return '<button class="home-search" data-action="drawer" aria-label="搜索应用"><span>⌕</span> 搜索应用</button>'; }
  function weather() { return '<button class="widget weather-widget" data-app="weather" aria-label="打开天气"><span class="widget-eyebrow">深圳 · 晴</span><div class="weather-main"><strong>23<span>°</span></strong><span class="sun-art">☀</span></div><span class="weather-bottom">空气优 <i> / </i> 18° — 25°</span></button>'; }
  function agenda() { return '<button class="widget agenda-widget" data-app="calendar" aria-label="打开日程"><span class="widget-eyebrow">11月11日 · 星期三</span><strong>把今天安排好</strong><span class="event"><i></i><span>10:30 <b>设计讨论</b></span></span><span class="event"><i></i><span>15:00 <b>留一点阅读时间</b></span></span></button>'; }
  function music() { const d=D();return `<div class="widget music-widget"><button class="album" data-app="music" aria-label="打开音乐"><span>♪</span></button><div class="music-meta"><span class="widget-eyebrow">${d.playing?'正在播放':'随时听一点'}</span><strong>${tracks[d.track]}</strong><span>Easy Radio · 本地演示</span></div><div class="music-controls"><button data-action="play" aria-label="${d.playing?'暂停':'播放'}音乐">${d.playing?'Ⅱ':'▶'}</button><button data-action="track" aria-label="下一首">▸▸</button></div></div>`; }
  function clock() { return '<div class="clock-widget"><button data-app="clock" aria-label="打开时钟">09<span>:</span>41</button><p>11月11日 星期三 <span>晴 · 23°</span></p></div>'; }
  function note() { return `<button class="widget note-widget" data-app="notes" aria-label="打开桌面便签"><span class="widget-eyebrow">随手记 <span>↗</span></span><p>${esc(state.note.split('\n')[0]).slice(0,46)}</p><span class="note-rule"></span></button>`; }
  function photo() { return '<button class="widget photo-widget" data-app="photos" aria-label="打开照片"><span class="photo-sun"></span><span class="photo-hill"></span><span class="photo-hill back"></span><span class="photo-caption">留住一片好天气 <small>我的照片 · 示例</small></span></button>'; }
  function recent() { return '<div class="widget recent-widget"><span class="widget-eyebrow">最近文件</span><button data-app="docs"><span class="file-type">W</span><span>十一月工作计划<small>今天 09:20 · 文档</small></span><b>›</b></button><button data-app="files"><span class="file-type green">F</span><span>桌面设计资料<small>昨天 18:40 · 文件夹</small></span><b>›</b></button></div>'; }
  function pageControls() { const count=pageCount();return `<div class="page-controls"><button data-action="page" data-step="-1" aria-label="上一页" ${count<2?'disabled':''}>‹</button><div class="page-dots">${Array.from({length:count},(_,i)=>`<button data-page="${i}" aria-label="第${i+1}页" aria-pressed="${D().page===i}"></button>`).join('')}</div><button data-action="page" data-step="1" aria-label="下一页" ${count<2?'disabled':''}>›</button><button data-action="drawer" class="page-search" aria-label="打开全部应用">⌕</button></div>`; }
  function renderHome() {
    const d=D(),c=C();$('#screen').dataset.theme=c.id;$('#screen').dataset.layout=c.layout;$('#screen').classList.toggle('editing',d.edit);$('#screen').classList.toggle('widgets-hidden',!d.widgets);
    let content='';
    if(c.layout==='classic')content=`<div class="hero classic-hero"><div class="date-heading"><p>11月11日 · 星期三</p><h2>每一天，都有新风景。</h2>${searchButton()}</div><div class="hero-widgets">${weather()}${agenda()}</div></div>${grid()}`;
    if(c.layout==='widgets')content=`<div class="hero widget-hero">${weather()}${agenda()}${music()}</div>${grid()}`;
    if(c.layout==='minimal')content=`<div class="hero minimal-hero">${clock()}${searchButton()}</div>${grid()}`;
    if(c.layout==='work')content=`<div class="work-top"><div class="space-tabs"><button data-space="personal" aria-pressed="${d.space==='personal'}">个人桌面</button><button data-space="work" aria-pressed="${d.space==='work'}">工作桌面</button></div>${searchButton()}</div><div class="work-layout"><div class="work-apps"><div class="work-greeting"><p>WEDNESDAY, NOVEMBER 11</p><h2>${d.space==='work'?'准备好，开始今天的工作。':'早上好，Vincent。'}</h2><span>常用工具，都在手边。</span></div>${grid()}</div><div class="work-widgets">${agenda()}${recent()}</div></div>`;
    if(c.layout==='dense')content=`<div class="hero dense-hero"><div><strong>09:41</strong><span>11月11日 星期三</span></div>${searchButton()}</div><div class="category-tabs" role="group" aria-label="桌面分类">${[['all','全部'],['work','工作'],['create','创作'],['life','生活'],['media','影音']].map(([id,label])=>`<button data-category="${id}" aria-pressed="${d.category===id}">${label}</button>`).join('')}</div>${grid()}`;
    if(c.layout==='paper')content=`<div class="hero paper-hero"><div><p>NOVEMBER / WEDNESDAY</p><strong>11<span>月</span>11<span>日</span></strong><span>给今天，留一点从容。</span></div><button data-app="weather">晴 <b>23°</b><span>深圳 · 空气优</span></button></div>${grid()}`;
    if(c.layout==='large')content=`<div class="hero large-hero"><div><p>11月11日 星期三</p><h2>今天也是好天气。</h2></div><button data-app="weather"><span>☀</span><strong>23°</strong><small>深圳 · 晴</small></button></div>${grid()}`;
    if(c.layout==='night')content=`<div class="hero night-hero">${clock()}${music()}</div>${grid()}`;
    if(c.layout==='folders')content=`<div class="folder-heading"><div><p>11月11日 · 星期三</p><h2>各归其位，随手就到。</h2></div>${searchButton()}</div><div class="hero big-folders">${Object.keys(groups).map(key=>`<button class="big-folder" data-folder="${key}" aria-label="打开${groupNames[key]}大文件夹"><span class="big-folder-grid">${groups[key].slice(0,4).map(id=>mark(id)).join('')}</span><span class="folder-label">${groupNames[key]}<small>${groups[key].length} 个应用 <b>↗</b></small></span></button>`).join('')}</div>${grid()}`;
    if(c.layout==='canvas')content=`<div class="canvas-heading"><p>11月11日 · 星期三</p>${searchButton()}</div><div class="canvas-layout"><div class="hero canvas-widgets">${photo()}${weather()}${note()}</div>${grid()}</div>`;
    $('#home').innerHTML=`<div class="home-top"><span>${d.edit?'编辑桌面':'EasyLauncher'}</span><button data-action="edit">${d.edit?'完成':'编辑桌面'}</button></div>${d.edit?`<div class="edit-toolbar"><span>点 ↑ 置顶 · 鼠标拖动可换位</span><button data-action="widgets">${d.widgets?'隐藏组件':'显示组件'}</button></div>`:''}<div class="desktop-content">${content}</div>${pageControls()}`;
    renderDock();
  }
  function renderDock() {
    $('#dock').innerHTML=`<button class="drawer-button" data-action="drawer" aria-label="全部应用"><span>${'<i></i>'.repeat(9)}</span></button><span class="dock-divider"></span>${['browser','wechat','notes','photos','settings'].map(id=>`<button data-app="${id}" aria-label="从常驻栏打开${apps.get(id).name}">${mark(id)}</button>`).join('')}<span class="dock-divider second"></span><button class="dock-home" data-action="home" aria-label="回到第一页">⌂</button>`;
  }
  function renderChrome() {
    const c=C();
    $('#concepts').innerHTML=designs.map(x=>`<button class="concept" data-design="${x.id}" aria-pressed="${x.id===state.design}" aria-label="方案${number(x.id)} ${x.name} ${x.subtitle}"><span class="concept-top">${number(x.id)}<span>${state.selected===x.id?'✓':state.shortlist.includes(x.id)?'●':'↗'}</span></span><span class="mini-screen" style="--mini:${x.color}" data-mini="${x.layout}"><span class="mini-widget"></span><span class="mini-icons">${'<i></i>'.repeat(8)}</span><span class="mini-dock"></span></span><strong>${x.name}</strong><small>${x.subtitle}</small></button>`).join('');
    $('#current-number').textContent=number(c.id);$('#current-name').textContent=c.name+' / '+c.subtitle;$('#detail-number').textContent=number(c.id)+' / 10';$('#detail-title').textContent=c.name;$('#detail-subtitle').textContent=c.subtitle;$('#detail-description').textContent=c.desc;$('#tags').innerHTML=c.tags.map(t=>`<span>${t}</span>`).join('');
    $('#interactions').innerHTML=['点击图标启动应用演示；文件夹点开后查看分组。','底部常驻栏随时可用；点击九宫格搜索全部应用。','左右翻页；长按图标或点「编辑桌面」调整顺序。'].map(x=>`<li>${x}</li>`).join('');
    $('#rotation').textContent=c.rotation;$('#shortlist').setAttribute('aria-pressed',String(state.shortlist.includes(c.id)));$('#shortlist').textContent=state.shortlist.includes(c.id)?'已加入候选 ✓':'加入候选 ＋';$('#select').textContent=state.selected===c.id?'已选定这个方向 ✓':'选定这个方向 ↗';$('#selection-status').textContent=(state.selected?'已选定「'+designs.find(x=>x.id===state.selected).name+'」 · ':'')+state.shortlist.length+' 个候选';renderComparison();
  }
  function renderComparison() {
    const visible=designs.filter(d=>state.filter==='all'||state.shortlist.includes(d.id));$('#candidate-count').textContent=state.shortlist.length;$('#empty').hidden=visible.length>0;
    $('#comparison-body').innerHTML=visible.map(c=>`<tr><td><button data-jump="${c.id}"><span>${number(c.id)}</span><strong>${c.name}</strong><small>${c.subtitle}</small>${state.selected===c.id?' ✓':''}</button></td><td>${c.focus}</td><td>${c.use}</td><td>${c.reflow}</td><td><button data-shortlist="${c.id}" aria-label="${state.shortlist.includes(c.id)?'移出':'加入'}${c.name}候选" aria-pressed="${state.shortlist.includes(c.id)}">${state.shortlist.includes(c.id)?'已加入 ✓':'＋'}</button></td></tr>`).join('');$$('[data-filter]').forEach(b=>b.setAttribute('aria-pressed',String(b.dataset.filter===state.filter)));
  }
  function fit() {
    const stage=$('#stage'),style=getComputedStyle(stage),w=stage.clientWidth-parseFloat(style.paddingLeft)-parseFloat(style.paddingRight),h=stage.clientHeight-parseFloat(style.paddingTop)-parseFloat(style.paddingBottom),direction=state.direction==='auto'?(innerWidth>=innerHeight?'landscape':'portrait'):state.direction;
    state.effective=direction;const [width,height]=direction==='landscape'?[1200,768]:[768,1200],border=state.immersive?0:14,scale=Math.max(.05,Math.min((w-border)/width,(h-border)/height));
    const screen=$('#screen');screen.dataset.direction=direction;screen.style.width=width+'px';screen.style.height=height+'px';screen.style.transform=`scale(${scale})`;$('#frame').style.width=(width*scale+border)+'px';$('#frame').style.height=(height*scale+border)+'px';$('#dimensions').textContent=width+' × '+height;$$('#directions [data-direction]').forEach(b=>b.setAttribute('aria-pressed',String(b.dataset.direction===state.direction)));
  }
  function render() { renderChrome();renderHome();fit(); }
  function chooseDesign(id) { if(!ids.includes(id)||state.design===id)return;close();state.design=id;storage.set('current',id);render(); }
  function shortlist(id) { state.shortlist=state.shortlist.includes(id)?state.shortlist.filter(x=>x!==id):[...state.shortlist,id];storage.set('shortlist',state.shortlist);renderChrome(); }
  function turnPage(step) { D().page=(D().page+step+pageCount())%pageCount();renderHome(); }
  function filteredApps(query='',key) { const q=query.trim().toLowerCase();return (key?groups[key].map(id=>apps.get(id)):appData).filter(a=>a.name.includes(q)||a.id.includes(q)); }
  function appResults(query='',key) { const list=filteredApps(query,key);return list.length?list.map(a=>iconCell(a.id)).join(''):'<p class="no-results">没有找到应用，换个名称试试。</p>'; }
  function open(o) { if(!state.overlay)state.opener=document.activeElement;state.overlay=o;renderOverlay();requestAnimationFrame(()=>$('input,textarea,button',$('#overlays'))?.focus({preventScroll:true})); }
  function close() { state.overlay=null;$('#overlays').innerHTML='';if(state.opener?.isConnected)state.opener.focus({preventScroll:true});state.opener=null; }
  function renderOverlay() {
    const o=state.overlay;if(!o){$('#overlays').innerHTML='';return;}
    let title='',body='',type=o.type;
    if(type==='drawer'||type==='folder'){
      title=type==='folder'?groupNames[o.key]:'全部应用';body=`<label class="drawer-search"><span>⌕</span><input id="app-search" aria-label="搜索${title}" placeholder="搜索应用名称或英文 ID" value="${esc(o.query||'')}" autocomplete="off"></label><div class="drawer-grid" id="app-results">${appResults(o.query||'',o.key)}</div><p class="overlay-hint">${type==='folder'?'点击图标打开应用':'应用库 · '+appData.length+' 个演示应用'}</p>`;
    }else{
      const a=apps.get(o.id);title=a.name;
      if(o.id==='notes')body=`<div class="note-date">2026年11月11日 · 今天</div><textarea id="note-input" aria-label="备忘录内容" placeholder="写下此刻的想法…">${esc(state.note)}</textarea><p class="overlay-hint">内容自动保存在本浏览器。</p>`;
      else if(o.id==='music')body=`<div class="app-demo music-demo">${music()}<p>播放状态为界面演示，不输出音频。</p></div>`;
      else if(o.id==='weather')body='<div class="app-demo weather-demo"><span>☀</span><h3>23°</h3><p>深圳 · 晴<br>空气优 · 18° — 25°</p></div>';
      else if(o.id==='calendar')body='<div class="app-demo calendar-demo"><span>11</span><h3>11月11日 · 星期三</h3><p>10:30　设计讨论<br>15:00　留一点阅读时间</p></div>';
      else if(o.id==='photos')body=`<div class="photo-demo">${photo()}<p>一片好天气 · 本地示意画面</p></div>`;
      else if(o.id==='files'||o.id==='docs')body=`<div class="files-demo">${recent()}</div>`;
      else body=`<div class="app-demo">${mark(o.id)}<h3>${a.name}已打开</h3><p>应用启动与返回的交互演示。<br>后续原生版本将打开设备上的对应应用。</p></div>`;
      body+='<button class="return-home" data-action="close">返回桌面</button>';
    }
    $('#overlays').innerHTML=`<div class="backdrop"><section class="overlay-panel ${type}" role="dialog" aria-modal="true" aria-label="${title}"><header><h2>${title}</h2><span>本地演示</span><button data-action="close" aria-label="关闭${title}">×</button></header>${body}</section></div>`;
  }
  let toastTimer,screenTimer;
  function toast(msg,inside=false) { const el=$(inside?'#screen-toast':'#toast');el.textContent=msg;el.classList.add('show');if(inside){clearTimeout(screenTimer);screenTimer=setTimeout(()=>el.classList.remove('show'),2300);}else{clearTimeout(toastTimer);toastTimer=setTimeout(()=>el.classList.remove('show'),2600);} }
  function edit() { D().edit=!D().edit;renderHome(); }
  function pin(id) { D().order=[id,...D().order.filter(x=>x!==id)];D().page=0;renderHome();toast(nameOf(id)+'已移到第一页首位',true); }
  async function fullscreen() {
    state.immersive=!state.immersive;document.body.classList.toggle('immersive',state.immersive);$('#exit').hidden=!state.immersive;
    if(state.immersive){state.previousDirection=state.direction;state.direction='auto';try{await document.documentElement.requestFullscreen?.();}catch{ /* CSS immersion remains available. */ }}else{state.direction=state.previousDirection||'landscape';if(document.fullscreenElement)try{await document.exitFullscreen();}catch{}}
    fit();
  }
  $('#concepts').addEventListener('click',e=>{const b=e.target.closest('[data-design]');if(b)chooseDesign(b.dataset.design);});
  $('#directions').addEventListener('click',e=>{const b=e.target.closest('[data-direction]');if(b){state.direction=b.dataset.direction;fit();}});
  $('#shortlist').addEventListener('click',()=>shortlist(state.design));
  $('#select').addEventListener('click',()=>{state.selected=state.design;if(!state.shortlist.includes(state.design))state.shortlist.push(state.design);storage.set('selected',state.selected);storage.set('shortlist',state.shortlist);renderChrome();toast('已选定「'+C().name+'」');});
  $('#export').addEventListener('click',()=>{
    if(!state.shortlist.length&&!state.selected){toast('先加入候选或选定一个方向');return;}
    const payload={project:'EasyLauncher',applicationId:'com.vincenthu.launcher',prototypeVersion:'0.3.0',selected:state.selected,shortlist:state.shortlist.map(id=>{const c=designs.find(x=>x.id===id);return{id,name:c.name,layout:c.subtitle,rotation:c.rotation};}),exportedAt:new Date().toISOString()};
    const url=URL.createObjectURL(new Blob([JSON.stringify(payload,null,2)],{type:'application/json'})),a=document.createElement('a');a.href=url;a.download='EasyLauncher-v3-selection.json';a.click();setTimeout(()=>URL.revokeObjectURL(url),1000);toast('选择已导出');
  });
  $('#filters').addEventListener('click',e=>{const b=e.target.closest('[data-filter]');if(b){state.filter=b.dataset.filter;renderComparison();}});
  $('#comparison-body').addEventListener('click',e=>{const b=e.target.closest('button');if(b?.dataset.shortlist)shortlist(b.dataset.shortlist);if(b?.dataset.jump){chooseDesign(b.dataset.jump);$('.studio').scrollIntoView({behavior:matchMedia('(prefers-reduced-motion:reduce)').matches?'instant':'smooth'});}});
  $('#reset').addEventListener('click',()=>{close();state.data[state.design]=fresh();renderHome();toast('当前桌面演示已重置',true);});
  $('#fullscreen').addEventListener('click',fullscreen);$('#exit').addEventListener('click',fullscreen);
  document.addEventListener('fullscreenchange',()=>{if(!document.fullscreenElement&&state.immersive){state.immersive=false;state.direction=state.previousDirection||'landscape';document.body.classList.remove('immersive');$('#exit').hidden=true;fit();}});
  let suppressUntil=0,pressTimer,gesture=null,dragId=null;
  $('#screen').addEventListener('click',e=>{
    if(Date.now()<suppressUntil)return;
    const b=e.target.closest('button');if(!b){if(e.target.classList.contains('backdrop'))close();return;}
    if(b.dataset.pin){pin(b.dataset.pin);return;}
    if(b.dataset.app){if(D().edit&&b.closest('#home')){toast('点图标角标 ↑ 置顶，或拖动交换位置',true);return;}open({type:'app',id:b.dataset.app});return;}
    if(b.dataset.folder){if(D().edit&&b.closest('#home')){toast('先点完成，再打开文件夹',true);return;}open({type:'folder',key:b.dataset.folder,query:''});return;}
    const a=b.dataset.action,d=D();
    if(a==='drawer')open({type:'drawer',query:''});
    if(a==='close')close();
    if(a==='home'){close();d.page=0;renderHome();}
    if(a==='edit')edit();
    if(a==='widgets'){d.widgets=!d.widgets;renderHome();}
    if(a==='page')turnPage(Number(b.dataset.step));
    if(b.dataset.page!==undefined){d.page=Number(b.dataset.page);renderHome();}
    if(b.dataset.category){d.category=b.dataset.category;d.page=0;renderHome();}
    if(b.dataset.space){d.space=b.dataset.space;d.page=0;const priority=d.space==='work'?groups.work:initialOrder;d.order=[...priority,...d.order.filter(id=>!priority.includes(id))];renderHome();}
    if(a==='play'||a==='track'){if(a==='play')d.playing=!d.playing;else d.track=(d.track+1)%tracks.length;renderHome();if(state.overlay)renderOverlay();}
  });
  $('#screen').addEventListener('input',e=>{
    if(e.target.id==='app-search'){state.overlay.query=e.target.value;$('#app-results').innerHTML=appResults(e.target.value,state.overlay.key);}
    if(e.target.id==='note-input'){state.note=e.target.value;storage.set('note',state.note);const preview=$('.note-widget p');if(preview)preview.textContent=state.note.split('\n')[0].slice(0,46);}
  });
  let longPressTriggered=false;
  $('#screen').addEventListener('pointerdown',e=>{
    if(e.button!==0||state.overlay)return;
    const icon=e.target.closest('#home [data-sort]');gesture={x:e.clientX,y:e.clientY,icon:!!icon};longPressTriggered=false;
    if(icon&&!D().edit)pressTimer=setTimeout(()=>{D().edit=true;longPressTriggered=true;renderHome();suppressUntil=Date.now()+500;},550);
  });
  $('#screen').addEventListener('pointermove',e=>{if(gesture&&Math.hypot(e.clientX-gesture.x,e.clientY-gesture.y)>12)clearTimeout(pressTimer);});
  $('#screen').addEventListener('pointerup',e=>{
    clearTimeout(pressTimer);if(!gesture)return;const dx=e.clientX-gesture.x,dy=e.clientY-gesture.y;gesture=null;if(longPressTriggered){longPressTriggered=false;return;}
    if(!D().edit&&!state.overlay&&Math.abs(dx)>50&&Math.abs(dx)>Math.abs(dy)*1.6&&!e.target.closest('.widget,input,textarea')){turnPage(dx<0?1:-1);suppressUntil=Date.now()+350;}
  });
  window.addEventListener('pointerup',()=>{clearTimeout(pressTimer);gesture=null;});window.addEventListener('pointercancel',()=>{clearTimeout(pressTimer);gesture=null;});
  $('#home').addEventListener('dragstart',e=>{const cell=e.target.closest('[data-sort]');if(!D().edit||!cell){e.preventDefault();return;}dragId=cell.dataset.sort;e.dataTransfer.setData('text/plain',dragId);e.dataTransfer.effectAllowed='move';});
  $('#home').addEventListener('dragover',e=>{if(D().edit&&e.target.closest('[data-sort]'))e.preventDefault();});
  $('#home').addEventListener('drop',e=>{const cell=e.target.closest('[data-sort]');if(!D().edit||!cell||!dragId)return;e.preventDefault();const order=D().order,a=order.indexOf(dragId),b=order.indexOf(cell.dataset.sort);if(a>=0&&b>=0)[order[a],order[b]]=[order[b],order[a]];dragId=null;renderHome();toast('已交换应用位置',true);});
  $('#home').addEventListener('dragend',()=>{dragId=null;});
  document.addEventListener('keydown',e=>{
    const input=/INPUT|TEXTAREA|SELECT/.test(e.target.tagName);
    if(e.key==='Escape'){if(state.overlay){close();e.preventDefault();}else if(D().edit){D().edit=false;renderHome();}else if(state.immersive)fullscreen();return;}
    if(state.overlay&&e.key==='Tab'){const fields=$$('button,input,textarea',$('#overlays')).filter(x=>!x.disabled),first=fields[0],last=fields[fields.length-1];if(e.shiftKey&&document.activeElement===first){last.focus();e.preventDefault();}else if(!e.shiftKey&&document.activeElement===last){first.focus();e.preventDefault();}return;}
    if(e.target.id==='app-search'&&e.key==='Enter'){const a=filteredApps(e.target.value,state.overlay.key)[0];if(a)open({type:'app',id:a.id});return;}
    if(input)return;
    if(e.key==='/'){e.preventDefault();open({type:'drawer',query:''});}
    if(!state.overlay&&['ArrowLeft','ArrowRight'].includes(e.key)){e.preventDefault();turnPage(e.key==='ArrowRight'?1:-1);}
  });
  new ResizeObserver(fit).observe($('#stage'));window.addEventListener('resize',fit);window.addEventListener('orientationchange',()=>requestAnimationFrame(fit));render();
})();
