package org.metadatacenter.sap;


public class Annotation {
    private final String term_id;
    private final String pref_label;
    private final String ontology_id;


    public Annotation(String term_id, String pref_label, String ontology_id)
    {
        this.term_id = term_id;
        this.pref_label = pref_label;
        this.ontology_id = ontology_id;
    }

    public String getTermURI() { return this.term_id; }
    public String getPrefLabel() { return this.pref_label; }
    public String getOntologyId() { return this.ontology_id; }


    @Override public String toString()
    {
        return "Annotation{" +
                "Term URI =" + term_id + "\n\n" +
                "Preferred Label=" + pref_label + "\n\n" +
                "Ontology ID =" + ontology_id +
                '}';
    }

}
