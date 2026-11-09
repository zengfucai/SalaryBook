# SalaryBook - 工资工时记录

**SalaryBook** 是一款基于 Android 平台开发的个人工资与工时记录应用，旨在帮助用户记录每日工作时间、管理工资档案、统计加班收入，并根据个人工作制度灵活计算工资。

不同用户的工作时间、薪资结构和加班规则可能存在差异，因此 SalaryBook 尽可能提供灵活的自定义配置，让用户根据自己的实际工作情况设置工资计算规则。

## 项目介绍

SalaryBook 是一个 Android 原生应用开发项目，采用 Kotlin 语言与 Jetpack Compose 构建界面，使用 Room 数据库进行本地数据存储。

项目主要面向需要记录工作时间、计算工资、统计加班收入以及管理历史工资记录的用户。

## 主要功能

- **工资档案管理**
  - 创建、编辑和删除工资档案
  - 支持多种工资计算方式
  - 不同档案可以设置独立的工资规则
  - 删除档案后保留历史工资记录

- **工时记录**
  - 记录每日上下班时间
  - 支持工作时段和休息时间设置
  - 支持工作时长统计
  - 支持修改和删除历史记录

- **加班管理**
  - 支持工作日加班、休息日加班和节假日加班
  - 支持自定义加班工资倍数
  - 支持根据日期自动识别周六、周日等休息日（具体规则可配置）

- **工资计算**
  - 支持时薪、月薪、日薪、周薪、计件工资和提成等薪资类型
  - 支持奖金、津贴、夜班补贴等收入项目
  - 支持自定义收入和扣款项目
  - 支持设置标准工作时长及工资计算规则

- **日历管理**
  - 通过日历查看每日工作记录
  - 查看指定日期的历史记录
  - 支持历史记录查看、修改和删除

- **工资统计**
  - 支持按月、季度和年度统计
  - 统计正常工资、加班工资及其他收入
  - 统计工作天数和工作时长
  - 查看工资收入和扣款情况

- **历史数据保护**
  - 保存历史工资规则快照
  - 避免修改当前工资档案后影响过去的工资计算结果
  - 尽可能保留已删除工资档案对应的历史记录

## 开发环境

| 项目 | 技术 / 工具 |
|---|---|
| 开发语言 | Kotlin |
| 开发 IDE | Android Studio |
| UI 框架 | Jetpack Compose |
| UI 组件库 | Material 3 |
| 数据库 | SQLite |
| 数据库框架 | Room |
| 异步处理 | Kotlin Coroutines / Flow |
| 构建工具 | Gradle |
| 构建脚本 | Kotlin DSL |
| 开发平台 | Android |
| 版本控制 | Git |
| 代码托管 | GitHub |

## 项目技术特点

- 使用 Kotlin 开发 Android 原生应用。
- 使用 Jetpack Compose 构建声明式 UI。
- 使用 Material 3 设计界面组件。
- 使用 Room 实现本地数据库存储。
- 使用 Kotlin Coroutines 和 Flow 处理异步任务及数据流。
- 采用模块化的代码组织方式，分离界面、数据和业务逻辑。
- 工资规则支持用户自定义，尽量避免固定化的薪资计算假设。
- 通过历史规则快照保护历史工资记录。

## 项目目录

```text
SalaryBook/
├── app/
│   └── src/
│       └── main/
│           ├── java/
│           │   └── com/
│           │       └── salarybook/
│           │           ├── data/       # 数据层
│           │           ├── ui/         # 界面层
│           │           ├── MainActivity.kt
│           │           └── ...
│           ├── res/                    # 图片、图标及资源
│           └── AndroidManifest.xml
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## 适用人群

- 需要记录每日工作时间的上班族
- 按小时计算工资的工作人员
- 有工作日、休息日和节假日加班需求的用户
- 需要统计每月实际收入的用户
- 有计件工资、提成或其他复杂薪资结构的用户

## 项目状态

项目目前处于持续开发阶段，功能和界面仍在完善中。

后续将继续优化工资计算规则、历史数据管理、统计报表以及整体使用体验。

## 免责声明

本项目仅用于个人工资和工时记录，不构成劳动法律、税务或薪资核算方面的专业建议。实际工资计算结果应以用户配置的规则及实际劳动合同、公司制度和适用法律为准。

---

# English

SalaryBook is a native Android application for tracking working hours, managing salary profiles, calculating overtime pay, and reviewing income statistics.

Built with Kotlin, Jetpack Compose, Material 3, and Room, the application provides customizable salary calculation rules to accommodate different employment arrangements.

**Key Features**
- Multiple salary profiles
- Daily work and break time tracking
- Customizable overtime rules
- Hourly, monthly, daily, weekly, piece-rate, and commission-based pay
- Custom income and deduction items
- Calendar-based work history
- Monthly, quarterly, and yearly salary reports
- Historical salary rule snapshots
- Local database storage

**Tech Stack**
- Kotlin
- Android Studio
- Jetpack Compose
- Material 3
- Room / SQLite
- Kotlin Coroutines / Flow
- Gradle
- Git / GitHub

SalaryBook is an ongoing personal development project.
