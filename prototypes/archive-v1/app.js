'use strict';
(() => {
  const $ = (selector, root = document) => root.querySelector(selector);
  const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
  const escapeHTML = value => String(value).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const storage = {
    get(key, fallback) { try { const value = localStorage.getItem('easylauncher.' + key); return value ? JSON.parse(value) : fallback; } catch { return fallback; } },
    set(key, value) { try { localStorage.setItem('easylauncher.' + key, JSON.stringify(value)); } catch { /* file:// or private mode: keep using in-memory state. */ } }
  };
  const concepts = [
    {id:'classic', name:'浮光', en:'Familiar, refined.', brief:'经典主屏', tags:['iPad 灵感','轻盈玻璃','熟悉易用'], description:'让熟悉感先发生。柔和的光影壁纸、规整的应用网格与悬浮 Dock，保留主屏最纯粹的样子。', interactions:[['图标，就是入口','点击启动；长按进入编辑，拖动图标调整位置。'],['常用应用始终在手边','Dock 固定常用应用，文件夹收纳同类工具。'],['顺手找到下一件事','横向滑动翻页，点击搜索快速找到应用。']], rotation:'横屏 6 列，竖屏 4 列。保持应用的线性顺序与当前页，Dock 始终居中置底。', structure:'图标网格 + 悬浮 Dock', primary:'分页 / 文件夹 / 拖拽排序', reflow:'6 列 → 4 列', fit:'希望最接近 iPad 的日常体验'},
    {id:'widgets', name:'晨屿', en:'A little more in view.', brief:'信息小组件', tags:['一眼即知','自然柔色','卡片层级'], description:'在打开应用之前，先看见今天。天气、日程与正在播放的音乐组成一座安静的信息岛，常用应用在旁边等你。', interactions:[['信息与应用并行','天气和日程点击展开；应用区依旧保持清晰。'],['轻触就能继续','音乐卡片可播放 / 暂停，状态随屏幕旋转保留。'],['日常，不必层层寻找','两页应用网格与快捷搜索，减少一次次往返。']], rotation:'横屏组件靠左、应用靠右；竖屏组件组合为顶部双栏，应用在下方重新排布。', structure:'小组件信息岛 + 应用网格', primary:'卡片展开 / 音乐 / 应用分页', reflow:'左右双区 → 上下堆叠', fit:'习惯先看日程、天气再开始'},
    {id:'focus', name:'留白', en:'Less, but closer.', brief:'专注极简', tags:['低干扰','文字优先','专注节奏'], description:'给视线留一点空白，也给时间。用更少的图标、更清楚的文字，把桌面变成一份由你决定的日常清单。', interactions:[['只留下真正需要的','六个精选应用以列表呈现，点击名称直接打开。'],['让一段时间属于自己','点击专注计时开始或暂停，旋转不会重置。'],['其余应用，随叫随到','搜索覆盖完整应用库，也可翻页切换精选列表。']], rotation:'横屏问候与应用列表左右并排；竖屏问候收至上方，列表向下展开，触点保持宽松。', structure:'大字问候 + 精选应用列表', primary:'文字入口 / 专注计时 / 搜索', reflow:'左右并列 → 纵向阅读', fit:'想减少打扰，让桌面更安静'},
    {id:'studio', name:'工作台', en:'Room for your ideas.', brief:'生产力空间', tags:['工作区','内容优先','键鼠友好'], description:'把桌面当作工作的起点。用不同空间切换应用组合，在备忘与文件预览之间快速接续上一件事。', interactions:[['按场景，而不是按页寻找','工作 / 创作 / 生活切换不同的应用组合与内容。'],['先看到，再继续','点击备忘与文件卡片进入内容演示，模拟接续任务。'],['键盘也能顺畅导航','Tab 访问入口；/ 呼出搜索，Esc 返回上一层。']], rotation:'横屏左侧工作区导航；竖屏变成顶部标签，内容预览从并排变为上下堆叠。', structure:'场景导航 + 内容预览', primary:'工作区切换 / 内容接续 / 键盘', reflow:'左侧导航 → 顶部标签', fit:'经常连接键盘，平板也用来工作'},
    {id:'orbit', name:'星轨', en:'Everything in its orbit.', brief:'应用分区', tags:['空间记忆','深色通透','分类直达'], description:'不再记住第几页，而是记住它在哪里。沟通、灵感、效率与生活各归其位，常用入口聚集在手边。', interactions:[['四个分区，建立空间记忆','每区直接显示常用应用，右上角展开全部。'],['把高频操作放低一点','底部 Dock 和搜索不随内容滚动，横竖屏都易触达。'],['展开，不离开当前位置','分类以浮层打开，关闭后仍停留在原来的桌面。']], rotation:'横屏左侧时间与问候，右侧四个分区；竖屏时间缩为顶栏，每区应用由 3 列变为 2 列。', structure:'时间侧栏 + 四个应用分区', primary:'分类展开 / 空间定位 / Dock', reflow:'时间侧栏 → 顶栏；分区 3 → 2 列', fit:'安装应用多，喜欢分类而非翻页'}
  ];
  const paths = {
    search:'<circle cx="10.5" cy="10.5" r="6.5"/><path d="m16 16 5 5"/>',
    browser:'<circle cx="12" cy="12" r="9"/><path d="m16 8-2.5 5.5L8 16l2.5-5.5Z"/><path d="M12 3v2m0 14v2M3 12h2m14 0h2"/>',
    mail:'<rect x="3" y="5" width="18" height="14" rx="2"/><path d="m3 6 9 7 9-7"/>',
    files:'<path d="M3 7a2 2 0 0 1 2-2h5l2 3h7a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2Z"/>',
    notes:'<path d="M3 8h18M3 13h18M3 18h13"/>',
    camera:'<path d="M8 6l2-3h4l2 3h4a1 1 0 0 1 1 1v12H3V7a1 1 0 0 1 1-1Z"/><circle cx="12" cy="12" r="4"/>',
    music:'<path d="M9 18V5l11-2v13M9 8l11-2"/><ellipse cx="6" cy="18" rx="3" ry="2.5"/><ellipse cx="17" cy="16" rx="3" ry="2.5"/>',
    settings:'<path d="m9 3 1-1h4l1 3 3 1 3 1 1 4-2 2v3l-2 3-3-1-2 3h-4l-1-3-3-1-3-1-1-4 2-2V7l2-3 3 1Z"/><circle cx="12" cy="12" r="4"/>',
    wechat:'<path d="M11 3C5 3 1 6 1 10c0 2 1 4 4 5l-1 4 5-3c6 1 11-2 11-6s-4-7-9-7Z"/><path d="M18 11c-4 0-7 2-7 5s3 5 7 5h2l3 2-1-3c2-1 3-2 3-4 0-3-3-5-7-5Z" opacity=".9"/>',
    books:'<path d="M12 5C8 2 4 3 2 4v16c4-2 7-1 10 1 3-2 6-3 10-1V4c-3-1-7-2-10 1Zm0 0v16"/>',
    maps:'<path d="m2 6 6-3 8 3 6-3v15l-6 3-8-3-6 3Zm6-3v15m8-12v15"/>',
    video:'<rect x="3" y="5" width="18" height="15" rx="4"/><path d="m10 9 6 4-6 4Z"/>',
    message:'<path d="M21 11c0 5-4 9-9 9H8l-5 3 1-6c-2-2-2-4-2-6 0-5 4-9 10-9s9 4 9 9Z"/><path d="M7 10h10m-10 4h6"/>',
    draw:'<path d="m5 17-2 5 5-2L21 7l-5-5Zm11-15 5 5M5 17l3 3"/>',
    sun:'<circle cx="12" cy="12" r="4"/><path d="M12 1v3m0 16v3M1 12h3m16 0h3M4 4l2 2m12 12 2 2M4 20l2-2M18 6l2-2"/>',
    wifi:'<path d="M2 8c6-5 14-5 20 0M5 12c4-4 10-4 14 0m-10 4c2-2 4-2 6 0"/><circle cx="12" cy="20" r=".7"/>',
    briefcase:'<rect x="2" y="7" width="20" height="14" rx="3"/><path d="M8 7V3h8v4M2 12c6 4 14 4 20 0m-10 0v5"/>',
    home:'<path d="m2 11 10-9 10 9M5 9v13h14V9M9 22v-8h6v8"/>',
    moon:'<path d="M21 13A9 9 0 0 1 11 3a9 9 0 1 0 10 10Z"/>',
    bluetooth:'<path d="m7 5 12 12-7 5V2l7 5L7 19"/>',
    qq:'<path d="M6 17c-3 2-3 5 0 4l3-1h6l3 1c3 1 3-2 0-4 2-8-1-14-6-14s-8 6-6 14Z"/><path d="M8 12h8M9 7h.1M15 7h.1"/>',
    shopping:'<path d="M4 7h16l2 15H2ZM8 8V5a4 4 0 0 1 8 0v3"/>',
    calculator:'<rect x="4" y="2" width="16" height="21" rx="3"/><path d="M7 7h10M7 12h2m6 0h2M7 16h2m6 0h2M7 20h2m6 0h2"/>',
    terminal:'<path d="m4 5 7 7-7 7m9 0h8"/>',
    health:'<path d="M20 4c-4-3-7 0-8 2-1-2-4-5-8-2-6 5 3 14 8 18 5-4 14-13 8-18Z"/>'
  };
  const svg = name => `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${paths[name] || paths.files}</svg>`;
  const apps = [
    ['calendar','日历','#fff','calendar'],['photos','照片','#fff','photos'],['camera','相机','linear-gradient(#e0e4e7,#aab3ba)','camera','#38434b'],['notes','备忘录','#fff','notes'],['browser','浏览器','linear-gradient(#5ed5f4,#2389dc)','browser'],['files','文件','linear-gradient(#8dccff,#448cda)','files'],
    ['wechat','微信','linear-gradient(#5de672,#16b550)','wechat'],['mail','邮件','linear-gradient(#62b9f7,#287be2)','mail'],['music','音乐','linear-gradient(#f884a0,#ee4e74)','music'],['maps','地图','linear-gradient(145deg,#bfe5ac,#4bad8f)','maps'],['books','阅读','linear-gradient(#f7bf65,#ed913e)','books'],['settings','设置','linear-gradient(#b9c2cb,#87939f)','settings'],
    ['qq','QQ','linear-gradient(#6dcbee,#3996c6)','qq'],['draw','画板','linear-gradient(#d1b3e1,#aa7dbf)','draw'],['video','视频','linear-gradient(#9cb9eb,#6d90d0)','video'],['clock','时钟','#272f34','clock'],['message','信息','linear-gradient(#97dca9,#45b46f)','message'],['shopping','购物','linear-gradient(#f6b28a,#e47d61)','shopping'],
    ['weather','天气','linear-gradient(#73bfed,#4595d6)','sun'],['calculator','计算器','linear-gradient(#e8ac7a,#c78458)','calculator'],['terminal','终端','linear-gradient(#465852,#293b35)','terminal'],['health','健康','linear-gradient(#f2b1b3,#d7798c)','health']
  ].map(([id,name,bg,icon,fg]) => ({id,name,bg,icon,fg}));
  const appMap = new Map(apps.map(a => [a.id,a]));
  const folders = {tools:['calculator','clock','files','settings','terminal','weather'], create:['draw','photos','camera','notes','music','video'], connect:['wechat','qq','mail','message'], inspiration:['draw','photos','books','music','video'], efficiency:['notes','calendar','files','mail','calculator','terminal'], life:['maps','shopping','health','weather','clock','settings']};
  const folderNames = {tools:'实用工具',create:'创作',connect:'沟通',inspiration:'灵感',efficiency:'效率',life:'生活'};
  const defaultOrders = {
    classic:['calendar','photos','camera','notes','browser','folder:tools','wechat','mail','music','maps','books','settings','qq','draw','video','clock','message','shopping','weather','calculator','terminal','health','folder:create','files'],
    widgets:['wechat','photos','camera','browser','notes','books','maps','folder:tools','mail','video','draw','settings','qq','calendar','files','shopping','clock','music','health','weather','message','calculator','terminal','folder:create'],
    focus:['notes','books','browser','music','wechat','calendar','photos','camera','files','maps','mail','settings']
  };
  const state = {
    concept: concepts.some(c => c.id === storage.get('concept','classic')) ? storage.get('concept','classic') : 'classic',
    orientation:'landscape', effectiveOrientation:'landscape', selected:storage.get('selection',null), immersive:false,
    pages:{classic:0,widgets:0,focus:0}, orders:Object.fromEntries(Object.entries(defaultOrders).map(([k,v]) => {const saved=storage.get('order.'+k,null);return [k,Array.isArray(saved)&&saved.length===v.length&&new Set(saved).size===v.length&&v.every(x=>saved.includes(x))?saved:[...v]];})),
    editing:false, editTarget:null, overlay:null, query:'', playing:false, space:'work', focusRemaining:25*60, focusRunning:false,
    controls:{wifi:true,bluetooth:true,focus:false}, brightness:80, note:storage.get('note','周二，留一点时间给新想法。\n\n01  整理本周灵感\n02  完成桌面方案选择\n03  去户外走走'), lastFocus:null
  };
  let toastTimer, globalToastTimer, longPressTimer, pointerStart, suppressClickUntil=0;
  function icon(app) {
    let content = svg(app.icon);
    if(app.icon === 'calendar') content = '<small>星期二</small><b>10</b>';
    if(app.icon === 'photos') content = '<span class="flower">'+['#fa6b78','#fa9a4f','#edce49','#80c96b','#6ccdbb','#75aee8','#9c89ce','#d88cb5'].map((color,i)=>`<i style="--petal:${color};--angle:${i*45}deg"></i>`).join('')+'</span>';
    if(app.icon === 'clock') content = '<span class="clock-face"></span>';
    return `<span class="app-icon ${app.icon}" style="--icon-bg:${app.bg};--icon-fg:${app.fg || '#fff'}">${content}</span>`;
  }
  function appButton(id, context='grid') {
    if(id.startsWith('folder:')) {
      const key = id.split(':')[1];
      return `<button class="app-item${state.editTarget===id?' edit-target':''}" data-folder="${key}" data-item="${id}" data-context="${context}" aria-label="打开${folderNames[key]}文件夹" ${state.editing&&context==='grid'?'draggable="true"':''}><span class="app-icon folder-icon">${folders[key].slice(0,9).map(x=>`<i style="--mini-color:${appMap.get(x).bg}"></i>`).join('')}</span><span class="app-name">${folderNames[key]}</span></button>`;
    }
    const app = appMap.get(id); if(!app) return '';
    return `<button class="app-item${state.editTarget===id?' edit-target':''}" data-app="${id}" data-item="${id}" data-context="${context}" aria-label="打开${app.name}" ${state.editing&&context==='grid'?'draggable="true"':''}>${icon(app)}<span class="app-name">${app.name}</span>${id==='wechat'&&context==='grid'?'<span class="badge">2</span>':''}</button>`;
  }
  function statusbar() { return `<div class="statusbar"><span>9:41 <span style="font-weight:400;opacity:.6;margin-left:10px;font-size:10px">11月10日 周二</span></span><button class="control-trigger" data-action="controls" aria-label="打开控制面板演示"><span class="status-right">${svg('wifi')}<span class="battery"></span><span>80%</span></span></button></div>`; }
  function dock() {
    const items = state.concept==='focus'?['browser','notes','wechat','files']:state.concept==='studio'?['browser','mail','notes','files','calendar']:['browser','mail','photos','notes','files'];
    const pageCount = state.concept==='focus'?2:state.concept==='classic'||state.concept==='widgets'?2:0;
    return `<div class="dock-wrap"><div class="desktop-bottom">${pageCount?`<div class="page-indicator" aria-label="桌面分页">${Array.from({length:pageCount},(_,i)=>`<button data-page="${i}" aria-label="第 ${i+1} 页" aria-current="${state.pages[state.concept]===i}"></button>`).join('')}</div>`:''}<button class="search-pill" data-action="search">${svg('search')} 搜索应用</button></div><div class="dock" aria-label="常用应用">${items.map(x=>appButton(x,'dock')).join('')}${state.concept!=='focus'?'<span class="dock-separator"></span>'+appButton('settings','dock'):''}</div></div>`;
  }
  function appGrid(ids,context='grid') { return `<div class="app-grid">${ids.map(id=>appButton(id,context)).join('')}</div>`; }
  function pageApps() {const limit=state.concept==='classic'?18:state.concept==='widgets'?12:6;const offset=(state.pages[state.concept]||0)*limit;return state.orders[state.concept].slice(offset,offset+limit);}
  function content() {
    switch(state.concept) {
      case 'classic':return `<div class="desktop-content classic-layout">${appGrid(pageApps())}</div>`;
      case 'widgets':return `<div class="desktop-content widgets-layout"><div class="widget-stack"><button class="widget-card weather-widget" data-app="weather"><p class="widget-label">深圳 · 晴</p><p class="weather-degree">23°</p><span class="sun-art">☀</span><p class="weather-bottom">最高 26° · 最低 19°</p></button><button class="widget-card agenda-widget" data-app="calendar"><div class="agenda-heading"><span>11月10日</span><b>星期二</b></div><div class="agenda-event"><i class="event-bar"></i><div><strong>留点时间给新想法</strong><small>14:00 – 15:00 · 创作时间</small></div></div></button><div class="widget-card music-widget"><span class="album-art"></span><div class="music-copy"><b>午后，慢一点</b><small>Easy Radio · 演示歌单</small></div><button class="music-toggle" data-action="music" aria-label="${state.playing?'暂停音乐演示':'播放音乐演示'}">${state.playing?'Ⅱ':'▶'}</button></div></div>${appGrid(pageApps())}</div>`;
      case 'focus':return `<div class="desktop-content focus-layout"><div class="focus-greeting"><p class="focus-date">11月10日，星期二</p><h2>留白，<br>才有<em>可能。</em></h2><p class="focus-subtitle">少一点打扰。<br>把今天留给重要的事。</p><div class="focus-line"></div><button class="focus-timer" style="border:0;background:none;padding:0;text-align:left" data-action="focus" aria-label="${state.focusRunning?'暂停':'开始'}专注计时"><span class="timer-orb">${state.focusRunning?'Ⅱ':'▶'}</span><span><span id="focus-time">${formatTime(state.focusRemaining)}</span> · ${state.focusRunning?'正在专注':'开始专注'}</span></button></div><div class="focus-list"><div class="focus-list-heading"><span>只留下需要的</span><span>0${state.pages.focus+1} / 02</span></div>${pageApps().map(x=>appButton(x)).join('')}</div></div>`;
      case 'studio': {
        const spaces={work:{title:'让想法，开始发生。',label:'YOUR WORKSPACE',note:'本周，值得做的事',copy:'整理灵感，保持好奇。<br>从最想完成的一件事开始。',apps:['notes','calendar','files','mail','browser','terminal']},create:{title:'把灵感，留在这里。',label:'CREATIVE SPACE',note:'灵感不必等明天',copy:'一张草图，一段旋律。<br>给每个未完成的想法一个位置。',apps:['draw','photos','camera','music','video','notes']},life:{title:'工作之外，也有生活。',label:'EVERYDAY LIFE',note:'今天的小小计划',copy:'读两页书，听一首歌。<br>再给在意的人发一条消息。',apps:['wechat','books','maps','health','shopping','music']}};
        const space=spaces[state.space];
        return `<nav class="studio-sidebar" aria-label="工作区"><div class="studio-brand">Easy.</div>${[['work','briefcase','工作'],['create','draw','创作'],['life','home','生活']].map(([id,i,n])=>`<button class="space-tab${state.space===id?' active':''}" data-space="${id}" aria-pressed="${state.space===id}">${svg(i)}${n}</button>`).join('')}<div class="studio-sidebar-footer">给想法一个空间。<br>让每一天井然有序。</div></nav><div class="desktop-content studio-layout"><div class="studio-topline"><div><small>${space.label}</small><h2>${space.title}</h2></div><button data-action="search">＋ 打开应用</button></div><div class="studio-windows"><button class="workspace-window" data-app="notes"><div class="window-header"><i></i><i></i><i></i><span>备忘录 · 内容预览</span></div><div class="window-body"><small>NOVEMBER 10 / TUESDAY</small><h3>${space.note}</h3><p>${space.copy}</p><div class="note-rule"></div></div></button><button class="workspace-window" data-app="files"><div class="window-header"><i></i><i></i><i></i><span>文件 · 最近项目</span></div><div class="window-body"><small>继续上次的工作</small><div class="file-row">${svg('files')}EasyLauncher 灵感集</div><div class="file-row">${svg('draw')}桌面设计 · 第一个想法</div><div class="file-row">${svg('notes')}十一月的计划</div></div></button></div><div class="studio-apps">${appGrid(space.apps,'space')}</div></div>`;
      }
      case 'orbit':return `<div class="desktop-content orbit-layout"><div class="orbit-intro"><div><div class="orbit-time">9:41</div><p class="orbit-date">11月10日 · 星期二</p><h2>万事，<br>各有其位。</h2><p>不必记住第几页。<br>你的习惯，自有轨迹。</p></div><div class="orbit-weather">${svg('sun')}23° · 深圳</div></div><div class="zone-grid">${[['connect','沟通','CONNECT'],['inspiration','灵感','INSPIRE'],['efficiency','效率','CREATE'],['life','生活','LIVING']].map(([key,name,en])=>`<section class="zone"><div class="zone-heading"><span>${name}<small>${en}</small></span><button data-folder="${key}" aria-label="展开${name}分区">全部 ↗</button></div>${appGrid(folders[key].slice(0,3),'zone')}</section>`).join('')}</div></div>`;
    }
  }
  function renderDesktop() {
    const screen=$('#screen');screen.dataset.concept=state.concept;screen.classList.toggle('is-editing',state.editing);
    $('#desktop').innerHTML=statusbar()+content()+dock()+(state.editing?`<div class="edit-banner"><span>编辑桌面 · 拖动图标，或点选后移动</span><button data-action="finish-edit">完成</button></div>${state.editTarget?`<div class="edit-tools"><span>${state.editTarget.startsWith('folder:')?folderNames[state.editTarget.split(':')[1]]:appMap.get(state.editTarget)?.name}</span><button data-action="move-prev">← 前移</button><button data-action="move-next">后移 →</button></div>`:''}`:'');
  }
  function renderChrome() {
    const c=concepts.find(x=>x.id===state.concept);
    $('#concepts').innerHTML=concepts.map((x,i)=>`<button class="concept-card" data-concept="${x.id}" aria-pressed="${x.id===state.concept}" aria-label="方案 ${i+1}：${x.name}，${x.brief}"><div class="concept-card-top"><span>0${i+1}</span><span class="card-state">${state.selected?.concept===x.id?'已选择':x.id===state.concept?'体验中 ↗':'探索 ↗'}</span></div><div class="mini-preview ${x.id}" aria-hidden="true">${x.id!=='classic'?'<i class="mini-widget"></i>':''}<div class="mini-grid">${'<i></i>'.repeat(x.id==='focus'?5:12)}</div></div><div class="concept-card-bottom"><strong>${x.name}</strong><span>${x.brief}</span></div></button>`).join('');
    $('#preview-number').textContent=String(concepts.indexOf(c)+1).padStart(2,'0');$('#preview-name').textContent=c.name;
    $('#note-number').textContent=`0${concepts.indexOf(c)+1} / 05`;$('#note-name').textContent=c.name;$('#note-en').textContent=c.en;$('#note-description').textContent=c.description;
    $('#note-tags').innerHTML=c.tags.map(t=>`<span>${t}</span>`).join('');
    $('#interaction-notes').innerHTML=c.interactions.map(([title,body],i)=>`<div class="interaction-note"><span>0${i+1}</span><div><b>${title}</b>${body}</div></div>`).join('');$('#rotation-note').textContent=c.rotation;
    const selected=state.selected?.concept===state.concept;$('#select-button').classList.toggle('is-selected',selected);$('#select-button').innerHTML=selected?'已选择此方案 <span>✓</span>':'选择此方案 <span>↗</span>';
    $('#selection-status').textContent=state.selected?`当前选择：${concepts.find(x=>x.id===state.selected.concept)?.name||'未选择'} · 可随时更换`:'选择会保存在当前浏览器';
    $('#comparison-body').innerHTML=concepts.map((x,i)=>`<tr class="${state.selected?.concept===x.id?'selected-row':''}"><td><span>0${i+1}</span>${x.name}${state.selected?.concept===x.id?' ✓':''}</td><td>${x.structure}</td><td>${x.primary}</td><td>${x.reflow}</td><td>${x.fit}</td></tr>`).join('');
  }
  function render() {renderChrome();renderDesktop();renderOverlay();fitScreen();}
  function fitScreen() {
    const stage=$('#preview-stage');const style=getComputedStyle(stage);const availableW=stage.clientWidth-parseFloat(style.paddingLeft)-parseFloat(style.paddingRight);const availableH=stage.clientHeight-parseFloat(style.paddingTop)-parseFloat(style.paddingBottom);
    const orientation=state.orientation==='auto'?(window.innerWidth>=window.innerHeight?'landscape':'portrait'):state.orientation;
    state.effectiveOrientation=orientation;
    const dims=orientation==='landscape'?[1200,768]:[768,1200];const border=state.immersive?0:14;
    const scale=Math.max(.1,Math.min((availableW-border)/dims[0],(availableH-border)/dims[1]));
    const screen=$('#screen');screen.dataset.orientation=orientation;screen.style.width=dims[0]+'px';screen.style.height=dims[1]+'px';screen.style.transform=`scale(${scale})`;
    $('#device').style.width=(dims[0]*scale+border)+'px';$('#device').style.height=(dims[1]*scale+border)+'px';$('#preview-dimension').textContent=`${dims[0]} × ${dims[1]} dp`;
    $$('[data-orientation]', $('.orientation-group')).forEach(b=>b.setAttribute('aria-pressed',String(b.dataset.orientation===state.orientation)));
  }
  function openOverlay(overlay) {
    if(state.editing) {state.editing=false;state.editTarget=null;renderDesktop();}
    if(!state.overlay) state.lastFocus=document.activeElement;
    state.overlay=overlay;renderOverlay();
    requestAnimationFrame(()=>{const root=$('#overlay-root');const field=$('input',root)||$('textarea',root)||$('button',root);field?.focus({preventScroll:true});});
  }
  function closeOverlay() {state.overlay=null;$('#overlay-root').innerHTML='';const opener=state.lastFocus;if(opener?.isConnected)opener.focus({preventScroll:true});state.lastFocus=null;}
  function renderOverlay() {
    if(!state.overlay){$('#overlay-root').innerHTML='';return;}
    const o=state.overlay;let body='',title='',cls='';
    if(o.type==='folder') {title=folderNames[o.key];cls='folder-sheet';body=appGrid(folders[o.key],'folder');}
    if(o.type==='search') {title='搜索应用';cls='search-sheet';body=`<input id="app-search" class="search-input" aria-label="输入应用名称或英文 ID" placeholder="应用名称，例如：微信、notes" value="${escapeHTML(state.query)}" autocomplete="off"><p class="search-subtitle">${state.query?'搜索结果':'全部应用'} · ${apps.length} 个演示应用</p><div id="search-results" class="search-results">${searchResults()}</div>`;}
    if(o.type==='app') {
      const app=appMap.get(o.id);title=app.name;cls='app-sheet';body=demoApp(o.id);
    }
    if(o.type==='controls') {title='控制面板';cls='control-sheet';body=`<div class="control-grid">${[['wifi','wifi','无线局域网'],['bluetooth','bluetooth','蓝牙'],['focus','moon','专注模式']].map(([key,i,name])=>`<button class="control-tile ${state.controls[key]?'active':''}" data-control="${key}" aria-pressed="${state.controls[key]}">${svg(i)}${name}<small style="display:block;font-size:10px;opacity:.65;margin-top:5px">${state.controls[key]?'已开启':'已关闭'}</small></button>`).join('')}<button class="control-tile" data-action="music">${svg('music')}${state.playing?'暂停播放':'播放音乐'}<small style="display:block;font-size:10px;opacity:.65;margin-top:5px">演示状态</small></button></div><label class="control-brightness">亮度 <input id="brightness" type="range" min="30" max="100" value="${state.brightness}" aria-label="演示亮度"><output id="brightness-output">${state.brightness}%</output></label><p class="control-disclaimer">面板为交互演示。开关不更改设备设置；原生阶段系统控制中心仍由 MagicOS 提供。</p>`;}
    $('#overlay-root').innerHTML=`<div class="overlay-backdrop"><section class="overlay-sheet ${cls}" role="dialog" aria-modal="true" aria-label="${title}"><div class="overlay-header"><h2>${title}</h2>${o.type==='app'?'<span class="demo-tag">应用演示</span>':''}<button class="close-overlay" data-action="close-overlay" aria-label="关闭${title}">×</button></div>${body}</section></div>`;
  }
  function searchResults() {const query=state.query.trim().toLowerCase();const results=apps.filter(a=>a.name.toLowerCase().includes(query)||a.id.includes(query));return results.length?results.map(a=>appButton(a.id,'search')).join(''):'<p class="empty-results">没有找到相关应用，换个关键词试试。</p>';}
  function demoApp(id) {
    if(id==='notes') return `<div class="demo-app-body"><textarea id="demo-note" class="demo-notepad" aria-label="可编辑备忘录" spellcheck="false">${escapeHTML(state.note)}</textarea><p class="note-saved">可直接编辑 · 自动保存在当前浏览器</p></div>`;
    if(id==='browser') return '<div class="demo-app-body"><div class="demo-url">easylauncher.local / start</div><div class="demo-browser-content"><h3>Take it easy.</h3><p>每一个新的开始，<br>都值得一张舒服的桌面。</p><div class="demo-links"><button data-app="notes">记录灵感 ↗</button><button data-app="books">继续阅读 ↗</button></div></div></div>';
    if(id==='calendar')return `<div class="demo-app-body"><p style="font-size:16px;color:#82916f;margin:10px 0">2026 年 11 月</p><div class="demo-calendar">${['一','二','三','四','五','六','日'].map(d=>`<small>${d}</small>`).join('')}${'<span></span>'.repeat(6)}${Array.from({length:30},(_,i)=>`<span class="${i===9?'today':''}">${i+1}</span>`).join('')}</div></div>`;
    if(id==='weather')return `<div class="demo-app-summary">${icon(appMap.get(id))}<strong>深圳 · 23°</strong><p>晴，适合出门走走。<br>最高 26° · 最低 19°<br><small>此处为示例天气数据</small></p></div>`;
    if(id==='files')return `<div class="demo-app-body"><div class="file-row">${svg('files')}EasyLauncher 灵感集</div><div class="file-row">${svg('draw')}桌面设计 · 第一个想法</div><div class="file-row">${svg('notes')}十一月的计划</div><p class="note-saved">示例文件列表 · 不读取设备文件</p></div>`;
    const app=appMap.get(id);return `<div class="demo-app-summary">${icon(app)}<strong>${app.name}</strong><p>应用打开动效演示。<br>原生 Launcher 将在这里启动设备上的对应应用。</p>${id==='music'?`<button class="select-button" style="width:200px" data-action="music">${state.playing?'暂停演示':'播放演示'} <span>${state.playing?'Ⅱ':'▶'}</span></button>`:''}</div>`;
  }
  function showToast(message,global=false) {const el=$(global?'#global-toast':'#device-toast');el.textContent=message;el.classList.add('show');if(global){clearTimeout(globalToastTimer);globalToastTimer=setTimeout(()=>el.classList.remove('show'),2600);}else{clearTimeout(toastTimer);toastTimer=setTimeout(()=>el.classList.remove('show'),2200);}}
  function changeConcept(id) {if(id===state.concept)return;closeOverlay();state.concept=id;state.editing=false;state.editTarget=null;storage.set('concept',id);render();}
  function finishEdit() {state.editing=false;state.editTarget=null;renderDesktop();}
  function startEdit(id) {if(!state.orders[state.concept]){showToast('此方案采用固定分区，可通过「全部」展开');return;}closeOverlay();state.editing=true;state.editTarget=id;renderDesktop();}
  function reorder(from,to) {const order=state.orders[state.concept];if(!order)return;const a=order.indexOf(from),b=order.indexOf(to);if(a<0||b<0||a===b)return;order.splice(a,1);order.splice(b,0,from);storage.set('order.'+state.concept,order);renderDesktop();}
  function setPage(page) {if(!(state.concept in state.pages)||page<0||page>1)return;state.pages[state.concept]=page;state.editTarget=null;renderDesktop();}
  async function toggleImmersive() {
    state.immersive=!state.immersive;document.body.classList.toggle('immersive',state.immersive);$('#exit-immersive').hidden=!state.immersive;
    if(state.immersive){state.previousOrientation=state.orientation;state.orientation='auto';try{await document.documentElement.requestFullscreen?.();}catch{/* CSS immersive mode remains available. */}}
    else{state.orientation=state.previousOrientation||'landscape';if(document.fullscreenElement)await document.exitFullscreen?.().catch(()=>{});}
    fitScreen();
  }
  document.addEventListener('fullscreenchange',()=>{if(!document.fullscreenElement&&state.immersive){state.immersive=false;state.orientation=state.previousOrientation||'landscape';document.body.classList.remove('immersive');$('#exit-immersive').hidden=true;fitScreen();}});
  $('#concepts').addEventListener('click',e=>{const b=e.target.closest('[data-concept]');if(b)changeConcept(b.dataset.concept);});
  $('.orientation-group').addEventListener('click',e=>{const b=e.target.closest('[data-orientation]');if(b){state.orientation=b.dataset.orientation;fitScreen();}});
  $('#compare-button').addEventListener('click',()=>$('#comparison').scrollIntoView({behavior:matchMedia('(prefers-reduced-motion: reduce)').matches?'instant':'smooth'}));
  $('#immersive-button').addEventListener('click',toggleImmersive);$('#exit-immersive').addEventListener('click',toggleImmersive);
  $('#home-indicator').addEventListener('click',()=>{closeOverlay();finishEdit();});
  $('#select-button').addEventListener('click',()=>{const c=concepts.find(x=>x.id===state.concept);state.selected={concept:c.id,name:c.name,orientation:state.effectiveOrientation,applicationId:'com.vincenthu.launcher',selectedAt:new Date().toISOString()};storage.set('selection',state.selected);renderChrome();showToast(`已选择「${c.name}」，可继续体验或导出选择`,true);});
  $('#export-button').addEventListener('click',()=>{if(!state.selected){showToast('请先选择一个设计方案',true);return;}const c=concepts.find(x=>x.id===state.selected.concept);const blob=new Blob([JSON.stringify({project:'EasyLauncher',...state.selected,rotation:c.rotation,prototypeVersion:'0.1.0'},null,2)],{type:'application/json'});const url=URL.createObjectURL(blob);const a=document.createElement('a');a.href=url;a.download=`EasyLauncher-${c.id}-selection.json`;a.click();setTimeout(()=>URL.revokeObjectURL(url),1000);showToast('方案选择已导出',true);});
  $('#reset-button').addEventListener('click',()=>{closeOverlay();finishEdit();if(state.orders[state.concept]){state.orders[state.concept]=[...defaultOrders[state.concept]];state.pages[state.concept]=0;storage.set('order.'+state.concept,state.orders[state.concept]);}if(state.concept==='studio')state.space='work';if(state.concept==='focus'){state.focusRunning=false;state.focusRemaining=1500;}if(state.concept==='widgets')state.playing=false;state.brightness=80;$('#screen').style.filter='';renderDesktop();showToast('已恢复此方案的初始布局');});
  $('#screen').addEventListener('click',e=>{
    if(Date.now()<suppressClickUntil){e.preventDefault();return;}
    const btn=e.target.closest('button');
    if(!btn){if(e.target.classList.contains('overlay-backdrop'))closeOverlay();return;}
    const action=btn.dataset.action;
    if(action){
      if(action==='search'){state.query='';openOverlay({type:'search'});}
      if(action==='controls')openOverlay({type:'controls'});
      if(action==='close-overlay')closeOverlay();
      if(action==='finish-edit')finishEdit();
      if(action==='focus'){state.focusRunning=!state.focusRunning;renderDesktop();}
      if(action==='music'){state.playing=!state.playing;renderDesktop();if(state.overlay){renderOverlay();}showToast(state.playing?'音乐播放状态演示 · 不播放音频':'已暂停演示');}
      if(action==='move-prev'||action==='move-next'){const order=state.orders[state.concept];const index=order.indexOf(state.editTarget);const target=index+(action==='move-prev'?-1:1);if(target>=0&&target<order.length){const id=state.editTarget;reorder(id,order[target]);state.pages[state.concept]=Math.floor(target/(state.concept==='classic'?18:state.concept==='widgets'?12:6));renderDesktop();}else showToast('已经到达边界');}
      return;
    }
    if(btn.dataset.control){state.controls[btn.dataset.control]=!state.controls[btn.dataset.control];renderOverlay();return;}
    if(btn.dataset.space){state.space=btn.dataset.space;renderDesktop();return;}
    if(btn.dataset.page!==undefined){setPage(Number(btn.dataset.page));return;}
    if(state.editing&&btn.dataset.context==='grid'){state.editTarget=btn.dataset.item;renderDesktop();return;}
    if(btn.dataset.folder){openOverlay({type:'folder',key:btn.dataset.folder});return;}
    if(btn.dataset.app){openOverlay({type:'app',id:btn.dataset.app});}
  });
  $('#screen').addEventListener('input',e=>{
    if(e.target.id==='app-search'){state.query=e.target.value;$('#search-results').innerHTML=searchResults();$('.search-subtitle').textContent=`${state.query?'搜索结果':'全部应用'} · ${apps.filter(a=>a.name.includes(state.query.trim())||a.id.includes(state.query.trim().toLowerCase())).length} 个应用`;}
    if(e.target.id==='demo-note'){state.note=e.target.value;storage.set('note',state.note);}
    if(e.target.id==='brightness'){state.brightness=Number(e.target.value);$('#brightness-output').textContent=state.brightness+'%';$('#screen').style.filter=`brightness(${.65+state.brightness*.0035})`;}
  });
  $('#screen').addEventListener('pointerdown',e=>{
    if(e.button!==0||e.target.closest('#overlay-root')||e.target.closest('.home-indicator'))return;
    const b=e.target.closest('.app-item[data-context=grid]');pointerStart={x:e.clientX,y:e.clientY,target:e.target,id:b?.dataset.item,pointerId:e.pointerId};
    if(b&&!state.editing)longPressTimer=setTimeout(()=>{suppressClickUntil=Date.now()+600;startEdit(b.dataset.item);pointerStart=null;},560);
  });
  $('#screen').addEventListener('pointermove',e=>{if(pointerStart&&Math.hypot(e.clientX-pointerStart.x,e.clientY-pointerStart.y)>10)clearTimeout(longPressTimer);});
  $('#screen').addEventListener('pointerup',e=>{
    clearTimeout(longPressTimer);if(!pointerStart)return;
    const dx=e.clientX-pointerStart.x,dy=e.clientY-pointerStart.y;
    if(state.editing&&pointerStart.id){const target=document.elementFromPoint(e.clientX,e.clientY)?.closest('.app-item[data-context=grid]');if(target&&target.dataset.item!==pointerStart.id){reorder(pointerStart.id,target.dataset.item);suppressClickUntil=Date.now()+400;}}
    else if(Math.abs(dx)>55&&Math.abs(dx)>Math.abs(dy)*1.4){setPage((state.pages[state.concept]||0)+(dx<0?1:-1));suppressClickUntil=Date.now()+400;}
    pointerStart=null;
  });
  function cancelPointer(){clearTimeout(longPressTimer);pointerStart=null;}
  $('#screen').addEventListener('pointercancel',cancelPointer);$('#screen').addEventListener('pointerleave',()=>clearTimeout(longPressTimer));
  window.addEventListener('pointerup',cancelPointer);window.addEventListener('blur',cancelPointer);
  $('#screen').addEventListener('contextmenu',e=>{const b=e.target.closest('.app-item[data-context=grid]');if(b){e.preventDefault();startEdit(b.dataset.item);}});
  $('#screen').addEventListener('dragstart',e=>{const b=e.target.closest('.app-item[data-context=grid]');if(b&&state.editing){e.dataTransfer.setData('text/plain',b.dataset.item);e.dataTransfer.effectAllowed='move';}else e.preventDefault();});
  $('#screen').addEventListener('dragover',e=>{if(state.editing&&e.target.closest('.app-item[data-context=grid]'))e.preventDefault();});
  $('#screen').addEventListener('drop',e=>{const b=e.target.closest('.app-item[data-context=grid]');if(b&&state.editing){e.preventDefault();reorder(e.dataTransfer.getData('text/plain'),b.dataset.item);suppressClickUntil=Date.now()+400;}});
  document.addEventListener('keydown',e=>{
    const input=/INPUT|TEXTAREA|SELECT/.test(e.target.tagName);
    if(e.key==='Escape'){if(state.overlay){closeOverlay();e.preventDefault();}else if(state.editing){finishEdit();e.preventDefault();}else if(state.immersive)toggleImmersive();return;}
    if(state.overlay&&e.key==='Tab'){const fields=$$('button,input,textarea,select,a[href]',$('#overlay-root')).filter(el=>!el.disabled);const first=fields[0],last=fields[fields.length-1];if(e.shiftKey&&document.activeElement===first){last.focus();e.preventDefault();}else if(!e.shiftKey&&document.activeElement===last){first.focus();e.preventDefault();}return;}
    if(input)return;
    if(e.key==='/'){e.preventDefault();state.query='';openOverlay({type:'search'});}
    if((e.key==='ArrowLeft'||e.key==='ArrowRight')&&!state.overlay&&!state.editing){setPage((state.pages[state.concept]||0)+(e.key==='ArrowRight'?1:-1));e.preventDefault();}
  });
  function formatTime(seconds){return `${Math.floor(seconds/60).toString().padStart(2,'0')}:${(seconds%60).toString().padStart(2,'0')}`;}
  let previousTick=Date.now();
  setInterval(()=>{const now=Date.now(),elapsed=Math.floor((now-previousTick)/1000);if(elapsed<1)return;previousTick=now;if(state.focusRunning){state.focusRemaining=Math.max(0,state.focusRemaining-elapsed);if($('#focus-time'))$('#focus-time').textContent=formatTime(state.focusRemaining);if(state.focusRemaining===0){state.focusRunning=false;if(state.concept==='focus')renderDesktop();showToast('这一段专注已完成。休息一下吧。',state.concept!=='focus');}}},1000);
  const observer=new ResizeObserver(fitScreen);observer.observe($('#preview-stage'));window.addEventListener('resize',fitScreen);window.addEventListener('orientationchange',()=>requestAnimationFrame(fitScreen));
  render();
})();
