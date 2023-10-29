package net.orionlab.protocodegen

import com.google.protobuf.DescriptorProtos.DescriptorProto
import com.google.protobuf.DescriptorProtos.FileDescriptorProto
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse
import net.orionlab.protocodegen.generators.DartCodeGen
import net.orionlab.protocodegen.generators.KotlinCodeGen
import net.orionlab.protocodegen.generators.LanguageCodeGenBase
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

private val optionMsgId = 50001
private val outFilePath = "generator_out.txt"
private var pw: PrintWriter? = null
private val isDebug = false

fun main(args: Array<String>) {
    try {
        val generators = listOf(KotlinCodeGen(), DartCodeGen())
        val codeGen = args.getOrNull(0)?.let { arg ->
            generators.find { it.generatorName().contains(arg, true) }
        } ?: throw Exception("Cant find generator for '${args.getOrNull(0)}'")
        if (isDebug) pw = PrintWriter(File(outFilePath))
        val codeGeneratorRequest = CodeGeneratorRequest.parseFrom(System.`in`)
        if (isDebug) pw?.println("Use generator for '$codeGen'")
        createResponse(codeGeneratorRequest, codeGen)?.writeTo(System.out)
        System.out.flush()
    } catch (ex: Throwable) {
        val sw = StringWriter()
        ex.printStackTrace(PrintWriter(sw))
        if (isDebug) pw?.println(sw.toString())
    } finally {
        if (isDebug) {
            pw?.flush()
            pw?.close()
        }
    }
}

private fun createResponse(request: CodeGeneratorRequest, codeGen: LanguageCodeGenBase): CodeGeneratorResponse? {
    var result: CodeGeneratorResponse? = null
    try {
        result = CodeGeneratorResponse.newBuilder().addAllFile(createFiles(request, codeGen)).build()
    } catch (ex: Throwable) {
        val sw = StringWriter()
        ex.printStackTrace(PrintWriter(sw))
        CodeGeneratorResponse.newBuilder().setError(sw.toString()).build()
        if (isDebug) pw?.println(sw.toString())
    }
    return result
}

private fun createFiles(
    request: CodeGeneratorRequest, codeGen: LanguageCodeGenBase
): List<CodeGeneratorResponse.File> {
    val result = mutableListOf<CodeGeneratorResponse.File>()
    val messages = getMessagesByPackage(request)
    messages.forEach { (packageName: String, messages: List<DescriptorProto>) ->
        result.add(codeGen.generateContent(optionMsgId, packageName, messages))
    }
    return result
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
            val descriptorProtos = item.messageTypeList.filter {
                it.hasOptions() && it.options.unknownFields.hasField(optionMsgId)
            }
            if (descriptorProtos.isNotEmpty()) {
                val items = result.getOrDefault(item.getPackage(), mutableListOf())
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