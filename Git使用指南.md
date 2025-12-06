# Git 本地版本管理使用指南

## 📋 目录
1. [查看版本历史](#查看版本历史)
2. [创建新版本](#创建新版本)
3. [查看文件变更](#查看文件变更)
4. [切换版本](#切换版本)
5. [创建标签（版本号）](#创建标签版本号)
6. [常用命令速查](#常用命令速查)

---

## 查看版本历史

### 查看所有提交记录
```bash
git log
```
显示完整的提交历史，包括作者、日期、提交信息

### 查看简洁的提交记录
```bash
git log --oneline
```
只显示提交ID和提交信息，更简洁

### 查看图形化的提交历史
```bash
git log --oneline --graph --all
```
以图形方式显示分支和提交关系

### 查看最近N条记录
```bash
git log --oneline -10    # 查看最近10条
git log -5               # 查看最近5条完整记录
```

### 查看某个文件的修改历史
```bash
git log -- 文件名
git log --oneline -- 文件名
```

---

## 创建新版本

### 基本流程（3步）

**第1步：查看当前状态**
```bash
git status
```
查看哪些文件被修改了、新增了或删除了

**第2步：添加文件到暂存区**
```bash
git add .                    # 添加所有修改的文件
git add 文件名               # 只添加特定文件
git add 文件夹/              # 添加整个文件夹
```

**第3步：创建提交（版本）**
```bash
git commit -m "提交说明"
```
例如：
- `git commit -m "修复登录bug"`
- `git commit -m "添加新功能：用户设置"`
- `git commit -m "更新依赖库版本"`

### 完整示例
```bash
# 1. 查看状态
git status

# 2. 添加所有修改
git add .

# 3. 创建提交
git commit -m "修复了蓝牙连接问题"
```

---

## 查看文件变更

### 查看工作区的变更（未提交的修改）
```bash
git diff                    # 查看所有文件的变更
git diff 文件名             # 查看特定文件的变更
```

### 查看已暂存的变更
```bash
git diff --staged
```

### 查看某个版本的具体变更
```bash
git show 提交ID             # 查看某个提交的详细变更
git show HEAD               # 查看最新提交的变更
```

### 查看两个版本之间的差异
```bash
git diff 提交ID1 提交ID2    # 比较两个提交
git diff HEAD~1 HEAD        # 比较最新提交和上一个提交
```

---

## 切换版本

### 查看所有提交ID
```bash
git log --oneline
```
会显示类似这样的内容：
```
a1b2c3d 修复登录bug
d4e5f6g 添加新功能
g7h8i9j 初始提交
```

### 切换到某个版本（只查看，不修改）
```bash
git checkout 提交ID
```
例如：`git checkout a1b2c3d`

### 切换回最新版本
```bash
git checkout main
```
或者
```bash
git checkout master
```

### 创建新分支（不影响主分支）
```bash
git branch 分支名            # 创建分支
git checkout 分支名          # 切换到分支
git checkout -b 分支名       # 创建并切换到新分支
```

---

## 创建标签（版本号）

### 创建带注释的标签（推荐）
```bash
git tag -a v1.0.0 -m "版本 1.0.0: 基础功能实现"
git tag -a v2.0.0 -m "版本 2.0.0: 重大更新"
```

### 创建轻量级标签
```bash
git tag v1.0.0
```

### 查看所有标签
```bash
git tag
git tag -l "v1.*"          # 查看v1开头的标签
```

### 查看标签详情
```bash
git show v1.0.0
```

### 切换到某个标签版本
```bash
git checkout v1.0.0
```

### 删除标签
```bash
git tag -d v1.0.0          # 删除本地标签
```

---

## 常用命令速查

### 基础操作
| 命令 | 说明 |
|------|------|
| `git status` | 查看当前状态 |
| `git add .` | 添加所有文件到暂存区 |
| `git commit -m "消息"` | 创建提交 |
| `git log` | 查看提交历史 |
| `git log --oneline` | 简洁版历史 |

### 查看和比较
| 命令 | 说明 |
|------|------|
| `git diff` | 查看未提交的变更 |
| `git show 提交ID` | 查看某个提交的详情 |
| `git log --oneline -10` | 查看最近10条记录 |

### 版本切换
| 命令 | 说明 |
|------|------|
| `git checkout 提交ID` | 切换到某个版本 |
| `git checkout main` | 回到最新版本 |
| `git tag` | 查看所有标签 |
| `git checkout v1.0.0` | 切换到某个标签版本 |

### 标签管理
| 命令 | 说明 |
|------|------|
| `git tag -a v1.0.0 -m "说明"` | 创建带注释的标签 |
| `git tag` | 列出所有标签 |
| `git show v1.0.0` | 查看标签详情 |

---

## 💡 实用技巧

### 1. 撤销未提交的修改
```bash
git checkout -- 文件名      # 撤销某个文件的修改
git reset --hard            # 撤销所有未提交的修改（危险！）
```

### 2. 修改最后一次提交信息
```bash
git commit --amend -m "新的提交信息"
```

### 3. 查看某个文件的历史版本
```bash
git log --oneline -- 文件名
git show 提交ID:文件名      # 查看某个版本的文件内容
```

### 4. 创建版本快照（推荐工作流）
```bash
# 1. 完成一些修改后
git add .
git commit -m "完成功能X"

# 2. 如果这是一个重要版本，创建标签
git tag -a v2.1.0 -m "版本 2.1.0: 完成功能X"

# 3. 查看所有版本
git log --oneline --graph --all
git tag
```

---

## 📝 建议的工作流程

1. **日常开发**
   - 修改代码
   - `git status` 查看修改
   - `git add .` 添加修改
   - `git commit -m "描述性信息"` 提交

2. **重要版本发布**
   - 完成开发后提交
   - `git tag -a v版本号 -m "版本说明"` 创建标签
   - 记录在 CHANGELOG.md 中

3. **查看历史**
   - `git log --oneline` 快速查看
   - `git show 提交ID` 查看详情
   - `git tag` 查看所有版本标签

---

## ⚠️ 注意事项

1. **提交信息要清晰**：使用有意义的提交信息，方便以后查找
2. **重要版本要打标签**：使用 `git tag` 标记重要版本
3. **定期提交**：不要积累太多修改再提交，建议每天或完成一个功能就提交
4. **查看后再切换**：切换版本前先用 `git log` 查看提交ID

---

## 🎯 快速开始示例

```bash
# 1. 查看当前状态
git status

# 2. 查看最近的提交历史
git log --oneline -5

# 3. 添加修改并提交
git add .
git commit -m "修复了某个bug"

# 4. 创建版本标签
git tag -a v2.18.17 -m "版本 2.18.17: 修复bug"

# 5. 查看所有标签
git tag

# 6. 查看某个版本的详情
git show v2.18.17
```

---

**提示**：如果忘记某个命令，可以随时查看这个文档，或者使用 `git help 命令名` 查看帮助。
