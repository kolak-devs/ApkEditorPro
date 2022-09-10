package jadx.api.data;

public interface IJavaNodeRef extends Comparable<IJavaNodeRef> {

    RefType getType();

    String getDeclaringClass();

    String getShortId();

    enum RefType {
        CLASS, FIELD, METHOD, PKG
    }
}
