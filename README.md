# Hello World App - 简易 Feed 流客户端

这是一个基于 Android 原生开发的 Feed 流客户端演示项目。项目模拟了真实的社交 APP 体验，包括天气信息展示、图文 Feed 流、个人中心等核心功能。

## ✨ 功能特性

### 1. 首页 (Home)
*   **多布局列表**：使用 RecyclerView 实现复杂的首页布局，包含顶部天气卡片和下方的 Feed 流列表。
*   **实时天气**：集成高德地图天气 API，实时展示当前城市的天气状况（支持昼夜图标切换）。
*   **下拉刷新**：集成 SwipeRefreshLayout，模拟从服务端拉取最新数据。
*   **无限加载**：支持列表滑动到底部自动加载更多数据（分页模拟）。
*   **平滑回顶**：向下滑动时显示“回到顶部”悬浮按钮，支持平滑/瞬移混合滚动效果。
*   **网络图片**：使用 Glide 加载图片，支持加载占位图和失败兜底图。

### 2. 天气详情 (Weather)
*   **未来预报**：展示未来几天的天气趋势、温度范围及风力信息。
*   **城市切换**：支持在多个城市（北京、杭州、上海等）之间快速切换。
*   **动态背景**：根据天气状况（晴、雨、云）和时间（昼/夜）动态改变页面背景风格。

### 3. 个人中心 (Mine)
*   **用户信息**：展示圆形头像、昵称及个性签名。
*   **数据持久化**：支持修改昵称和签名，并使用 SharedPreferences 进行本地持久化存储。
*   **功能菜单**：提供个人信息、收藏、浏览历史等功能入口（UI 演示）。

### 4. 登录与启动 (Login & Splash)
*   **启动动画**：包含简单的启动页淡入动画。
*   **登录模拟**：包含用户名/密码输入框及简单的登录校验逻辑（SQLite）。

## 🛠 技术栈

*   **语言**：Kotlin
*   **UI 组件**：
    *   RecyclerView (MultiType Adapter)
    *   ConstraintLayout / NestedScrollView
    *   CardView / Material Components
    *   SwipeRefreshLayout
*   **网络请求**：OkHttp3
*   **数据解析**：Gson
*   **图片加载**：Glide
*   **本地存储**：SharedPreferences, SQLiteOpenHelper

## 🚀 快速开始

1.  **克隆项目**
    ```bash
    git clone https://github.com/your_username/HelloWorldApp.git
    ```
2.  **打开项目**
    *   使用 Android Studio 打开项目根目录。
    *   等待 Gradle Sync 完成。
3.  **运行**
    *   连接 Android 设备或启动模拟器。
    *   点击 Run 按钮 (Shift+F10)。

## 📝 开发说明

*   **数据源**：为了演示方便，项目中的 Feed 流数据采用**本地模拟生成**策略（模拟网络延迟 + 随机内容），图片资源使用本地 Drawable 资源模拟网络加载效果。
*   **天气 API**：使用了高德地图免费天气 API，Key 配置在 `AppConfig.kt` 中。

## 📂 项目结构

```
com.example.helloworldapp
├── HomeActivity.kt       # 首页：Feed 流与天气入口
├── WeatherActivity.kt    # 天气页：详情与预报
├── UserInfoActivity.kt   # 我的：个人信息管理
├── LoginActivity.kt      # 登录页
├── AppConfig.kt          # 全局配置 (API Key 等)
└── ...
```
