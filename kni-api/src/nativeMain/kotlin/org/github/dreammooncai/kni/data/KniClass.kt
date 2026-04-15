package org.github.dreammooncai.kni.data

import org.github.dreammooncai.kni.factory.JavaClass
import org.github.dreammooncai.kni.data.base.JniField
import org.github.dreammooncai.kni.data.base.JniMethod
import kotlinx.cinterop.ExperimentalForeignApi
import org.github.dreammooncai.kni.KniBridge
import platform.jni.jarray
import platform.jni.jclass

@OptIn(ExperimentalForeignApi::class)
class KniClass internal constructor(bridge: KniBridge,jClass: jclass): KniAny(bridge,jClass) {
    companion object {
        fun create(bridge: KniBridge, jClass: jclass) = with(bridge) { KniClass(bridge, jClass.ref) }
    }

    override val ref: KniClass by lazy { this }

    val name by lazy {
        methodBasis("getName", "()Ljava/lang/String;",JavaClass,this).string()
    }

    val isArray by lazy {
        methodBasis("isArray", "()Z",JavaClass,this).boolean()
    }

    val isEnum by lazy {
        methodBasis("isEnum", "()Z",JavaClass,this).boolean()
    }

    val isPrimitive by lazy {
        methodBasis("isPrimitive", "()Z",JavaClass,this).boolean()
    }

    val componentType by lazy {
        methodBasis("getComponentType","()Ljava/lang/Class;",JavaClass,this).call()!!.asKniClass
    }

    val methods by lazy {
        val methodsArray: jarray = methodBasis("getMethods","()[Ljava/lang/reflect/Method;",JavaClass,this).call() ?: error("调用 getMethods 失败")
        methodsArray.asArray.mapNotNull {
            it?.let { it1 -> JniMethod.create(bridge, it1.jObject) }
        }
    }

    val fields by lazy {
        val methodsArray: jarray = methodBasis("getFields","()[Ljava/lang/reflect/Field;",JavaClass,this).call() ?: error("调用 getFields 失败")
        methodsArray.asArray.mapNotNull {
            it?.let { it1 -> JniField.create(bridge, it1.jObject) }
        }
    }

    fun isAssignableFrom(clazz: KniAny) = methodBasis("isAssignableFrom", "(Ljava/lang/Class;)Z",JavaClass,this).boolean(clazz)

    override fun constructor(block: KniMethod.Build.() -> Unit) = KniMethod.create(this) {
        methodDeclaringClass = this@KniClass
        name = "<init>"
        block()
    }

    override fun method(thisRef: KniAny?, block: KniMethod.Build.()->Unit) = KniMethod.create(this) {
        methodDeclaringClass = this@KniClass
        if (thisRef != null) this.thisRef = thisRef
        block()
    }

    override fun field(thisRef: KniAny?, block: KniField.Build.() -> Unit) = KniField.create(this) {
        fieldDeclaringClass = this@KniClass
        if (thisRef != null) this.thisRef = thisRef
        block()
    }

    override fun toString(): String {
        return buildString {
            append("KniClass(\n")
            append("  name = $name\n")
            append("  isArray = $isArray\n")
            append("  isPrimitive = $isPrimitive\n")
            if (isArray) {
                append("  componentType = ${componentType.name}\n")
            }
            append("  methods = [\n")
            methods.forEach { append("    ${it.name}${it.asSign.methodSign}\n") }
            append("  ]\n")
            append("  fields = [\n")
            fields.forEach { append("    ${it.name}:${it.type}\n") }
            append("  ]\n")
            append(")")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as KniClass

        return name == other.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}