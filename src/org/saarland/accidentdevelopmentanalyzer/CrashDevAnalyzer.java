package org.saarland.accidentdevelopmentanalyzer;

import org.saarland.accidentconstructor.AccidentConstructorUtil;
import org.saarland.accidentconstructor.ConsoleLogger;
import org.saarland.accidentelementmodel.ActionDescription;
import org.saarland.accidentelementmodel.TestCaseInfo;
import org.saarland.accidentelementmodel.VehicleAttr;
import org.saarland.crashanalyzer.DamagedComponentAnalyzer;
import org.saarland.environmentanalyzer.EnvironmentAnalyzer;
import org.saarland.nlptools.Stemmer;
import org.saarland.ontologyparser.OntologyHandler;

import javax.swing.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class TextualCrashDescriptionAnalyzer {

    OntologyHandler ontologyHandler;
    Stemmer stemmer;

    public TextualCrashDescriptionAnalyzer(OntologyHandler ontoHandler) {
        ontologyHandler = ontoHandler;
        stemmer = new Stemmer();
    }
    //

    public LinkedList<ActionDescription> analyzeCrashDevelopment(
            LinkedList<LinkedList<String>> relevantTaggedWordsAndDependencies, EnvironmentAnalyzer environmentAnalyzer,
            ArrayList<VehicleAttr> vehicleList, TestCaseInfo testCase)
    {
        LinkedList<String> tagWordList = relevantTaggedWordsAndDependencies.get(0);
        ConsoleLogger.print('d', tagWordList.toString());

        LinkedList<ActionDescription> actionList = new LinkedList<ActionDescription>();

        LinkedList<String> dependencyList = relevantTaggedWordsAndDependencies.get(1);

        DamagedComponentAnalyzer damagedComponentAnalyzer = new DamagedComponentAnalyzer(vehicleList, ontologyHandler,
                testCase.getName());

        for (String sentenceDep : dependencyList)
        {
            if (sentenceDep.startsWith("nsubj"))
            {
                String[] actionAndActor = AccidentConstructorUtil.getWordPairFromDependency(sentenceDep);

                ActionDescription actionDescription = new ActionDescription();

                String action = AccidentConstructorUtil.getWordFromToken(actionAndActor[0]);
                String actor = AccidentConstructorUtil.getWordFromToken(actionAndActor[1]);
                ConsoleLogger.print('d', "First found Actor is " + actor);

                // If this is a vehicle, find its action
                if (actor.startsWith("vehicle"))
                {
                    String relatedWords = AccidentConstructorUtil.findAllConnectedWords(dependencyList, actor,
                            actor, 0, 3);

                    ConsoleLogger.print('d', String.format("Related Words of %s is %s", actor, relatedWords));
                }
            } // End processing main action of vehicle from nsubj dependency
        } // End loop through each dependency in the sentence
        return actionList;
    }
}
