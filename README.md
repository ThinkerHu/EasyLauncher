# EasyLauncher

独立 Android 平板桌面工程，包名 `com.vincenthu.launcher`。采用「云山」布局：山景背景、天气与日历双卡片、真实应用图标、分页网格和底部常驻栏。

仓库地址：[ThinkerHu/EasyLauncher](https://github.com/ThinkerHu/EasyLauncher)。本仓库包含原生 Android 应用、H5 设计原型和设计 / 验证文档，可独立克隆、构建与开发。

## 当前实现

- Compose 原生 HOME 应用，Android 8 及以上；compile SDK 36、target SDK 35，当前版本 `0.2.0`。
- 读取设备上可启动的应用，保留真实图标和名称，通过组件 Intent 启动；系统安装、卸载或更新后刷新应用库。
- 横屏 7 列 × 2 行、竖屏 4 列 × 4 行，每页最多 14 个入口；旋转时保持应用顺序、搜索状态和已保存设置。
- 全部应用库支持名称 / 包名搜索；编辑模式支持拖动换位、移除入口、加入常驻栏、应用信息和最多 4 个常驻应用。
- 支持新建、重命名、打开、解散文件夹；应用顺序、文件夹、常驻栏、配色、天气城市和天气缓存保存到本机。
- 日期来自设备系统；天气接入和风 Weather v1 与 Geo v2，支持城市搜索、实况、逐小时 / 多日预报、体感、湿度、风速及日出日落。桌面天气卡片支持上下滑动切换城市，联网失败时保留缓存并提示。
- 天气背景随当前城市实况及昼夜变化，支持晴光、流云、雨雪、雾气和星空。省电模式、减少动态效果或桌面不可见时停止背景连续动画。
- Dock 按实际图标数量自适应宽度，采用四宫格全部应用入口、固定常驻区和最多 3 个最近使用应用；按包名去重，常驻位置稳定，记录持久保存。
- 日历通过 `CalendarContract.Instances` 读取当天最多 5 项事件；首次使用由系统请求 `READ_CALENDAR`，拒绝后仍可正常使用桌面。
- 支持 `AppWidgetHost` 系统小组件：选择 provider、走系统绑定确认、处理配置 Activity、持久化 widgetId、旋转重建和编辑模式移除。
- 动效覆盖首屏卡片与应用错峰入场、天气状态切换、分页滑动、编辑态轻微倾斜、常驻栏图标和浮层过渡；首帧使用云山色启动背景，减少 Compose 初始化时的白屏闪现。
- 桌面翻页保持手指连续跟手，释放后用弹簧吸附到相邻页；浮层与桌面同步做短距离缩放 / 位移和遮罩淡入淡出，图标点按有轻微按压反馈。设置中的“减少动态效果”可以关闭这些效果，并跟随系统动画时长设置。
- 使用 edge-to-edge 和 `adjustResize` 处理系统栏、输入法与横竖屏安全区域。设置显示当前默认桌面状态；“设为默认桌面”通过 Android RoleManager / 系统默认应用页请求用户确认，不会在应用内静默替换。

## 构建原生版

使用 JDK 17、Android SDK 36 和 Build Tools，设置 `ANDROID_HOME` 或在 `android/local.properties` 中配置 `sdk.dir`。工程自带 Gradle wrapper。

```bash
git clone https://github.com/ThinkerHu/EasyLauncher.git
cd EasyLauncher/android
# 确保 JAVA_HOME 指向 JDK 17，并配置 Android SDK 路径。
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain
# 发布校验（当前为未签名 release 产物）
./gradlew :app:assembleRelease :app:lintRelease --console=plain
```

首次构建需要联网下载 Gradle 和 Maven 依赖；依赖已缓存后可使用 `--offline`。`android/local.properties`、Gradle 缓存和构建产物均不入库。

APK 输出：`android/app/build/outputs/apk/debug/app-debug.apk`。当前调试签名仅用于开发验证；量产发布仍需要正式 keystore、release 签名、R8 策略和完整设备矩阵。

安装与启动（将 `DEVICE_SERIAL` 替换为目标设备序列号）：

```bash
adb -s DEVICE_SERIAL install -r app/build/outputs/apk/debug/app-debug.apk
adb -s DEVICE_SERIAL shell am start -a android.intent.action.MAIN -c android.intent.category.HOME -n com.vincenthu.launcher/.MainActivity
```

## 配置天气

在应用的天气页面进入连接设置，填写自己的和风 API Host 和 API Key。凭据通过 Android Keystore 加密保存在当前设备，不写入源码或普通偏好设置；仓库不包含任何可直接使用的账户密钥。未配置时仍可使用桌面其他功能。

## H5 设计原型

```bash
cd EasyLauncher
npm run dev
```

打开 <http://127.0.0.1:8765/?design=cloud> 预览方案一「云山」。H5 无 npm 依赖、无 CDN、无外部字体 / 图片请求，保留十套方向、横竖屏切换、文件夹、应用库、编辑和选择交互。

| 方案 | 桌面结构 | 横屏 → 竖屏 |
| --- | --- | --- |
| **云山 · 已选** | **天气 / 日历 + 经典应用网格 + 常驻栏** | **7 → 4 列** |
| 光屿 | 天气、日程、音乐组件增强 | 8 → 4 列 |
| 静界 | 大时钟与疏朗图标 | 6 → 4 列 |
| 书桌 | 应用网格与右侧信息区 | 侧栏 / 6 列 → 底栏 / 4 列 |
| 星格 | 分类筛选与高密度图标 | 8 → 6 列 |
| 素纸 | 单色图标、日期与天气 | 6 → 4 列 |
| 轻彩 | 大图标、简明天气与常驻栏 | 5 → 3 列 |
| 夜航 | 深色壁纸、时钟与音乐条 | 7 → 4 列 |
| 分区 | 大文件夹与常用图标 | 文件夹并排 → 2×2 |
| 自在 | 照片 / 便签 / 天气与图标混排 | 左右 → 上下 |

旧方案完整归档在 `prototypes/archive-v1/` 和 `prototypes/archive-v2/`。

## 数据与量产边界

天气服务使用用户配置的和风账户。生产环境需要确认服务订阅、额度、商用授权、隐私政策与分发时的凭据方案。应用不申请定位权限，按用户所选城市请求天气。系统日历数据只在本机查询；系统小组件的数据和权限由 provider 负责。详细说明见 [data-sources.md](design/data-sources.md)。

尚未完成的量产工作包括正式签名、签名后产物审计、完整自动化测试、多型号 / 多字体 / 多窗口设备矩阵、网络安全审查和崩溃上报策略。音乐 MediaSession 卡片暂未作为内置数据源实现，可通过系统小组件扩展。

## 验证记录

`npm run check` 检查当前和历史 JavaScript 语法。Android 使用 `assembleDebug`、`testDebugUnitTest` 和 `lintDebug`，当前包含 25 项单元测试；华为 BAH3-W59（Android 10 / API 29，1200×2000，320 dpi）已进行横竖屏、编辑整理、文件夹持久化、天气联网 / 缓存、日历权限及系统小组件验证。默认桌面是否生效取决于设备固件的角色 / 默认应用政策，应用保留标准 Android 请求流程并显示实际状态。详细记录见 [validation-v3.md](design/validation-v3.md)、[weather-validation-2026-11-18.md](design/weather-validation-2026-11-18.md)、[dock-validation-2026-11-18.md](design/dock-validation-2026-11-18.md) 和 [production-plan.md](design/production-plan.md)。本地设备截图与 UI 层级文件位于 `qa/`，该目录不提交到仓库。

```text
EasyLauncher/
├── android/                 # 独立 Gradle / Compose 工程
│   └── app/src/main/        # Manifest、应用模型、天气 / 日历 / widget、桌面 UI
├── prototypes/              # H5 方案选择与交互原型
└── design/                  # 数据边界、设计规格、验证和量产计划
```
