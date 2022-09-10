package jadx.core.dex.attributes.nodes;

import static jadx.core.utils.Utils.isEmpty;

import java.util.Collections;
import java.util.Set;

import jadx.api.plugins.input.data.attributes.IJadxAttribute;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.instructions.args.ArgType;

/**
 * Set of known type variables at current method
 */
public class MethodTypeVarsAttr implements IJadxAttribute {
    private static final MethodTypeVarsAttr EMPTY = new MethodTypeVarsAttr(Collections.emptySet());
    private final Set<ArgType> typeVars;

    private MethodTypeVarsAttr(Set<ArgType> typeVars) {
        this.typeVars = typeVars;
    }

    public static MethodTypeVarsAttr build(Set<ArgType> typeVars) {
        if (isEmpty(typeVars)) {
            return EMPTY;
        }
        return new MethodTypeVarsAttr(typeVars);
    }

    public Set<ArgType> getTypeVars() {
        return typeVars;
    }

    @Override
    public AType<MethodTypeVarsAttr> getAttrType() {
        return AType.METHOD_TYPE_VARS;
    }

    @Override
    public String toString() {
        if (this == EMPTY) {
            return "TYPE_VARS: EMPTY";
        }
        return "TYPE_VARS: " + typeVars;
    }
}
