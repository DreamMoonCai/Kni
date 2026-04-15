package org.github.dreammooncai.kni.util

import com.martmists.multiplatform.reflect.asClass
import com.martmists.multiplatform.reflect.declaringClassName
import com.martmists.multiplatform.reflect.declaringClassNameWithDollar
import com.martmists.multiplatform.reflect.getter
import com.martmists.multiplatform.reflect.nameWithDollar
import com.martmists.multiplatform.reflect.params
import org.github.dreammooncai.kni.data.KniAny
import org.github.dreammooncai.kni.data.KniField
import org.github.dreammooncai.kni.data.KniMethod
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty

object DexSignUtil {
    private val primitiveMap: Map<String, String> = mutableMapOf(
        "boolean" to "Z",
        "byte" to "B",
        "char" to "C",
        "short" to "S",
        "int" to "I",
        "float" to "F",
        "long" to "J",
        "double" to "D",
        "void" to "V"
    )

    private val primitiveTypeNameMap: Map<String, String> = mutableMapOf(
        "Z" to "boolean",
        "B" to "byte",
        "C" to "char",
        "S" to "short",
        "I" to "int",
        "F" to "float",
        "J" to "long",
        "D" to "double",
        "V" to "void"
    )

    private fun primitiveTypeName(typeSign: String): String {
        return primitiveTypeNameMap[typeSign]
            ?: throw IllegalArgumentException("Unknown primitive typeSign: $typeSign")
    }

    /**
     * Convert descriptor to type name.
     * ----------------
     * 转换描述符为类名。
     *
     *     getTypeName("Ljava/lang/String;") -> "java.lang.String"
     *     getTypeName("[Ljava/lang/String;") -> "java.lang.String[]"
     *     getTypeName("[[Ljava/lang/String;") -> "java.lang.String[][]"
     *     getTypeName("[I") -> "int[]"
     *
     * @param typeSign type sign / 类型签名
     * @return simple name / 类名
     */
    fun getTypeName(typeSign: String): String {
        if (typeSign[0] == '[') {
            return getTypeName(typeSign.substring(1)) + "[]"
        }
        if (typeSign.length == 1) {
            return primitiveTypeName(typeSign)
        }
        if (typeSign[0] != 'L' || typeSign[typeSign.length - 1] != ';') {
            return typeSign.replace('/', '.')
        }
        return typeSign.substring(1, typeSign.length - 1).replace('/', '.')
    }

    /**
     * Convert class to type name.
     * ----------------
     * 转换类为类名。
     *
     *     getTypeName(String.class) -> "java.lang.String"
     *     getTypeName(int.class) -> "int"
     *     getTypeName(int[].class) -> "int[]"
     *     getTypeName(int[][].class) -> "int[][]"
     *
     * @param clazz class / 类
     * @return simple name / 类名
     */
    fun getTypeName(clazz: KniAny): String {
        if (clazz.asKniClass.isArray) {
            return getTypeName(clazz.asKniClass.componentType) + "[]"
        }
        val name = clazz.asKniClass.name
        if (clazz.asKniClass.isPrimitive) {
            primitiveMap.forEach { (k,v) ->
                if (name == k || name == v)
                    return k
            }
        }
        return name
    }

    /**
     * Get parameter type names from parameter sign.
     * ----------------
     * 从参数签名获取参数类型名。
     *
     *     getParamTypeNames("Ljava/lang/String;I[I") -> ["java.lang.String", "int", "int[]"]
     *
     * @param paramSigns parameter sign / 参数签名
     * @return parameter type names / 参数类型名
     */
    fun getParamTypeNames(paramSigns: String): List<String> {
        val params = mutableListOf<String>()
        var left = 0
        var right = 0
        while (right < paramSigns.length) {
            val c = paramSigns[right]
            if (c == '[') {
                right++
                continue
            } else if (c == 'L') {
                val end = paramSigns.indexOf(';', right)
                right = end
            }
            val sign = paramSigns.substring(left, right + 1)
            params.add(getTypeName(sign))
            left = ++right
        }
        if (left != right) {
            throw IllegalStateException("Unknown signString: $paramSigns")
        }
        return params
    }

    /**
     * Convert type to type sign.
     * ----------------
     * 转换类型为类型签名。
     *
     *     getTypeSign(boolean.class) -> "Z"
     *     getTypeSign(int.class) -> "I"
     *     getTypeSign(String.class) -> "Ljava/lang/String;"
     *     getTypeSign(String[].class) -> "[Ljava/lang/String;"
     *
     * @param type type / 类型
     * @return type sign / 类型签名
     */
    fun getTypeSign(type: KniAny): String {
        if (type.asKniClass.isPrimitive) {
            val name = type.asKniClass.name
            primitiveMap.forEach { (k,v) ->
                if (name == k || name == v)
                    return v
            }
        }
        return if (type.asKniClass.isArray) "[" + getTypeSign(type.asKniClass.componentType)
        else "L" + type.asKniClass.name.replace('.', '/') + ";"
    }

    /**
     * Convert type name to type sign.
     * ----------------
     * 转换类型名为类型签名。
     *
     *     getTypeSign("int") -> "I"
     *     getTypeSign("java.lang.String") -> "Ljava/lang/String;"
     *     getTypeSign("java.lang.String[]") -> "[Ljava/lang/String;"
     *
     * @param typeName type name / 类型名
     * @return type sign / 类型签名
     */
    fun getTypeSign(typeName: String): String {
        if (typeName.endsWith("[]")) {
            return "[" + getTypeSign(typeName.substring(0, typeName.length - 2))
        }
        if (typeName.startsWith("[") || typeName.startsWith("L")) {
            return typeName.replace('.', '/')
        }
        return primitiveMap[typeName] ?: ("L" + typeName.replace('.', '/') + ";")
    }

    // java/lang/Long
    fun toFindClass(name: String) = when {
        // Already a JVM array descriptor like "[I"
        name.startsWith("[") -> name

        // Ljava/lang/String; → java/lang/String
        name.startsWith("L") && name.endsWith(";") ->
            name.substring(1, name.length - 1)

        // kotlin/Java syntax like java.lang.String[] → [Ljava/lang/String;
        name.endsWith("[]") -> {
            when (val element = name.removeSuffix("[]")) {
                "int" -> "[I"
                "long" -> "[J"
                "float" -> "[F"
                "double" -> "[D"
                "byte" -> "[B"
                "char" -> "[C"
                "short" -> "[S"
                "boolean" -> "[Z"
                else -> "[L${element.replace('.', '/')};"
            }
        }

        name.contains('.') -> name.replace('.', '/')

        else -> name
    }

    /**
     * Get method sign.
     * ----------------
     * 获取方法签名。
     *
     *     getMethodSign(String.class.getMethod("length")) -> "()I"
     *
     * @param method method / 方法
     * @return method sign / 方法签名
     */
    fun getMethodSign(method: KniMethod): String {
        return buildString {
            append("(")
            append(method.parameterTypes.joinToString("") { getTypeSign(it) })
            append(")")
            append(getTypeSign(method.returnType))
        }
    }

    /**
     * Get method sign.
     * ----------------
     * 获取方法签名。
     *
     *     getMethodSign(String.class.getMethod("length")) -> "()I"
     *
     * @param method method / 方法
     * @return method sign / 方法签名
     */
    fun getMethodSign(method: KFunction<*>): String {
        return buildString {
            append("(")
            append(method.params.joinToString("") { getTypeSign(it?.asClass?.nameWithDollar ?: "") })
            append(")")
            append(getTypeSign(method.returnType.asClass.nameWithDollar ?: ""))
        }
    }

    /**
     * Convert class to class descriptor.
     * ----------------
     * 转换类为类描述符。
     *
     *     getDescriptor(String.class) -> "Ljava/lang/String;"
     *
     * @param clazz class / 类
     * @return class descriptor / 类描述符
     */
    fun getDescriptor(clazz: KniAny): String {
        return getTypeSign(clazz)
    }

    /**
     * Convert class to class descriptor.
     * ----------------
     * 转换方法为方法描述符。
     *
     *     getClassDescriptor(String.class) -> "Ljava/lang/String;"
     *
     * @param clazz class / 类
     * @return class descriptor / 类描述符
     */
    fun getClassDescriptor(clazz: KniAny): String {
        return getDescriptor(clazz)
    }

    /**
     * Convert method to method descriptor.
     * ----------------
     * 转换方法为方法描述符。
     *
     *     getDescriptor(String.class.getMethod("length")) -> "Ljava/lang/String;->length()I"
     *
     * @param method method / 方法
     * @return method descriptor / 方法描述符
     */
    fun getDescriptor(method: KniMethod): String {
        return buildString {
            append(getTypeSign(method.declaringClass))
            append("->")
            append(method.name)
            append(getMethodSign(method))
        }
    }

    /**
     * Convert method to method descriptor.
     * ----------------
     * 转换方法为方法描述符。
     *
     *     getDescriptor(String.class.getMethod("length")) -> "Ljava/lang/String;->length()I"
     *
     * @param method method / 方法
     * @return method descriptor / 方法描述符
     */
    fun getDescriptor(method: KFunction<*>): String {
        return buildString {
            append(getTypeSign(method.declaringClassNameWithDollar))
            append("->")
            append(method.name)
            append(getMethodSign(method))
        }
    }

    /**
     * Convert method to method descriptor.
     * ----------------
     * 转换构造方法为方法描述符。
     *
     *     getMethodDescriptor(String.class.getMethod("length")) -> "Ljava/lang/String;->length()I"
     *
     * @param method method / 方法
     * @return method descriptor / 方法描述符
     */
    fun getMethodDescriptor(method: KniMethod): String {
        return getDescriptor(method)
    }

    /**
     * Convert method to method descriptor.
     * ----------------
     * 转换构造方法为方法描述符。
     *
     *     getMethodDescriptor(String.class.getMethod("length")) -> "Ljava/lang/String;->length()I"
     *
     * @param method method / 方法
     * @return method descriptor / 方法描述符
     */
    fun getMethodDescriptor(method: KFunction<*>): String {
        return getDescriptor(method)
    }

    /**
     * Convert field to field descriptor.
     * ----------------
     * 转换字段为字段描述符。
     *
     *     getDescriptor(String.class.getField("CASE_INSENSITIVE_ORDER")) -> "Ljava/lang/String;->CASE_INSENSITIVE_ORDER:Ljava/util/Comparator;"
     *
     * @param field field / 字段
     * @return field descriptor / 字段描述符
     */
    fun getDescriptor(field: KniField): String {
        return buildString {
            append(getTypeSign(field.declaringClass))
            append("->")
            append(field.name)
            append(":")
            append(getTypeSign(field.type))
        }
    }

    /**
     * Convert field to field descriptor.
     * ----------------
     * 转换字段为字段描述符。
     *
     *     getDescriptor(String.class.getField("CASE_INSENSITIVE_ORDER")) -> "Ljava/lang/String;->CASE_INSENSITIVE_ORDER:Ljava/util/Comparator;"
     *
     * @param field field / 字段
     * @return field descriptor / 字段描述符
     */
    fun getDescriptor(field: KProperty<*>): String {
        return buildString {
            append(getTypeSign(field.getter.declaringClassNameWithDollar))
            append("->")
            append(field.name)
            append(":")
            append(getTypeSign(field.returnType.asClass.nameWithDollar ?: ""))
        }
    }

    /**
     * Convert field to field descriptor.
     * ----------------
     * 转换字段为字段描述符。
     *
     *     getFieldDescriptor(String.class.getField("CASE_INSENSITIVE_ORDER")) -> "Ljava/lang/String;->CASE_INSENSITIVE_ORDER:Ljava/util/Comparator;"
     *
     * @param field field / 字段
     * @return field descriptor / 字段描述符
     */
    fun getFieldDescriptor(field: KniField): String {
        return getDescriptor(field)
    }

    /**
     * Convert field to field descriptor.
     * ----------------
     * 转换字段为字段描述符。
     *
     *     getFieldDescriptor(String.class.getField("CASE_INSENSITIVE_ORDER")) -> "Ljava/lang/String;->CASE_INSENSITIVE_ORDER:Ljava/util/Comparator;"
     *
     * @param field field / 字段
     * @return field descriptor / 字段描述符
     */
    fun getFieldDescriptor(field: KProperty<*>): String {
        return getDescriptor(field)
    }
}