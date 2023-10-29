package net.orionlab.protocodegen.generators

import com.google.protobuf.DescriptorProtos.DescriptorProto
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse
import java.io.StringWriter
import java.util.function.Consumer

class DartCodeGen : LanguageCodeGenBase {
    override fun generatorName(): String = "dart"

    override fun generateContent(
        optionMsgId: Int,
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
            outWriter.append("      case ${getMsgId(optionMsgId, m)}:\n")
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
            outWriter.append("      result = ${getMsgId(optionMsgId, m)};\n")
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

    private fun getMsgId(optionMsgId: Int, d: DescriptorProto): Long {
        return d.options.unknownFields.getField(optionMsgId).varintList[0]
    }
}