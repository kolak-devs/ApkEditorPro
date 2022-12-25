# apk_editor_pro

# Update JaDX
### REPLACE:
    jadx.core.utils.android.Res9patchStreamDecoder


# Update ApkTool
### REPLACE:
    brut.androlib.res.decoder.Res9patchStreamDecoder

### UPDATE:
    brut.androlib.res.xml.ResXmlEncoders.isPrintableChar()
    brut.androlib.res.data.value.ResValueFactory.factory():
        0x08: TypedValue.TYPE_DYNAMIC_ATTRIBUTE:
        0x07: TypedValue.TYPE_DYNAMIC_REFERENCE:
    brut.androlib.res.decoder.AXmlResourceParser.getAttributeValueType() - remove @override
    brut.androlib.res.decoder.AXmlResourceParser.getAttributeValueData() - remove @override
    brut.androlib.res.decoder.StringBlock.getUtf8() - private to public
    brut.androlib.res.decoder.StringBlock.getUtf16() - private to public
    brut.util.AaptManager.getAapt()
    brut.androlib.Androlib.writeMetaFile() - yaml to json
    brut.androlib.Androlib.readMetaFile() - yaml to json
    brut.androlib.res.AndrolibResources.aapt2Package
    brut.androlib.res.AndrolibResources.aapt1Package
    
    Вывод нормальных ошибок при компиляции дексов
    brut.androlib.mod.SmaliMod

### ADD:
    brut.androlib.res.data.ResResSpec.getAllResources()
    brut.androlib.res.data.value.ResScalarValue.getRawValue()
    brut.androlib.res.data.value.getPath()