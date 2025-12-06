# 版本备份说明 - v2.18.17

## 备份日期
2024年（具体日期待填写）

## 版本信息
- **版本号**: v2.18.17
- **versionCode**: 17
- **versionName**: "2.18.17"
- **应用名称**: 食品快速检测仪
- **目标设备**: NIR-M-R2

## 本次主要修改内容

### 1. 代码注释清理
- **删除所有英文注释和JavaDoc注释**
  - 移除了所有 `/** */` 格式的JavaDoc注释
  - 移除了所有英文单行注释 `//`
  - 移除了所有英文块注释 `/* */`
- **添加中文逻辑注释**
  - 在关键逻辑处添加了简洁的中文注释
  - 保留了必要的方法说明（使用中文）
  - 注释更加清晰易懂

### 2. CSV保存功能简化
- **修改CSV保存格式**
  - 只保存三列数据：波长(nm)、信号强度、参比
  - 移除了所有其他信息（如配置信息、时间戳等）
  - 简化了CSV文件结构，便于数据分析
- **修复CSV表头格式**
  - 表头从单个字符串改为三个独立的列
  - 确保CSV格式正确，每列数据独立

### 3. 代码优化
- 清理了所有未使用的注释代码
- 统一了注释风格（全部使用中文）
- 提高了代码可读性

## 功能特性

### 核心功能
- ✅ 蓝牙BLE设备连接（目标设备：NIR-M-R2）
- ✅ 手动扫描模式（唯一支持的扫描模式）
- ✅ 实时光谱数据显示（强度/吸光度/反射率）
- ✅ CSV数据保存（仅保存波长、信号强度、参比三列）
- ✅ 图表显示（支持切换显示模式）
- ✅ 设备配置管理
- ✅ 灯控制功能（打开/关闭/自动模式）
- ✅ 预热功能

### 已移除的功能
- ❌ Normal扫描模式
- ❌ QuickSet扫描模式
- ❌ Maintain扫描模式
- ❌ 连续扫描功能
- ❌ ViewPager多标签图表显示
- ❌ HomeViewActivity（启动页面）
- ❌ InformationViewActivity
- ❌ LicenseViewActivity

## 文件结构

### 主要Activity
- `ScanViewActivity.java` - 主扫描界面（启动Activity）
- `SettingsViewActivity.java` - 设置界面
- `ConfigureViewActivity.java` - 配置界面
- `DeviceInfoViewActivity.java` - 设备信息界面
- `DeviceStatusViewActivity.java` - 设备状态界面
- `ScanConfigurationsViewActivity.java` - 扫描配置界面
- `SelectDeviceViewActivity.java` - 设备选择界面
- `AddScanConfigViewActivity.java` - 添加扫描配置界面
- `ActiveConfigDetailViewActivity.java` - 活动配置详情界面
- `ActivationViewActivity.java` - 激活界面
- `AdvanceDeviceStatusViewActivity.java` - 高级设备状态界面
- `ErrorStatusViewActivity.java` - 错误状态界面
- `AdvanceErrorStatusViewActivity.java` - 高级错误状态界面

### 主要布局文件
- `activity_new_scan.xml` - 主扫描界面布局
- 其他Activity对应的布局文件

### 资源文件
- `strings.xml` - 字符串资源（应用名称：食品快速检测仪）
- 各种drawable资源
- 菜单资源

## 技术栈

- **开发语言**: Java
- **最低SDK版本**: 21 (Android 5.0)
- **目标SDK版本**: 33 (Android 13)
- **编译SDK版本**: 33
- **图表库**: MPAndroidChart
- **CSV库**: OpenCSV
- **蓝牙**: BLE (Bluetooth Low Energy)
- **SDK**: ISCNIRScanSDK

## 重要配置

### 设备配置
- **目标设备名称**: NIR-M-R2（无空格）
- **目标设备MAC部分**: D11R013
- **设备名称过滤**: NIR

### 应用配置
- **应用名称**: 食品快速检测仪
- **包名**: com.Innospectra.ISCScanNano
- **命名空间**: com.Innospectra.ISCScanNano

## CSV文件格式

### 保存位置
- Android 11以下：`/Documents/ISC_Report/`
- Android 11及以上：使用MediaStore API保存到Documents/ISC_Report/

### 文件格式
```csv
波长(nm),信号强度,参比
900.0,1234,5678
901.0,1235,5679
...
```

### 文件命名规则
`{前缀}_{配置名称}_{时间戳}.csv`

例如：`ISC_Column1_20241201_120000.csv`

## 已知问题

无

## 测试建议

1. **连接测试**
   - 测试蓝牙设备连接功能
   - 验证设备名称识别（NIR-M-R2）
   - 验证MAC地址匹配（D11R013）

2. **扫描测试**
   - 测试手动扫描功能
   - 验证光谱数据显示
   - 验证图表切换（强度/吸光度/反射率）

3. **CSV保存测试**
   - 验证CSV文件是否正确保存
   - 验证CSV格式（三列数据）
   - 验证文件命名规则
   - 验证数据完整性

4. **UI测试**
   - 验证应用名称显示（食品快速检测仪）
   - 验证图表默认显示（直线）
   - 验证配置折叠/展开功能

## 版本对比

### 与 v2.18.16 的主要区别
1. **代码注释**：全部改为中文注释
2. **CSV格式**：简化为三列数据
3. **代码清理**：移除了更多未使用的代码和注释

## Git标签建议

如果使用Git进行版本控制，建议创建标签：
```bash
git tag -a v2.18.17 -m "版本 2.18.17: 注释清理和CSV格式简化"
```

## 备份文件清单

### 源代码文件
- `app/src/main/java/com/Innospectra/NanoScan/ScanViewActivity.java` - 主扫描Activity（已清理注释）
- 其他Activity文件
- 布局文件
- 资源文件

### 配置文件
- `app/build.gradle` - 构建配置（versionCode: 17, versionName: "2.18.17"）
- `app/src/main/AndroidManifest.xml` - 清单文件
- `app/src/main/res/values/strings.xml` - 字符串资源

### 文档文件
- `CHANGELOG.md` - 更新日志
- `BACKUP_v2.18.17.md` - 本备份文档
- `代码清理总结_v2.18.17.md` - 代码清理总结
- `清理总结_v2.18.17.md` - 清理总结
- `资源清理总结_v2.18.17.md` - 资源清理总结

## 恢复说明

如果需要恢复到本版本：
1. 确保Git仓库已初始化
2. 检出对应标签：`git checkout v2.18.17`
3. 或从备份文件恢复

## 注意事项

1. **代码注释**：所有注释已改为中文，便于维护
2. **CSV格式**：CSV文件只包含三列数据，如需其他信息需要修改代码
3. **设备名称**：确保设备名称正确（NIR-M-R2，无空格）
4. **应用名称**：应用显示名称为"食品快速检测仪"

## 后续建议

1. 继续优化代码结构
2. 添加单元测试
3. 完善错误处理
4. 优化用户体验

