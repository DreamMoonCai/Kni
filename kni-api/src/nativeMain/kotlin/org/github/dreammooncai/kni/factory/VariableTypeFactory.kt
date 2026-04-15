@file:OptIn(ExperimentalForeignApi::class)

package org.github.dreammooncai.kni.factory

import org.github.dreammooncai.kni.KniBridge
import kotlinx.cinterop.ExperimentalForeignApi
import org.github.dreammooncai.kni.data.KniClass

val KniBridge.JavaClass get() = "java.lang.Class".toClass()

val KniBridge.JavaKClass get() = "kotlin.reflect.KClass".toClass()

val KniBridge.JavaStringClass get() = "java.lang.String".toClass()

val KniBridge.JavaCharSequenceClass get() = "java.lang.CharSequence".toClass()

val KniBridge.JavaListClass get() = "java.util.List".toClass()

val KniBridge.BooleanArrayClass get() = "[Z".toClass()

val KniBridge.ByteArrayClass get() = "[B".toClass()

val KniBridge.CharArrayClass get() = "[C".toClass()

val KniBridge.ShortArrayClass get() = "[S".toClass()

val KniBridge.IntArrayClass get() = "[I".toClass()

val KniBridge.LongArrayClass get() = "[J".toClass()

val KniBridge.FloatArrayClass get() = "[F".toClass()

val KniBridge.DoubleArrayClass get() = "[D".toClass()

val KniBridge.ObjectArrayClass get() = "[java.lang.Object".toClass()

val KniBridge.MethodClass get() = "java.lang.reflect.Method".toClass()

val KniBridge.ConstructorClass get() = "java.lang.reflect.Constructor".toClass()

val KniBridge.FieldClass get() = "java.lang.reflect.Field".toClass()

val KniBridge.AnyClass get() = "java.lang.Object".toClass()

val KniBridge.BooleanType get() = "Z".toClass()

val KniBridge.BooleanClass get() = "java.lang.Boolean".toClass()

val KniBridge.ByteType get() = "B".toClass()

val KniBridge.ByteClass get() = "java.lang.Byte".toClass()

val KniBridge.CharType get() = "C".toClass()

val KniBridge.CharClass get() = "java.lang.Character".toClass()

val KniBridge.ShortType get() = "S".toClass()

val KniBridge.ShortClass get() = "java.lang.Short".toClass()

val KniBridge.IntType get() = "I".toClass()

val KniBridge.IntClass get() = "java.lang.Integer".toClass()

val KniBridge.FloatType get() = "F".toClass()

val KniBridge.FloatClass get() = "java.lang.Float".toClass()

val KniBridge.LongType get() = "J".toClass()

val KniBridge.LongClass get() = "java.lang.Long".toClass()

val KniBridge.DoubleType get() = "D".toClass()

val KniBridge.DoubleClass get() = "java.lang.Double".toClass()

val KniBridge.UnitType get() = "V".toClass()

val KniBridge.UnitClass get() = "java.lang.Void".toClass()

val KniBridge.MapClass get() = "java.util.Map".toClass()

val KniBridge.LinkedHashMapClass get() = "java.util.LinkedHashMap".toClass()

val KniBridge.JavaIteratorClass get() = "java.util.Iterator".toClass()

val KniBridge.JavaCollectionClass get() = "java.util.Collection".toClass()

val KniBridge.JavaSetClass get() = "java.util.Set".toClass()

val KniBridge.MapEntryClass get() = $$"java.util.Map$Entry".toClass()

val KniBridge.ListClass get() = $$"java.util.Arrays$ArrayList".toClass()

val KniBridge.ArrayListClass get() = "java.util.ArrayList".toClass()

val KniBridge.JavaIterableClass get() = "java.lang.Iterable".toClass()

val KniBridge.FunctionClass get() = "java.util.function.Function".toClass()

val KniBridge.PredicateClass get() = "java.util.function.Predicate".toClass()

val KniBridge.SupplierClass get() = "java.util.function.Supplier".toClass()

val KniBridge.ConsumerClass get() = "java.util.function.Consumer".toClass()

val KniBridge.BiFunctionClass get() = "java.util.function.BiFunction".toClass()

// Kotlin JVM function bridge classes
val KniBridge.KFunction0Class get() = "kotlin.jvm.functions.Function0".toClass()

val KniBridge.KFunction1Class get() = "kotlin.jvm.functions.Function1".toClass()

val KniBridge.KFunction2Class get() = "kotlin.jvm.functions.Function2".toClass()

val KniBridge.ThrowableClass get() = "java.lang.Throwable".toClass()

val KniBridge.ExceptionClass get() = "java.lang.Exception".toClass()

val KniBridge.RuntimeExceptionClass get() = "java.lang.RuntimeException".toClass()

val KniBridge.IllegalArgumentExceptionClass get() = "java.lang.IllegalArgumentException".toClass()

val KniBridge.EnumClass get() = "java.lang.Enum".toClass()

val KniBridge.NumberClass get() = "java.lang.Number".toClass()

val KniBridge.ComparableClass get() = "java.lang.Comparable".toClass()

val KniBridge.StringBuilderClass get() = "java.lang.StringBuilder".toClass()

val KniBridge.StringBufferClass get() = "java.lang.StringBuffer".toClass()

val KniBridge.ThreadClass get() = "java.lang.Thread".toClass()

val KniBridge.RunnableClass get() = "java.lang.Runnable".toClass()

val KniBridge.PairClass get() = "kotlin.Pair".toClass()

val KniBridge.TripleClass get() = "kotlin.Triple".toClass()

val KniBridge.ClassLoaderClass get() = "java.lang.ClassLoader".toClass()

val KniBridge.SerializationClass get() = "kotlinx.serialization.json.Json".toClass()

val KniBridge.KniExpansionClass get() = "org.github.dreammooncai.kni.KniExpansionKt".toClass()

val KniBridge.KSerializerClass get() = "kotlinx.serialization.KSerializer".toClass()

val KniBridge.SerializationDefaultClass get() = "org.github.dreammooncai.kni.IKniSerializationKt".toClass()

val KniBridge.SerializationStrategyClass get() = "kotlinx.serialization.SerializationStrategy".toClass()

val KniBridge.DeserializationStrategyClass get() = "kotlinx.serialization.DeserializationStrategy".toClass()
