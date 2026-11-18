# EasyLauncher 云山 · 真机验证记录

日期：2026-11-11。设备：荣耀 BAH3-W59，Android 10 / API 29，物理分辨率 1200×2000，320 dpi。应用包名 `com.vincenthu.launcher`，版本 `0.2.0`。

## 构建

- `npm run check` 通过，覆盖当前第三轮原型和两套历史原型 JavaScript。
- JDK 17、Gradle 8.13、AGP 8.13.2、Kotlin 2.1.0；compile SDK 36、target SDK 35、min SDK 26。
- `assembleDebug lintDebug --offline --console=plain` 成功，Android Lint 0 errors。
- 调试 APK 安装成功；应用版本查询为 `0.2.0`。量产 release 已启用 R8，但正式签名尚未配置。

## 阶段一：云山布局

- 横屏 UI 为 7 列 × 2 行，竖屏 UI 为 4 列 × 4 行，每页最多 14 个入口。
- 天气和日历双卡片、页码、应用库、底部常驻栏可见；方向切换保持应用顺序。
- 设备截图和层级文件：`qa/cloud-phase1/landscape.png`、`portrait.png`、`landscape.xml`、`portrait.xml`、`page2.xml`、`search.xml`。

## 阶段二：整理与持久化

- “整理”进入编辑模式；拖动应用可直接换位，非编辑模式仍以点按启动、长按打开操作菜单。
- 新建 QA 文件夹、重命名为 `QA_Renamed`、强制停止并重启后名称和成员恢复。
- 解散文件夹后检查 SharedPreferences，`folders` 恢复为空；测试数据已清理。
- 应用顺序、常驻栏、配色和文件夹配置均使用本机 SharedPreferences 保存。

## 阶段三：天气与日历

- 联网天气真实返回深圳和北京数据；切换城市后 UI 更新为“北京 · 天气 / 7° 晴 / 0° — 11°”。
- 临时关闭 Wi-Fi、强制停止并重启后仍显示北京缓存天气；随后恢复 Wi-Fi，设备网络状态恢复。
- 日历首次点击弹出系统 `READ_CALENDAR` 授权；拒绝时保持“授权后显示今天的安排”，允许后查询当天实例。设备当天无事件，空状态符合设计。
- 记录：`stage3-start.xml`、`calendar-permission.xml`、`calendar-denied.xml`、`calendar-granted.xml`、`weather-dialog.xml`、`weather-beijing.xml`、`stage3-offline2.xml`。

## 阶段四：系统小组件

- 设置中列出设备公开的 AppWidgetProvider；选择 AI News 后出现系统确认“是否允许该应用创建并访问窗口小部件”。
- 点击“创建”后 provider 的 RemoteViews 显示在云山信息区下方；调试截图为 `qa/cloud-phase1/widgets-added.png`。
- 强制停止并重启后小组件仍恢复；切到竖屏后小组件和 4 列应用网格均可见，截图为 `widgets-portrait.png`。
- 编辑模式显示“移除”，移除后 widgetId 从模型和本机配置清除；测试结束 `widgets=[]`，未留下测试小组件。
- provider 内容由第三方决定，层级树对 RemoteViews 文本可能不可见，以截图和宿主节点存在性为准。

## 稳定性与系统状态

- 多次安装、启动、强制停止、旋转和浮层操作后，crash buffer 未发现 `FATAL EXCEPTION`。
- 自动旋转已恢复为 `accelerometer_rotation=1`、`user_rotation=0`；未将 EasyLauncher 自动设为默认桌面，默认 HOME 仍由系统确认流程控制。
- 尚未覆盖：release 签名、完整荣耀 / Android 版本矩阵、字体缩放、多窗口、长时间待机、provider 卸载后的全量组合测试、媒体会话内置卡片。

## 发布加固

- `assembleDebug assembleRelease lintDebug lintRelease --offline --console=plain` 通过；debug 与 release Lint 均无错误。
- release 已启用 R8 minification、资源压缩和优化默认规则，产物为 `android/app/build/outputs/apk/release/app-release-unsigned.apk`，包名和版本已用 `aapt` 核对。
- release 产物当前未签名，正式 keystore、签名校验、商店上传和完整设备矩阵仍需在发布阶段完成。

## 云山视觉细化（2026-11-12）

- 依据 `qa/v3/cloud-landscape-desktop.png` 和 `cloud-portrait-desktop.png` 对原生界面重新校准：柔和山脊背景、顶部留白、天气 / 日历卡比例、天气主温度字号、日程空状态、搜索胶囊、页点和常驻栏分隔线。
- 天气图标改为 Compose Canvas 绘制，避免 Android Emoji 字体在不同荣耀系统版本上出现样式漂移。
- 横屏与竖屏重新安装验证通过；最终截图为 `qa/cloud-refinement/final-landscape.png` 和 `final-portrait.png`。
- UI Automator 能读取 `EasyLauncher`、`编辑桌面`、`设置` 和当前城市天气文本；最终设备自动旋转仍为开启状态，未发现崩溃。

## 动效与启动加固（2026-11-12）

- 主屏进入增加天气 / 日历卡片错峰淡入滑入、应用网格按位置错峰淡入缩放、文件夹与常驻栏图标入场、系统小组件入场和页面滑动时轻微透明度 / 缩放过渡。
- 天气状态使用可打断的 `AnimatedContent`，加载、成功、错误和城市切换时温度、图标与高低温平滑替换；编辑态图标以轻微交替倾斜提示可拖动。
- 搜索、天气、设置、文件夹和应用整理浮层使用淡入缩放进入；动效时长控制在 180–460 ms，避免阻塞点击和翻页。
- 发现荣耀设备在 `adb install -r` 的应用替换期间会显示系统提示“EasyLauncher 正在自动升级，需要关闭应用”，该提示不属于 EasyLauncher；不重新安装、直接 `force-stop` / `start` 后不会出现。
- Activity 主题增加与云山首帧一致的启动背景，真机重启采样 `0.2 s`、`0.65 s` 和 `1.5 s`：前两帧保持云山色背景，最终进入完整桌面，未再出现白屏闪烁。截图：`qa/cloud-refinement/theme-restart-02s.png`、`theme-restart-065s.png`、`theme-restart-final.png`。
- 真机点按天气卡打开天气浮层，确认浮层进入动画、城市筛选与关闭流程；左右滑动桌面后 UI Automator 读取到“第 2 页，共 5 页”，再返回第一页。浮层截图：`qa/cloud-refinement/weather-modal-final.png`；翻页截图：`qa/cloud-refinement/pager-motion-final.png`。
- 动效改动后的 `assembleDebug lintDebug` 和 `assembleRelease lintRelease` 均通过；应用已安装至 目标验证平板，强制停止 / 重启后 `mResumedActivity` 为 `com.vincenthu.launcher/.MainActivity`，未发现崩溃。

## 页面动效细化（2026-11-12）

- 对齐 Apple 当前 iOS 27 交互方向，并按 Apple HIG 的 motion 指引实现：动效跟随直接触摸、短促且可取消、有明确的进出方向；频繁操作避免多余的整页淡化，减少动态效果可选。此处复用交互原则，不复刻 Apple 私有实现或 Liquid Glass 视觉。
- 平板原先 `window_animation_scale`、`transition_animation_scale`、`animator_duration_scale` 均为 `0`，此前 Compose 动效被系统全局设置直接跳过。用户已选择恢复为 `1×`，应用另提供持久化“减少动态效果”开关，并监听系统 animator duration scale。
- `HorizontalPager` 使用带阻尼的弹簧吸附；页面在拖动中直接跟手，邻页预加载，缩放幅度限制在 1.2%，不在每页下叠加 Alpha 混合。云山背景只做轻微水平视差，页码胶囊跟随拖动连续移动。
- 浮层保留单一 Dialog 生命周期至收起动画结束，遮罩使用 Compose 绘制并随弹簧退出；卡片短距离上移、淡入和微缩放。浮层内容采用独立手势边界，确保点击卡片空白区域不会穿透到遮罩并意外关闭。
- 卡片、图标进入动画在真实 `MutableTransitionState` 初值下触发；修正原先部分 `AnimatedVisibility(visible = true)` 从一开始已处于可见终态、没有实际入场过渡的问题。编辑工具行采用高度和透明度过渡，图标点按提供 0.94 倍轻压反馈。
- 真机发布构建回归共 14 项通过：系统关闭减少动态效果、强制停止 / 重启后偏好保留；应用库搜索输入；系统返回先收起键盘；打开应用库后旋转到竖屏时保留弹层和查询；竖屏天气浮层打开、卡片空白点击不误关闭、返回关闭；恢复横屏；重复打开 / 关闭浮层后无残留。UI 自动化记录：`qa/ios-motion/interaction-checks.json`。
- 真机录屏和逐帧检查：`qa/ios-motion/release-transitions.mp4`、`release-transition-review.png`。最终设备保持自动旋转开启、竖屏角度复位为 0，HOME 仍由荣耀系统确认管理；release 签名验证用的本地 debug key 仅用于设备回归，release 交付包仍为 unsigned。
- `assembleDebug assembleRelease lintDebug lintRelease --offline --console=plain` 通过。帧性能采样见 `qa/ios-motion/pager-warm-release-gfxinfo.txt`；设备可见动效，采样同时受设备负载与同步触控影响，不作为稳定 60 fps 认证。尚需针对目标荣耀机型做更长时间、无录屏条件下的帧时间测试。
# 默认桌面设置验证（2026-11-16）

- 安装后通过 PackageManager 确认 `com.vincenthu.launcher/.MainActivity` 同时声明 `MAIN + HOME + DEFAULT` 与普通 `MAIN + LAUNCHER`；桌面设置入口可见。
- 应用通过 Android 10 `RoleManager.ROLE_HOME` 请求系统确认；从 Activity 恢复时读取 HOME Role holder 和系统 HOME resolver，设置页据此显示是否已生效。
- 当前荣耀 BAH3-W59 固件的系统角色确认器实际拒绝 EasyLauncher（PermissionController 日志：`Component ... not allowed to be default home`），系统“桌面”设置列表只列出华为桌面。设备当前默认值未被更改。
- 这是该测试固件的系统限制，应用无法越过系统授权策略；其他 Android 设备仍需各自实机验证默认桌面确认流程。

# 桌面间距与 Dock 优化验证（2026-11-17）

- 横屏卡片区域由 190 dp 缩至 164 dp；竖屏由 156 dp 缩至 132 dp，紧凑模式分别为 124 / 96 dp。卡片与应用网格之间增加 16 dp（紧凑模式 8 dp）间距。
- Dock 改为分层半透明玻璃质感、细边框和低频流动高光；图标行占满并居中分布。最右侧改为最近一次从 EasyLauncher 启动的应用图标，并保存在本地偏好中；实机启动备忘录后返回验证通过。
- 应用库 Dialog 使用与动画进度同步的平台窗口 dim，统一覆盖系统栏和内容区；荣耀 Android 10 实机截图确认状态栏上沿不再出现分界暗条。
- `assembleDebug lintDebug --offline --console=plain` 通过；已安装到 BAH3-W59，桌面和应用库可正常显示，前台 Activity 正常且未发现崩溃。

## 编辑状态与图标菜单（2026-11-17）

- 编辑操作移入原有顶栏，不再新增工具行挤压网格；桌面图标取消编辑时倾斜和按压缩放，改为省略号角标。真机横屏 14 个应用图标编辑前后的边界完全一致（124 × 124 px），竖屏同样保持一致（136 × 136 px）。
- 桌面、Dock、应用库和文件夹中的应用图标使用独立锚点菜单，宽 228 dp，自动避让屏幕边缘和底部。位置调整归入二级菜单；名称和操作项直接读取当前模型，避免首次加载时缺失。
- 华为 BAH3-W59 真机回归 13 项通过，覆盖编辑尺寸、角标、编辑时点按菜单、位置二级菜单及返回、系统返回仅关闭菜单、右边缘避让、Dock 向上展开、应用库内菜单及关闭后保留应用库、竖屏尺寸、运行日志。自动旋转已恢复开启。
- assembleDebug / lintDebug 通过，最新 APK 已安装并启动。验证结果、XML 和截图保存在 qa/edit-context-menu/interaction-checks.json 及同目录。

## 图标拖动与跨屏整理（2026-11-17）

- 编辑模式下图标长按后进入拖动：原位置淡出，悬浮图标以弹簧缩放和阴影跟随手指，目标格交换时使用网格布局动画回弹。
- 交互分流：普通模式长按图标打开附近菜单；编辑模式短按不再弹菜单，长按并拖动图标调整位置。
- 手指停留在分页区域左右边缘时自动翻到相邻页，拖动状态持续；松手后顺序写入本机配置。普通横向翻页在编辑模式关闭，避免与图标拖动手势冲突。
- 华为 BAH3-W59 横屏真机检查了拖动预览、目标图标交换、跨页自动翻页和安装重启；拖动中截图保存在 `qa/icon-drag/cross-page-drag.png`。本轮最新代码完成 `assembleDebug lintDebug --offline --console=plain`，构建成功并重新安装启动。
- 编辑模式拖动到常驻栏可将应用加入 Dock 并从桌面网格移除；Dock 最多 4 个固定应用，已满时显示提示且松手不更改位置。文件夹不接受拖入 Dock。
