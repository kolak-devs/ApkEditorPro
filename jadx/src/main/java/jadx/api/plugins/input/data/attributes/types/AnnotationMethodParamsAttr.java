package jadx.api.plugins.input.data.attributes.types;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import jadx.api.plugins.input.data.annotations.IAnnotation;
import jadx.api.plugins.input.data.attributes.JadxAttrType;
import jadx.api.plugins.input.data.attributes.PinnedAttribute;

public class AnnotationMethodParamsAttr extends PinnedAttribute {

    private final List<AnnotationsAttr> paramList;

    private AnnotationMethodParamsAttr(List<AnnotationsAttr> paramsList) {
        this.paramList = paramsList;
    }

    @Nullable
    public static AnnotationMethodParamsAttr pack(List<List<IAnnotation>> annotationRefList) {
        if (annotationRefList.isEmpty()) {
            return null;
        }
        List<AnnotationsAttr> list = new ArrayList<>(annotationRefList.size());
        for (List<IAnnotation> annList : annotationRefList) {
            list.add(AnnotationsAttr.pack(annList));
        }
        return new AnnotationMethodParamsAttr(list);
    }

    public List<AnnotationsAttr> getParamList() {
        return paramList;
    }

    @Override
    public JadxAttrType<AnnotationMethodParamsAttr> getAttrType() {
        return JadxAttrType.ANNOTATION_MTH_PARAMETERS;
    }

    @Override
    public String toString() {
        return paramList.toString();
    }
}
