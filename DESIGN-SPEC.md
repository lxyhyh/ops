# OpsPermissionManager — MIUI X / 澎湃OS 风格 UI 设计规范（DESIGN-SPEC）

> 应用类型：权限管理工具（AppOps / Shizuku）
> 页面范围：应用列表（主屏）、批量管理、权限历史、设置（含 Root 引导）、应用详情
> 色彩模式：浅色 / 深色双模式强制成对
> 强调色：**初音绿 #39C5BB**（沿用项目原有主题，保持品牌识别；预设 5 套可切换）
> 设计语言核心：**生命感美学** —— 大圆角卡片、柔和光影与层级、轻盈通透、流畅动效、清晰信息层级
> 本规范为唯一事实来源，组件与动效只引用本文件令牌，不得另写新数值。

---

## 一、设计原则

1. **层次靠色差，不靠描边**：同一页内用 `surface`（卡片）与 `background`（页面）的明度差分层，阴影只做柔和弥散，不画硬边框。
2. **大圆角+G2 连续曲线**：所有容器使用 Squircle（超椭圆）连续曲率，视觉比普通圆弧更"澎湃"。
3. **通透 + 模糊**：顶栏/悬浮底栏使用实时模糊（Android 12+ `RenderEffect`，低端机自动降级纯色半透明）。
4. **动效有节奏**：每个交互都有明确的时长与缓动（见第四节），禁止无过渡的硬跳变。
5. **强调色克制**：初音绿只用于主行动点、选中态、进度与数字角标，不铺满。

---

## 二、设计令牌（Design Tokens）

> 深色规则：背景避免纯黑（用偏暖的 #0F0F11 防 OLED 锯齿）；文字不反转纯白（用 #F5F5F5）；阴影减弱，靠色差分层。

### 2.1 色彩（浅色 / 深色）

| 令牌 | 浅色 | 深色 |
|---|---|---|
| `bg_page` | #F5F5F7 | #0F0F11 |
| `surface_card` | #FFFFFF | #1C1C1E |
| `surface_sub`（二级/输入框底） | #F2F3F5 | #2C2C2E |
| `bar_blur`（顶栏/底栏模糊层底色） | #FFFFFF 70% | #1C1C1E 70% |
| `divider` | #E5E5EA 60% | #3A3A3C 60% |
| `text_primary` | #1A1A1A | #F5F5F5 |
| `text_secondary` | #666666 | #8E8E93 |
| `text_tertiary` | #999999 | #6E6E73 |
| `success` / `warning` / `error` | #00B578 / #FF8F1F / #FA5151 | #34C759 / #FFD60A / #FF453A |

### 2.2 强调色系统（初音绿，一次只用一套）

| 状态 | 浅色 | 深色 |
|---|---|---|
| `accent`（主色） | #39C5BB | #5DD9D0 |
| `accent_pressed`（按压=主色加深 10%） | #2EA79E | #4EC7BE |
| `accent_disabled`（禁用=主色透明度 30%） | #39C5BB 30% | #5DD9D0 30% |
| `accent_tint`（选中底衬=主色透明度 15%） | #39C5BB 15% | #5DD9D0 15% |
| `accent_on`（主色上的文字） | #00332F | #00332F |

> Material You 动态取色（可选）：系统壁纸取色，深色下提亮保证对比；默认用初音绿种子色 `keyColor=0xFF39C5BB`。

### 2.3 字体 / 圆角 / 间距 / 阴影 / 模糊

| 类目 | 数值 |
|---|---|
| **字体** | MiSans（免费商用），回退系统无衬线。字阶：大标题 24sp Medium / 标题 18–20sp Medium / 正文 14–16sp / 辅助 12–13sp / 说明 11sp |
| **圆角（G2/Squircle）** | 大卡 20dp；列表容器 16dp；输入框/搜索胶囊 12–16dp；主按钮全圆角或 12dp；弹窗 24dp；**悬浮胶囊底栏 30dp**；选中高亮全圆角。实现：miuix-squircle `SquircleShape`（`top.yukonga.miuix.kmp.shape.squircle`) |
| **间距（4dp 栅格）** | 页面左右 16dp；卡片间距 12dp；卡片内边距 16dp；底栏左右 16dp / 底部 10dp / 距底 12dp；列表项上下 12dp 左右 16dp |
| **阴影** | 柔和弥散：`ambient/elevation` 小数值（默认 0dp），层高主要靠色差；深色模式阴影减弱 |
| **模糊** | 半径 24dp；栏底色 70% 透明度；顶栏滚动标题收缩；低端机降级纯色半透明；模糊层顶部可加 1px `#FFFFFF 20%` 高光 |

---

## 三、组件规范

### 3.1 悬浮胶囊底栏（硬性要求，本项目已具备，按规范修正）
- 不贴屏幕底边：距底 **10dp**、左右各 **16dp**
- 容器：圆角 **30dp**（Squircle）、`bar_blur` 模糊 + 柔和阴影 + 半透明，内容可透过
- 选中项：`accent_tint` 15% 底衬胶囊高亮 + `accent` 图标/文字；未选中项线性图标 + `text_secondary`
- 图标：线性 2dp 描边，24dp 尺寸；选中项可带数字角标（如批量待处理数）
- 动效：指示胶囊位移动画（见 4.4）

### 3.2 页面顶栏
- 应用详情页等二级页：只保留返回箭头（左 4dp），无标题（标题放内容区）
- 主界面：无独立顶栏，状态栏区域透出页面背景
- 顶栏背景：透明，滚动时可选 `bar_blur` 模糊

### 3.3 列表与大卡片
- 应用列表/批量/历史：整页卡片化，每个分组一张大卡（圆角 20dp，`surface_card`，内边距 16dp）
- 列表项：圆角 12dp，按压态 `accent_tint` 5% 或 `surface_sub`，图标 32dp 圆角 10dp
- 状态标签：胶囊徽章（全圆角），`accent_tint` 底衬 + `accent` 文字

### 3.4 按钮 / 开关 / 弹窗
- 主按钮：全圆角或 12dp，`accent` 底 + `accent_on` 文字，按压 `accent_pressed`，加载态转圈
- 开关：miuix Switch（240ms 标准减速）
- 弹窗：圆角 24dp，缩放进入（见 4.3）
- 输入框（搜索）：胶囊 16dp，`surface_sub` 底

### 3.5 设置项（settings 页，用 miuix-preference）
- 分组卡片（圆角 20dp），组内 `PreferenceItem`：左图标 24dp、主标题 15sp、副标题 12sp、右值 `text_secondary`、可选开关/箭头
- 根引导页：大标题 + 行动按钮 + 说明文字，遵循大圆角卡片

---

## 四、动效规范（时长 + 缓动曲线）

| 编号 | 动效 | 时长 | 曲线（cubic-bezier） | Compose |
|---|---|---|---|---|
| 4.1 | 页面转场（详情进入/返回） | 300ms | `standard` emphasis (0.2,0,0,1) | tween + FastOutSlowInEasing |
| 4.2 | 按压反馈 | 100ms + 回弹 250ms | 0.34,1.56,0.64,1（回弹） | scale 0.97 |
| 4.3 | 弹窗进入 | 250ms | (0,0,0.2,1) 减速 | scale 0.9→1.0 + fadeIn |
| 4.4 | 悬浮底栏指示胶囊位移 | 350ms | 弹性回弹 (0.34,1.56,0.64,1) | offset + Spring |
| 4.5 | 开关 | 200ms | (0,0,0.2,1) | miuix Switch 内置 |
| 4.6 | 列表加载骨架微光 | 持续 | 线性 | shimmer |

---

## 五、落地与适配说明

1. **令牌**：直接以 miuix `ColorScheme` 方式接入（`MiuixTheme(controller = ThemeController(Monet, keyColor=0xFF39C5BB))`），深浅由 `ColorSchemeMode` 控制。
2. **组件**：核心用 miuix（`top.yukonga.miuix.kmp.*`）：`MiuixTheme`、`Card`、`BackTopBar`、`SwitchItem`、`PreferenceItem`、`HoverableIconButton` 等；Material3 仅保留无冲突的基础布局组件（Box/Column/LazyColumn）。
3. **模糊**：`miuix-blur` 的 `Modifier.blur(...)`（内部 `RenderEffect.blurBitmap`，Android 12+），低端机按 Build.VERSION 降级半透明色块。
4. **G2 圆角**：`miuix-squircle` 的 `SquircleShape`（连续曲率），替代 `RoundedCornerShape`。
5. **降级边界**：minSdk=29，Android 11 及以下无 RenderEffect → 模糊层退化为 `bar_blur` 纯色半透明，功能与布局不变。

---

> 变更记录：文档初稿于 2026-08-19，基于 miuix 0.9.3 + Compose 1.9 系制定；后续随组件落地同步更新数值。
