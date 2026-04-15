@file:OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)

package org.github.dreammooncai.kni

import com.martmists.multiplatform.reflect.asClass
import com.martmists.multiplatform.reflect.declaringClassNameWithDollar
import com.martmists.multiplatform.reflect.getter
import com.martmists.multiplatform.reflect.nameWithDollar
import com.martmists.multiplatform.reflect.params
import com.martmists.multiplatform.reflect.propertyName
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import org.github.dreammooncai.kni.factory.AnyClass
import org.github.dreammooncai.kni.factory.BooleanClass
import org.github.dreammooncai.kni.factory.BooleanType
import org.github.dreammooncai.kni.factory.ByteClass
import org.github.dreammooncai.kni.factory.ByteType
import org.github.dreammooncai.kni.factory.CharClass
import org.github.dreammooncai.kni.factory.CharType
import org.github.dreammooncai.kni.factory.DoubleClass
import org.github.dreammooncai.kni.factory.DoubleType
import org.github.dreammooncai.kni.factory.FloatClass
import org.github.dreammooncai.kni.factory.FloatType
import org.github.dreammooncai.kni.factory.IntClass
import org.github.dreammooncai.kni.factory.IntType
import org.github.dreammooncai.kni.factory.JavaClass
import org.github.dreammooncai.kni.factory.JavaStringClass
import org.github.dreammooncai.kni.data.base.JniField
import org.github.dreammooncai.kni.data.KniAny
import org.github.dreammooncai.kni.data.KniClass
import org.github.dreammooncai.kni.data.KniField
import org.github.dreammooncai.kni.data.KniMethod
import org.github.dreammooncai.kni.factory.LinkedHashMapClass
import org.github.dreammooncai.kni.factory.LongClass
import org.github.dreammooncai.kni.factory.LongType
import org.github.dreammooncai.kni.factory.MapClass
import org.github.dreammooncai.kni.factory.ShortClass
import org.github.dreammooncai.kni.factory.ShortType
import org.github.dreammooncai.kni.factory.UnitClass
import org.github.dreammooncai.kni.factory.UnitType
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.cstr
import kotlinx.cinterop.get
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.nativeNullPtr
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Runnable
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import org.github.dreammooncai.kni.data.KniThrowable
import org.github.dreammooncai.kni.factory.ArrayListClass
import org.github.dreammooncai.kni.factory.BooleanArrayClass
import org.github.dreammooncai.kni.factory.ByteArrayClass
import org.github.dreammooncai.kni.factory.CharArrayClass
import org.github.dreammooncai.kni.factory.ComparableClass
import org.github.dreammooncai.kni.factory.DeserializationStrategyClass
import org.github.dreammooncai.kni.factory.DoubleArrayClass
import org.github.dreammooncai.kni.factory.EnumClass
import org.github.dreammooncai.kni.factory.ExceptionClass
import org.github.dreammooncai.kni.factory.FloatArrayClass
import org.github.dreammooncai.kni.factory.IllegalArgumentExceptionClass
import org.github.dreammooncai.kni.factory.IntArrayClass
import org.github.dreammooncai.kni.factory.JavaCharSequenceClass
import org.github.dreammooncai.kni.factory.JavaCollectionClass
import org.github.dreammooncai.kni.factory.JavaIterableClass
import org.github.dreammooncai.kni.factory.JavaIteratorClass
import org.github.dreammooncai.kni.factory.JavaListClass
import org.github.dreammooncai.kni.factory.JavaSetClass
import org.github.dreammooncai.kni.factory.KFunction0Class
import org.github.dreammooncai.kni.factory.KFunction1Class
import org.github.dreammooncai.kni.factory.KFunction2Class
import org.github.dreammooncai.kni.factory.KniDslMarker
import org.github.dreammooncai.kni.factory.LongArrayClass
import org.github.dreammooncai.kni.factory.NumberClass
import org.github.dreammooncai.kni.factory.ObjectArrayClass
import org.github.dreammooncai.kni.factory.PairClass
import org.github.dreammooncai.kni.factory.RunnableClass
import org.github.dreammooncai.kni.factory.RuntimeExceptionClass
import org.github.dreammooncai.kni.factory.SerializationClass
import org.github.dreammooncai.kni.factory.SerializationStrategyClass
import org.github.dreammooncai.kni.factory.ShortArrayClass
import org.github.dreammooncai.kni.factory.StringBuilderClass
import org.github.dreammooncai.kni.factory.ThrowableClass
import org.github.dreammooncai.kni.factory.TripleClass
import org.github.dreammooncai.kni.util.DexSignUtil
import platform.jni.JNIEnvVar
import platform.jni.JNIGlobalRefType
import platform.jni.JNIInvalidRefType
import platform.jni.JNILocalRefType
import platform.jni.JNINativeMethod
import platform.jni.JNIWeakGlobalRefType
import platform.jni.JNI_FALSE
import platform.jni.JNI_TRUE
import platform.jni.jarray
import platform.jni.jboolean
import platform.jni.jbooleanArray
import platform.jni.jbooleanVar
import platform.jni.jbyte
import platform.jni.jbyteArray
import platform.jni.jchar
import platform.jni.jcharArray
import platform.jni.jclass
import platform.jni.jdouble
import platform.jni.jdoubleArray
import platform.jni.jfloat
import platform.jni.jfloatArray
import platform.jni.jint
import platform.jni.jintArray
import platform.jni.jlong
import platform.jni.jlongArray
import platform.jni.jobject
import platform.jni.jobjectArray
import platform.jni.jshort
import platform.jni.jshortArray
import platform.jni.jstring
import platform.jni.jvalue
import kotlin.experimental.ExperimentalNativeApi
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.time.measureTime

private val classCache = mutableMapOf<String, KniClass>()
private val classCacheLock = reentrantLock()
private val refCache = mutableListOf<jobject>()
private val refCacheLock = reentrantLock()

@KniDslMarker
open class KniBridge internal constructor(
    var createEnv: () -> CPointer<JNIEnvVar> = {
        KniVM.getCurrentThreadEnv() ?: KniVM.attachCurrentThread()
    }
) {
    internal constructor(bridge: KniBridge) : this(bridge.createEnv)

    val env: CPointer<JNIEnvVar> get() = createEnv()
    private val innerEnv get() = env.pointed.pointed!!
    private val fPushLocalFrame get() = innerEnv.PushLocalFrame!!
    private val fPopLocalFrame get() = innerEnv.PopLocalFrame!!
    private val fRegisterNatives get() = innerEnv.RegisterNatives!!
    private val fExceptionCheck get() = innerEnv.ExceptionCheck!!
    private val fExceptionOccurred get() = innerEnv.ExceptionOccurred!!
    private val fExceptionClear get() = innerEnv.ExceptionClear!!
    private val fIsSameObject get() = innerEnv.IsSameObject!!

    protected val fNewStringUTF get() = innerEnv.NewStringUTF!!
    protected val fNewIntArray get() = innerEnv.NewIntArray!!
    protected val fSetIntArrayRegion get() = innerEnv.SetIntArrayRegion!!

    protected val fNewBooleanArray get() = innerEnv.NewBooleanArray!!
    protected val fSetBooleanArrayRegion get() = innerEnv.SetBooleanArrayRegion!!

    protected val fNewShortArray get() = innerEnv.NewShortArray!!
    protected val fSetShortArrayRegion get() = innerEnv.SetShortArrayRegion!!

    protected val fNewByteArray get() = innerEnv.NewByteArray!!
    protected val fSetByteArrayRegion get() = innerEnv.SetByteArrayRegion!!

    protected val fNewLongArray get() = innerEnv.NewLongArray!!
    protected val fSetLongArrayRegion get() = innerEnv.SetLongArrayRegion!!

    protected val fNewCharArray get() = innerEnv.NewCharArray!!
    protected val fSetCharArrayRegion get() = innerEnv.SetCharArrayRegion!!

    protected val fNewFloatArray get() = innerEnv.NewFloatArray!!
    protected val fSetFloatArrayRegion get() = innerEnv.SetFloatArrayRegion!!

    protected val fNewDoubleArray get() = innerEnv.NewDoubleArray!!
    protected val fSetDoubleArrayRegion get() = innerEnv.SetDoubleArrayRegion!!

    internal val fNewObjectArray get() = innerEnv.NewObjectArray!!
    internal val fSetObjectArrayElement get() = innerEnv.SetObjectArrayElement!!

    protected val fGetStringUTFChars get() = innerEnv.GetStringUTFChars!!
    protected val fFindClass get() = innerEnv.FindClass!!

    protected val fGetObjectField get() = innerEnv.GetObjectField!!
    protected val fGetStaticObjectField get() = innerEnv.GetStaticObjectField!!

    protected val fSetObjectField get() = innerEnv.SetObjectField!!
    protected val fSetStaticObjectField get() = innerEnv.SetStaticObjectField!!

    protected val fNewObject get() = innerEnv.NewObjectA!!

    protected val fCallObjectMethodA get() = innerEnv.CallObjectMethodA!!
    protected val fCallStaticObjectMethodA get() = innerEnv.CallStaticObjectMethodA!!
    protected val fCallNonvirtualObjectMethodA get() = innerEnv.CallNonvirtualObjectMethodA!!

    protected val fCallVoidMethodA get() = innerEnv.CallVoidMethodA!!
    protected val fCallStaticVoidMethodA get() = innerEnv.CallStaticVoidMethodA!!
    protected val fCallNonvirtualVoidMethodA get() = innerEnv.CallNonvirtualVoidMethodA!!

    protected val fGetIntField get() = innerEnv.GetIntField!!
    protected val fGetStaticIntField get() = innerEnv.GetStaticIntField!!
    protected val fSetIntField get() = innerEnv.SetIntField!!
    protected val fSetStaticIntField get() = innerEnv.SetStaticIntField!!

    internal val fCallIntMethodA get() = innerEnv.CallIntMethodA!!
    protected val fCallStaticIntMethodA get() = innerEnv.CallStaticIntMethodA!!
    protected val fCallNonvirtualIntMethodA get() = innerEnv.CallNonvirtualIntMethodA!!

    protected val fGetByteField get() = innerEnv.GetByteField!!
    protected val fGetStaticByteField get() = innerEnv.GetStaticByteField!!
    protected val fSetByteField get() = innerEnv.SetByteField!!
    protected val fSetStaticByteField get() = innerEnv.SetStaticByteField!!

    protected val fCallByteMethodA get() = innerEnv.CallByteMethodA!!
    protected val fCallStaticByteMethodA get() = innerEnv.CallStaticByteMethodA!!
    protected val fCallNonvirtualByteMethodA get() = innerEnv.CallNonvirtualByteMethodA!!

    protected val fGetLongField get() = innerEnv.GetLongField!!
    protected val fGetStaticLongField get() = innerEnv.GetStaticLongField!!
    protected val fSetLongField get() = innerEnv.SetLongField!!
    protected val fSetStaticLongField get() = innerEnv.SetStaticLongField!!

    protected val fCallLongMethodA get() = innerEnv.CallLongMethodA!!
    protected val fCallStaticLongMethodA get() = innerEnv.CallStaticLongMethodA!!
    protected val fCallNonvirtualLongMethodA get() = innerEnv.CallNonvirtualLongMethodA!!

    protected val fGetShortField get() = innerEnv.GetShortField!!
    protected val fGetStaticShortField get() = innerEnv.GetStaticShortField!!
    protected val fSetShortField get() = innerEnv.SetShortField!!
    protected val fSetStaticShortField get() = innerEnv.SetStaticShortField!!

    protected val fCallShortMethodA get() = innerEnv.CallShortMethodA!!
    protected val fCallStaticShortMethodA get() = innerEnv.CallStaticShortMethodA!!
    protected val fCallNonvirtualShortMethodA get() = innerEnv.CallNonvirtualShortMethodA!!

    protected val fGetDoubleField get() = innerEnv.GetDoubleField!!
    protected val fGetStaticDoubleField get() = innerEnv.GetStaticDoubleField!!
    protected val fSetDoubleField get() = innerEnv.SetDoubleField!!
    protected val fSetStaticDoubleField get() = innerEnv.SetStaticDoubleField!!

    protected val fCallDoubleMethodA get() = innerEnv.CallDoubleMethodA!!
    protected val fCallStaticDoubleMethodA get() = innerEnv.CallStaticDoubleMethodA!!
    protected val fCallNonvirtualDoubleMethodA get() = innerEnv.CallNonvirtualDoubleMethodA!!

    protected val fGetFloatField get() = innerEnv.GetFloatField!!
    protected val fGetStaticFloatField get() = innerEnv.GetStaticFloatField!!
    protected val fSetFloatField get() = innerEnv.SetFloatField!!
    protected val fSetStaticFloatField get() = innerEnv.SetStaticFloatField!!

    protected val fCallFloatMethodA get() = innerEnv.CallFloatMethodA!!
    protected val fCallStaticFloatMethodA get() = innerEnv.CallStaticFloatMethodA!!
    protected val fCallNonvirtualFloatMethodA get() = innerEnv.CallNonvirtualFloatMethodA!!

    protected val fGetCharField get() = innerEnv.GetCharField!!
    protected val fGetStaticCharField get() = innerEnv.GetStaticCharField!!
    protected val fSetCharField get() = innerEnv.SetCharField!!
    protected val fSetStaticCharField get() = innerEnv.SetStaticCharField!!

    protected val fCallCharMethodA get() = innerEnv.CallCharMethodA!!
    protected val fCallStaticCharMethodA get() = innerEnv.CallStaticCharMethodA!!
    protected val fCallNonvirtualCharMethodA get() = innerEnv.CallNonvirtualCharMethodA!!

    protected val fGetBooleanField get() = innerEnv.GetBooleanField!!
    protected val fGetStaticBooleanField get() = innerEnv.GetStaticBooleanField!!
    protected val fSetBooleanField get() = innerEnv.SetBooleanField!!
    protected val fSetStaticBooleanField get() = innerEnv.SetStaticBooleanField!!

    protected val fCallBooleanMethodA get() = innerEnv.CallBooleanMethodA!!
    protected val fCallStaticBooleanMethodA get() = innerEnv.CallStaticBooleanMethodA!!
    protected val fCallNonvirtualBooleanMethodA get() = innerEnv.CallNonvirtualBooleanMethodA!!

    protected val fGetObjectClass get() = innerEnv.GetObjectClass!!

    private val fGetArrayLength get() = innerEnv.GetArrayLength!!

    internal val fToReflectedMethod get() = innerEnv.ToReflectedMethod!!
    internal val fToReflectedField get() = innerEnv.ToReflectedField!!

    private val fGetLongArrayElements get() = innerEnv.GetLongArrayElements!!
    private val fGetIntArrayElements get() = innerEnv.GetIntArrayElements!!
    private val fGetShortArrayElements get() = innerEnv.GetShortArrayElements!!
    private val fGetBooleanArrayElements get() = innerEnv.GetBooleanArrayElements!!
    private val fGetByteArrayElements get() = innerEnv.GetByteArrayElements!!
    private val fGetCharArrayElements get() = innerEnv.GetCharArrayElements!!
    private val fGetDoubleArrayElements get() = innerEnv.GetDoubleArrayElements!!
    private val fGetFloatArrayElements get() = innerEnv.GetFloatArrayElements!!
    private val fGetObjectArrayElements get() = innerEnv.GetObjectArrayElement!!

    protected val fIsInstanceOf get() = innerEnv.IsInstanceOf!!

    protected val fNewGlobalRef get() = innerEnv.NewGlobalRef!!
    protected val fNewWeakGlobalRef get() = innerEnv.NewWeakGlobalRef!!
    protected val fNewLocalRef get() = innerEnv.NewLocalRef!!
    protected val fGetObjectRefType get() = innerEnv.GetObjectRefType!!
    protected val fDeleteGlobalRef get() = innerEnv.DeleteGlobalRef!!
    protected val fDeleteWeakGlobalRef get() = innerEnv.DeleteWeakGlobalRef!!
    protected val fDeleteLocalRef get() = innerEnv.DeleteLocalRef!!
    protected val fReleaseStringUTFChars get() = innerEnv.ReleaseStringUTFChars!!
    protected val fReleaseByteArrayElements get() = innerEnv.ReleaseByteArrayElements!!
    protected val fReleaseBooleanArrayElements get() = innerEnv.ReleaseBooleanArrayElements!!
    protected val fReleaseShortArrayElements get() = innerEnv.ReleaseShortArrayElements!!
    protected val fReleaseIntArrayElements get() = innerEnv.ReleaseIntArrayElements!!
    protected val fReleaseLongArrayElements get() = innerEnv.ReleaseLongArrayElements!!
    protected val fReleaseCharArrayElements get() = innerEnv.ReleaseCharArrayElements!!
    protected val fReleaseFloatArrayElements get() = innerEnv.ReleaseFloatArrayElements!!
    protected val fReleaseDoubleArrayElements get() = innerEnv.ReleaseDoubleArrayElements!!

    internal val fGetStaticMethodID get() = innerEnv.GetStaticMethodID!!
    internal val fGetMethodID get() = innerEnv.GetMethodID!!
    internal val fGetStaticFieldID get() = innerEnv.GetStaticFieldID!!
    internal val fGetFieldID get() = innerEnv.GetFieldID!!

    companion object {
        val jboolean.asKni get() = this == JNI_TRUE.toUByte()

        val jchar.asKni get() = this.toInt().toChar()

        val Boolean.asJni get() = if (this) JNI_TRUE.toUByte() else JNI_FALSE.toUByte()

        private fun formClass(kni: KniBridge, name: String): jclass = with(kni) {
            memScoped { runJavaCatching { fFindClass(env, name.cstr.ptr) }.getOrNull() } ?: error(
                "未能成功转换或找到类: ${
                    DexSignUtil.getTypeName(
                        name
                    )
                }"
            )
        }
    }

    private var errorCallback: KniBridge.(err: Throwable) -> Any? = {
        logger.error(it) {
            "Kni 遇到错误: ${it.message}"
        }
        null
    }

    @KniDslMarker
    fun setErrorCallback(callback: (KniBridge.(err: Throwable) -> Any?)?): KniBridge {
        callback?.let { errorCallback = callback }
        return this
    }

    @KniDslMarker
    fun register(block: KniRegister.() -> Unit): Boolean {
        val register = KniRegister(this).apply(block)
        val registerKniMap = register.registerKniMap
        val registerKFunctionMap = register.registerKFunctionMap
        if (registerKniMap.isEmpty() && registerKFunctionMap.isEmpty()) return false

        return memScoped {
            registerKniMap.entries.groupBy { (method, _) -> method.methodDeclaringClass ?: method.methodThisRef }
                .all { (clazz, methods) ->
                    val nMethods = allocArray<JNINativeMethod>(methods.size)
                    methods.forEachIndexed { index, (method, callback) ->
                        nMethods[index].name = method.methodName.cstr.ptr
                        nMethods[index].signature = buildString {
                            append("(")
                            append(method.methodParamTypes.joinToString("") { DexSignUtil.getTypeSign(it) })
                            append(")")
                            append(DexSignUtil.getTypeSign(method.methodReturnType ?: "void"))
                        }.cstr.ptr
                        nMethods[index].fnPtr = callback
                    }
                    fRegisterNatives(env, clazz?.asClass ?: error("Unknown in Class"), nMethods, methods.size) == 0
                } && registerKFunctionMap.entries.groupBy { (method, _) -> method.declaringClassNameWithDollar }
                .all { (clazz, methods) ->
                    val nMethods = allocArray<JNINativeMethod>(methods.size)
                    methods.forEachIndexed { index, (method, callback) ->
                        nMethods[index].name = method.propertyName.cstr.ptr

                        nMethods[index].signature = buildString {
                            append("(")
                            append(method.params.joinToString("") {
                                DexSignUtil.getTypeSign(
                                    it?.asKniClass?.name ?: ""
                                )
                            })
                            append(")")
                            append(DexSignUtil.getTypeSign(method.returnType.asKniClass.name))
                        }.cstr.ptr
                        nMethods[index].fnPtr = callback
                    }
                    fRegisterNatives(env, clazz.toClass().asClass, nMethods, methods.size) == 0
                }
        }
    }

    @Suppress("UNCHECKED_CAST")
    val <T : jobject> T.ref: T
        get() = this.let { ref ->
            if (isInvalidRef) error("对象 $ref 已被释放")
            if (isGlobalRef) return@let ref
            val result = refCacheLock.withLock { refCache.find { fIsSameObject(env, ref, it).asKni } }
            if (result != null) return@let result
            fNewGlobalRef(env, ref)?.also {
                refCacheLock.withLock { refCache.add(it) }
            } ?: error("对象 $ref 无法获取全局引用")
        } as T

    @Suppress("UNCHECKED_CAST")
    val <T : jobject> T.refToWeak
        get() = if (isInvalidRef) error("对象 $this 已被释放") else if (isGlobalRef || isWeakGlobalRef) this else fNewWeakGlobalRef(env, this).also { ref ->
            ref ?: error("对象 $this 无法获取弱全局引用")
            refCacheLock.withLock {
                refCache.removeAll {
                    if (fIsSameObject(env, this, it).asKni) {
                        fDeleteGlobalRef(env, it)
                        true
                    } else it.isInvalidRef
                }
            }
        } as T

    @Suppress("UNCHECKED_CAST")
    val <T: jobject> T.refToLocal: T
        get() = this.let { ref ->
            if (isInvalidRef) error("对象 $ref 已被释放")
            if (isLocalRef) return@let ref
            val local = fNewLocalRef(env, ref)
            refCacheLock.withLock {
                refCache.removeAll {
                    if (fIsSameObject(env, ref, it).asKni) {
                        if (isGlobalRef) fDeleteGlobalRef(env, it) else if (isWeakGlobalRef) fDeleteWeakGlobalRef(env, it)
                        true
                    } else it.isInvalidRef
                }
            }
            local
        } as T

    val jobject?.isInvalidRef: Boolean get() = this == null || this.rawValue == nativeNullPtr || fGetObjectRefType(env, this) == JNIInvalidRefType

    val jobject?.isLocalRef: Boolean get() = this.let { ref ->
        if (this == null || isInvalidRef) return@let false
        fGetObjectRefType(env, ref) == JNILocalRefType
    }

    val jobject?.isGlobalRef: Boolean get() = this.let { ref ->
        if (this == null || isInvalidRef) return@let false
        fGetObjectRefType(env, ref) == JNIGlobalRefType
    }

    val jobject?.isWeakGlobalRef: Boolean get() = this.let { ref ->
        if (this == null || isInvalidRef) return@let false
        fGetObjectRefType(env, ref) == JNIWeakGlobalRefType
    }

    fun jobject?.deleteRef() {
        if (this == null || isInvalidRef || isLocalRef) return
        refCacheLock.withLock {
            refCache.removeAll {
                fIsSameObject(env, this, it).asKni || it.isInvalidRef
            }
        }
        if (isGlobalRef) fDeleteGlobalRef(env, this@deleteRef) else if (isWeakGlobalRef) fDeleteWeakGlobalRef(env, this@deleteRef)
    }

    fun jobject?.deleteLocalRef() {
        if (this == null || isInvalidRef || !isLocalRef) return
        fDeleteLocalRef(env, this@deleteLocalRef)
    }

    fun jobject?.isInstanceOf(clazz: KniAny): Boolean = this.let { ref ->
        if (ref == null || isInvalidRef) return@let false
        val targetClass = if (clazz is KniClass) clazz.jObject else clazz.asClass
        if (targetClass.isInvalidRef) return@let false
        fun eq(clazz: jclass) = fIsInstanceOf(env, ref, clazz).asKni

        eq(targetClass)
    }

    val jobject?.isClass get() = isInstanceOf(JavaClass)

    val jobject.asKni
        get() = if (isInvalidRef) error("对象 $this 已被释放") else if (isClass) KniClass.create(this@KniBridge, this) else KniAny.create(
            this@KniBridge,
            this
        )

    val String.asJni: jstring
        get() = memScoped {
            runJavaCatching { fNewStringUTF(env, this@asJni.cstr.ptr) ?: error("转换字符串失败") }.getOrThrow()
        }

    /**
     * 将JNI字符串转换为Kotlin字符串 UFT-8编码
     */
    val jstring.asString
        get() = this.let { str ->
            memScoped {
                val isCopy = alloc<jbooleanVar>()
                val chars = fGetStringUTFChars(env, str, isCopy.ptr)
                try {
                    chars?.toKString() ?: error("转换字符串失败")
                } finally {
                    if (chars != null) {
                        fReleaseStringUTFChars(env, str, chars) // 补充释放
                    }
                }
            }
        }

    val jbyteArray.asByteList
        get() = this.let { array ->
            memScoped {
                val length = fGetArrayLength(env, array)
                val elements = runJavaCatching { fGetByteArrayElements(env, array, alloc<jbooleanVar>().ptr) }.getOrNull()
                if (elements != null) try{
                    List(length) { elements[it] }
                } finally {
                    fReleaseByteArrayElements(env, array, elements, 0)
                } else emptyList()
            }
        }

    val jbooleanArray.asBooleanList
        get() = this.let { array ->
            memScoped {
                val length = fGetArrayLength(env, array)
                val elements = runJavaCatching { fGetBooleanArrayElements(env, array, alloc<jbooleanVar>().ptr) }.getOrNull()
                if (elements != null) try{
                    List(length) { elements[it] }
                } finally {
                    fReleaseBooleanArrayElements(env, array, elements, 0)
                } else emptyList()
            }
        }

    val jshortArray.asShortList
        get() = this.let { array ->
            memScoped {
                val length = fGetArrayLength(env, array)
                val elements = runJavaCatching { fGetShortArrayElements(env, array, alloc<jbooleanVar>().ptr) }.getOrNull()
                if (elements != null) try{
                    List(length) { elements[it] }
                } finally {
                    fReleaseShortArrayElements(env, array, elements, 0)
                } else emptyList()
            }
        }

    val jintArray.asIntList
        get() = this.let { array ->
            memScoped {
                val length = fGetArrayLength(env, array)
                val elements = runJavaCatching { fGetIntArrayElements(env, array, alloc<jbooleanVar>().ptr) }.getOrNull()
                if (elements != null) try{
                    List(length) { elements[it] }
                } finally {
                    fReleaseIntArrayElements(env, array, elements, 0)
                } else emptyList()
            }
        }

    val jlongArray.asLongList
        get() = this.let { array ->
            memScoped {
                val length = fGetArrayLength(env, array)
                val elements = runJavaCatching { fGetLongArrayElements(env, array, alloc<jbooleanVar>().ptr) }.getOrNull()
                if (elements != null) try{
                    List(length) { elements[it] }
                } finally {
                    fReleaseLongArrayElements(env, array, elements, 0)
                } else emptyList()
            }
        }

    val jcharArray.asCharList
        get() = this.let { array ->
            memScoped {
                val length = fGetArrayLength(env, array)
                val elements = runJavaCatching { fGetCharArrayElements(env, array, alloc<jbooleanVar>().ptr) }.getOrNull()
                if (elements != null) try{
                    List(length) { elements[it] }
                } finally {
                    fReleaseCharArrayElements(env, array, elements, 0)
                } else emptyList()
            }
        }

    val jfloatArray.asFloatList
        get() = this.let { array ->
            memScoped {
                val length = fGetArrayLength(env, array)
                val elements = runJavaCatching { fGetFloatArrayElements(env, array, alloc<jbooleanVar>().ptr) }.getOrNull()
                if (elements != null) try{
                    List(length) { elements[it] }
                } finally {
                    fReleaseFloatArrayElements(env, array, elements, 0)
                } else emptyList()
            }
        }

    val jdoubleArray.asDoubleList
        get() = this.let { array ->
            memScoped {
                val length = fGetArrayLength(env, array)
                val elements = runJavaCatching { fGetDoubleArrayElements(env, array, alloc<jbooleanVar>().ptr) }.getOrNull()
                if (elements != null) try{
                    List(length) { elements[it] }
                } finally {
                    fReleaseDoubleArrayElements(env, array, elements, 0)
                } else emptyList()
            }
        }

    val jarray.asArray
        get() = this.let { array ->
            val length = fGetArrayLength(env, array)
            val list = mutableListOf<KniAny?>()
            for (i in 0 until length) {
                val element = runJavaCatching { fGetObjectArrayElements(env, array, i) }.getOrNull()
                if (element != null)
                    list += element.asKni
            }
            list.toTypedArray()
        }

    inline fun <reified T> jobject.iterateJavaIterable(item: (item: T?) -> Unit) {
        val any = this.ref.asKni

        // iterator()
        val itrAny = any.method {
            name = "iterator"
            returnType = JavaIteratorClass
            thisRef = any
        }.call()?.ref?.asKni ?: return

        // hasNext()
        val hasNext = itrAny.method {
            name = "hasNext"
            returnType = BooleanType
            thisRef = itrAny
        }

        // next()
        val next = itrAny.method {
            name = "next"
            returnType = AnyClass
            thisRef = itrAny
        }

        while (hasNext.boolean()) {
            val e = next.call()
            item(e?.asAnyKni<T>())
        }

        itrAny.jObject.deleteRef()
        any.jObject.deleteRef()
    }

    val jobject.asList: List<KniAny?>
        get() = asList<KniAny?>()

    inline fun <reified T> jobject.asList(): List<T> {
        val result = mutableListOf<T>()
        iterateJavaIterable<T> {
            result.add(it as T)
        }
        return result
    }

    val jobject.asSet: Set<KniAny?>
        get() = asSet<KniAny?>()

    inline fun <reified T> jobject.asSet(): Set<T> {
        val result = mutableSetOf<T>()
        iterateJavaIterable<T> {
            result.add(it as T)
        }
        return result
    }

    @Suppress("UNCHECKED_CAST")
    val Any.asKniClass: KniClass
        get() = this.let { value ->
            when (value) {
                is KniClass -> value
                is KniAny -> value.asKniClass
                is KClass<*> -> value.javaPrimitive
                is KType -> {
                    if (value.asClass == Array::class)
                        "${value.arguments.first().type!!.asClass.javaPrimitive.name}[]".toClass()
                    else value.asClass.asKniClass
                }

                is COpaquePointer -> (value as jobject).asKni.asKniClass
                else -> value::class.java
            }
        }

    fun String.toClass(): KniClass = this.let { name ->
        val cache = classCacheLock.withLock {
            classCache[name]
        }
        if (cache != null) return@let cache
        val clazz = KniClass.create(this@KniBridge, memScoped {
            val formattedClassName = DexSignUtil.toFindClass(name)

            fun getPrimitiveType(clazz: KniClass): jclass {
                val id = fGetStaticFieldID(
                    env,
                    clazz.asClass,
                    "TYPE".cstr.ptr,
                    DexSignUtil.getTypeSign("java.lang.Class").cstr.ptr
                )
                return fGetStaticObjectField(env, clazz.asClass, id) as jclass
            }

            // primitive signatures
            when (formattedClassName) {
                "Z", "boolean" -> getPrimitiveType(BooleanClass)
                "B", "byte" -> getPrimitiveType(ByteClass)
                "C", "char" -> getPrimitiveType(CharClass)
                "S", "short" -> getPrimitiveType(ShortClass)
                "I", "int" -> getPrimitiveType(IntClass)
                "F", "float" -> getPrimitiveType(FloatClass)
                "J", "long" -> getPrimitiveType(LongClass)
                "D", "double" -> getPrimitiveType(DoubleClass)
                "V", "void" -> getPrimitiveType(UnitClass)

                else -> formClass(this@KniBridge, formattedClassName)
            }
        })
        classCache[name] = clazz
        clazz
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> Array<*>.toJValue(isObj: Boolean = false, block: MemScope.(value: CPointer<jvalue>) -> T) = memScoped {
        val value = allocArray<jvalue>(size)
        if (isEmpty()) { return@memScoped block(value) }
        val releaseList = mutableListOf<jobject?>()

        fun getLocalJniRef(value: Any): jobject? = when (value) {
            is COpaquePointer -> value as jobject
            is KniAny -> value.jObject
            else -> {
                val local = value.asObjJni()
                if (local != null) releaseList.add(local)
                local?.refToLocal
            }
        }

        for (index in indices) {
            val it = this@toJValue[index]
            if (it == null) {
                value[index].l = null
                continue
            }
            if (!isObj) {
                when (it) {
                    is Int -> value[index].i = it
                    is Long -> value[index].j = it
                    is Byte -> value[index].b = it
                    is Short -> value[index].s = it
                    is Double -> value[index].d = it
                    is Float -> value[index].f = it
                    is Char -> value[index].c = it.code.toUShort()
                    is Boolean -> value[index].z = (if (it) JNI_TRUE else JNI_FALSE).toUByte()
                    else -> {
                        value[index].l = getLocalJniRef(it) ?: throw Error("Unsupported conversion for ${it::class.simpleName}")
                        if (value[index].l.isInvalidRef) error("在 $index $it 中参数对象已释放")
                    }
                }
            } else {
                value[index].l = getLocalJniRef(it) ?: throw Error("Unsupported conversion for ${it::class.simpleName}")
                if (value[index].l.isInvalidRef) error("在 $index $it 中参数对象已释放")
            }
        }

        val result = block(value)
        releaseList.forEach { it?.deleteLocalRef() }
        result
    }

    val ByteArray.asJni: jbyteArray
        get() = usePinned { pin ->
            val array = fNewByteArray(env, size) ?: error("转换数组失败")
            if (isNotEmpty()) runJavaCatching {
                fSetByteArrayRegion(
                    env,
                    array,
                    0,
                    size,
                    pin.addressOf(0).reinterpret()
                )
            }.getOrThrow()
            array
        }

    val ShortArray.asJni: jshortArray
        get() = usePinned { pin ->
            val array = fNewShortArray(env, size) ?: error("转换数组失败")
            if (isNotEmpty()) runJavaCatching {
                fSetShortArrayRegion(
                    env,
                    array,
                    0,
                    size,
                    pin.addressOf(0).reinterpret()
                )
            }.getOrThrow()
            array
        }

    val IntArray.asJni: jintArray
        get() = usePinned { pin ->
            val array = fNewIntArray(env, size) ?: error("转换数组失败")
            if (isNotEmpty()) runJavaCatching {
                fSetIntArrayRegion(
                    env,
                    array,
                    0,
                    size,
                    pin.addressOf(0).reinterpret()
                )
            }.getOrThrow()
            array
        }

    val BooleanArray.asJni: jbooleanArray
        get() = this.map { it.asJni }.toTypedArray().toUByteArray().usePinned { pin ->
            val array = fNewBooleanArray(env, size) ?: error("转换数组失败")
            if (isNotEmpty()) runJavaCatching {
                fSetBooleanArrayRegion(
                    env,
                    array,
                    0,
                    size,
                    pin.addressOf(0).reinterpret()
                )
            }.getOrThrow()
            array
        }

    val LongArray.asJni: jlongArray
        get() = usePinned { pin ->
            val array = fNewLongArray(env, size) ?: error("转换数组失败")
            if (isNotEmpty()) runJavaCatching {
                fSetLongArrayRegion(
                    env,
                    array,
                    0,
                    size,
                    pin.addressOf(0).reinterpret()
                )
            }.getOrThrow()
            array
        }

    val FloatArray.asJni: jfloatArray
        get() = usePinned { pin ->
            val array = fNewFloatArray(env, size) ?: error("转换数组失败")
            if (isNotEmpty()) runJavaCatching {
                fSetFloatArrayRegion(
                    env,
                    array,
                    0,
                    size,
                    pin.addressOf(0).reinterpret()
                )
            }.getOrThrow()
            array
        }

    val DoubleArray.asJni: jdoubleArray
        get() = usePinned { pin ->
            val array = fNewDoubleArray(env, size) ?: error("转换数组失败")
            if (isNotEmpty()) runJavaCatching {
                fSetDoubleArrayRegion(
                    env,
                    array,
                    0,
                    size,
                    pin.addressOf(0).reinterpret()
                )
            }.getOrThrow()
            array
        }

    val CharArray.asJni: jcharArray
        get() = usePinned { pin ->
            val array = fNewCharArray(env, size) ?: error("转换数组失败")
            if (isNotEmpty()) runJavaCatching {
                fSetCharArrayRegion(
                    env,
                    array,
                    0,
                    size,
                    pin.addressOf(0).reinterpret()
                )
            }.getOrThrow()
            array
        }

    val Enum<*>.asJni
        get() = this::class.java.method {
            name = "valueOf"
            param(JavaStringClass)
            returnType = this@asJni::class.java
        }.call(name) ?: error("无法获取枚举在 $this")

    inline fun <reified T : Enum<T>> jobject.asEnum(): T {
        val name = asKniClass.method {
            name = "name"
            returnType = JavaStringClass
            thisRef = this@asEnum.asKni
        }.string()
        return enumValueOf(name)
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> Any?.asAnyKni(): T? {
        if (this == null) return null

        // 原始 jobject 的类型
        val kni = when (this) {
            is KniAny -> this
            is COpaquePointer -> (this as? jobject)?.asKni
            else -> null
        }
        val obj = kni?.jObject

        // ========== 泛型 T 的判断 ==========
        return when (T::class) {

            Unit::class -> Unit

            // -------- 基础类型 --------
            Int::class -> this as? Int
                ?: IntClass.method {
                    name = "intValue"
                    returnType = IntType
                    thisRef = kni ?: error("thisRef is null: ${this@asAnyKni}")
                }.int(obj)

            Long::class -> this as? Long
                ?: LongClass.method {
                    name = "longValue"
                    returnType = LongType
                    thisRef = kni ?: error("thisRef is null: ${this@asAnyKni}")
                }.long(obj)

            Float::class -> this as? Float
                ?: FloatClass.method {
                    name = "floatValue"
                    returnType = FloatType
                    thisRef = kni ?: error("thisRef is null: ${this@asAnyKni}")
                }.float(obj)

            Double::class -> this as? Double
                ?: DoubleClass.method {
                    name = "doubleValue"
                    returnType = DoubleType
                    thisRef = kni ?: error("thisRef is null: ${this@asAnyKni}")
                }.double(obj)

            Byte::class -> this as? Byte
                ?: ByteClass.method {
                    name = "byteValue"
                    returnType = ByteType
                    thisRef = kni ?: error("thisRef is null: ${this@asAnyKni}")
                }.byte(obj)

            Short::class -> this as? Short
                ?: ShortClass.method {
                    name = "shortValue"
                    returnType = ShortType
                    thisRef = kni ?: error("thisRef is null: ${this@asAnyKni}")
                }.short(obj)

            Boolean::class -> (this as? jboolean)?.asKni
                ?: BooleanClass.method {
                    name = "booleanValue"
                    returnType = BooleanType
                    thisRef = kni ?: error("thisRef is null: ${this@asAnyKni}")
                }.boolean(obj)

            Char::class -> this as? Char
                ?: CharClass.method {
                    name = "charValue"
                    returnType = CharType
                    thisRef = kni ?: error("thisRef is null: ${this@asAnyKni}")
                }.char(obj)

            // -------- String --------
            String::class -> obj?.asString

            // -------- 数组类型 --------
            ByteArray::class -> (this as? jbyteArray)?.asByteList
            IntArray::class -> (this as? jintArray)?.asIntList
            LongArray::class -> (this as? jlongArray)?.asLongList
            BooleanArray::class -> (this as? jbooleanArray)?.asBooleanList
            DoubleArray::class -> (this as? jdoubleArray)?.asDoubleList
            FloatArray::class -> (this as? jfloatArray)?.asFloatList
            ShortArray::class -> (this as? jshortArray)?.asShortList
            CharArray::class -> (this as? jcharArray)?.asCharList

            // -------- Java 对象数组 --------
            Array::class -> (this as? jarray)?.asArray

            // -------- Java List / Set / Collection --------
            Collection::class, MutableCollection::class, List::class, MutableList::class -> obj?.asList
            Set::class, MutableSet::class -> obj?.asSet

            // -------- Java Map --------
            Map::class, MutableMap::class,
            HashMap::class, LinkedHashMap::class -> obj?.asMap

            // -------- 其他对象 --------
            else -> runCatching { kni?.deserialize<T>() }.getOrElse { kni }
        } as? T
    }

    private fun Any?.asJniPacking(): jobject? = when (this) {
        is Int -> IntClass.method {
            name = "valueOf"
            param(IntType)
            returnType = IntClass
        }.call(this)

        is Long -> LongClass.method {
            name = "valueOf"
            param(LongType)
            returnType = LongClass
        }.call(this)

        is Byte -> ByteClass.method {
            name = "valueOf"
            param(ByteType)
            returnType = ByteClass
        }.call(this)

        is Short -> ShortClass.method {
            name = "valueOf"
            param(ShortType)
            returnType = ShortClass
        }.call(this)

        is Double -> DoubleClass.method {
            name = "valueOf"
            param(DoubleType)
            returnType = DoubleClass
        }.call(this)

        is Float -> FloatClass.method {
            name = "valueOf"
            param(FloatType)
            returnType = FloatClass
        }.call(this)

        is Char -> CharClass.method {
            name = "valueOf"
            param(CharType)
            returnType = CharClass
        }.call(this)

        is Boolean -> BooleanClass.method {
            name = "valueOf"
            param(BooleanType)
            returnType = BooleanClass
        }.call(this)

        else -> null
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> Any?.localLocalJniRelease(block: (value: jobject?) -> T) = when (this) {
        is COpaquePointer -> block(this as jobject)
        is KniAny -> {
            block(this.jObject)
        }

        else -> {
            val local = asObjJni()
            val result = block(local)
            local?.deleteLocalRef()
            result
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun Any?.asObjJni(): jobject? = when (this) {
        Unit, null -> null
        is COpaquePointer -> this as jobject
        is KniAny -> this.jObject
        is Enum<*> -> this.asJni
        is Number, is Char, is Boolean -> this.asJniPacking()
        is String -> this.asJni
        is ByteArray -> this.asJni
        is ShortArray -> this.asJni
        is IntArray -> this.asJni
        is BooleanArray -> this.asJni
        is LongArray -> this.asJni
        is FloatArray -> this.asJni
        is DoubleArray -> this.asJni
        is CharArray -> this.asJni
        is Array<*> -> if (isNotEmpty()) this.asJniNotEmpty else null
        is Collection<*> -> this.asJni
        is Map<*, *> -> this.asJni
        else -> getSerializer(this::class as KClass<Any>,json)?.let {
            runJavaCatching {
                this::class.java.serialize(json.encodeToString(it,this)).jObject
            }.onFailure { _ ->
                logger.error { "Unsupported conversion for ${this::class.simpleName}, serialize: ${json.encodeToString(it,this)}" }
            }.getOrThrow()
        } ?: throw Error("Unsupported conversion for ${this::class.simpleName}")
    }

    val Array<*>.asJniNotEmpty: jobjectArray
        get() {
            val array =
                runJavaCatching { fNewObjectArray(env, size, firstNotNullOf { it }.asKniClass.asClass, null)?.refToWeak }.getOrNull() ?: error("转换对象数组失败")
            forEachIndexed { index, element ->
                runJavaCatching {
                    element.localLocalJniRelease { element ->
                        fSetObjectArrayElement(env, array, index, element)
                    }
                }.getOrThrow()
            }
            return array
        }

    val Collection<*>.asJni: jobject
        get() {
            val list = ArrayListClass.constructor {
                param(IntType)
            }.new(size).refToWeak

            if (isEmpty()) return list
            val addMethod = ArrayListClass.method {
                thisRef = list.asKni
                name = "add"
                param(AnyClass)
                returnType = BooleanType
            }
            for (element in this) {
                addMethod.boolean(element)
            }

            return list
        }

    val Map<*, *>.asJni: jobject
        get() {
            val map = LinkedHashMapClass.constructor {
                param(IntType)
            }.new(size).refToWeak
            if (isEmpty()) return map
            val put = MapClass.method {
                thisRef = map.asKni
                name = "put"
                param(AnyClass, AnyClass)
                returnType = AnyClass
            }
            forEach { (key, value) ->
                put.call(key, value)
            }
            return map
        }

    val jobject.asMap: Map<KniAny, KniAny?>
        get() = asMap<KniAny, KniAny?>()

    inline fun <reified K, reified V> jobject.asMap(): Map<K, V> {
        val result = mutableMapOf<K, V>()

        // entrySet()
        val entrySet = asKni.method {
            name = "entrySet"
            returnType = JavaSetClass
            thisRef = asKni
        }.call() ?: return result

        // 使用 iterateJavaIterable
        entrySet.iterateJavaIterable<KniAny> { entryItem ->

            val entryAny = entryItem ?: return@iterateJavaIterable

            // getKey()
            val keyObj = entryAny.method {
                name = "getKey"
                returnType = AnyClass
                thisRef = entryAny
            }.call() ?: return@iterateJavaIterable

            val key = keyObj.asAnyKni<K>() ?: return@iterateJavaIterable

            // getValue()
            val valueObj = entryAny.method {
                name = "getValue"
                returnType = AnyClass
                thisRef = entryAny
            }.call()

            val value = valueObj?.asAnyKni<V>()

            result[key] = value as V
        }

        return result
    }

    fun Array<*>.asJni(clazz: KniClass): jobjectArray {
        return if (isEmpty()) {
            runJavaCatching { fNewObjectArray(env, size, clazz.asClass, null) }.getOrNull() ?: error("转换对象数组失败")
        } else asJniNotEmpty
    }

    inline fun <reified T> Array<T?>.asJni(): jobjectArray = asJni(T::class.java)

    @KniDslMarker
    open fun constructor(block: KniMethod.Build.() -> Unit = {}) = KniMethod.create(this@KniBridge) {
        name = "<init>"
        block()
    }

    @KniDslMarker
    open fun method(thisRef: KniAny? = null, block: KniMethod.Build.() -> Unit) = KniMethod.create(this@KniBridge) {
        if (thisRef != null) this.thisRef = thisRef
        block()
    }

    fun methodBasis(methodName: String, methodParamSign: String, thisRefClass: KniClass, thisRef: KniAny? = null) =
        KniMethod.createBasis(this@KniBridge, methodName, methodParamSign, thisRefClass, thisRef)

    fun method(jMethod: jobject) = JniField.create(this@KniBridge, jMethod)

    @KniDslMarker
    open fun field(thisRef: KniAny? = null, block: KniField.Build.() -> Unit) = KniField.create(this@KniBridge) {
        if (thisRef != null) this.thisRef = thisRef
        block()
    }

    fun field(fieldName: String, fieldType: String, thisRefClass: KniClass, thisRef: KniAny? = null) =
        KniField.createBasis(this@KniBridge, fieldName, fieldType, thisRefClass, thisRef)

    fun field(jField: jobject) = JniField.create(this@KniBridge, jField)

    val KClass<*>.java
        get() = when (this) {

            // primitive wrapper
            Int::class -> IntClass
            Long::class -> LongClass
            Byte::class -> ByteClass
            Short::class -> ShortClass
            Double::class -> DoubleClass
            Float::class -> FloatClass
            Char::class -> CharClass
            Boolean::class -> BooleanClass

            // primitive array
            IntArray::class -> IntArrayClass
            LongArray::class -> LongArrayClass
            ByteArray::class -> ByteArrayClass
            ShortArray::class -> ShortArrayClass
            DoubleArray::class -> DoubleArrayClass
            FloatArray::class -> FloatArrayClass
            CharArray::class -> CharArrayClass
            BooleanArray::class -> BooleanArrayClass

            // Kotlin Array<*> —— 没法区分 T，所以按你的定义返回 Object[]
            Array::class -> ObjectArrayClass

            // kotlin builtins
            String::class -> JavaStringClass
            CharSequence::class -> JavaCharSequenceClass
            Any::class -> AnyClass
            Unit::class -> UnitClass

            // collections
            List::class -> JavaListClass
            MutableList::class -> JavaListClass

            Set::class -> JavaSetClass
            MutableSet::class -> JavaSetClass

            Map::class -> MapClass
            MutableMap::class -> MapClass

            Collection::class -> JavaCollectionClass
            MutableCollection::class -> JavaCollectionClass

            Iterable::class -> JavaIterableClass

            ArrayList::class -> ArrayListClass
            LinkedHashMap::class -> LinkedHashMapClass

            // Kotlin function bridge
            Function0::class -> KFunction0Class
            Function1::class -> KFunction1Class
            Function2::class -> KFunction2Class

            // exceptions
            Throwable::class -> ThrowableClass
            Exception::class -> ExceptionClass
            RuntimeException::class -> RuntimeExceptionClass
            IllegalArgumentException::class -> IllegalArgumentExceptionClass

            // java lang
            Runnable::class -> RunnableClass
            Number::class -> NumberClass
            Comparable::class -> ComparableClass
            Enum::class -> EnumClass
            StringBuilder::class -> StringBuilderClass

            // Kotlin data helpers
            Pair::class -> PairClass
            Triple::class -> TripleClass

            // kotlinx.serialization
            Json::class -> SerializationClass
            SerializationStrategy::class -> SerializationStrategyClass
            DeserializationStrategy::class -> DeserializationStrategyClass

            else -> nameWithDollar?.toClass()
                ?: error("不支持全限定名: $nameWithDollar")
        }

    val KClass<*>.javaPrimitive
        get() = when (this) {
            Int::class -> IntType
            Long::class -> LongType
            Byte::class -> ByteType
            Short::class -> ShortType
            Double::class -> DoubleType
            Float::class -> FloatType
            Char::class -> CharType
            Boolean::class -> BooleanType
            Unit::class -> UnitType
            else -> java
        }

    val KClass<*>.isPrimitive
        get() = when (this) {
            jint::class, jlong::class, jbyte::class, jshort::class, jdouble::class, jfloat::class, jchar::class, jboolean::class, Unit::class -> true
            else -> false
        }

    @KniDslMarker
    fun KFunction<*>.asKniMethod(thisRef: KniAny? = null, block: KniMethod.Build.() -> Unit = {}) = method {
        declaringClass = declaringClassNameWithDollar
        name = this@asKniMethod.propertyName
        param(*params.toTypedArray().requireNoNulls())
        returnType = this@asKniMethod.returnType
        if (thisRef != null) this.thisRef = thisRef
        block()
    }

    @KniDslMarker
    fun KProperty<*>.asKniField(thisRef: KniAny? = null, block: KniField.Build.() -> Unit = {}) = field {
        declaringClass = getter.declaringClassNameWithDollar
        name = this@asKniField.name
        type = this@asKniField.returnType
        if (thisRef != null) this.thisRef = thisRef
        block()
    }

    fun <T, R> T.runJavaCatching(isNotifyErrorCallback: Boolean = true,block: T.() -> R): Result<R> = runCatching {
        val result = block()
        if (fExceptionCheck(env).asKni) {
            val jThrowable = fExceptionOccurred(env)!!
            fExceptionClear(env)   // 清除 JVM 异常状态
            throw KniThrowable(this@KniBridge, jThrowable).asKotlinThrowable()
        }
        result
    }.onFailure {
        if (isNotifyErrorCallback) errorCallback(it)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> tryException(block: () -> T): T = runJavaCatching(false) { block() }.getOrElse {
        runCatching { fPopLocalFrame(env, null) }
        errorCallback(it) as T
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> tryLocalFrame(isResultJava: Boolean = false,localFrameCapacity: Int = 8192, block: () -> T): T {
        if (fPushLocalFrame(env, localFrameCapacity) < 0) throw Error("Cannot push new local frame")
        val result = block()  // 执行 block，可能产生局部引用
        if (fExceptionCheck(env).asKni) {
            val jThrowable = fExceptionOccurred(env) ?: error("Cannot get exception")
            fExceptionClear(env)   // 清除 JVM 异常状态
            throw KniThrowable(this, jThrowable).asKotlinThrowable()
        }
        return if (result != null && !result::class.isPrimitive && isResultJava) {
            val local = fPopLocalFrame(env, result.asObjJni())
            when (result) {
                is COpaquePointer -> local
                is KniAny -> local?.asKni
                else -> result
            }
        } else {
            fPopLocalFrame(env, null)  // 清空局部引用栈
            result
        } as T
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> tryLocalFrameResultJava(localFrameCapacity: Int = 8192, block: () -> T): T = tryLocalFrame(true,localFrameCapacity,block)

    @KniDslMarker
    @Suppress("UNCHECKED_CAST")
    internal fun <T> internalKni(isResultJava: Boolean = false,localFrameCapacity: Int = 1024, block: KniBridge.() -> T): T = runCatching {
        tryLocalFrame(isResultJava,localFrameCapacity) { block() }
    }.getOrElse {
        runCatching { fPopLocalFrame(env, null) }
        errorCallback(it) as T
    }
}