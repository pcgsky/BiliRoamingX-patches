package app.revanced.patches.bilibili.misc.darkswitch.patch

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.bilibili.misc.darkswitch.fingerprints.SwitchDarkModeFingerprint
import app.revanced.patches.bilibili.utils.cloneMutable
import app.revanced.patches.bilibili.utils.toPublic
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Patch(
    name = "Dark switch",
    description = "我的页面深色模式切换弹框确认",
    compatiblePackages = [CompatiblePackage(name = "tv.danmaku.bili"), CompatiblePackage(name = "tv.danmaku.bilibilihd")]
)
object DarkSwitchPatch : BytecodePatch(setOf(SwitchDarkModeFingerprint)) {
    override fun execute(context: BytecodeContext) {
        fun Method.findIsNightFollowSystemMethod(): MethodReference? {
            return implementation!!.instructions.firstNotNullOfOrNull { inst ->
                if (inst.opcode == Opcode.INVOKE_STATIC && inst is Instruction35c) {
                    (inst.reference as MethodReference).takeIf {
                        it.returnType == "Z" && it.parameterTypes == listOf("Landroid/content/Context;")
                    }
                } else null
            }
        }

        val utilsClass = context.findClass("Lapp/revanced/bilibili/utils/Utils;")!!.mutableClass
        val utilsIsNightFollowSystemMethod = utilsClass.methods.first { it.name == "isNightFollowSystem" }
        val utilsGetContextMethod = utilsClass.methods.first { it.name == "getContext" }
        val isNightFollowSystemMethod = SwitchDarkModeFingerprint.result?.method?.findIsNightFollowSystemMethod()
            ?: throw PatchException("not found isNightFollowSystem method")
        SwitchDarkModeFingerprint.result?.run {
            mutableMethod.cloneMutable(registerCount = 2, clearImplementation = true).apply {
                addInstructions(
                    """
                    invoke-static {p0, p1}, Lapp/revanced/bilibili/patches/DarkSwitchPatch;->switchDarkMode(${mutableClass.type}Z)V
                    return-void
                """.trimIndent()
                )
            }.also {
                mutableMethod.name = "switchDarkMode_Origin"
                mutableMethod.accessFlags = mutableMethod.accessFlags.toPublic()
                mutableClass.methods.add(it)
            }
        } ?: throw SwitchDarkModeFingerprint.exception
        utilsIsNightFollowSystemMethod.also { utilsClass.methods.remove(it) }.cloneMutable(
            registerCount = 1, clearImplementation = true
        ).apply {
            addInstructions(
                """
                invoke-static {}, $utilsGetContextMethod
                move-result-object v0
                invoke-static {v0}, $isNightFollowSystemMethod
                move-result v0
                return v0
            """.trimIndent()
            )
        }.also { utilsClass.methods.add(it) }
    }
}
