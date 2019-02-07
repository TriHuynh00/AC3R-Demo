package org.saarland.ontologyparser;

import java.util.HashMap;

/**
 * Created by Dell on 11/10/2017.
 */
public class AccidentConcept {
    private String inputGroup; // mainlvl
    private String category; // interesting lvl
    private String conceptGroup; // leaf lvl
    private String conceptName; // instance
    private HashMap<String, String> dataProperties;

    public AccidentConcept(){};

    public AccidentConcept(String inputGroup, String category, String conceptName, String conceptGroup, HashMap<String, String> dataProperties) {
        this.inputGroup = inputGroup;
        this.category = category;
        this.conceptName = conceptName;
        this.conceptGroup = conceptGroup;
        this.dataProperties = dataProperties;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String Category) {
        this.category = Category;
    }

    public String getInputGroup() {
        return inputGroup;
    }

    public void setInputGroup(String inputGroup) {
        this.inputGroup = inputGroup;
    }

    public String getConceptName() {
        return conceptName;
    }

    public void setConceptName(String conceptName) {
        this.conceptName = conceptName;
    }

    public String getLeafLevelName() {
        return conceptGroup;
    }

    public void setLeafLevelName(String conceptGroup) {
        this.conceptGroup = conceptGroup;
    }

    public HashMap<String, String> getDataProperties() {
        return dataProperties;
    }

    public void setDataProperties(HashMap<String, String> dataProperties) {
        this.dataProperties = dataProperties;
    }
}
