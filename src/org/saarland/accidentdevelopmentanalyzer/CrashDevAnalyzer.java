package org.saarland.accidentdevelopmentanalyzer;

import org.saarland.accidentconstructor.AccidentConstructor;
import org.saarland.accidentconstructor.AccidentConstructorUtil;
import org.saarland.accidentconstructor.ConsoleLogger;
import org.saarland.accidentelementmodel.ActionDescription;
import org.saarland.accidentelementmodel.VehicleAttr;
import org.saarland.configparam.AccidentParam;
import org.saarland.configparam.VelocityCode;
import org.saarland.crashanalyzer.DamagedComponentAnalyzer;
import org.saarland.nlptools.Stemmer;
import org.saarland.ontologyparser.AccidentConcept;
import org.saarland.ontologyparser.OntologyHandler;

import java.util.ArrayList;
import java.util.LinkedList;

public class CrashDevAnalyzer {

    OntologyHandler ontologyHandler;
    Stemmer stemmer;
    int wordInParagraphCount = 0;

    public CrashDevAnalyzer(OntologyHandler ontoHandler) {
        ontologyHandler = ontoHandler;
        stemmer = new Stemmer();
    }
    //

    public LinkedList<ActionDescription> analyzeCrashDevelopment(
            LinkedList<LinkedList<String>> relevantTaggedWordsAndDependencies,
            ArrayList<VehicleAttr> vehicleList,
            LinkedList<ActionDescription> actionList)
    {
        LinkedList<String> tagWordList = relevantTaggedWordsAndDependencies.get(0);
        ConsoleLogger.print('d', tagWordList.toString());

        LinkedList<String> dependencyList = relevantTaggedWordsAndDependencies.get(1);
        LinkedList<String> processedWords = new LinkedList<String>();

        for (String sentenceDep : dependencyList)
        {
            ConsoleLogger.print('d', "Processing Dependency " + sentenceDep);
//            if (sentenceDep.contains("vehicle"))
//            {
                String[] actionAndActor = AccidentConstructorUtil.getWordPairFromDependency(sentenceDep);

                ActionDescription actionDescription = new ActionDescription();

                String action = AccidentConstructorUtil.getWordFromToken(actionAndActor[0]);
                String actor = AccidentConstructorUtil.getWordFromToken(actionAndActor[1]);
                String wordToken = "";

                boolean validAction = false;

                // Only action exists in AC3R knowledge base will be processed
                for (String word : actionAndActor) {
                    AccidentConcept wordCon = ontologyHandler.findExactConcept(
                            stemmer.stem(AccidentConstructorUtil.getWordFromToken(word)));

                    ConsoleLogger.print('d', "Processing Word " + word);
                    ConsoleLogger.print('d', "Word Concept is null " + (wordCon == null));

                    if (wordCon != null) {
                        if (wordCon.getLeafLevelName().equals("vehicle_direction")
                                || wordCon.getLeafLevelName().equals("vehicle_action")) {

                            if (processedWords.contains(word))
                            {
                                validAction = false;
                            }
                            else
                            {
                                wordToken = word;
                                processedWords.add(word);
                                action = wordCon.getConceptName();
                                validAction = true;
                            }
                        }
                    }
                }

                if (!validAction)
                    continue;

                // If actor is not a vehicle, swap it with action
                if (!actor.startsWith("vehicle") && action.startsWith("vehicle"))
                {
                    String temp = actor;
                    actor = action;
                    action = temp;
                }

                ConsoleLogger.print('d', "First found Actor is " + actor);
                ConsoleLogger.print('d', "First found Action is " + action);

                // find action of vehicle
                String relatedWords = AccidentConstructorUtil.findAllConnectedWordsTopDown(dependencyList, wordToken,
                    wordToken, 0, 4);

//                relatedWords = AccidentConstructorUtil.findAllConnectedWordsBottomUp(dependencyList, wordToken,
//                    wordToken, 0, 4);

                ConsoleLogger.print('d', String.format("Related Words of %s is %s",
                    wordToken, relatedWords));

                String[] relatedWordsArr = relatedWords.split(",");

                // Analyze to see whether the action is dynamic (travel) or static (park, stop)
                AccidentConcept actionConcept = ontologyHandler.findExactConcept(
                    AccidentConstructorUtil.getWordFromToken(action));


//                for (String actionWord : relatedWordsArr) {

                    ConsoleLogger.print('d', "processing action " + action);

                    //AccidentConcept actionConcept = ontologyHandler.findExactConcept(action);

                    if (actionConcept != null) {

                        int actorIndex = -1;
                        // Found actor of the action, actor word starts with "vehicle"
                        int maxLength = relatedWordsArr.length <= 7 ? relatedWordsArr.length : 7;
                        // Loop through the first 5 words in related words
                        // if vehicle word appears after more than 5 words,
                        // the vehicle is not guaranteed to be the subject
                        // If no "vehicle[ID]" string is found, attempt to find the type of vehicle instead

                        // Find exact vehicle[ID] first, if the actor is not a vehicle with specific ID
                        for (int i = 0; i < maxLength; i++) {
                            AccidentConcept relatedWordConcept = ontologyHandler.findExactConcept(
                                AccidentConstructorUtil.getWordFromToken(relatedWordsArr[i]));

                            // Set the vehicle as actor
                            if ((!actor.matches("vehicle\\d") && relatedWordsArr[i].startsWith("vehicle"))
                                || (actor.matches("vehicle\\d") && relatedWordsArr[i].startsWith(actor))) {
                                actor = AccidentConstructorUtil.getWordFromToken(relatedWordsArr[i]);
                                ConsoleLogger.print('d', String.format("action is %s, acting Vehicle is %s",
                                    action, actor));

                                actorIndex = i;
                                break;
                            }
                        }

                        // find vehicle type if a specific vehicle is not found
                        if (!actor.startsWith("vehicle")) {
                            for (int i = 0; i < maxLength; i++) {
                                AccidentConcept relatedWordConcept = ontologyHandler.findExactConcept(
                                    AccidentConstructorUtil.getWordFromToken(relatedWordsArr[i]));

                                String vehicleType = relatedWordConcept != null ?
                                    relatedWordConcept.getLeafLevelName() :
                                    "";

                                // If no acting vehicle with specific ID is found,
                                // find a vehicle type instead
                                if ((vehicleType.equals("vehicle_type") ||
                                        vehicleType.equals("car_model"))) {

                                    ConsoleLogger.print('d', String.format("action is %s, vehicle type is %s",
                                        action, relatedWordsArr[i]));

                                    if (!actor.matches("vehicle\\d")) {
                                        actor = AccidentConstructorUtil.getWordFromToken(relatedWordsArr[i]);
                                    }

                                    actorIndex = i;
                                    break;
                                }
                            }
                        }
                        // If actor is not a vehicle, skip
                        if (actorIndex == -1)
                        {
                            continue;
                        }

                        if (actionConcept.getLeafLevelName().equals("vehicle_action")) {
                            action = stemmer.stem(actionConcept.getConceptName());

                            int actionVelocity = VelocityCode.UNKNOWN;

                            try {
                                actionVelocity = AccidentConstructorUtil.getVelocityOfAction(action, ontologyHandler);
                            } catch (Exception ex) {
                                ConsoleLogger.print('e', "Action Velocity not found for " + action);
                            }

                            ConsoleLogger.print('d', "Word token is " + wordToken);
                            int wordTokenIndex = AccidentConstructorUtil.getPositionFromToken(wordToken);
                            String modifiedWordToken = stemmer.stem(AccidentConstructorUtil.getWordFromToken(wordToken))
                                                        + "-" + wordTokenIndex;
//                            ConsoleLogger.print('d', String.format("Stemmed token is %s wordInPara %d", modifiedWordToken, wordInParagraphCount));
                            modifiedWordToken = renumberingWordToken(modifiedWordToken, wordInParagraphCount);
//                            ConsoleLogger.print('d', String.format("Modified word after renumbering %s", modifiedWordToken));


                            // if this is a travelling action, attempt to find the travelling direction
                            if (actionVelocity > 0 && actionVelocity != 1000) {
                                // Found direction for travelling or left/right side of turning action
                                boolean foundDirection = false;


                                // If the action event is not in the action list, add the action event to the action list
                                if (!hasDuplicatedAction(actionList, actor, modifiedWordToken)
                                    && actor.startsWith("vehicle")) {

                                    actionDescription.setSubject(actor);

                                    actionDescription.setVerb(modifiedWordToken);

                                    for (int j = 0; j < relatedWordsArr.length; j++) {

                                        // If this is a turning action, find whether this is a left/right turn
                                        int turnAngle = 0;

                                        try {
                                            String turnAngleStr = actionConcept.getDataProperties().get("turn_angle");
                                            turnAngle = Integer.parseInt(turnAngleStr);
                                        } catch (Exception ex) {
                                            ConsoleLogger.print('e', "No turn angle found, set to 0! \n"
                                                + ex.toString());
                                        }

                                        if (turnAngle > 0) {

                                            for (int k = 0; k < relatedWordsArr.length; k++) {

                                                if (relatedWordsArr[k].startsWith("left") ||
                                                    relatedWordsArr[k].startsWith("right")) {
                                                    String turnSide = AccidentConstructorUtil.getWordFromToken(relatedWordsArr[k]);
                                                    actionDescription.getVerbProps().add(turnSide);
                                                    foundDirection = true;
//                                                    actionDescription.setVerb(action + " " + turnSide);
                                                    ConsoleLogger.print('d', String.format("Found turn side of %s is %s",
                                                        action, turnSide));
                                                    break;
                                                }

                                            }
                                        } else {
                                            ConsoleLogger.print('d', action + " has no turn angle");
                                            LinkedList<AccidentConcept> directionConcepts = ontologyHandler.getDirectionConcepts();
                                            // Find and note the cardinal travelling direction of the acting vehicle
                                            for (AccidentConcept directionConcept : directionConcepts) {
                                                //ConsoleLogger.print('d', String.format("currWord is %s directionConcept %s",
                                                //        relatedWordsArr[j], directionConcept.getConceptName()));
                                                String travelDirection = directionConcept.getConceptName();
                                                if (AccidentConstructorUtil.getWordFromToken(relatedWordsArr[j])
                                                    .equals(travelDirection)) {

                                                    actionDescription.getVerbProps().add(travelDirection);
                                                    ConsoleLogger.print('d', String.format("Found direction of %s is %s",
                                                        action, relatedWordsArr[j]));

                                                    // find the right actor to record the travelling direction
                                                    if (actor.matches("vehicle\\d")) {
                                                        VehicleAttr actVehicle = AccidentConstructorUtil.
                                                            findVehicle(actor, vehicleList);

                                                        actVehicle.setTravellingDirection(
                                                            AccidentConstructorUtil.convertDirectionWordToDirectionLetter(
                                                                travelDirection)
                                                        );
                                                    }

                                                    foundDirection = true;
                                                    break;
                                                }
                                            }
                                        } // End processing straight movement action

                                        if (foundDirection)
                                            break;
                                    }
                                    actionList.add(actionDescription);
                                    processedWords.add(wordToken);
                                }

//                                if (foundDirection)
//                                    break;
                            } // End processing dynamic action
                            // Processing impact action
                            else if (actionVelocity == VelocityCode.IMPACT)
                            {
                                if (!hasDuplicatedAction(actionList, actor, modifiedWordToken)
                                        && actor.startsWith("vehicle")) {

                                    actionDescription.setSubject(actor);

                                    ConsoleLogger.print('d', String.format("Record Impact action of %s as %s",
                                        actor, modifiedWordToken));

                                    actionDescription.setVerb(modifiedWordToken);

                                    DamagedComponentAnalyzer dmgAnalyzer = new DamagedComponentAnalyzer(vehicleList, "test");

//                                    String dmgZone = findImpactedSideAndArea(actorIndex, relatedWordsArr);
                                    String dmgZone = findImpactedSideAndArea(actor, dependencyList, dmgAnalyzer, tagWordList);

                                    VehicleAttr actVehicleObj = AccidentConstructorUtil.findVehicle(actor, vehicleList);

                                    if (actVehicleObj == null) {
                                      continue;
                                    }

                                    boolean foundOtherVehicle = false;
                                    for (int k = 0; k < relatedWordsArr.length; k++) {
                                        String otherVehicle =
                                                AccidentConstructorUtil.getWordFromToken(relatedWordsArr[k]);

                                        if (otherVehicle.startsWith("vehicle") &&
                                                !otherVehicle.equals(actor)) {
                                            actionDescription.getVerbProps().add(otherVehicle);
                                            ConsoleLogger.print('d', String.format("Found Impact action of %s as %s with %s",
                                                    actor, action, otherVehicle));
                                            foundOtherVehicle = true;

//                                            String otherVehicleDmgZone = findImpactedSideAndArea(k, relatedWordsArr);
                                            String otherVehicleDmgZone =
                                                findImpactedSideAndArea(otherVehicle, dependencyList, dmgAnalyzer, tagWordList);
                                            VehicleAttr otherVehicleObj =
                                                AccidentConstructorUtil.findVehicle(otherVehicle, vehicleList);

//                                            otherVehicleObj.getDamagedComponents().add(otherVehicleDmgZone);

                                            break;
                                        }

                                    }
                                    actionList.add(actionDescription);
//                                    if (foundOtherVehicle)
//                                    {
//                                        break;
//                                    }
                                }
                            } // End processing impact actions
                            // Process static actions, such as park or stop
                            else if (actionVelocity == VelocityCode.STOP)
                            {

                                if (action.equals("park")) {
                                    ConsoleLogger.print('d', "Found park action ");
                                    // Find the pavement type
                                    ArrayList<AccidentConcept> pavementConcepts =
                                            ontologyHandler.findConceptsByLeafLvlName("pavement");

                                    int vehicleID = AccidentParam.UNKNOWN_VEHICLE;

                                    try {
                                        vehicleID = Integer.parseInt(actor.replace("vehicle", ""));
                                    } catch (Exception ex) {
                                        ConsoleLogger.print('d', "No specific vehicle ID found, use Unknown code");
                                    }

                                    VehicleAttr actingVehicle = AccidentConstructorUtil.findVehicleBasedOnId(vehicleID,
                                            vehicleList);

                                    for (AccidentConcept concept : pavementConcepts)
                                    {
                                        String conceptName = concept.getConceptName();
                                        if (relatedWords.contains("," + conceptName))
                                        {
                                            if (actingVehicle.getStandingStreet() != null) {
                                                actingVehicle.getStandingStreet().putValToKey("pavement_type",
                                                        conceptName);

                                            }
                                            // Find whether the parking side is "left" or "right"
                                            for (String side : AccidentParam.LEFTRIGHTARR) {
                                                if (relatedWords.contains("," + side)) {
                                                    actingVehicle.setStandingRoadSide(side);
                                                }

                                                ConsoleLogger.print('d', "Determined standing road side: "
                                                        + actingVehicle.getStandingRoadSide());
                                            }
//                                            // If there is only 1 road, assign the pavement type to that road
//                                            else if (testCase.getStreetList().size() == 1) {
//                                                testCase.getStreetList().get(0).putValToKey("pavement_type",
//                                                        relatedWord);
//                                            }
                                        }
                                    }

                                }
                                actionDescription.setSubject(actor);
                                actionDescription.setVerb(modifiedWordToken);
                                ConsoleLogger.print('d', String.format("record action %s of actor %s", modifiedWordToken, actor));
                                actionList.add(actionDescription);
                            }
                            else
                            {
                                continue;
                            }

                        } // End Processing vehicle action
                        else if (actionConcept.getLeafLevelName().equals("vehicle_direction"))
                        {
                            int wordTokenIndex = AccidentConstructorUtil.getPositionFromToken(wordToken);
                            // If "[cardinal direction]bound" is not a verb, don't process it
                            ConsoleLogger.print('d', "WordToken is " + wordToken);
                            ConsoleLogger.print('d', "tagWordList is " + tagWordList.toString());
                            ConsoleLogger.print('d', "tagWordList split is " +
                              tagWordList.get(wordTokenIndex - 1).split("/")[0]);
//                            if (!tagWordList.get(wordTokenIndex - 1).split("/")[1].startsWith("VB"))
//                            {
//                                continue;
//                            }
                            String travelToken = "travel-" + wordTokenIndex;
                            LinkedList<String> verbProp = new LinkedList<>();
                            verbProp.add(action);
                            if (!hasSameActionDuplicatedVerbProp(actionList, actor, travelToken, verbProp)
                                    && actor.startsWith("vehicle")) {
                                actionDescription.setSubject(actor);

                                travelToken = renumberingWordToken(travelToken, wordInParagraphCount);

                                actionDescription.setVerb(travelToken);
                                actionDescription.getVerbProps().add(actionConcept.getConceptName());

                                ConsoleLogger.print('d', String.format("Found travelling action with direction of %s for %s",
                                        actionConcept.getConceptName(), actor));

                                actionList.add(actionDescription);

                                // Record the travelling direction of acting vehicle
                                if (actor.matches("vehicle\\d")) {
                                    VehicleAttr actVehicle = AccidentConstructorUtil.
                                            findVehicle(actor, vehicleList);

                                    actVehicle.setTravellingDirection(
                                        AccidentConstructorUtil.convertDirectionWordToDirectionLetter(
                                            actionConcept.getConceptName()
                                        )
                                    );
                                }
//                                break;
                            }
                        }

                    } // End processing action concept
//                }

            } // End processing main action of vehicle from nsubj dependency
//        } // End loop through each dependency in the sentence
        // Reorder events chronologically
        verifyCrashEvents(actionList);
        wordInParagraphCount += relevantTaggedWordsAndDependencies.get(1).size();
        return actionList;
    }

    // Remove the position of action word in the paragraph. This function is called after AC3R analyzes the
    public void removeIndexInActionWord(LinkedList<ActionDescription> actionList)
    {
        for (ActionDescription actionDesc : actionList)
        {
            String verb = AccidentConstructorUtil.getWordFromToken(actionDesc.getVerb());
            actionDesc.setVerb(verb);
        }
    }

    public void constructVehicleActionEventList(LinkedList<ActionDescription> actionList, ArrayList<VehicleAttr> vehicleList) {

        for (ActionDescription actionDesc : actionList)
        {
            String subject = actionDesc.getSubject().trim();
            String verb    = AccidentConstructorUtil.getWordFromToken(actionDesc.getVerb().trim());

            // Avoid processing irrelevant subject
            if (actionDesc.getSubject().matches("vehicle\\d") || actionDesc.getSubject().startsWith("pedestrian"))
            {
                int vehicleID = Integer.parseInt(subject.replace("vehicle", ""));
                VehicleAttr referredVehicle = AccidentConstructorUtil.findVehicleBasedOnId(vehicleID, vehicleList);

                // If this is an impact, the ID of impacted vehicle will be included in the verb properties
                // record this impact action in impacted vehicle as well
                int velocity = AccidentConstructorUtil.getVelocityOfAction(verb, ontologyHandler);
                if (velocity == VelocityCode.IMPACT)
                {
                    String objVehicleName = "";

                    // This action has unknown object, remove it because such impact cannot be constructed
                    try {
                        objVehicleName = actionDesc.getVerbProps().get(0);
                    }
                    catch (Exception ex) {
                        actionList.remove(actionDesc);
                        continue;
                    }

                    if (objVehicleName.matches("vehicle\\d") || actionDesc.getSubject().startsWith("pedestrian"))
                    {
                        int objVehicleID = Integer.parseInt(objVehicleName.replace("vehicle", ""));
                        VehicleAttr objVehicle = AccidentConstructorUtil.findVehicleBasedOnId(objVehicleID, vehicleList);
                        objVehicle.getActionList().add("hit*");
                        objVehicle.getActionDescriptionList().add(actionDesc);
                    }
                    referredVehicle.getActionList().add("hit");
                    referredVehicle.getActionDescriptionList().add(actionDesc);

                }
                else
                {
                    referredVehicle.getActionList().add(verb);
                    referredVehicle.getActionDescriptionList().add(actionDesc);
                }
            }
        }
    }

    // Modify the word index by taking number of words from previous sentence plus the word index.
    private String renumberingWordToken(String wordToken, int currentWordCount)
    {
        String word = AccidentConstructorUtil.getWordFromToken(wordToken);
        int wordIndex = AccidentConstructorUtil.getPositionFromToken(wordToken);

        return word + "-" + (wordIndex + currentWordCount);
    }

    // First crash action should be placed after the first vehicle action.
    private void verifyCrashEvents(LinkedList<ActionDescription> actionList)
    {
        if (actionList != null)
        {
            ConsoleLogger.print('d', "Action list before verification");
            for (ActionDescription act : actionList)
            {
                ConsoleLogger.print('d', String.format("%s %s %s",
                        act.getSubject(), act.getVerb(), act.getVerbProps().toString()));
            }
        }

        // Remove duplicated action
        for (int i = actionList.size() - 1; i > 0; i--) {
            ActionDescription curActionDescription = actionList.get(i);
            ActionDescription prevActionDescription = actionList.get(i - 1);
            
            String currentVerb = curActionDescription.getVerb();
            String prevVerb = prevActionDescription.getVerb();
            
            
            if (curActionDescription.getSubject().equals(prevActionDescription.getSubject())
                && AccidentConstructorUtil.getWordFromToken(currentVerb).equals(AccidentConstructorUtil.getWordFromToken(prevVerb))
                && checkVerbPropsMatched(curActionDescription.getVerbProps(), prevActionDescription.getVerbProps())) {
                actionList.remove(i);
            }
        }

        for (int i = actionList.size() - 1; i > 0; i--) {
            if (AccidentConstructorUtil.getPositionFromToken(actionList.get(i).getVerb()) <
                    AccidentConstructorUtil.getPositionFromToken(actionList.get(i - 1).getVerb()))
            {
                ActionDescription temp = actionList.get(i - 1);
                actionList.set(i - 1, actionList.get(i));
                actionList.set(i, temp);
            }
        }

    }

    private boolean checkVerbPropsMatched(LinkedList<String> verbPropList1, LinkedList<String> verbPropList2)
    {

        if (verbPropList1.size() != verbPropList2.size())
        {
            return false;
        }
        else
        {
            boolean verbPropMatch = true;
            for (int i = 0; i < verbPropList1.size() - 1; i++) {
                if (!verbPropList1.get(i).equals(verbPropList2.get(i)))
                {
                    verbPropMatch = false;
                    break;
                }
            }
            return verbPropMatch;
        }

    }

    // Check if an action made by a vehicle is already recorded in a given actionList
    private boolean hasDuplicatedAction(LinkedList<ActionDescription> actionList, String subject, String action)
    {
        for (ActionDescription actDescription : actionList)
        {
            if (actDescription.getSubject().equals(subject) && actDescription.getVerb().equals(action))
            {
                return true;
            }
        }
        return false;
    }

    // Check if an action made by a vehicle is already recorded in a given actionList
    private boolean hasSameActionDuplicatedVerbProp(LinkedList<ActionDescription> actionList, String subject,
                                                    String action, LinkedList<String> verbProps)
    {
        for (ActionDescription actDescription : actionList)
        {
            if (actDescription.getSubject().equals(subject)
                && AccidentConstructorUtil.getWordFromToken(actDescription.getVerb()).equals
                    (AccidentConstructorUtil.getWordFromToken(action))
                && checkVerbPropsMatched(verbProps, actDescription.getVerbProps()))
            {
                return true;
            }
        }
        return false;
    }

    private int findingNearbyWord(String[] strArray, int baseWordIndex, String searchWord,
                                  int leftDistance, int rightDistance)
    {
        for (int j = baseWordIndex - leftDistance >= 0 ?
                     baseWordIndex - leftDistance :
                     0; // lower bound at index 0
             j < (baseWordIndex + rightDistance < strArray.length - 1 ?
                  baseWordIndex + rightDistance :
                  strArray.length - 1); // upper bound at arr length
             j++)
        {
            if (AccidentConstructorUtil.getWordFromToken(strArray[j]).equals(searchWord))
                return j;
        }
        return -1;
    }

    // Find the side (left, right) and damaged area (front, rear) of a vehicle in an impact by searching the words
    // related to the impact word. The procedure is:
    // 1 - Locate the index of vehicleID (V-index) in the list of related words
    // 2 - Scan words from range V-index - 2 to V-index + 2 to find impacted side and area
    // 3 - Return the "[side] [area]" String
    private String findImpactedSideAndArea(int actorIndex, String[] relatedWordsArr)
    {
        String damagedZone = "";

        int foundSideIndex = -1;

        // Find damage side
        for (String side : AccidentParam.LEFTRIGHTARR)
        {
            foundSideIndex = findingNearbyWord(relatedWordsArr, actorIndex, side, 2, 2);
            if (foundSideIndex > -1)
            {
                damagedZone += side + " ";
                break;
            }
        }

        // Find damaged area
        ArrayList<AccidentConcept> dmgAreaConcepts = ontologyHandler.findConceptsByLeafLvlName("vehicle_impact_side");
        for (AccidentConcept concept : dmgAreaConcepts)
        {
            int dmgAreaIndex = findingNearbyWord(relatedWordsArr, actorIndex, concept.getConceptName(), 2, 2);

            if (dmgAreaIndex > -1) {
                damagedZone += concept.getConceptName();
                // Attempt to find damage side if not found yet by scanning outside the initial window
                if (foundSideIndex == -1 && (dmgAreaIndex >= actorIndex - 2 || dmgAreaIndex <= actorIndex + 2))
                {
                    for (String side : AccidentParam.LEFTRIGHTARR) {
                        foundSideIndex = findingNearbyWord(relatedWordsArr, dmgAreaIndex, side, 2, 0);
                        damagedZone = side + " " + damagedZone;
                    }
                }
            }

        }
        return damagedZone;
    }

    private String findImpactedSideAndArea(String vehicleName, LinkedList<String> dependencyList,
                                           DamagedComponentAnalyzer damagedComponentAnalyzer, LinkedList<String> tagWordList)
    {
        String finalVictimDmgSide = "";
        if (vehicleName.matches("vehicle\\d+")) {
            String wordChain = AccidentConstructorUtil
                .findAllConnectedWordsBottomUp(dependencyList, vehicleName, vehicleName, 0, 2);

            for (String dmgComponent : wordChain.split(",")) {
                AccidentConcept elemConcept = ontologyHandler.findExactConcept(
                    AccidentConstructorUtil.getWordFromToken(dmgComponent));

                if (elemConcept != null && elemConcept.getLeafLevelName()
                    .equals("vehicle_impact_side")) {
                    // TODO: use findAllConnectedWords instead of looping through dependencies
                    finalVictimDmgSide = damagedComponentAnalyzer
                        .findSideOfCrashedComponents(dependencyList, dmgComponent, vehicleName,
                            tagWordList);

                    ConsoleLogger.print('d', "Final victim damaged side in nsubjpass "
                        + finalVictimDmgSide);
                }
            }

        }
        return finalVictimDmgSide;
    }


}
