# Kni

[English](./README.md) | [中文](./README_zh.md)

**Kni** 是一个 Kotlin 多平台桥接库，实现了 Kotlin/Native（通过 Kotlin/Native 编译）与 Java/JVM 之间的无缝双向通信。与传统的 JNI 桥接不同，传统方式需要手动编写 C/C++ 胶水代码，而 Kni 允许你用纯 Kotlin 编写整个桥接实现，同时保持原生级性能。

## 安装

### Gradle 配置

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}
```

### 依赖引入

在 `build.gradle.kts` 中添加依赖：

```kotlin
dependencies {
    // 核心 API（所有平台都需要）
    implementation("io.github.dreammooncai:kni-api:1.0.3")
}
```

### 多平台配置

```kotlin
// build.gradle.kts
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.dreammooncai:kni-api:1.0.3")
        }
    }
}
```

### 仓库说明

库发布在 **Maven Central**，如果已配置 `mavenCentral()` 则无需额外配置。

## 核心理念

核心理念很简单：**在 `common` 中定义一次，在 JVM 加载，在 Native 实现，随时调用**。

```
┌─────────────────────────────────────────────────────────────┐
│                         commonMain                          │
│                   expect object StringUtil                   │
│               actual external fun reverse(str: String): String │
└─────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
┌───────────────┐    ┌───────────────┐    ┌───────────────┐
│   jvmMain     │    │  nativeMain   │    │ androidMain   │
│ actual object │    │ actual object │    │   (IPC)       │
│ :IKniRegister │    │  external fun │    └───────────────┘
│ init {loader }│    │ impl + register│
└───────────────┘    └───────────────┘
```

## 核心概念

### expect/actual 三层结构

Kni 使用 Kotlin Multiplatform 的 expect/actual 机制，但每个模块的角色更精确：

| 模块 | 角色 | 内容 |
|------|------|------|
| `commonMain` | **声明** | 声明所有需要 JVM 和 Native 桥接的类、函数、属性 |
| `jvmMain` | **加载** | 实现类，在 `init` 块加载 Native 动态链接库，使用 `actual external fun` 空实现所有函数 |
| `nativeMain` | **实现** | 实现 `IKniRegister` 接口，在注册函数中为每个 JNI 方法绑定具体的实现回调 |

```
┌─────────────────────────────────────────────────────────────┐
│                         commonMain                          │
│                 声明所有需要桥接的类、函数、属性              │
│            expect object StringUtil                         │
│         actual external fun reverse(str: String): String   │
└─────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┴─────────────────────┐
        ▼                                           ▼
┌───────────────────────┐           ┌───────────────────────────┐
│       jvmMain         │           │       nativeMain           │
│                       │           │                           │
│ actual object:        │           │ actual object:             │
│ - init { loader() }  │           │ - 实现 common 的所有函数   │
│ - actual external fun │           │ - 实现 IKniRegister 接口   │
│   = 空实现            │           │ - 在 onRegister 中绑定    │
└───────────────────────┘           │   JNI 回调                │
                                    └───────────────────────────┘
```

### :: 双冒号引用的多种用法

`::` 在 Kni 中用于定位函数或属性，Kotlin 会根据类型自动推断使用哪个 `register` 重载：

```kotlin
// 基本用法：直接引用当前类的函数
::reverse.register(staticCFunction { _, _, str: jstring ->
    kniResultJava {
        str.asString.reversed().asJni  // jstring 转换需要 kniResultJava
    }
})

// 指定 this 域：引用外部的函数
this@OuterClass::innerFunction.register(...)

// 类名引用：引用其他类的静态函数或伴生对象函数
OtherClass::method.register(...)

// 属性引用：引用属性
::myProperty.register(...)

// 同名重载函数：通过变量指定类型
val getInt: KFunction1<Int, Int> = ::get  // 只匹配 (Int) -> Int
getInt.register(staticCFunction { ... })

// 函数引用变量：需要转换为 KFunction
val add: (Int, Int) -> Int = ::add
add.asKFunction().register(staticCFunction { _, _, a: jint, b: jint ->
    a + b
})
```

### 泛型函数的处理

对于包含泛型的函数，直接使用 `::` 会因为类型推断失败而报错：

```kotlin
// ❌ 报错：Cannot infer type for type parameter 'T'
::callOriginal.register(staticCFunction { ... })

// ✅ 解决：显式指定类型
val callOriginal: (HookParam, Array<out ValueWrapper>, (Any) -> Unit) -> Unit = ::callOriginal
callOriginal.asKFunction().register(staticCFunction { _, _,
    param: jobject,
    args: jarray,
    result: jobject ->
    // ...
})
```

### CFunction 回调的固定签名

JNI 回调的前两个参数固定是 `JNIEnv` 和 `jobject`：

```kotlin
staticCFunction { env: CPointer<JNIEnvVar>, obj: jobject, ... ->
    // env: JNI 环境指针
    // obj: 调用此方法的 Java 对象（this）
    // 其余参数: 根据函数签名自动推断
}
```

如果不需要使用，可以省略类型标注：`staticCFunction { _, _, param1, param2 -> ... }`

## 快速开始

### 1. 在 commonMain 中定义

```kotlin
// commonMain/kotlin/org/example/StringUtil.kt
package org.example

expect object StringUtil {
    /**
     * 反转字符串
     * @param input 输入字符串
     * @return 反转后的字符串
     */
    fun reverse(input: String): String
}
```

### 2. 在 jvmMain 中加载库

在 `jvmMain` 中，加载原生库并用 `external` 声明所有函数：

```kotlin
// jvmMain/kotlin/org/example/StringUtil.kt
package org.example

actual object StringUtil {
    init {
        System.loadLibrary("native_tool")  // 加载原生库
    }

    // 使用 external 关键字声明，JNI 会在 Native SO 中查找实现
    actual external fun reverse(input: String): String
}
```

### 3. 在 nativeMain 中实现

在 `nativeMain` 中实现函数，并在类上实现 `IKniRegister` 接口：

```kotlin
// nativeMain/kotlin/org/example/StringUtil.kt
package org.example

actual object StringUtil : IKniRegister {
    init {
        System.loadLibrary("native_tool")  // 加载原生库
    }

    // 用 notImplemented() 占位，实际逻辑在注册回调中
    actual fun reverse(input: String): String = notImplemented()

    override fun KniRegister.onRegister() {
        // 注册 JNI 回调
        ::reverse.register(staticCFunction { _, _, str: jstring ->
            kni {
                str.asString.reversed()  // 只用 asString，不需要 asJni
            }
        })
    }
}
```

JVM 实现有两种写法：**普通实现**和**内联实现**。

#### 方式 A：普通实现（推荐简单场景）

逻辑写在 `actual fun` 中，注册回调直接调用它：

```kotlin
// jvmMain 中
override fun KniRegister.onRegister() {
    ::reverse.register(staticCFunction { _, _, str: jstring ->
        reverse(str.asString).asJni  // 调用 actual fun
    })
}

actual fun reverse(input: String): String =
    input.reversed()  // 实际逻辑在这里
```

#### 方式 B：内联实现（推荐复杂场景）

`actual fun` 只用占位符，所有逻辑写在注册回调中：

```kotlin
// jvmMain/kotlin/org/example/StringUtil.kt
package org.example

actual object StringUtil: IKniRegister {
    override fun KniRegister.onRegister() {
        // ::reverse 只用于获取类名、函数名、参数类型
        ::reverse.register(staticCFunction { _, _, str: jstring ->
            kniResultJava {
                str.asString.reversed().asJni  // 逻辑直接在这里
            }
        })
    }

    actual fun reverse(input: String): String =
        notImplemented()  // 不需要实现
}
```

**两者的区别**：

| 方式 | actual fun | 使用场景 |
|------|------------|----------|
| 普通实现 | 包含实际逻辑 | 简单场景，逻辑可以直接用 Kotlin 表达 |
| 内联实现 | `notImplemented()` | 复杂场景，需要在回调中处理 JNI 参数转换 |

### 4. 在 Native SO 中导出注册函数

```kotlin
// nativeMain/kotlin/org/example/Bridge.kt
package org.example

fun KniRegister.initBridge() {
    register(StringUtil)  // 注册 StringUtil 中的所有方法
    kniOnRegisterPlatform()
}

@CName("JNI_OnLoad")
fun kniOnLoad(vm: CPointer<JavaVMVar>, reserved: COpaquePointer): jint {
    KniVM.onLoad(vm) {
        initBridge()
    }
    return JNI_VERSION_1_6
}

@CName("JNI_OnUnload")
fun kniOnUnload(vm: CPointer<JavaVMVar>, reserved: COpaquePointer) {
    KniVM.onUnload()
}
```

## 进阶用法

### 复杂参数：枚举、列表、回调

当函数参数包含复杂类型时，需要在回调中进行转换：

```kotlin
// commonMain - 定义一个格式化器
expect object DataFormatter {
    fun format(
        data: List<String>,
        style: FormatStyle,  // 枚举参数
        callback: OnResultListener  // 回调参数
    ): String
}

enum class FormatStyle { JSON, XML, CSV }
expect fun interface OnResultListener {
    fun onResult(result: String)
}
```

```kotlin
// jvmMain - 内联实现
actual object DataFormatter: IKniRegister {
    override fun KniRegister.onRegister() {
        ::format.register(staticCFunction { _, _,
            data: jobject,
            style: jobject,
            callback: jobject ->

            // 使用 kniResultJava 自动转换返回值
            kniResultJava {
                // Java 枚举 → Kotlin 枚举
                val formatStyle = style.asEnum<FormatStyle>()

                // Java List → Kotlin List
                val items = data.asList.map { it!!.jObject.asString }

                // Java 回调 → Kotlin Lambda
                val listener = callback.asKniCallback<KniAny, Unit>()

                // 执行业务逻辑
                val result = when (formatStyle) {
                    FormatStyle.JSON -> items.joinToString(",", "[", "]")
                    FormatStyle.XML -> items.joinToString("") { "<item>$it</item>" }
                    FormatStyle.CSV -> items.joinToString(",")
                }

                // 回调通知
                listener(KniAny(result))

                result
            }
        })
    }

    actual fun format(...): String = notImplemented()
}
```

### kni vs kniResultJava

| 函数 | 用途 | 返回值处理 |
|------|------|----------|
| `kni {}` | 只使用 `asString` 等转换，无需 `asJni` | 返回值不会被 PopLocalFrame 清理 |
| `kniResultJava {}` | 需要将返回值转换为 Java 对象 | 自动 `asJni`，返回值不会被 PopLocalFrame 清理 |

两者都是 `KniBridge` 的扩展函数，内部都使用 `tryLocalFrame` 管理局部引用。

```kotlin
// 使用 kni {}：只转换参数，不转换返回值
// 返回值是 Kotlin String，无需转为 Java
::reverse.register(staticCFunction { _, _, str: jstring ->
    kni {
        str.asString.reversed()  // 只使用 asString，不需要 asJni
    }
})

// 使用 kniResultJava {}：需要将返回值转为 Java 对象
::greet.register(staticCFunction { _, _, name: jstring ->
    kniResultJava {
        "Hello, ${name.asString}!".asJni  // 返回值需要转为 Java String
    }
})
```

### 类型转换方法

Kni 提供了丰富的基础转换方法：

```kotlin
// 字符串转换（都需要 kniResultJava 上下文）
val kotlinStr: String = kniResultJava { jstring.jObject.asString }
val jstr: jstring = kniResultJava { kotlinStr.asJni }

// 枚举转换
val kotlinEnum: FormatStyle = kniResultJava { javaEnumObj.asEnum<FormatStyle>() }
val javaEnum: jobject = kniResultJava { kotlinEnum.asJni }

// 列表转换
val kotlinList: List<KniAny?> = javaListObj.asList

// 类名字符串转 Java Class
val StringClass: KniClass = "java.lang.String".toClass()

// 回调转换：Java → Kotlin
val kotlinLambda: (KniAny) -> Unit = jobject.asKniCallback()

// 回调转换：Kotlin → Java
val javaCallback: jobject = kotlinLambda.asJni()
```

### KniAny：Java 对象包装器

`KniAny` 包装 Java 对象，提供序列化/反序列化能力：

```kotlin
// 序列化：Kotlin 对象 → Java 对象
val kotlinUser = User(name = "Alice", id = 1001)
val javaUser = User::class.java.serialize(kotlinUser)

// 反序列化：Java 对象 → Kotlin 对象
val backToKotlin = User::class.java.deserialize<User>(javaUser)

// 获取字段值
val name: String = javaUser.jObject.asAnyKni<String>("name")

// 转为 JSON
val json: String = javaUser.toJson()
```

### 反射调用（Java 反射）

Kni 提供了简洁的字符串类名反射 API：

#### 方法调用

```kotlin
// 获取当前时间（等价于 System.currentTimeMillis()）
val time: Long = "java.lang.System".toClass().method { name = "currentTimeMillis" }.long()

// 调用实例方法（等价于 user.getName()）
val name: String = "com.example.User".toClass().method {
    name = "getName"
    thisRef = userObj.asKni  // 实例方法的 this 引用
}.string()

// 调用带参数的方法
val result: Boolean = "com.example.StringUtil".toClass().method {
    name = "validate"
    param(StringClass, IntClass)
}.boolean(param1, param2)
```

#### 字段访问

```kotlin
// 获取字段值（等价于 user.name）
val name: String = "com.example.User".toClass().field {
    name = "name"
    thisRef = userObj.asKni
}.string()

// 设置字段值（等价于 user.name = "NewName"）
"com.example.User".toClass().field {
    name = "name"
    thisRef = userObj.asKni
}.set("NewName".asJni)
```

#### 参数类型支持

`param()` 支持多种类型，Kni 会自动转换：

| 参数类型 | 示例 | 说明 |
|----------|------|------|
| 字符串类名 | `param("java.lang.String")` | 自动调用 `toClass()` |
| KClass | `param(String::class)` | Native 的 Class，自动识别基本类型 |
| KType | `param(property.returnType)` | 如 `KProperty1.returnType` |
| KniClass | `param(StringClass)` | Kni 内部类类型 |

```kotlin
// 多种参数类型混用
val result = "com.example.Utils".toClass().method {
    name = "process"
    // 使用字符串类名
    param("java.lang.String")
    // 使用 KClass
    param(Int::class)
    // 使用已有变量
    param(StringClass)
    // 使用 KType
    param(userNameProperty.returnType)
}.boolean()
```

**KClass 自动转换规则**：
- 基本类型 `Int::class` → `int`（JNI 基本类型）
- 其他类型 `String::class` → `java.lang.String`（完整类名）

#### 使用描述符

```kotlin
// 通过描述符直接调用方法
val result: String = "com.example.Utils".toClass().method {
    descriptor = "calculate(Ljava/lang/String;I)Ljava/lang/String;"
}.string("param", 123)
```

#### 返回类型简写

| 简写方法 | Java 返回类型 |
|----------|---------------|
| `.string()` | `jstring` |
| `.int()` | `jint` |
| `.long()` | `jlong` |
| `.boolean()` | `jboolean` |
| `.byte()` | `jbyte` |
| `.char()` | `jchar` |
| `.short()` | `jshort` |
| `.float()` | `jfloat` |
| `.double()` | `jdouble` |
| `.object()` | `jobject` |

#### 注意事项 ⚠️

反射通过 JNI 的 `GetMethodID` / `GetStaticMethodID` 实现，**必须写全类型**：

1. **类名必须是全限定名**
   ```kotlin
   // ✅ 正确：全限定名
   "java.lang.System".toClass()
   // ❌ 错误：会找不到类
   "System".toClass()
   ```

2. **方法名必须完全匹配**
   ```kotlin
   // ✅ 正确
   name = "getName"
   // ❌ 错误：大小写、空格都不能错
   name = "GetName"
   ```

3. **参数类型必须是 JNI 签名格式**
   ```kotlin
   // ✅ 正确：使用类引用
   param(StringClass, IntClass)
   param("java.lang.String".toClass(), "int".toClass())

   // ❌ 错误：简写或类名不全
   param(String, Integer)  // 找不到！
   ```

4. **使用描述符更可靠**（推荐复杂场景使用）
   ```kotlin
   // 描述符格式：返回类型(参数类型...)
   descriptor = "Ljava/lang/String;->substring(II)Ljava/lang/String;"
   //         类全限定名  ^^^^^^^^ 方法名 ^^^^^^^^ 参数 ^^ 返回类型
   ```

5. **找不到方法会直接报错**
   ```
   error: 无法在 com.example.User 找到 getName 方法
   ```
   请检查类名、方法名、参数类型是否完全正确。

#### Kotlin 函数/属性反射扩展

Kni 提供了将 Kotlin `KFunction` / `KProperty` 直接转换为 JNI 方法/字段调用的扩展：

```kotlin
// 将 Kotlin 函数转换为 JNI 方法调用
val method: KniMethod = ::myFunction.asKniMethod(thisRef = obj.asKni) {
    // 需要时添加额外配置
}
val result = method.string()

// 将 Kotlin 属性转换为 JNI 字段访问
val field: KniField = ::myProperty.asKniField(thisRef = obj.asKni)
val value = field.string()
```

这些扩展会自动提取：
- `declaringClass` - 函数/属性所属的类
- `name` - 函数/属性名称
- `param` - 函数参数类型
- `returnType` / `type` - 返回值类型

### 局部引用管理

```kotlin
// 自动管理局部引用
bridge.tryLocalFrame {
    val result = someMethod()
    result
}

// 返回 Java 对象时
bridge.tryLocalFrameResultJava {
    createJavaObject()
}
```

## IKniRegister 接口

实现 `IKniRegister` 接口的类表示支持动态 JNI 注册：

```kotlin
interface IKniRegister {
    fun KniRegister.onRegister() {}
}

// 扩展函数：注册单个 IKniRegister
fun IKniRegister.register() { onRegister() }

// 扩展函数：注册多个 IKniRegister
fun KniRegister.register(vararg registers: IKniRegister) {
    registers.forEach { r -> with(r) { onRegister() } }
}
```

## KniVM 生命周期

```kotlin
// 在 JNI_OnLoad 中初始化
@CName("JNI_OnLoad")
fun kniOnLoad(vm: CPointer<JavaVMVar>, reserved: COpaquePointer): jint {
    // onLoad 默认注册了 KniCallbackProxy 和 KniLogger
    KniVM.onLoad(vm) {
        // 此处有 KniRegister 的 this 上下文
        register(StringUtil)        // 注册 StringUtil 的所有方法
        register(DataFormatter)    // 注册 DataFormatter 的所有方法
    }
    return JNI_VERSION_1_6
}

// 在 JNI_OnUnload 中清理
@CName("JNI_OnUnload")
fun kniOnUnload(vm: CPointer<JavaVMVar>, reserved: COpaquePointer) {
    KniVM.onUnload()
}
```

### onLoad 默认注册的组件

`KniVM.onLoad` 会自动注册两个基础组件：

- **KniCallbackProxy**：处理 Kotlin → Native 的回调代理
- **KniLogger**：处理日志记录

你不需要手动注册它们，只需要注册你自定义的组件。

## 平台支持

| 平台 | 状态 | 备注 |
|------|------|------|
| Android | ✅ | 完整 JNI + IPC 支持 |
| iOS | ✅ | 通过 Kotlin/Native CInterop |
| macOS | ✅ | 通过 Kotlin/Native CInterop |
| Windows | ✅ | 通过 Kotlin/Native CInterop |
| Desktop JVM | ✅ | 通过 KniLoader |

## Android IPC (androidMain)

对于 Android 跨进程通信，Kni 提供了 `DreamSmartIPC`，支持一键实现跨进程通信。

### 核心概念

```
┌─────────────────┐         Binder         ┌─────────────────┐
│   客户端 App     │ ◄──────────────────► │   服务端 App     │
│                 │   DreamSmartIPC      │                 │
│  asClient/      │                      │   asServer()    │
│  asClientAutoProxy                    │                 │
└─────────────────┘                      └─────────────────┘
```

### IPC 规则与限制

#### 接口要求

- **IPC 接口必须继承 `IDreamIPC`**
- **必须是真正的 Kotlin/Java 接口**（不能用普通类）

```kotlin
// ✅ 正确：接口继承 IDreamIPC
interface ICalculatorService : IDreamIPC {
    fun add(a: Int, b: Int): Int
}

// ❌ 错误：普通类无法用于 IPC
class CalculatorService { ... }
```

#### 参数和返回值序列化规则

IPC 会根据参数和返回值的类型自动选择处理方式：

| 类型 | 处理方式 | 示例 |
|------|----------|------|
| 可序列化类型 | JSON 序列化 | `Int`、`String`、`@Serializable data class` |
| 接口类型 | 代理传输 | `ICallback`、`IUser` |
| 无法序列化且无接口 | ❌ 抛出错误 | 自定义普通类 |

```kotlin
interface ICalculatorService : IDreamIPC {
    // ✅ Int/String 可序列化
    fun add(a: Int, b: Int): Int

    // ✅ @Serializable data class
    fun getUser(id: Long): User
    fun searchUsers(query: String): List<User>

    // ✅ 接口类型会代理传输
    fun setCallback(callback: IOnResultListener<String>)

    // ❌ 自定义普通类无法传输
    // fun getCustomObject(): CustomClass  // 会抛出错误！
}
```

#### asClient vs asClientAutoProxy

```kotlin
// asClient：需要手动处理返回值代理
val service = DreamSmartIPC.asClient<ICalculatorService>(binder, ICalculatorService::class)

// asClientAutoProxy：自动代理所有参数和返回值（推荐）
val service = DreamSmartIPC.asClientAutoProxy<ICalculatorService>(binder, ICalculatorService::class)
```

### 完整示例

假设我们有一个计算器服务需要在不同进程间共享：

#### 1. 定义 IPC 接口

```kotlin
// androidMain
interface ICalculatorService : IDreamIPC {
    fun add(a: Int, b: Int): Int
    fun multiply(a: Int, b: Int): Int
    fun calculateAsync(a: Int, b: Int, callback: IOnResultListener<Int>)
}
```

#### 2. 服务端实现

```kotlin
// 服务端 - 实现 IPC 接口
object CalculatorService : ICalculatorService {
    actual fun add(a: Int, b: Int): Int = a + b
    actual fun multiply(a: Int, b: Int): Int = a * b
    actual fun calculateAsync(a: Int, b: Int, callback: IOnResultListener<Int>) {
        callback.onResult(a * b)
    }
}
```

#### 3. 服务端暴露

IPC 支持多种方式暴露服务，**RootService 只是其中一种**（用于需要 root 权限的场景）：

```kotlin
// 方式 A：普通 Service（不需要 root）
class CalculatorService : Service() {
    override fun onBind(intent: Intent): IBinder =
        DreamSmartIPC.asServer(CalculatorService, ICalculatorService::class)
}

// 方式 B：Root Service（需要 root 权限）
class CalculatorRootService : RootService() {
    override fun onBind(intent: Intent): IBinder =
        DreamSmartIPC.asServer(CalculatorService, ICalculatorService::class)
}
```

#### 4. 客户端连接

```kotlin
// 客户端
object CalculatorClient : ServiceConnection {
    var service: ICalculatorService? = null

    fun start(context: Context) {
        val intent = Intent(context, CalculatorService::class.java)
        context.bindService(intent, this, Context.BIND_AUTO_CREATE)
    }

    override fun onServiceConnected(name: ComponentName?, binder: IBinder) {
        // 自动代理所有复杂类型
        service = DreamSmartIPC.asClientAutoProxy(binder, ICalculatorService::class)
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        service = null
    }
}
```

#### 5. 客户端使用

```kotlin
CalculatorClient.start(this)

// 调用跨进程方法，就像调用本地方法一样
val result = CalculatorClient.service?.add(10, 20)
CalculatorClient.service?.calculateAsync(5, 6) { r ->
    println("结果: $r")
}
```

### 三种传输模式

| 模式 | 说明 | 限制 | 推荐场景 |
|------|------|------|----------|
| `DreamJsonBinder` | JSON 序列化传输 | ~1MB | 小数据量，简单对象 |
| `DreamSharedMemoryBinder` | 共享内存传输 | ~4GB | 大数据量，高性能 |
| `DreamJsonChunkBinder` | 分块 JSON 传输 | ~4GB | 中等数据，兼容性好 |

**自动选择**：默认 `isChunk = true`，SDK 27+ 使用 SharedMemory，低于 27 使用分块 JSON。

## 架构

```
┌─────────────────────────────────────────────────────────────┐
│                         commonMain                           │
│         IKniRegister  │  KniExtension  │  KniLogger         │
└─────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
┌───────────────┐    ┌───────────────┐    ┌───────────────┐
│   jvmMain     │    │  nativeMain   │    │ androidMain   │
│               │    │               │    │               │
│ KniLoader     │    │ KniBridge     │    │ DreamSmartIPC │
│ KniCallback   │    │ KniVM         │    │ Binder Series │
│ Proxy         │    │ KniMethod     │    │               │
│               │    │ KniField      │    │               │
│               │    │ KniAny        │    │               │
│               │    │ KniCallback   │    │               │
│               │    │ Factory       │    │               │
└───────────────┘    └───────────────┘    └───────────────┘
```

## 许可证

Apache License 2.0
