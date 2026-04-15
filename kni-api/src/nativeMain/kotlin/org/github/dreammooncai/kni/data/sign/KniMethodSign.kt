package org.github.dreammooncai.kni.data.sign

import com.martmists.multiplatform.reflect.asClass
import com.martmists.multiplatform.reflect.declaringClassName
import com.martmists.multiplatform.reflect.declaringClassNameWithDollar
import com.martmists.multiplatform.reflect.nameWithDollar
import com.martmists.multiplatform.reflect.params
import org.github.dreammooncai.kni.data.base.JniMethod
import org.github.dreammooncai.kni.util.DexSignUtil.getParamTypeNames
import org.github.dreammooncai.kni.util.DexSignUtil.getTypeName
import org.github.dreammooncai.kni.util.DexSignUtil.getTypeSign
import kotlin.reflect.KFunction

class KniMethodSign {

    companion object {
        fun deserialize(descriptor: String) = KniMethodSign(descriptor)
    }

    val className: String
    val name: String
    val paramTypeNames: List<String>
    val returnTypeName: String

    val declaredClassName get() = className

    /**
     * method sign
     * ----------------
     * 方法签名
     */
    val methodSign by lazy {
        getSign()
    }

    private fun getSign() = buildString {
        append("(")
        append(paramTypeNames.joinToString("") { getTypeSign(it) })
        append(")")
        append(getTypeSign(returnTypeName))
    }

    /**
     * Whether the method is a constructor method
     * ----------------
     * 该方法是否为构造方法
     */
    val isConstructor get() = name == "<init>"

    /**
     * Whether the method is a static initializer
     * ----------------
     * 该方法是否为静态初始化方法
     */
    val isStaticInitializer get() = name == "<clinit>"

    /**
     * Whether the method is a normal method
     * ----------------
     * 该方法是否为普通方法
     */
    val isMethod get() = !isStaticInitializer && !isConstructor

    /**
     * Convert method descriptor to [KniMethodSign].
     * ----------------
     * 转换方法描述符为 [KniMethodSign]。
     *
     * @param descriptor method descriptor / 方法描述符
     */
    constructor(descriptor: String) {
        val idx1 = descriptor.indexOf("->")
        val idx2 = descriptor.indexOf("(", idx1 + 1)
        val idx3 = descriptor.indexOf(")", idx2 + 1)
        if (idx1 == -1 || idx2 == -1 || idx3 == -1) {
            error("not method descriptor: $descriptor")
        }
        className = getTypeName(descriptor.substring(0, idx1))
        name = descriptor.substring(idx1 + 2, idx2)
        paramTypeNames = getParamTypeNames(descriptor.substring(idx2 + 1, idx3))
        returnTypeName = getTypeName(descriptor.substring(idx3 + 1))
    }

    /**
     * Convert method to [KniMethodSign].
     * ----------------
     * 转换方法为 [KniMethodSign]。
     *
     * @param method method / 方法
     */
    constructor(method: JniMethod) {
        className = getTypeName(method.declaringClass.name)
        name = method.name
        paramTypeNames = method.parameterTypes.map { getTypeName(it.name) }
        returnTypeName = getTypeName(method.returnType.name)
    }

    /**
     * Convert method to [KniMethodSign].
     * ----------------
     * 转换方法为 [KniMethodSign]。
     *
     * @param method method / 方法
     */
    constructor(method: KFunction<*>) {
        className = getTypeName(method.declaringClassNameWithDollar)
        name = method.name
        paramTypeNames = method.params.map { getTypeName(it?.asClass?.nameWithDollar ?: "") }
        returnTypeName = getTypeName(method.returnType.asClass.nameWithDollar ?: "")
    }

    override fun toString(): String {
        return buildString {
            append(getTypeSign(className))
            append("->")
            append(name)
            append(methodSign)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KniMethodSign) return false
        return className == other.className
                && name == other.name
                && paramTypeNames == other.paramTypeNames
                && returnTypeName == other.returnTypeName
    }

    override fun hashCode(): Int {
        return className.hashCode() * 31 +
                name.hashCode() * 31 +
                paramTypeNames.hashCode() * 31 +
                returnTypeName.hashCode()
    }
}