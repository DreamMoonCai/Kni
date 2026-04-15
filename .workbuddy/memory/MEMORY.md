# Kni 项目核心记忆

## 项目概述
Kni 是一个 Kotlin 多平台桥接库（Kotlin Multiplatform Bridge Library），实现 Kotlin/JVM 与 Kotlin/Native 之间的无缝通信。

## 核心模块
- **kni-api**: 核心 API 模块
  - `commonMain`: 公共定义（IKniRegister, KniExtension, KniLogger）
  - `nativeMain`: Native 实现（KniBridge, KniVM, KniMethod, KniField, 回调工厂）
  - `androidMain`: Android IPC 实现（DreamSmartIPC, DreamRpcCall, Binder 系列）
  - `jvmMain`: JVM 加载器（KniLoader, KniCallbackProxy）

## 关键功能
1. **JNI 桥接**: 通过 KniBridge 封装 JNI 调用
2. **动态加载**: KniVM.onLoad() + KniLoader 动态加载原生库
3. **高阶函数**: KniCallbackFactory / JniCallbackFactory 支持 Lambda 跨边界
4. **Android IPC**: DreamSmartIPC 支持一键跨进程通信，支持 SharedMemory
5. **类型系统**: 完善的自动装箱/拆箱，DexSignUtil 类型签名转换

## 技术栈
- Kotlin Multiplatform
- JNI / CInterop
- kotlinx.serialization
- kotlinx.coroutines
- kotlin-logging

## 平台支持
Android, iOS, macOS, Windows, Desktop JVM

## 更新记录
- 2026-04-15: 创建项目 README.md（英文）和 README_zh.md（中文）
