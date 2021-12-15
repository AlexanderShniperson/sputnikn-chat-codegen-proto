package net.orionlab

import com.google.protobuf.DescriptorProtos.DescriptorProto
import com.google.protobuf.DescriptorProtos.FileDescriptorProto
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.util.function.Consumer

private val optionMsgId = 50001
private val outFilePath = "generator_out.txt"
private var pw: PrintWriter? = null
private val isDebug = false

enum class LanguageCodeGenType {
    Kotlin,
    Dart
}

fun main(args: Array<String>) {
    try {
        val langCodeGen = args.getOrNull(0)?.let { arg ->
            LanguageCodeGenType.values().find { it.name.contains(arg, true) }
        } ?: LanguageCodeGenType.Kotlin
        if (isDebug) pw = PrintWriter(File(outFilePath))
        val codeGeneratorRequest = CodeGeneratorRequest.parseFrom(System.`in`)
        createResponse(codeGeneratorRequest, langCodeGen)?.writeTo(System.out)
        System.out.flush()
    } catch (ex: Throwable) {
        val sw = StringWriter()
        ex.printStackTrace(PrintWriter(sw))
        if (isDebug) pw?.println(sw.toString())
    }
    if (isDebug) {
        pw?.flush()
        pw?.close()
    }
}

private fun createResponse(request: CodeGeneratorRequest, langCodeGen: LanguageCodeGenType): CodeGeneratorResponse? {
    var result: CodeGeneratorResponse? = null
    try {
        result = CodeGeneratorResponse.newBuilder().addAllFile(createFiles(request, langCodeGen)).build()
    } catch (ex: Throwable) {
        val sw = StringWriter()
        ex.printStackTrace(PrintWriter(sw))
        CodeGeneratorResponse.newBuilder().setError(sw.toString()).build()
        if (isDebug) pw?.println(sw.toString())
    }
    return result
}

private fun createFiles(
    request: CodeGeneratorRequest,
    langCodeGen: LanguageCodeGenType
): List<CodeGeneratorResponse.File> {
    val result = mutableListOf<CodeGeneratorResponse.File>()
    getMessagesByPackage(request)
        .forEach { (packageName: String, messages: List<DescriptorProto>) ->
            when (langCodeGen) {
                LanguageCodeGenType.Kotlin -> result.add(genKotlin(packageName, messages))
                LanguageCodeGenType.Dart -> result.add(genDart(packageName, messages))
                else -> Unit
            }
        }
    return result
}

private fun genKotlin(
    packageName: String,
    messages: List<DescriptorProto>
): CodeGeneratorResponse.File {
    val outWriter = StringWriter()
    outWriter.append("package ").append(packageName).append("\n")
        .append("import com.google.protobuf.GeneratedMessageV3\n")
        .append("import net.orionlab.common.SerializerBase\n")
        .append("class Serializer: SerializerBase {\n")
        .append("override fun bytesToMessage(msgId: Int, bytes: ByteArray): GeneratedMessageV3? {\n")
        .append("var result: GeneratedMessageV3? = null\n")
        .append("try {\n")
        .append("when(msgId) {\n")
    messages.forEach(Consumer { m: DescriptorProto ->
        outWriter.append(
            "${getMsgId(m)} -> result = ${messageClass(m, packageName)}.parseFrom(bytes)\n"
        )
    })
    outWriter.append("else -> Unit\n")
    outWriter.append("}\n")
        .append("} catch (ex: Throwable){}\n")
        .append("return result\n}\n\n")
        .append("override fun messageToId(message: GeneratedMessageV3): Int {\n")
        .append("var result = 0\n")
        .append("try {\n")
    outWriter.append("when(message) {\n")
    messages.forEach(Consumer { m: DescriptorProto ->
        outWriter.append(
            "is ${messageClass(m, packageName)} -> result = ${getMsgId(m)}\n"
        )
    })
    outWriter.append("else -> Unit\n}\n")
    outWriter.append("} catch(ex: Throwable){}\n")
        .append("return result\n}\n")
        .append("}")
    return CodeGeneratorResponse.File.newBuilder()
        .setName("${packageName.replace(".", "/")}/Serializer.kt")
        .setContent(outWriter.toString())
        .build()
}

private fun genDart(
    packageName: String,
    messages: List<DescriptorProto>
): CodeGeneratorResponse.File {
    val outWriter = StringWriter()
    outWriter.append("import 'package:protobuf/protobuf.dart';\n")
    outWriter.append("import '../serializer_base.dart';\n")
    outWriter.append("import './chat_message.pb.dart';\n\n")

    outWriter.append("class Serializer extends SerializerBase {\n")
    outWriter.append("  @override\n")
    outWriter.append("  GeneratedMessage? bytesToMessage(int msgId, List<int> bytes) {\n")
    outWriter.append("    GeneratedMessage? result;\n")
    outWriter.append("    switch (msgId) {\n")
    messages.forEach(Consumer { m: DescriptorProto ->
        outWriter.append("      case ${getMsgId(m)}:\n")
        outWriter.append("        result = ${m.name}.fromBuffer(bytes);\n")
        outWriter.append("        break;\n")
    })
    outWriter.append("    }\n")
    outWriter.append("    return result;\n")
    outWriter.append("  }\n\n")

    outWriter.append("  @override\n")
    outWriter.append("  int messageToId(GeneratedMessage message) {\n")
    outWriter.append("    int result = -1;\n")
    messages.forEach(Consumer { m: DescriptorProto ->
        outWriter.append("    if (message is ${m.name}) {\n")
        outWriter.append("      result = ${getMsgId(m)};\n")
        outWriter.append("    }\n")
    })
    outWriter.append("    return result;\n")
    outWriter.append("  }\n")
    outWriter.append("}\n")

    return CodeGeneratorResponse.File.newBuilder()
        .setName("serializer.dart")
        .setContent(outWriter.toString())
        .build()
}

private fun messageClass(d: DescriptorProto, packageName: String): String {
    return packageName + "." + d.name
}

private fun getMsgId(d: DescriptorProto): Long {
    return d.options.unknownFields.getField(optionMsgId).varintList[0]
}

private fun getMessagesByPackage(request: CodeGeneratorRequest): Map<String, List<DescriptorProto>> {
    val result = hashMapOf<String, MutableList<DescriptorProto>>()
    val files = mutableListOf<FileDescriptorProto>()
    try {
        for (item in request.protoFileList) {
            if (request.fileToGenerateList.contains(item.name)) files.add(item)
        }
        if (isDebug) pw?.println("Proto Files")
        for (item in files) {
            if (isDebug) pw?.print(String.format("%s; ", item.name))
            val descriptorProtos =
                item.messageTypeList.filter {
                    it.hasOptions() && it.options.unknownFields.hasField(optionMsgId)
                }
            if (descriptorProtos.isNotEmpty()) {
                val items =
                    result.getOrDefault(item.getPackage(), mutableListOf())
                for (i in items.indices.reversed()) {
                    val subItem = items[i]
                    if (descriptorProtos.contains(subItem)) items.remove(subItem)
                }
                items.addAll(descriptorProtos)
                result[item.getPackage()] = items
            }
        }
        if (isDebug) pw?.println()
    } catch (ex: Throwable) {
        val sw = StringWriter()
        ex.printStackTrace(PrintWriter(sw))
        if (isDebug) pw?.println(sw.toString())
    }
    return result
}