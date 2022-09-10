package jadx.api.metadata;

public interface ICodeAnnotation {

    AnnType getAnnType();

    enum AnnType {
        CLASS,
        FIELD,
        METHOD,
        VAR,
        VAR_REF,
        DECLARATION,
        OFFSET
    }
}
