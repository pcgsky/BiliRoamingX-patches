package app.revanced.patches.bilibili.misc.theme.patch

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.bilibili.misc.theme.fingerprints.*
import app.revanced.patches.bilibili.utils.cloneMutable
import app.revanced.patches.bilibili.utils.removeFinal
import app.revanced.patches.bilibili.utils.toPublic
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Patch(
    name = "custom theme color",
    description = "自定义主题色",
    compatiblePackages = [CompatiblePackage(name = "tv.danmaku.bili"), CompatiblePackage(name = "tv.danmaku.bilibilihd")]
)
object CustomThemePatch : BytecodePatch(
    setOf(
        BuiltInThemesFingerprint,
        SkinListFingerprint,
        ThemeClickFingerprint,
        ThemeColorsFingerprint,
        ThemeHelperFingerprint,
        ThemeNameFingerprint,
        ThemeProcessorFingerprint,
        WebActivityBuildUriFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {
        val patchClass = context.findClass("Lapp/revanced/bilibili/patches/CustomThemePatch;")!!.mutableClass

        ThemeNameFingerprint.result?.mutableClass?.fields?.first {
            it.type == "Ljava/util/Map;"
        }?.let { field ->
            field.accessFlags = field.accessFlags.toPublic().removeFinal()
            patchClass.methods.run {
                first { it.name == "getThemeNames" }.also { remove(it) }.cloneMutable(
                    registerCount = 1, clearImplementation = true
                ).apply {
                    addInstructions(
                        """
                        sget-object v0, $field
                        return-object v0
                    """.trimIndent()
                    )
                }.also { add(it) }
            }
        } ?: throw ThemeNameFingerprint.exception

        ThemeHelperFingerprint.result?.let { r ->
            r.mutableClass.fields.first { it.name == r.method.name }
        }?.let { field ->
            field.accessFlags = field.accessFlags.toPublic().removeFinal()
            patchClass.methods.run {
                first { it.name == "getColorArray" }.also { remove(it) }.cloneMutable(
                    registerCount = 1, clearImplementation = true
                ).apply {
                    addInstructions(
                        """
                        sget-object v0, $field
                        return-object v0
                    """.trimIndent()
                    )
                }.also { add(it) }
            }
        } ?: throw ThemeHelperFingerprint.exception

        BuiltInThemesFingerprint.result?.mutableClass?.fields?.first {
            it.type == "Ljava/util/Map;"
        }?.let { field ->
            field.accessFlags = field.accessFlags.toPublic().removeFinal()
            patchClass.methods.run {
                first { it.name == "getAllThemes" }.also { remove(it) }.cloneMutable(
                    registerCount = 1, clearImplementation = true
                ).apply {
                    addInstructions(
                        """
                        sget-object v0, $field
                        return-object v0
                    """.trimIndent()
                    )
                }.also { add(it) }
            }
        } ?: throw BuiltInThemesFingerprint.exception

        ThemeColorsFingerprint.result?.mutableClass?.methods?.first {
            it.name == "<init>" && AccessFlags.PRIVATE.isSet(it.accessFlags)
        }?.let { method ->
            method.accessFlags = method.accessFlags.toPublic()
            patchClass.methods.run {
                first { it.name == "newTheme" }.also { remove(it) }.cloneMutable(
                    registerCount = 36, clearImplementation = true
                ).apply {
                    addInstructions(
                        """
                        new-instance v0, ${method.definingClass}
                        move-object v1, v0
                        move-wide/from16 v4, p2
                        move-wide/from16 v6, p4
                        move-wide/from16 v8, p6
                        move-wide/from16 v10, p8
                        move-wide/from16 v12, p10
                        move-wide/from16 v14, p12
                        move-wide/from16 v16, p14
                        move/from16 v18, p16
                        move-object/from16 v2, p0
                        move-object/from16 v3, p1
                        invoke-direct/range {v1 .. v18}, $method
                        return-object v0
                    """.trimIndent()
                    )
                }.also { add(it) }
            }
        } ?: throw ThemeColorsFingerprint.exception

        val onSetSkinListMethod = patchClass.methods.first { it.name == "onSetSkinList" }
        SkinListFingerprint.result?.mutableMethod?.addInstructions(
            0, """
            invoke-static {p1}, $onSetSkinListMethod
        """.trimIndent()
        ) ?: throw SkinListFingerprint.exception

        val onClickOriginListenerType = "Lapp/revanced/bilibili/widget/OnClickOriginListener;"
        val onThemeClickMethod = patchClass.methods.first { it.name == "onThemeClick" }
        ThemeClickFingerprint.result?.run {
            mutableClass.interfaces.add(onClickOriginListenerType)
            mutableMethod.also {
                mutableClass.methods.add(it.cloneMutable(name = "onClick_Origin"))
            }.addInstructionsWithLabels(
                0, """
                invoke-static {p0, p1}, $onThemeClickMethod
                move-result v0
                if-eqz v0, :jump
                return-void
                :jump
                nop
            """.trimIndent()
            )
        } ?: throw ThemeClickFingerprint.exception

        val onThemeResetMethod = patchClass.methods.first { it.name == "onThemeReset" }
        ThemeProcessorFingerprint.result?.mutableClass?.methods?.filter { m ->
            m.parameterTypes.isEmpty() && m.accessFlags == 0 && m.returnType == "V"
        }?.forEach { m ->
            m.addInstructionsWithLabels(
                0, """
                invoke-static {}, $onThemeResetMethod
                move-result v0
                if-eqz v0, :jump
                return-void
                :jump
                nop
            """.trimIndent()
            )
        } ?: throw ThemeProcessorFingerprint.exception

        WebActivityBuildUriFingerprint.result?.method?.implementation?.instructions?.firstNotNullOfOrNull { inst ->
            if (inst.opcode == Opcode.INVOKE_STATIC && inst is Instruction35c) {
                (inst.reference as MethodReference).let {
                    if (it.parameterTypes.isEmpty() && it.returnType == "I") it.definingClass else null
                }
            } else null
        }?.let { context.findClass(it)!! }?.mutableClass?.fields?.find {
            it.type == "Landroid/util/SparseArray;"
        }?.let { field ->
            field.accessFlags = field.accessFlags.toPublic().removeFinal()
            patchClass.methods.run {
                first { it.name == "getColorIds" }.also { remove(it) }.cloneMutable(
                    registerCount = 1, clearImplementation = true
                ).apply {
                    addInstructions(
                        """
                        sget-object v0, $field
                        return-object v0
                    """.trimIndent()
                    )
                }.also { add(it) }
            }
        } ?: throw WebActivityBuildUriFingerprint.exception
    }
}
