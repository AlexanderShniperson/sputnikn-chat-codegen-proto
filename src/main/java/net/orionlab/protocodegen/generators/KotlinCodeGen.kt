package net.orionlab.protocodegen.generators

import com.google.protobuf.DescriptorProtos.DescriptorProto
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse
import java.io.StringWriter
import java.util.function.Consumer

class KotlinCodeGen : LanguageCodeGenBase {
    override fun generatorName(): String = "kotlin"

    override fun generateContent(
        optionMsgId: Int,
        packageName: String,
        messages: List<DescriptorProto>
    ): CodeGeneratorResponse.File {
        val outWriter = StringWriter()
        outWriter.append("package ").append(packageName).append("\n")
            .append("import com.google.protobuf.GeneratedMessageV3\n")
            .append("import net.orionlab.common.SerializerBase\n")
            .append("class Serializer: SerializerBase {\n")
            .append("override fun bytesToMessage(msgId: Int, bytes: ByteArray): GeneratedMessageV3? {\n")
            .append("var result: GeneratedMessageV3? = null\n").append("try {\n").append("when(msgId) {\n")
        messages.forEach(Consumer { m: DescriptorProto ->
            outWriter.append(
                "${getMsgId(optionMsgId, m)} -> result = ${messageClass(m, packageName)}.parseFrom(bytes)\n"
            )
        })
        outWriter.append("else -> Unit\n")
        outWriter.append("}\n").append("} catch (ex: Throwable){}\n").append("return result\n}\n\n")
            .append("override fun messageToId(message: GeneratedMessageV3): Int {\n").append("var result = 0\n")
            .append("try {\n")
        outWriter.append("when(message) {\n")
        messages.forEach(Consumer { m: DescriptorProto ->
            outWriter.append(
                "is ${messageClass(m, packageName)} -> result = ${getMsgId(optionMsgId, m)}\n"
            )
        })
        outWriter.append("else -> Unit\n}\n")
        outWriter.append("} catch(ex: Throwable){}\n").append("return result\n}\n").append("}")
        return CodeGeneratorResponse.File.newBuilder()
            .setName("${packageName.replace(".", "/")}/Serializer.kt")
            .setContent(outWriter.toString())
            .build()
    }

    private fun messageClass(d: DescriptorProto, packageName: String): String {
        return packageName + "." + d.name
    }

    private fun getMsgId(optionMsgId: Int, d: DescriptorProto): Long {
        return d.options.unknownFields.getField(optionMsgId).varintList[0]
    }
}