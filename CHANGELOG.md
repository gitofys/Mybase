# 版本更新日志 (Changelog)

本文档记录项目的所有版本更新和修改历史。

## 版本管理建议

### Git 版本控制
建议使用 Git 进行版本管理：

1. **初始化 Git 仓库**（如果还没有）：
   ```bash
   git init
   git add .
   git commit -m "初始版本"
   ```

2. **创建版本标签**：
   ```bash
   git tag -a v1.0.0 -m "版本 1.0.0: 基础功能实现"
   ```

3. **查看版本历史**：
   ```bash
   git log --oneline
   git tag -l
   ```

### 版本号规范
建议使用语义化版本号：`主版本号.次版本号.修订号`
- **主版本号**：不兼容的 API 修改
- **次版本号**：向下兼容的功能性新增
- **修订号**：向下兼容的问题修正

---

## 版本历史

### [v2.18.17] - 2024-XX-XX

#### 代码清理和优化
- **版本更新**：从 v2.18.16 升级到 v2.18.17
  - versionCode: 16 → 17
  - versionName: "2.18" → "2.18.17"
- **代码清理**：
  - 移除未使用的 ViewPager 相关代码（CustomPagerAdapter, CustomPagerEnum）
  - 移除 ViewPager 导入和变量声明
  - 简化 TitleBarEvent 方法，移除 Tab 导航相关代码
  - 注释掉未使用的 ViewPager 初始化代码
  - 修复注释块嵌套问题（将嵌套的 `/*...*/` 改为单行注释 `//`）
- **删除未使用的 Activity**：
  - 删除 `InformationViewActivity.java`（已在 AndroidManifest 中注释，不再使用）
  - 删除 `LicenseViewActivity.java`（仅被已删除的 InformationViewActivity 使用）
- **删除未使用的布局文件**：
  - 删除 `page_graph_absorbance.xml`（ViewPager 相关，已不再使用）
  - 删除 `page_graph_intensity.xml`（ViewPager 相关，已不再使用）
  - 删除 `page_graph_reference.xml`（ViewPager 相关，已不再使用）
  - 删除 `page_graph_reflectance.xml`（ViewPager 相关，已不再使用）
  - 删除 `activity_info.xml`（InformationViewActivity 的布局）
  - 删除 `license_view.xml`（LicenseViewActivity 的布局）
- **AndroidManifest 更新**：
  - 移除 HomeViewActivity 的注册（不再作为启动 Activity）
  - 移除 InformationViewActivity 的注册（已删除）
  - 移除 LicenseViewActivity 的注册（已删除）
- **清理未使用的变量和导入**：
  - 移除 `tabPosition` 变量（ViewPager 相关，不再使用）
  - 注释掉 `isScan` 变量的使用（ViewPager 相关，不再使用）
  - 移除 `FragmentTransaction` 导入（不再使用）
  - 移除 `LayoutInflater` 导入（仅在注释代码中使用）
  - 移除 `ViewGroup` 导入（仅在注释代码中使用）
  - 移除 `Easing` 导入（仅在注释代码中使用）
- **清理未使用的资源**：
  - 注释掉 `graph_tab_index` 字符串数组（ViewPager 相关，不再使用）
- **删除大量注释代码**：
  - 完全删除 CustomPagerAdapter 和 CustomPagerEnum 的注释代码（约 340 行，引用了已删除的布局文件）
  - 删除 ViewPager 初始化相关的注释代码
- **代码清理（ScanViewActivity.java）**：
  - **删除未使用的变量声明**（约 50+ 个变量）：
    - Normal 模式相关：`btn_normal`, `ly_normal_config`, `tv_normal_scan_conf`, `toggle_btn_continuous_scan`, `et_normal_interval_time`, `et_normal_scan_repeat`, `btn_normal_continuous_stop`
    - QuickSet 模式相关：所有 `et_quickset_*`, `spin_quickset_*`, `btn_quickset_*`, `quickset_*_index`, `quickset_init_*` 变量
    - Maintain 模式相关：`Toggle_Button_maintain_reference`
    - 连续扫描相关：`continuous_count`, `show_finish_continous_dialog`, `stop_continuous`, `continuous`
  - **删除未使用的方法和监听器**：
    - `InitialNormalComponent()`, `InitialQuicksetComponent()`, `InitialMaintainComponent()`（已清空）
    - 所有 QuickSet 相关的监听器（约 9 个监听器类）
    - `Continuous_Scan_Stop_Click` 监听器
  - **简化 ScanMethod 枚举**：从 4 个模式简化为只有 `Manual`
  - **删除 Maintain 模式相关逻辑**：配置保存、参考扫描等
  - **删除连续扫描逻辑**：`DoScanComplete()` 中的连续扫描处理代码
  - **迁移 storeCalibration**：从 `HomeViewActivity` 移到 `ScanViewActivity`
  - **重命名变量**：`init_viewpage_valuearray` → `chartDataInitialized`
  - **删除对已删除视图的引用**：`tv_normal_scan_conf` 的所有引用

- **清理未使用的资源文件**：
  - **布局文件（3个）**：删除 `row_graph_list_item.xml`、`row_info_item.xml`、`activity_error_scan_item.xml`
  - **菜单文件（2个）**：删除 `menu_info.xml`（InformationViewActivity已删除）、`menu_settings.xml`（未使用）
  - **动画文件（1个）**：删除 `alpha_splash.xml`（未使用）
  - **Drawable资源（约45个文件）**：删除未使用的图标资源，包括：
    - `ic_info.png`（5个密度版本）
    - `ic_info_isc.png`（5个密度版本）
    - `ic_mail.png`（5个密度版本）
    - `ic_search.png`（5个密度版本）
    - `ic_connect.png`（5个密度版本）
    - `info_flatx.png`（5个密度版本，在main_page.xml中被注释）
    - `isc_info.png`（1个密度版本）
    - `scan_flatx.png`（5个密度版本，只有scan_flatx_isc在使用）
    - `ic_splash_screen.png`（5个密度版本，只有ic_splash_screen_isc在使用）
    - `screenshot_back.png`（1个文件）
  - **字符串资源**：删除 `info_title_array`、`info_body_array`、`info_url_array`（InformationViewActivity已删除）

#### 技术细节
- 修改文件：`app/build.gradle`
  - 更新 versionCode 和 versionName
- 修改文件：`ScanViewActivity.java`
  - 移除 ViewPager 相关导入
  - 移除 mViewPager 变量
  - 简化 TitleBarEvent 方法
  - 注释掉 CustomPagerAdapter 和 CustomPagerEnum（保留代码以备参考）

#### 备份
- 创建备份说明文件：`BACKUP_v2.18.16.md`
- 建议使用 Git 进行版本控制

---

### [未发布] - 2024-XX-XX

#### 修改内容
- **设备名称修正**：将目标设备名称从 "NIR-M-R 2" 改为 "NIR-M-R2"（去掉空格）
- **应用名称本地化**：将应用显示名称从 "ISC NIRScan" 改为 "食品快速检测仪"
- **图表初始化优化**：
  - 在应用启动时，图表不再显示转圈等待状态
  - 添加默认的水平直线显示（波长范围：900-1700nm，强度值：0）
  - 当接收到实际光谱数据时，自动更新图表显示
  - 默认显示模式为强度（Intensity）模式

#### 技术细节
- 修改文件：`ScanViewActivity.java`
  - 第243行：`TARGET_DEVICE_NAME` 常量更新
- 修改文件：`strings.xml`
  - 第2行：`app_name` 字符串资源更新
- 修改文件：`ScanViewActivity.java`
  - `initializeChart()` 方法：添加默认数据线显示逻辑

---

### [v2.18.16] - 2024-XX-XX

#### 主要功能重构
- **UI 简化**：
  - 移除初始 HomeViewActivity，应用直接启动到 ScanViewActivity
  - 移除多种扫描模式（Normal、QuickSet、Maintain），仅保留 Manual 模式
  - 移除 ViewPager，使用单个 LineChart 配合 Spinner 选择显示模式
  - 将反射率、吸光度、强度三个按钮合并为一个下拉选择器
  - 添加可折叠的配置区域，默认隐藏高级配置选项

- **连接流程优化**：
  - 应用启动时自动扫描并连接目标设备 "NIR-M-R2 <D11R013>"
  - 添加"连接设备"按钮用于手动重连
  - 添加设备连接状态指示器
  - 连接失败时显示提示对话框，提供重试选项

- **灯光控制功能**：
  - 添加灯光设置按钮，提供以下选项：
    - **预热功能**：可设置预热时间，自动打开灯光并倒计时，期间禁用其他操作
    - **模式选择**：支持"开启"、"关闭"、"自动"三种模式
      - 自动模式：扫描前自动打开灯光，扫描完成后自动关闭

- **本地化**：
  - 将主要 UI 文本和提示信息翻译为中文

#### 技术细节
- 修改文件：`AndroidManifest.xml`
  - 将启动 Activity 从 `HomeViewActivity` 改为 `ScanViewActivity`
- 修改文件：`activity_new_scan.xml`
  - 重构布局，移除多模式相关组件
  - 添加连接状态栏、灯光设置按钮、显示模式选择器
  - 优化图表区域大小和比例
- 修改文件：`ScanViewActivity.java`
  - 合并权限检查逻辑
  - 实现自动设备扫描和连接
  - 实现灯光控制和预热功能
  - 实现单图表多模式显示切换
  - 移除所有已删除布局组件的引用

#### 修复的问题
- 修复 Gradle 版本不匹配问题
- 修复 `getBaseContext()` 使用不当的问题
- 修复编译错误：移除对已删除布局元素的引用
- 修复资源链接错误：添加缺失的颜色资源

---

### [v2.18.15] - 2024-XX-XX

#### 修改内容
（此版本之前的修改历史，请根据实际情况补充）

---

## 如何记录新版本

当完成一次修改后，请按以下步骤记录：

1. **更新 CHANGELOG.md**：
   - 在文件顶部添加新版本条目
   - 详细描述修改内容
   - 列出涉及的文件和关键代码位置

2. **创建 Git 提交**：
   ```bash
   git add .
   git commit -m "版本 X.X.X: 修改描述"
   ```

3. **创建版本标签**（可选）：
   ```bash
   git tag -a vX.X.X -m "版本 X.X.X: 详细描述"
   ```

4. **推送到远程仓库**（如果有）：
   ```bash
   git push origin main
   git push origin vX.X.X
   ```

---

## 版本对比工具

可以使用以下工具对比不同版本：

```bash
# 对比两个版本的文件
git diff v1.0.0 v2.0.0

# 查看某个版本的提交历史
git log v1.0.0..v2.0.0

# 查看某个文件的修改历史
git log -p -- filename
```

---

## 注意事项

- 每次修改后及时更新此文档
- 版本号要遵循语义化版本规范
- 重要修改要创建 Git 标签
- 保持修改描述的清晰和详细

