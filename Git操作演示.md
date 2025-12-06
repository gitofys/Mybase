# Git 操作演示总结

## ✅ 已完成的演示操作

### 1. 添加文件并创建提交
```bash
# 添加新文件到暂存区
git add "Git使用指南.md"

# 创建提交（新版本）
git commit -m "添加Git使用指南文档"
```

### 2. 创建版本标签
```bash
# 为当前版本创建标签 v2.18.16
git tag -a v2.18.16 -m "版本 2.18.16: 添加Git使用指南"
```

---

## 📖 常用查看命令演示

### 查看提交历史

**简洁版（推荐日常使用）：**
```bash
git log --oneline
```
输出示例：
```
a1b2c3d 添加Git使用指南文档
d4e5f6g 初始提交
```

**查看最近N条：**
```bash
git log --oneline -5    # 查看最近5条
git log --oneline -10   # 查看最近10条
```

**完整详细信息：**
```bash
git log
```
显示完整的提交信息，包括作者、日期、完整提交信息

**图形化显示：**
```bash
git log --oneline --graph --all
```
以图形方式显示分支关系

### 查看提交详情

**查看最新提交的详细信息：**
```bash
git show HEAD
```

**查看某个特定提交：**
```bash
git show 提交ID
# 例如：git show a1b2c3d
```

**只查看文件变更统计：**
```bash
git show HEAD --stat
```

### 查看标签

**查看所有标签：**
```bash
git tag
```

**查看标签详情：**
```bash
git show v2.18.16
```

**查看特定模式的标签：**
```bash
git tag -l "v2.*"    # 查看v2开头的所有标签
```

### 查看当前状态

**查看工作区状态：**
```bash
git status
```

**简洁版状态：**
```bash
git status --short
```

**查看未提交的修改：**
```bash
git diff              # 查看所有文件的变更
git diff 文件名       # 查看特定文件的变更
```

---

## 🎯 实际使用场景演示

### 场景1：日常开发流程

```bash
# 1. 修改了一些代码文件后，先查看状态
git status

# 2. 查看具体改了什么
git diff

# 3. 添加所有修改
git add .

# 4. 创建提交
git commit -m "修复了蓝牙连接问题"

# 5. 查看提交历史确认
git log --oneline -3
```

### 场景2：创建重要版本

```bash
# 1. 完成开发并提交
git add .
git commit -m "完成新功能：用户设置界面"

# 2. 创建版本标签
git tag -a v2.18.17 -m "版本 2.18.17: 添加用户设置功能"

# 3. 查看所有版本
git tag

# 4. 查看版本详情
git show v2.18.17
```

### 场景3：查看历史版本

```bash
# 1. 查看所有提交
git log --oneline

# 2. 查看某个提交的详细信息
git show a1b2c3d

# 3. 切换到某个版本查看（只读模式）
git checkout a1b2c3d

# 4. 回到最新版本
git checkout main
# 或
git checkout master
```

### 场景4：查看文件历史

```bash
# 查看某个文件的修改历史
git log --oneline -- CHANGELOG.md

# 查看某个文件在某个版本的内容
git show v2.18.16:CHANGELOG.md
```

---

## 📝 快速参考卡片

### 最常用的5个命令

1. **`git status`** - 查看当前状态
2. **`git log --oneline`** - 查看提交历史
3. **`git add .`** - 添加所有修改
4. **`git commit -m "说明"`** - 创建提交
5. **`git tag`** - 查看所有标签

### 创建版本的3步流程

```bash
git status          # 第1步：查看状态
git add .           # 第2步：添加修改
git commit -m "说明" # 第3步：提交
```

### 查看版本信息

```bash
git log --oneline           # 查看提交历史
git tag                     # 查看所有标签
git show 提交ID或标签       # 查看详情
```

---

## 💡 提示

1. **提交信息要清晰**：使用有意义的描述，如"修复登录bug"而不是"修改"
2. **重要版本打标签**：使用 `git tag` 标记重要版本点
3. **定期查看历史**：使用 `git log --oneline` 快速回顾
4. **查看差异**：提交前用 `git diff` 确认修改内容

---

## 🔍 如何查找提交

### 方法1：通过提交信息查找
```bash
git log --oneline --grep="关键词"
# 例如：git log --oneline --grep="蓝牙"
```

### 方法2：通过文件查找
```bash
git log --oneline -- 文件名
# 例如：git log --oneline -- CHANGELOG.md
```

### 方法3：通过日期查找
```bash
git log --oneline --since="2024-01-01"
git log --oneline --until="2024-12-31"
```

---

现在您已经了解了基本的Git操作！可以开始使用Git管理您的项目版本了。
