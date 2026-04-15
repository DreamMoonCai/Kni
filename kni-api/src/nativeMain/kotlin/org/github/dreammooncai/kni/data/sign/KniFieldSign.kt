package org.github.dreammooncai.kni.data.sign

import org.github.dreammooncai.kni.data.base.JniField
import org.github.dreammooncai.kni.util.DexSignUtil.getTypeName
import org.github.dreammooncai.kni.util.DexSignUtil.getTypeSign

class KniFieldSign {

    companion object {
        fun deserialize(descriptor: String) = KniFieldSign(descriptor)
    }

    val className: String
    val name: String
    val typeName: String

    val declaredClassName get() = className

    /**
     * field type sign
     * ----------------
     * 字段类型签名
     */
    val typeSign by lazy {
        getSign()
    }

    private fun getSign(): String {
        return getTypeSign(typeName)
    }

    /**
     * Convert field descriptor to [KniFieldSign].
     * ----------------
     * 转换字段描述符为 [KniFieldSign]。
     *
     * @param descriptor field descriptor / 字段描述符
     */
    constructor(descriptor: String) {
        val idx1 = descriptor.indexOf("->")
        val idx2 = descriptor.indexOf(":", idx1 + 1)
        if (idx1 == -1 || idx2 == -1) {
            error("not field descriptor: $descriptor")
        }
        className = getTypeName(descriptor.substring(0, idx1))
        name = descriptor.substring(idx1 + 2, idx2)
        typeName = getTypeName(descriptor.substring(idx2 + 1))
    }

    /**
     * Convert field to [KniFieldSign].
     * ----------------
     * 转换字段为 [KniFieldSign]。
     *
     * @param field field / 字段
     */
    constructor(field: JniField) {
        className = getTypeName(field.declaringClass)
        name = field.name
        typeName = getTypeName(field.type)
    }

    override fun toString(): String {
        return buildString {
            append(getTypeSign(className))
            append("->")
            append(name)
            append(":")
            append(typeSign)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KniFieldSign) return false
        return className == other.className
                && name == other.name
                && typeName == other.typeName
    }

    override fun hashCode(): Int {
        return className.hashCode() * 31 +
                name.hashCode() * 31 +
                typeName.hashCode()
    }
}