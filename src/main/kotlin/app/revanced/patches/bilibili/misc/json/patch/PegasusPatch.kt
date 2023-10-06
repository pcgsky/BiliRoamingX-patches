package app.revanced.patches.bilibili.misc.json.patch

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.bilibili.misc.json.fingerprints.CardClickProcessorFingerprint
import app.revanced.patches.bilibili.misc.json.fingerprints.PegasusParserFingerprint
import app.revanced.patches.bilibili.utils.cloneMutable
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Field
import com.android.tools.smali.dexlib2.iface.value.StringEncodedValue

@Patch(
    name = "Pegasus hook",
    description = "首页推荐流hook",
    compatiblePackages = [CompatiblePackage(name = "tv.danmaku.bili"), CompatiblePackage(name = "tv.danmaku.bilibilihd")]
)
object PegasusPatch : BytecodePatch(setOf(PegasusParserFingerprint, CardClickProcessorFingerprint)) {
    override fun execute(context: BytecodeContext) {
        PegasusParserFingerprint.result?.run {
            val method = mutableClass.methods.first { it.returnType == "Lcom/bilibili/okretro/GeneralResponse;" }
            method.cloneMutable(registerCount = 2, clearImplementation = true).apply {
                method.name += "_Origin"
                addInstructions(
                    """
                    invoke-virtual {p0, p1}, $method
                    move-result-object p1
                    invoke-static {p1}, Lapp/revanced/bilibili/patches/json/PegasusPatch;->pegasusHook(Lcom/bilibili/okretro/GeneralResponse;)V
                    return-object p1
                """.trimIndent()
                )
            }.also {
                mutableClass.methods.add(it)
            }
        } ?: throw PegasusParserFingerprint.exception
        var bannerItemFiled: Field? = null
        var stockBannersItemClass: ClassDef? = null
        for (classDef in context.classes) {
            if (classDef.superclass == "Lcom/bilibili/pegasus/api/model/BasicIndexItem;") {
                val field = classDef.fields.find { f ->
                    f.annotations.find { a ->
                        a.type == "Lcom/alibaba/fastjson/annotation/JSONField;" && a.elements.find { e ->
                            e.name == "name" && e.value.let {
                                it is StringEncodedValue && it.value == "banner_item"
                            }
                        } != null
                    } != null
                }
                if (field != null) {
                    stockBannersItemClass = classDef
                    bannerItemFiled = field
                    break
                }
            }
        }
        if (bannerItemFiled == null || stockBannersItemClass == null)
            throw PatchException("not found banner item field")
        val myBannersItemClassName = "Lapp/revanced/bilibili/meta/pegasus/BannersItem;"
        val myBannersItemClass = context.findClass(myBannersItemClassName)!!
        context.proxy(stockBannersItemClass).mutableClass.setSuperClass(myBannersItemClassName)
        myBannersItemClass.mutableClass.methods.run {
            find { it.name == "getBanners" }?.also { remove(it) }
                ?.cloneMutable(3, clearImplementation = true)
                ?.apply {
                    addInstructions(
                        """
                        move-object v0, p0
                        check-cast v0, $stockBannersItemClass
                        iget-object v1, v0, $bannerItemFiled
                        return-object v1
                    """.trimIndent()
                    )
                }?.also { add(it) }
        }
        CardClickProcessorFingerprint.result?.mutableMethod?.addInstructionsWithLabels(
            0, """
            invoke-static {p3}, Lapp/revanced/bilibili/patches/json/PegasusPatch;->onFeedClick(Lcom/bilibili/app/comm/list/common/data/DislikeReason;)Z
            move-result v0
            if-eqz v0, :jump
            return-void
            :jump
            nop
        """.trimIndent()
        ) ?: throw CardClickProcessorFingerprint.exception
    }
}
