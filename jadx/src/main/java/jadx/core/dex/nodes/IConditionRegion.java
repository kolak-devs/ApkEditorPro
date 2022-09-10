package jadx.core.dex.nodes;

import org.jetbrains.annotations.Nullable;

import java.util.List;

import jadx.core.dex.regions.conditions.IfCondition;

public interface IConditionRegion extends IRegion {

    @Nullable
    IfCondition getCondition();

    /**
     * Blocks merged into condition
     * Needed for backtracking
     * TODO: merge into condition object ???
     */
    List<BlockNode> getConditionBlocks();

    void invertCondition();

    boolean simplifyCondition();

    int getConditionSourceLine();
}
