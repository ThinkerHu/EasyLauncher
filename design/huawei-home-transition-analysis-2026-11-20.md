# 当前华为固件的桌面返回动画实现核查

日期：2026-11-20。设备：Huawei BAH3-W59，Android 10 / API 29；序列号 AKHBB21603200297。结论限定本机固件，不外推至其他 HarmonyOS / EMUI 版本。

## 结论

华为原生桌面包含比 EasyLauncher 当前窗口缩放更完整的返回动画。原生实现有窗口级图标衔接，以及使用远程窗口控制的手势返回路径。因此不能把“第三方桌面当前没有权限”描述成“这台设备无法实现”。

本机 EasyLauncher 已是默认 HOME，但系统最近任务组件仍为华为桌面。设备端实测确认，EasyLauncher 自身 UID 调用华为的图标信息接口时，系统拒绝接受。仅添加桌面属性、反射调用方法或修改动画曲线不足以启用原生完整路径。

## 原生实现的两条路径

### 系统管理的应用退出到图标

从设备的 /system/app/HwLauncher6/HwLauncher6.apk 及对应预编译 OAT 导出实际代码，检查 AppSwitchUtil、ExitAppAnimationManager。桌面通过 com.huawei.android.app.WindowManagerEx.setAppWindowExitInfo(Bundle, Bitmap) 将图标位图、中心位置、尺寸等信息传给系统。Bundle 使用 pivotX、pivotY、iconWidth、iconHeight 等字段。

同机 hwServices 的 HwWindowManagerServiceEx 在接受图标信息前调用 isUidCanSetIcon：桌面退出动画分支要求 mRecentTasks.isCallerRecents(callingUid)，另一浮窗分支要求功能开启且与系统 UID 1000 签名匹配。

另有独立的窗口匹配条件：HwWmConstants.containsLauncherCmpName 只识别本固件中的华为 UniHomeLauncher、DrawerLauncher 和 NewSimpleLauncher 组件。它不等价于查询当前用户默认 HOME。loadAppWindowExitToLauncherAnimation / isAppExitToLauncher 使用该条件选择动画。

HwAppTransitionImpl.createAppExitToIconAnimation 按真实窗口尺寸和图标尺寸分别计算横纵缩放，使用分段曲线。当前平板分支 FIND_ICON_ANIM_DURATION 为 550ms。系统还可通过窗口动画器传递图标信息，完成应用画面与图标的衔接；这不是普通桌面 View 的缩放动画。

### 远程窗口控制的手势返回

HwLauncher 的 ReturnToHomeAnim 与 WindowTransformSwipeHandler 使用 RemoteAnimationTargetCompat、ClipAnimationHelper 和同步 Surface 事务。返回动画分别计算横向位移、纵向位移、缩放和裁切，使用弹簧插值，并承接手势速度。相关裁切/圆角能力来自窗口 Surface 控制路径，不能只复制参数到普通 Compose 图层后就获得其他应用窗口的控制权。

这两条路径均存在于原生代码中；本次没有切换系统导航模式或录制华为默认桌面，不能将代码中存在的每个分支都声称为当前导航模式已经执行的分支。

## 本机验证

- dumpsys activity recents：mRecentsUid=10091，mRecentsComponent=com.huawei.android.launcher/com.huawei.android.launcher.quickstep.RecentsActivity。
- EasyLauncher 包 UID=10671；默认 HOME 解析仍为 com.vincenthu.launcher/.MainActivity。
- 先使用 run-as / app_process 做一次探测，但该环境属于 runas_app SELinux 域，访问 window 服务被拒绝，出现空服务引用。该结果不能当作正常 App 进程的调用结论。
- 为排除上述干扰，另制作与 Debug 应用同签名的临时 instrumentation APK，在 EasyLauncher 正常目标进程内调用接口。参数中的 launcherPkgName 使用 EasyLauncher 自身包名，不冒用华为身份；非空 Bundle、空 Bitmap，避免上传实际图标内容。静态检查确认非空 Bundle 的调用先经过 UID 校验，与 Bitmap 是否为空无关。
- instrumentation 输出 callerUid=10671、targetPackage=com.vincenthu.launcher，Binder 正常返回。随后系统日志明确记录：`HwWindowManagerServiceEx: callingUid is not qualified!`。该接口返回 void，正常返回不代表数据被系统接受。
- 因为服务已在 UID 校验处拒绝，未将这条调用集成到产品代码，也未声称触发原生窗口动画。上面的组件匹配限制来自同机实现的静态核查。
- 临时 instrumentation APK、应用私有目录中的探测 dex 和 /data/local/tmp 下的导出文件均已清理。未修改默认桌面、系统组件、系统导航模式或设备权限配置。

## 第一轮调研时的处理

保留上一次已安装并验证的缩放、独立归位、末段淡出和桌面回落实现。第一轮只补充调研文档与本地 QA 证据，没有更改正式应用代码或重新替换 APK；后续实现见下一节。

可以借鉴原生运动的分段、缩放与位移分离、横竖屏适配及图标衔接思路；完整的窗口圆角裁切、图标位图融合与跟手手势需要获得相应系统窗口控制能力。本次结果不支持通过普通 Manifest 属性直接启用这些能力。

原始 APK、导出的 dex、关键类反编译结果、临时测试源码、instrumentation 输出与系统日志保存在 qa/huawei-launcher-transition-2026-11-20/。QA 目录被 Git 忽略；不将华为二进制或反编译源码复制进应用源码或纳入提交。


## 基于调研继续实现：独立轴缩放与图标尺寸收尾

- 实验过原生 cliprect 矩形裁切，编译与功能验证通过，但实机逐帧图没有呈现预期裁切。内部资源格式兼容性不满足要求，因此撤回裁切代码、专用资源与选择分支。QA 录像保存于 qa/home-return-crop-2026-11-20/；不把该实验描述为成功实现裁切。
- 改为借鉴华为窗口动画的独立轴缩放：使用公开 scale 动画资源属性，将横纵轴的终态尺寸都设为 60dp，原生 ScaleAnimation 根据真实窗口尺寸分别换算缩放比例。该终态不依赖固定屏幕分辨率或横竖屏比例。
- 在当前 2× 密度平板上，终态为 120×120px，与实测桌面图标 124/136px、Dock 图标 112px 接近；它是统一的视觉终态，并非每个图标逐像素匹配。桌面和 Dock 仍使用各自实时位置。
- 550ms 收缩与独立归位，340–510ms 渐隐；保留阻尼节奏和桌面回落。形状变化来自横纵轴缩放比例不同，不能描述为真实窗口裁切或圆角变形。
- 保留原有 2601 个动画 ID，未新增实验性的私有属性或裁切变体。不改变 HOME 生命周期、应用库关闭和 Dock 重排落点处理。
- 目标桌面图标、Dock 图标或包含应用的文件夹在窗口渐隐末段轻微放大至 110%，再以阻尼回落。仅对实际选中的目标触发；关闭动态效果、普通 BACK、无有效目标时不触发。新返回取消旧动效，不排队播放。

### 本轮正式版验证

- Debug / Release 构建、36 项单元测试、lintDebug 全部通过；Release 资源表确认 2601 个返回动画 ID 保留。
- 正式 APK 覆盖安装至 AKHBB21603200297。横屏、竖屏各检查桌面计算器、桌面设置、Dock 计算器、应用库计算器，共 8 项启动及 HOME 返回均通过。
- 对上述 8 项录像逐帧检查，观察到真实窗口向对应图标位置收缩，末段长宽比向方形变化；未观察到黑边或左右侧滑。局部放大序列确认目标图标有放大后回落的接合效果。未宣称窗口圆角裁切或图标位图融合已经实现。
- 额外回归：BACK 恢复可见桌面、HOME 保留启动前第二页、再次 HOME 回第一页，均通过。
- QA 位于 qa/home-return-icon-shape-2026-11-20/，包含原始录像、逐帧图、landing-detail.jpg 和 after.gif。设备旋转设置恢复为测试前的 1 / 1，默认桌面保持 EasyLauncher。
