package app.revanced.patches.bilibili.misc.other.patch

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.bilibili.misc.other.fingerprints.BLRouteBuilderFingerprint
import app.revanced.patches.bilibili.misc.other.fingerprints.RouteRequestFingerprint

@Patch(
    name = "BL route intercept",
    description = "哔哩哔哩页面路由修改",
    compatiblePackages = [CompatiblePackage(name = "tv.danmaku.bili"), CompatiblePackage(name = "tv.danmaku.bilibilihd")]
)
object BLRoutePatch : BytecodePatch(setOf(BLRouteBuilderFingerprint, RouteRequestFingerprint)) {
    override fun execute(context: BytecodeContext) {
        BLRouteBuilderFingerprint.result?.mutableClass?.methods?.find { m ->
            m.name == "<init>" && m.parameterTypes.let { it.size == 1 && it[0] == "Landroid/net/Uri;" }
        }?.addInstructions(
            0, """
            invoke-static {p1}, Lapp/revanced/bilibili/patches/BLRoutePatch;->intercept(Landroid/net/Uri;)Landroid/net/Uri;
            move-result-object p1
        """.trimIndent()
        ) ?: throw BLRouteBuilderFingerprint.exception
        RouteRequestFingerprint.result?.mutableClass?.methods?.find { m ->
            m.name == "<init>" && m.parameterTypes.let { it.size == 2 && it[0] == "Landroid/net/Uri;" }
        }?.addInstructions(
            0, """
            invoke-static {p1}, Lapp/revanced/bilibili/patches/BLRoutePatch;->intercept(Landroid/net/Uri;)Landroid/net/Uri;
            move-result-object p1
        """.trimIndent()
        ) ?: throw RouteRequestFingerprint.exception
    }
}
