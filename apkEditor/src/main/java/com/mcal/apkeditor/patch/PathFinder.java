package com.mcal.apkeditor.patch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mcal.apkeditor.R;
import com.mcal.apkeditor.patch.filter.PathFilterComponent;
import com.mcal.apkeditor.patch.filter.PathFilterExactEntry;
import com.mcal.apkeditor.patch.filter.PathFilterWildcard;
import com.mcal.apkeditor.patch.interfaces.IPatchContext;

import java.util.ArrayList;
import java.util.List;

public class PathFinder {

    private List<PathFilter> filters = new ArrayList<>();

    // pathStr should look like: [APPLICATION] [ACTIVITIES]
    public PathFinder(IPatchContext ctx, String pathStr, int line) {
        // Pre-process the target path
        String expanded = PatchRule.assignValues(ctx, pathStr);
        if (expanded != null) {
            pathStr = expanded;
        }

        if (pathStr.startsWith("[") && pathStr.endsWith("]")) {
            // Get every filter
            List<String> words = splitWords(pathStr);
            for (String word : words) {
                PathFilter filter = createFilter(ctx, word, line);
                if (filter != null) {
                    filters.add(filter);
                } else { // error
                    filters = null;
                    break;
                }
            }
        } else if (pathStr.contains("*")) {
            filters.add(new PathFilterWildcard(ctx, pathStr));
        } else {
            filters.add(new PathFilterExactEntry(ctx, pathStr));
        }
    }

    @Nullable
    private PathFilter createFilter(IPatchContext ctx, String word,
                                    int lineIdx) {
        if ("APPLICATION".equals(word)) {
            return new PathFilterComponent(ctx,
                    PathFilterComponent.ComponentType.APPLICATION);
        } else if ("ACTIVITIES".equals(word)) {
            return new PathFilterComponent(ctx,
                    PathFilterComponent.ComponentType.ACTIVITY);
        } else if ("LAUNCHER_ACTIVITIES".equals(word)) {
            return new PathFilterComponent(ctx,
                    PathFilterComponent.ComponentType.LAUNCHER_ACTIVITY);
        }else if ("SIGNATURE".equals(word)) {
            return new PathFilterComponent(ctx,
                    PathFilterComponent.ComponentType.SIGNATURE);
        } else {
            ctx.error(R.string.patch_error_invalid_target, lineIdx);
            return null;
        }
    }

    @NonNull
    private List<String> splitWords(@NonNull String pathStr) {
        List<String> result = new ArrayList<>();

        int startPos = 1;
        int endPos = pathStr.indexOf(']');
        while (startPos > 0 && endPos > startPos) {
            String word = pathStr.substring(startPos, endPos);
            result.add(word);
            startPos = pathStr.indexOf('[', endPos) + 1;
            if (startPos > 0) {
                endPos = pathStr.indexOf(']', startPos);
            }
        }

        return result;
    }

    public boolean isSmaliNeeded() {
        if (filters != null) {
            for (int i = 0; i < filters.size(); ++i) {
                if (filters.get(i).isSmaliNeeded()) {
                    return true;
                }
            }
        }
        return false;
    }

    public String getNextPath() {
        if (filters == null) {
            return null;
        }

        String nextEntry = filters.get(0).getNextEntry();

        // Multiple filters
        if (filters.size() > 1) {

            while (nextEntry != null) {

                // Check other filter matches or not
                boolean matches = true;
                for (int i = 1; i < filters.size(); ++i) {
                    if (!filters.get(i).isTarget(nextEntry)) {
                        matches = false;
                        break;
                    }
                }

                if (matches) {
                    break;
                } else {
                    nextEntry = filters.get(0).getNextEntry();
                }
            }
        }

        // DEBUG
//        if (nextEntry != null) {
//            Log.d("DEBUG", "Path: " + nextEntry);
//        } else {
//            Log.d("DEBUG", "NULL");
//        }

        return nextEntry;
    }

    public boolean isValid() {
        return filters != null;
    }

    public boolean isWildMatch() {
        if (filters != null) {
            for (PathFilter filter : filters) {
                if (!filter.isWildMatch()) {
                    return false;
                }
            }
            // All filters are wild match
            return true;
        }
        return false;
    }
}
