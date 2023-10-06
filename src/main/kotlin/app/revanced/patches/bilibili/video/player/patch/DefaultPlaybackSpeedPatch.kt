package app.revanced.patches.bilibili.video.player.patch

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.bilibili.video.player.fingerprints.PlayerOnPreparedFingerprint
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Patch(
    name = "Default playback speed",
    description = "自定义播放器默认播放速度",
    compatiblePackages = [CompatiblePackage(name = "tv.danmaku.bili"), CompatiblePackage(name = "tv.danmaku.bilibilihd")]
)
object DefaultPlaybackSpeedPatch : BytecodePatch(setOf(PlayerOnPreparedFingerprint)) {
    override fun execute(context: BytecodeContext) {
        PlayerOnPreparedFingerprint.result?.mutableMethod?.run {
            val (index, register) = implementation!!.instructions.withIndex().firstNotNullOfOrNull { (index, inst) ->
                if (inst.opcode == Opcode.INVOKE_VIRTUAL && ((inst as BuilderInstruction35c).reference as MethodReference)
                        .let { it.parameterTypes == listOf("F") && it.returnType == "V" }
                ) index to inst.registerD else null
            } ?: throw PatchException("not found updateSpeed method")
            addInstructions(
                index, """
                invoke-static {p1, v$register}, Lapp/revanced/bilibili/patches/PlaybackSpeedPatch;->defaultSpeed(Ltv/danmaku/ijk/media/player/IMediaPlayer;F)F
                move-result v$register
            """.trimIndent()
            )
        } ?: throw PlayerOnPreparedFingerprint.exception
    }
}
