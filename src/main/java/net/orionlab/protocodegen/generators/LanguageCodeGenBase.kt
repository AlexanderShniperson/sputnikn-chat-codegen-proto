package net.orionlab.protocodegen.generators

import com.google.protobuf.DescriptorProtos
import com.google.protobuf.compiler.PluginProtos

interface LanguageCodeGenBase {
    fun generatorName(): String

    fun generateContent(
        optionMsgId: Int,
        packageName: String,
        messages: List<DescriptorProtos.DescriptorProto>
    ): PluginProtos.CodeGeneratorResponse.File
}
