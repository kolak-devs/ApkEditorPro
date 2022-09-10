package jadx.api.plugins.input.data;

import org.jetbrains.annotations.Nullable;

import java.util.List;

import jadx.api.plugins.input.data.attributes.IJadxAttribute;

public interface IMethodData {

    IMethodRef getMethodRef();

    int getAccessFlags();

    @Nullable
    ICodeReader getCodeReader();

    String disassembleMethod();

    List<IJadxAttribute> getAttributes();
}
