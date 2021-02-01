package org.saarland.accidentelementmodel;

import java.util.LinkedList;

public class ActionDescription {
    private String subject;
    private String verb;
    private LinkedList<String> verbProps;

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getVerb() {
        return verb;
    }

    public void setVerb(String verb) {
        this.verb = verb;
    }

    public LinkedList<String> getVerbProps() {
        return verbProps;
    }

    public void setVerbProps(LinkedList<String> verbProps) {
        this.verbProps = verbProps;
    }

    public ActionDescription(){
        verbProps = new LinkedList<String>();
    }

    public ActionDescription(String actor, String action, LinkedList<String> affectedObj)
    {
        subject = actor;
        verb = action;
        verbProps = affectedObj;
    }


}
