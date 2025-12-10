# Hello World App - 高扩展性 Feed 流客户端

这是一个基于 Android 原生开发的高性能 Feed 流客户端演示项目。
项目采用**插件化卡片架构**，支持服务端动态下发卡片样式与排版策略，实现了单/双列流的无缝切换与多样式混排，并集成了**全链路曝光监控**系统。

## ✨ 核心特性

### 1. 插件化 Feed 流系统 (Architecture)
*   **高扩展性**：基于 `CardStyleRegistry` 和 `CardProcessor` 的插件式设计。新增卡片样式只需实现 Processor 接口并注册，无需修改 Adapter 核心逻辑。
*   **多样式混排**：目前内置四种卡片样式，均支持单/双列两种形态：
    *   🖼️ **图文卡片** (ImageCard)
    *   📄 **文章卡片** (ArticleCard)
    *   🎬 **视频卡片** (VideoCard)
    *   📢 **广告卡片** (AdCard)
*   **服务端驱动 UI**：模拟服务端下发数据协议（`styleId` + `data`），客户端根据下发的样式标识动态渲染视图。

### 2. 动态布局管理
*   **布局切换**：支持一键在 **列表模式 (Single Column)** 和 **瀑布流模式 (Waterfall Grid)** 之间切换。
*   **排版策略**：模拟服务端根据当前的布局模式（LayoutType），动态下发适配单列 (`_list`) 或双列 (`_grid`) 的样式数据，实现精准的 UI 控制。
*   **智能分页**：封装了带 Footer Loading 状态的无限加载机制，支持平滑滚动与状态回调。

### 3. 全链路曝光监控 (Exposure)
*   **精准监测**：集成自定义 `ExposureManager`，利用 `GlobalLayoutListener` 和 `ScrollListener` 实时计算 Item 可见比例。
*   **去重机制**：确保同一 Item 在未移出屏幕前不会重复上报，支持 50% 可见度阈值判定。
*   **可视化控制台**：内置 `ExposureActivity` 实时展示曝光日志流，方便调试与验证。

### 4. 交互与体验
*   **沉浸式天气**：顶部集成实时天气卡片，支持根据天气状况（雨/雪/晴/夜间）动态改变背景与图标。
*   **手势操作**：
    *   **长按删除**：支持长按 Feed 卡片弹出删除确认框。
    *   **下拉刷新**：集成 SwipeRefreshLayout 实现无感数据重置。
    *   **一键回顶**：智能显示的悬浮按钮，支持“瞬移+平滑”混合滚动算法。

### 5. 个人中心
*   **用户信息管理**：支持头像、昵称、签名的展示与本地持久化修改 (SharedPreferences)。
*   **现代化 UI**：采用 Material Design 风格的悬浮卡片设计，视觉通透。

## 🛠 技术架构

*   **语言**：Kotlin
*   **核心组件**：
    *   `RecyclerView` (配合 `StaggeredGridLayoutManager` & `LinearLayoutManager`)
    *   `CardView` & `ConstraintLayout`
    *   `SwipeRefreshLayout`
*   **设计模式**：
    *   **Registry Pattern**：用于卡片样式的注册与分发。
    *   **Factory/Processor**：用于 ViewHodler 的创建与数据绑定。
*   **监控系统**：
    *   `ViewTreeObserver`：布局变化监听
    *   `Rect`：视图区域计算
*   **网络与数据**：
    *   `OkHttp3`：网络请求
    *   `Gson`：多态数据解析
    *   `Glide`：图片加载与缓存

## 📂 核心类说明

```
com.example.helloworldapp
├── FeedCardSystem.kt     # 核心架构：定义 Feedable 接口与 CardStyleRegistry
├── CardProcessors.kt     # 业务实现：包含所有卡片样式的 Processor 与 ViewHolder
├── HomeActivity.kt       # 业务逻辑：负责数据模拟、布局切换与事件分发
├── FeedAdapter.kt        # 通用适配器：基于 Registry 动态分发视图渲染
├── ExposureManager.kt    # 曝光监控：核心算法实现
└── ExposureActivity.kt   # 调试工具：实时曝光日志展示
```

## 🚀 快速开始

1.  **环境要求**：Android Studio Iguana 或更高版本，JDK 17+。
2.  **构建运行**：
    *   Clone 项目到本地。
    *   Sync Gradle 等待依赖下载。
    *   连接真机或模拟器运行 (Run 'app')。
3.  **体验功能**：
    *   点击 Toolbar 右上角的切换按钮，体验单双列布局变换。
    *   下滑列表体验自动加载更多。
    *   观察 Logcat 或进入调试页面查看曝光埋点数据。

## 📝 数据源说明

为了演示方便，项目内置了强大的**本地模拟引擎**：
*   **模拟网络延迟**：在 `loadFeedData` 中模拟真实请求耗时。
*   **动态样式生成**：根据当前的全局布局模式（Grid/List），自动生成对应的 `styleId`（例如 `video_card_grid` 或 `video_card_list`），完美还原服务端控制客户端排版的场景。

## 📄 License

[MIT License](LICENSE)