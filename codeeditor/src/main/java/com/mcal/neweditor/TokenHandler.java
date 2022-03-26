package com.mcal.neweditor;

import com.mcal.neweditor.TokenMarker.LineContext;

public interface TokenHandler {
    void handleToken(Segment segment, byte b, int i, int i2, LineContext lineContext);

    void setLineContext(LineContext lineContext);
}