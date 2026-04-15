# Kni

[English](./README.md) | [中文](./README_zh.md)

**Kni** is a Kotlin Multiplatform bridge library that enables seamless bidirectional communication between Kotlin/Native (compiled via Kotlin/Native) and Java/JVM. Unlike traditional JNI bridging which requires manual C/C++ glue code, Kni allows you to implement the entire bridge in pure Kotlin while maintaining native-level performance.

## Installation

### Gradle

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}
```

### Dependencies

Add dependency to your `build.gradle.kts`:

```kotlin
dependencies {
    // Core API (required for all platforms)
    implementation("io.github.dreammooncai:kni-api:1.0.3")
}
```

### Multiplatform Configuration

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

### Repository

The library is published to **Maven Central**. No additional repositories are needed if `mavenCentral()` is already configured.

## Core Philosophy

The core philosophy is simple: **Define once in `common`, load in JVM, implement in Native, call anywhere**.

```
┌─────────────────────────────────────────────────────────────┐
│                         commonMain                            │
│                   expect object StringUtil                    │
│               actual external fun reverse(str: String): String│
└─────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
┌───────────────┐    ┌───────────────┐    ┌───────────────┐
│   jvmMain     │    │  nativeMain   │    │ androidMain   │
│ actual object │    │ actual object │    │   (IPC)       │
│ init {loader }│    │ impl + register│    └───────────────┘
└───────────────┘    └───────────────┘
```

## Core Concepts

### expect/actual Three-Layer Structure

Kni uses Kotlin Multiplatform's expect/actual mechanism, but each module has a more precise role:

| Module | Role | Content |
|--------|------|---------|
| `commonMain` | **Declaration** | Declare all classes, functions, and properties needing JVM/Native bridging |
| `jvmMain` | **Load & Declare** | Load native dynamic library in `init`, use `actual external fun` for empty implementations |
| `nativeMain` | **Implement** | Implement `IKniRegister` interface, bind JNI callbacks in `onRegister()` |

```
┌─────────────────────────────────────────────────────────────┐
│                         commonMain                           │
│            Declare all bridging classes, functions, props     │
│            expect object StringUtil                          │
│         actual external fun reverse(str: String): String     │
└─────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┴─────────────────────┐
        ▼                                           ▼
┌───────────────────────┐           ┌───────────────────────────┐
│       jvmMain         │           │       nativeMain           │
│                       │           │                           │
│ actual object:        │           │ actual object:            │
│ - init { loader() }   │           │ - Implement all common    │
│ - actual external fun │           │   functions               │
│   = empty impl        │           │ - Implement IKniRegister  │
│                       │           │ - Bind JNI callbacks      │
└───────────────────────┘           └───────────────────────────┘
```

### Multiple Ways to Use :: (Double Colon)

`::` is used to locate functions or properties in Kni. Kotlin infers which `register` overload to use based on types:

```kotlin
        // Basic: reference current class's function
        ::reverse.register(staticCFunction { _, _, str: jstring ->
            kniResultJava {
                str.asString.reversed().asJni  // jstring conversions need kniResultJava
            }
        })

// Specify this scope: reference outer function
this@OuterClass::innerFunction.register(...)

// Class name: reference other class's static or companion functions
OtherClass::method.register(...)

// Property reference
::myProperty.register(...)

// Overloaded functions: use variable to specify type
val getInt: KFunction1<Int, Int> = ::get  // Only matches (Int) -> Int
getInt.register(staticCFunction { ... })

// Function reference variable: needs KFunction conversion
val add: (Int, Int) -> Int = ::add
add.asKFunction().register(staticCFunction { _, _, a: jint, b: jint ->
    a + b
})
```

### Handling Generic Functions

For functions with generics, using `::` directly fails due to type inference:

```kotlin
// ❌ Error: Cannot infer type for type parameter 'T'
::callOriginal.register(staticCFunction { ... })

// ✅ Solution: explicitly specify type
val callOriginal: (HookParam, Array<out ValueWrapper>, (Any) -> Unit) -> Unit = ::callOriginal
callOriginal.asKFunction().register(staticCFunction { _, _,
    param: jobject,
    args: jarray,
    result: jobject ->
    // ...
})
```

### CFunction Callback Fixed Signature

JNI callbacks always have the first two parameters fixed as `JNIEnv` and `jobject`:

```kotlin
staticCFunction { env: CPointer<JNIEnvVar>, obj: jobject, ... ->
    // env: JNI environment pointer
    // obj: Java object that invoked this method (this)
    // Remaining params: inferred from function signature
}
```

If not needed, type annotations can be omitted: `staticCFunction { _, _, param1, param2 -> ... }`

## Quick Start

### 1. Define in commonMain

```kotlin
// commonMain/kotlin/org/example/StringUtil.kt
package org.example

expect object StringUtil {
    /**
     * Reverse a string
     * @param input input string
     * @return reversed string
     */
    fun reverse(input: String): String
}
```

### 2. Load in jvmMain

In `jvmMain`, load the native library and declare all functions with `external`:

```kotlin
// jvmMain/kotlin/org/example/StringUtil.kt
package org.example

actual object StringUtil {
    init {
        System.loadLibrary("native_tool")  // Load native library
    }

    // Use external keyword declaration, JNI will find implementation in Native SO
    actual external fun reverse(input: String): String
}
```

### 3. Implement in nativeMain

In `nativeMain`, implement all functions and implement `IKniRegister` interface:

```kotlin
// nativeMain/kotlin/org/example/StringUtil.kt
package org.example

actual object StringUtil : IKniRegister {
    init {
        System.loadLibrary("native_tool")  // Load native library
    }

    // Use notImplemented() as placeholder, actual logic is in register callback
    actual fun reverse(input: String): String = notImplemented()

    override fun KniRegister.onRegister() {
        // Register JNI callback
        ::reverse.register(staticCFunction { _, _, str: jstring ->
            kni {
                str.asString.reversed()  // Only use asString, no asJni needed
            }
        })
    }
}
```

JVM implementation has two styles: **normal implementation** and **inline implementation**.

#### Style A: Normal Implementation (Recommended for Simple Cases)

Logic is in `actual fun`, register callback just calls it:

```kotlin
// In jvmMain
override fun KniRegister.onRegister() {
    ::reverse.register(staticCFunction { _, _, str: jstring ->
        reverse(str.asString).asJni  // Call actual fun
    })
}

actual fun reverse(input: String): String =
    input.reversed()  // Actual logic here

### 4. Export Register Function in Native SO

```kotlin
// nativeMain/kotlin/org/example/Bridge.kt
package org.example

fun KniRegister.initBridge() {
    register(StringUtil)  // Register all methods in StringUtil
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
fun kniOnUnload(vm: CPointer<JavaVMVar>, reserved: COpaquePointer): jint {
    KniVM.onUnload()
    return 0
}
```

## Advanced Usage

### Complex Parameters: Enums, Lists, Callbacks

When function parameters contain complex types, conversion is needed in the callback:

```kotlin
// commonMain - Define a formatter
expect object DataFormatter {
    fun format(
        data: List<String>,
        style: FormatStyle,  // Enum parameter
        callback: OnResultListener  // Callback parameter
    ): String
}

enum class FormatStyle { JSON, XML, CSV }
expect fun interface OnResultListener {
    fun onResult(result: String)
}
```

```kotlin
// jvmMain - Inline implementation
actual object DataFormatter: IKniRegister {
    override fun KniRegister.onRegister() {
        ::format.register(staticCFunction { _, _,
            data: jobject,
            style: jobject,
            callback: jobject ->

            // Use kniResultJava for automatic return value conversion
            kniResultJava {
                // Java enum → Kotlin enum
                val formatStyle = style.asEnum<FormatStyle>()

                // Java List → Kotlin List
                val items = data.asList.map { it!!.jObject.asString }

                // Java callback → Kotlin Lambda
                val listener = callback.asKniCallback<KniAny, Unit>()

                // Business logic
                val result = when (formatStyle) {
                    FormatStyle.JSON -> items.joinToString(",", "[", "]")
                    FormatStyle.XML -> items.joinToString("") { "<item>$it</item>" }
                    FormatStyle.CSV -> items.joinToString(",")
                }

                // Callback notification
                listener(KniAny(result))

                result
            }
        })
    }

    actual fun format(...): String = notImplemented()
}
```

### kni vs kniResultJava

| Function | Purpose | Return Value Handling |
|----------|---------|----------------------|
| `kni {}` | Only use `asString` etc., no `asJni` needed | Return value not Pop-cleaned |
| `kniResultJava {}` | Need to convert return value to Java | Auto `asJni`, return value not Pop-cleaned |

Both are `KniBridge` extension functions, both use `tryLocalFrame` for local reference management.

```kotlin
// Use kni {}: only convert parameters, not return value
// Return value is Kotlin String, no need to convert to Java
::reverse.register(staticCFunction { _, _, str: jstring ->
    kni {
        str.asString.reversed()  // Only use asString, no asJni needed
    }
})

// Use kniResultJava {}: need to convert return value to Java
::greet.register(staticCFunction { _, _, name: jstring ->
    kniResultJava {
        "Hello, ${name.asString}!".asJni  // Return value needs to be Java String
    }
})
```

### Type Conversion Methods

Kni provides rich basic conversion methods:

```kotlin
// String conversion (ALL require kniResultJava context)
val kotlinStr: String = kniResultJava { jstring.jObject.asString }
val jstr: jstring = kniResultJava { kotlinStr.asJni }

// Enum conversion
val kotlinEnum: FormatStyle = kniResultJava { javaEnumObj.asEnum<FormatStyle>() }
val javaEnum: jobject = kniResultJava { kotlinEnum.asJni }

// List conversion
val kotlinList: List<KniAny?> = javaListObj.asList

// Class name string to Java Class
val StringClass: KniClass = "java.lang.String".toClass()

// Callback conversion: Java → Kotlin
val kotlinLambda: (KniAny) -> Unit = jobject.asKniCallback()

// Callback conversion: Kotlin → Java
val javaCallback: jobject = kotlinLambda.asJni()
```

### KniAny: Java Object Wrapper

`KniAny` wraps Java objects and provides serialization/deserialization:

```kotlin
// Serialize: Kotlin object → Java object
val kotlinUser = User(name = "Alice", id = 1001)
val javaUser = User::class.java.serialize(kotlinUser)

// Deserialize: Java object → Kotlin object
val backToKotlin = User::class.java.deserialize<User>(javaUser)

// Get field value
val name: String = javaUser.jObject.asAnyKni<String>("name")

// Convert to JSON
val json: String = javaUser.toJson()
```

### Reflection (Java Reflection)

Kni provides a fluent reflection API using string class names:

#### Method Invocation

```kotlin
// Get current time (equivalent to System.currentTimeMillis())
val time: Long = "java.lang.System".toClass().method { name = "currentTimeMillis" }.long()

// Call instance method (equivalent to user.getName())
val name: String = "com.example.User".toClass().method {
    name = "getName"
    thisRef = userObj.asKni  // This reference for instance method
}.string()

// Call method with parameters
val result: Boolean = "com.example.StringUtil".toClass().method {
    name = "validate"
    param(StringClass, IntClass)
}.boolean(param1, param2)
```

#### Field Access

```kotlin
// Get field value (equivalent to user.name)
val name: String = "com.example.User".toClass().field {
    name = "name"
    thisRef = userObj.asKni
}.string()

// Set field value (equivalent to user.name = "NewName")
"com.example.User".toClass().field {
    name = "name"
    thisRef = userObj.asKni
}.set("NewName".asJni)
```

#### Parameter Types Supported

`param()` supports multiple types, Kni automatically converts them:

| Type | Example | Description |
|------|---------|-------------|
| String class name | `param("java.lang.String")` | Auto calls `toClass()` |
| KClass | `param(String::class)` | Native Class, auto detects primitives |
| KType | `param(property.returnType)` | e.g. `KProperty1.returnType` |
| KniClass | `param(StringClass)` | Kni internal class type |

```kotlin
// Mix different parameter types
val result = "com.example.Utils".toClass().method {
    name = "process"
    // String class name
    param("java.lang.String")
    // KClass
    param(Int::class)
    // Existing variable
    param(StringClass)
    // KType
    param(userNameProperty.returnType)
}.boolean()
```

**KClass Auto-conversion Rules**:
- Primitive `Int::class` → `int` (JNI primitive type)
- Other types `String::class` → `java.lang.String` (fully qualified)

#### Using Descriptor

```kotlin
// Direct method by descriptor
val result: String = "com.example.Utils".toClass().method {
    descriptor = "calculate(Ljava/lang/String;I)Ljava/lang/String;"
}.string("param", 123)
```

#### Return Type Shorthands

| Shorthand | Java Return Type |
|-----------|------------------|
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

#### Important Notes ⚠️

Reflection uses JNI's `GetMethodID` / `GetStaticMethodID` — **you must provide complete type information**:

1. **Class name must be fully qualified**
   ```kotlin
   // ✅ Correct: fully qualified name
   "java.lang.System".toClass()
   // ❌ Wrong: will not find the class
   "System".toClass()
   ```

2. **Method name must match exactly**
   ```kotlin
   // ✅ Correct
   name = "getName"
   // ❌ Wrong: case-sensitive, no typos allowed
   name = "GetName"
   ```

3. **Parameter types must be JNI signature format**
   ```kotlin
   // ✅ Correct: use class references
   param(StringClass, IntClass)
   param("java.lang.String".toClass(), "int".toClass())

   // ❌ Wrong: shorthand or incomplete names won't work
   param(String, Integer)  // Will fail!
   ```

4. **Descriptors are more reliable** (recommended for complex cases)
   ```kotlin
   // Descriptor format: returnType(paramTypes...)
   descriptor = "Ljava/lang/String;->substring(II)Ljava/lang/String;"
   //             fully.qualified.Class  ^^^^^^^^ method ^^^^^^^^ params ^^ return
   ```

5. **Method not found throws error directly**
   ```
   error: Cannot find method getName in com.example.User
   ```
   Double-check class name, method name, and parameter types.

#### Kotlin Function/Property Reflection Extensions

Kni provides extensions to directly convert Kotlin `KFunction` / `KProperty` to JNI method/field calls:

```kotlin
// Convert Kotlin function to JNI method call
val method: KniMethod = ::myFunction.asKniMethod(thisRef = obj.asKni) {
    // Additional configuration if needed
}
val result = method.string()

// Convert Kotlin property to JNI field access
val field: KniField = ::myProperty.asKniField(thisRef = obj.asKni)
val value = field.string()
```

These extensions automatically extract:
- `declaringClass` from the function/property's declaring class
- `name` from the function/property name
- `param` types from function parameters
- `returnType` / `type` from return type

### Local Reference Management

```kotlin
// Automatic local reference management
bridge.tryLocalFrame {
    val result = someMethod()
    result
}

// When returning Java objects
bridge.tryLocalFrameResultJava {
    createJavaObject()
}
```

## IKniRegister Interface

Classes implementing `IKniRegister` support dynamic JNI registration:

```kotlin
interface IKniRegister {
    fun KniRegister.onRegister() {}
}

// Extension function: register single IKniRegister
fun IKniRegister.register() { onRegister() }

// Extension function: register multiple IKniRegisters
fun KniRegister.register(vararg registers: IKniRegister) {
    registers.forEach { r -> with(r) { onRegister() } }
}
```

## KniVM Lifecycle

```kotlin
// Initialize in JNI_OnLoad
@CName("JNI_OnLoad")
fun kniOnLoad(vm: CPointer<JavaVMVar>, reserved: COpaquePointer): jint {
    // onLoad automatically registers KniCallbackProxy and KniLogger
    KniVM.onLoad(vm) {
        // KniRegister's this context is available here
        register(StringUtil)        // Register all methods in StringUtil
        register(DataFormatter)     // Register all methods in DataFormatter
    }
    return JNI_VERSION_1_6
}

// Cleanup in JNI_OnUnload
@CName("JNI_OnUnload")
fun kniOnUnload(vm: CPointer<JavaVMVar>, reserved: COpaquePointer) {
    KniVM.onUnload()
}
```

### Default Registered Components

`KniVM.onLoad` automatically registers two base components:

- **KniCallbackProxy**: Handles Kotlin → Native callback proxies
- **KniLogger**: Handles logging

You don't need to register them manually—just register your custom components.

## Platform Support

| Platform | Status | Notes |
|----------|--------|-------|
| Android | ✅ | Full JNI + IPC support |
| iOS | ✅ | Via Kotlin/Native CInterop |
| macOS | ✅ | Via Kotlin/Native CInterop |
| Windows | ✅ | Via Kotlin/Native CInterop |
| Desktop JVM | ✅ | Via KniLoader |

## Android IPC (androidMain)

For Android cross-process communication, Kni provides `DreamSmartIPC`, enabling seamless IPC between apps.

### Core Concept

```
┌─────────────────┐         Binder         ┌─────────────────┐
│   Client App    │ ◄──────────────────► │   Server App    │
│                 │   DreamSmartIPC      │                 │
│  asClient/      │                      │   asServer()    │
│  asClientAutoProxy                    │                 │
└─────────────────┘                      └─────────────────┘
```

### IPC Rules and Restrictions

#### Interface Requirements

- **IPC interface must extend `IDreamIPC`**
- **Must be a real Kotlin/Java interface** (cannot be a regular class)

```kotlin
// ✅ Correct: interface extending IDreamIPC
interface ICalculatorService : IDreamIPC {
    fun add(a: Int, b: Int): Int
}

// ❌ Wrong: regular class cannot be used for IPC
class CalculatorService { ... }
```

#### Parameter and Return Value Serialization Rules

IPC automatically selects the handling method based on parameter/return value types:

| Type | Handling | Example |
|------|----------|---------|
| Serializable types | JSON serialization | `Int`, `String`, `@Serializable data class` |
| Interface types | Proxy transmission | `ICallback`, `IUser` |
| Non-serializable without interface | ❌ Throws error | Custom regular class |

```kotlin
interface ICalculatorService : IDreamIPC {
    // ✅ Int/String are serializable
    fun add(a: Int, b: Int): Int

    // ✅ @Serializable data class
    fun getUser(id: Long): User
    fun searchUsers(query: String): List<User>

    // ✅ Interface types will be proxied
    fun setCallback(callback: IOnResultListener<String>)

    // ❌ Custom regular class cannot be transmitted
    // fun getCustomObject(): CustomClass  // Will throw error!
}
```

#### asClient vs asClientAutoProxy

```kotlin
// asClient: manual handling of return value proxy
val service = DreamSmartIPC.asClient<ICalculatorService>(binder, ICalculatorService::class)

// asClientAutoProxy: automatic proxy for all parameters and return values (recommended)
val service = DreamSmartIPC.asClientAutoProxy<ICalculatorService>(binder, ICalculatorService::class)
```

### Complete Example

#### 1. Define IPC Interface

```kotlin
// androidMain
interface ICalculatorService : IDreamIPC {
    fun add(a: Int, b: Int): Int
    fun multiply(a: Int, b: Int): Int
    fun calculateAsync(a: Int, b: Int, callback: IOnResultListener<Int>)
}
```

#### 2. Server Implementation

```kotlin
// Server - implement IPC interface
object CalculatorService : ICalculatorService {
    actual fun add(a: Int, b: Int): Int = a + b
    actual fun multiply(a: Int, b: Int): Int = a * b
    actual fun calculateAsync(a: Int, b: Int, callback: IOnResultListener<Int>) {
        callback.onResult(a * b)
    }
}
```

#### 3. Server Exposure

IPC supports multiple ways to expose services. **RootService is just one option** (for scenarios requiring root privileges):

```kotlin
// Option A: Regular Service (no root required)
class CalculatorService : Service() {
    override fun onBind(intent: Intent): IBinder =
        DreamSmartIPC.asServer(CalculatorService, ICalculatorService::class)
}

// Option B: Root Service (requires root privileges)
class CalculatorRootService : RootService() {
    override fun onBind(intent: Intent): IBinder =
        DreamSmartIPC.asServer(CalculatorService, ICalculatorService::class)
}
```

#### 4. Client Connection

```kotlin
// Client
object CalculatorClient : ServiceConnection {
    var service: ICalculatorService? = null

    fun start(context: Context) {
        val intent = Intent(context, CalculatorService::class.java)
        context.bindService(intent, this, Context.BIND_AUTO_CREATE)
    }

    override fun onServiceConnected(name: ComponentName?, binder: IBinder) {
        // Auto proxy all complex types
        service = DreamSmartIPC.asClientAutoProxy(binder, ICalculatorService::class)
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        service = null
    }
}
```

#### 5. Client Usage

```kotlin
CalculatorClient.start(this)

// Call IPC methods as if they were local
val result = CalculatorClient.service?.add(10, 20)
CalculatorClient.service?.calculateAsync(5, 6) { r ->
    println("Result: $r")
}
```

### Three Transfer Modes

| Mode | Description | Limit | Use Case |
|------|-------------|-------|----------|
| `DreamJsonBinder` | JSON serialization | ~1MB | Small data, simple objects |
| `DreamSharedMemoryBinder` | SharedMemory | ~4GB | Large data, high performance |
| `DreamJsonChunkBinder` | Chunked JSON | ~4GB | Medium data, good compatibility |

**Auto-selection**: Default `isChunk = true`, SDK 27+ uses SharedMemory, below 27 uses chunked JSON.

## Architecture

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

## License

Apache License 2.0
