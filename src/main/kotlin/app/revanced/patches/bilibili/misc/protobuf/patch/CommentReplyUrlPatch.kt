package app.revanced.patches.bilibili.misc.protobuf.patch

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch

@Patch(
    name = "Comment word search url",
    description = "屏蔽评论关键词搜索功能",
    compatiblePackages = [CompatiblePackage(name = "tv.danmaku.bili"), CompatiblePackage(name = "tv.danmaku.bilibilihd")]
)
object CommentReplyUrlPatch : BytecodePatch() {
    override fun execute(context: BytecodeContext) {
        context.findClass("Lcom/bapis/bilibili/main/community/reply/v1/Content;")
            ?.mutableClass?.methods?.find { it.name == "internalGetUrls" }?.addInstruction(
                1, """
                invoke-static {v0}, Lapp/revanced/bilibili/patches/CommentReplyUrlPatch;->filterUrls(Lcom/google/protobuf/MapFieldLite;)V
            """.trimIndent()
            ) ?: throw PatchException("can not found internalGetUrls method")
    }
}
