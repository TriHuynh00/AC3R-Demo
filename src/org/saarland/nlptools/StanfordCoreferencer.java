package org.saarland.nlptools;

import java.io.StringReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.nlp.coref.CorefCoreAnnotations;
import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.parser.nndep.DependencyParser;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.IntPair;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import org.saarland.accidentconstructor.ConsoleLogger;

public class StanfordCoreferencer {
    private MaxentTagger tagger;
    private DependencyParser parser;

    private String modelPath = DependencyParser.DEFAULT_MODEL;
    private String taggerPath = "edu/stanford/nlp/models/pos-tagger/english-left3words/english-left3words-distsim.tagger";

    Properties props = null;

    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);


    final static String maxentTaggerModelStr = "models/wsj-0-18-left3words-distsim.tagger";

    private final static String[] specialChars = {",", ".", ":", ";"};
    private final static String[] dependencyArr = {
            "compound",
            "acl",
            "acl:relcl",
            "amod", 
            "advmod",
            "appos",
            "case",
            "conj",
            "csubj",
            "dep",
            "neg",
            "nmod:at",
            "nmod:agent",
            "nmod:between",
            "nmod:for",
            "nmod:tmod",
            "nmod:poss",
            "nmod:with",
            "nmod:to",
            "nmod:on",
            "nmod:of",
            "nmod:from",
            "nmod:in",
            "nmod:into",
            "rcmod",
            "nsubj",
            "nsubj:xsubj",
            "nsubjpass",
            "num",
            "number",
            "nummod",
            "poss",
            "possessive",
            "dobj",
            "xcomp",
            "iobj"};

    private final static List<String> dependencyList = Arrays.asList(dependencyArr);

    public StanfordCoreferencer()
    {
        Collections.sort(dependencyList);
        props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner,parse,mention,coref");
        props.setProperty("tokenize.language", "en");
        pipeline = new StanfordCoreNLP(props);
        ConsoleLogger.print('d',"Coreference Loaded! Begin to load Stanford Dependencies Parser ");
        tagger = new MaxentTagger(taggerPath);
        parser = DependencyParser.loadFromModelFile(modelPath);
    }

//    public void initParser()
//    {
//        tagger = new MaxentTagger(taggerPath);
//        parser = DependencyParser.loadFromModelFile(modelPath);
//    }

//    public void destroyCoref()
//    {
//        pipeline = null;
//        props = null;
//    }

    public String findCoreference(String text)
    {

        String[] wordsInText = text.toLowerCase().split(" ");

        boolean hasCoref = false;

        // Check if co-reference is needed, by looking if words like 'it', 'they', etc. exists.
        
        for (String word : wordsInText)
        {
            word = word.trim().replace(".", "").replace(",", "").replace(";", "");
            if (word.equals("it") || word.equals("its")
                    || word.equals("they")
                    || word.equals("them")
                    || word.equals("their")
                    || word.equals("he")
                    || word.equals("she")
                    || word.equals("him")
                    || word.equals("her"))
            {
                ConsoleLogger.print('d',"coref word: " + word);
                hasCoref = true;
                break;
            }
        }
       
        ConsoleLogger.print('d',"has coref " + hasCoref);
        if (hasCoref)
        {
            String vehicleID = "";

            Annotation document = new Annotation(text);

            //        props.setProperty("coref.model", "edu/stanford/nlp/models/coref/neural/english-model-conll.ser.gz");
            pipeline.annotate(document);
            ConsoleLogger.print('d',"---");
            ConsoleLogger.print('d',"coref chains");

            String modifiedText = text;
            modifiedText = modifiedText.replace(",", " ,");
            // Append space into text if space is not at the beginning, do this because CoreNLP start counting from 1

            for (CorefChain cc : document.get(CorefCoreAnnotations.CorefChainAnnotation.class).values()) {
                ConsoleLogger.print('d',"\t" + cc);
                Map<IntPair, Set<CorefChain.CorefMention>> mentionMap = cc.getMentionMap();

                // If there exists coref, proceed to next step
                if (mentionMap.size() > 0) {
                    if (modifiedText.indexOf(" ") != 0) {
                        modifiedText = " " + modifiedText;
                    }
                } else // Otherwise, stop
                {
                    break;
                }
                String[] modifiedTextArr = modifiedText.trim().split(" ");
                ConsoleLogger.print('n',"Modified Text: ");
                for (String word : modifiedTextArr)
                    ConsoleLogger.print('n',word + " ");
                ConsoleLogger.print('d', "");

                Iterator iterator = mentionMap.entrySet().iterator();
                String mentionSubject = "";
                while (iterator.hasNext()) {
                    Map.Entry pair = (Map.Entry) iterator.next();
                    Set<CorefChain.CorefMention> corefMentionSet = (Set<CorefChain.CorefMention>) pair.getValue();
                    for (CorefChain.CorefMention mention : corefMentionSet) {
                        String mentionSpan = mention.mentionSpan;

                        // Get the first phrase of a "phrase1,phrase2" that contains vehicle
                        if (mentionSpan.contains(",") && mentionSpan.contains("vehicle"))
                        {
                            vehicleID = mentionSpan.split(",")[0].trim();
                            ConsoleLogger.print('d',"Found Vehicle ID " + vehicleID + " in " + mentionSpan);
                        }
                        else if (mentionSpan.contains("vehicle"))
                        {
                            Pattern vehicleNumberPattern = Pattern.compile("(v|vehicle)\\d{1,2}");
                            Matcher matcherVehicleNumber = vehicleNumberPattern.matcher(mentionSpan);
                            if (matcherVehicleNumber.find())
                            {
                                vehicleID = matcherVehicleNumber.group();
                            }
                        }
                        else
                        {
                            ConsoleLogger.print('d',"VehicleID " + vehicleID);
                            // The first mention, if not a vehicle, is a mentioned subject
                            if (mention.mentionID == 1 && mentionSpan.contains("vehicle"))
                            {
                                if (mentionSpan.contains(","))
                                {
                                    mentionSubject = mentionSpan.split(",")[0];

                                }
                                else
                                {
                                    mentionSubject = mentionSpan;
                                }
                                continue;
                            }

//                            if (mentionSpan.equals("it") || mentionSpan.equals("he") || mentionSpan.equals("she"))
//                            {
//                                int location = checkAdjacentWord(mention.startIndex, mentionSpan, modifiedTextArr);
//                                if (location > -1)
//                                {
//                                    if (!vehicleID.equals(""))
//                                    {
//                                        modifiedTextArr[location] = vehicleID;
//                                    }
//                                    else if (!mentionSubject.equals(""))
//                                    {
//                                        modifiedTextArr[location] = mentionSubject;
//                                    }
//                                }
//                                //                            textSB.delete(mention.startIndex, mention.endIndex);
//                                //                            textSB.insert(mention.startIndex, vehicleID);
//
//                            }
//                            else if (mentionSpan.equals("its") || mentionSpan.equals("his") || mentionSpan.equals("her"))
//                            {
//                                int location = checkAdjacentWord(mention.startIndex, mentionSpan, modifiedTextArr);
//                                if (location > -1)
//                                {
//                                    if (!vehicleID.equals(""))
//                                    {
//                                        modifiedTextArr[location] = vehicleID + "'s";
//                                    }
//                                    else if (!mentionSubject.equals(""))
//                                    {
//                                        modifiedTextArr[location] = mentionSubject + "'s";
//                                    }
//
//                                }
//                            }
                        }
                        ConsoleLogger.print('d',"Mention Name " + mentionSpan + " Value " + mention.startIndex + " " + mention.headIndex);
                    }



                }

                iterator = mentionMap.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry pair = (Map.Entry) iterator.next();
                    Set<CorefChain.CorefMention> corefMentionSet = (Set<CorefChain.CorefMention>) pair.getValue();
                    for (CorefChain.CorefMention mention : corefMentionSet) {
                        String mentionSpan = mention.mentionSpan;
                        if (mentionSpan.equals("it") || mentionSpan.equals("he") || mentionSpan.equals("she")) {
                            ConsoleLogger.print('d',"FOund it and Vehicle ID = " + vehicleID);
                            int location = checkAdjacentWord(mention.startIndex, mentionSpan, modifiedTextArr);
                            if (location > -1) {
                                if (!vehicleID.equals("")) {
                                    modifiedTextArr[location] = vehicleID;
                                } else if (!mentionSubject.equals("")) {
                                    modifiedTextArr[location] = mentionSubject;
                                }
                            }
                            //                            textSB.delete(mention.startIndex, mention.endIndex);
                            //                            textSB.insert(mention.startIndex, vehicleID);

                        } else if (mentionSpan.equals("its") || mentionSpan.equals("his") || mentionSpan.equals("her")) {
                            int location = checkAdjacentWord(mention.startIndex, mentionSpan, modifiedTextArr);
                            if (location > -1) {
                                if (!vehicleID.equals("")) {
                                    modifiedTextArr[location] = vehicleID + "'s";
                                } else if (!mentionSubject.equals("")) {
                                    modifiedTextArr[location] = mentionSubject + "'s";
                                }

                            }
                        }
                    }
                }
                StringBuilder resultSB = new StringBuilder();
                for (String word : modifiedTextArr) {
                    resultSB.append(word + " ");
                }
                String result = resultSB.toString().trim();
                for (String specialChar : specialChars) {
                    result = result.replace(" " + specialChar, specialChar);
                }
                result = result + ".";
                ConsoleLogger.print('d',"Result: " + result);
                return result;
            }
        }
        return text;

    }

    public LinkedList<LinkedList<String>> findDependencies(String text)
    {


        LinkedList<LinkedList<String>> resultList = new LinkedList<LinkedList<String>>();
        LinkedList<String> taggedWordList = new LinkedList<String>();
        LinkedList<String> extractedDependencyList = new LinkedList<String>();

//        MaxentTagger tagger;
//        DependencyParser parser;

//        if (tagger == null || parser == null)
//        {
////            destroyCoref();
//            tagger = new MaxentTagger(taggerPath);
//            parser = DependencyParser.loadFromModelFile(modelPath);
//        }

        DocumentPreprocessor tokenizer = new DocumentPreprocessor(new StringReader(text));
        for (List<HasWord> sente: tokenizer) {
            List<TaggedWord> tagged = tagger.tagSentence(sente);
            ConsoleLogger.print('d',"Tagged Word List ");
            ConsoleLogger.print('d',tagged);
            for (TaggedWord tagWord : tagged)
            {
                taggedWordList.add(tagWord.toString());
            }
            GrammaticalStructure gs = parser.predict(tagged);

            // Print typed dependencies
            List<TypedDependency> dependenciesList = gs.typedDependenciesEnhancedPlusPlus();
            for (TypedDependency dependency : dependenciesList)
            {
                // Add relevant dependency
                if (dependencyList.contains(dependency.reln().toString())) {
                    ConsoleLogger.print('n',dependency);
                    ConsoleLogger.print('n'," Relation " + dependency.reln().toString());
                    ConsoleLogger.print('n'," Gov " + dependency.gov().word());
                    ConsoleLogger.print('d'," Dep " + dependency.dep().word());
                    extractedDependencyList.add(dependency.toString());
                }
                else {
                    ConsoleLogger.print('n',"other Dependency " + dependency);
                    ConsoleLogger.print('n'," Relation " + dependency.reln().toString());
                    ConsoleLogger.print('n'," Gov " + dependency.gov().word());
                    ConsoleLogger.print('d'," Dep " + dependency.dep().word());
                }
            }
        }
        resultList.add(taggedWordList);
        resultList.add(extractedDependencyList);
        return resultList;
    }

    // Check if nearby words are the same as the current keyWord
    private int checkAdjacentWord(int startIndex, String keyWord, String[] wordArray) {
        int radius = 1;

        int lowestIndexValue = startIndex - radius < 0 ? 0 : startIndex - radius;

        int largestIndexValue = startIndex + radius <= wordArray.length ? startIndex + radius : wordArray.length;

        for (int i = lowestIndexValue; i < largestIndexValue; i++)
        {
            if (wordArray[i].equalsIgnoreCase(keyWord)) {
                return i;
            }
        }
        return -1;
    }
}

//
//import edu.stanford.nlp.coref.CorefCoreAnnotations;
//import edu.stanford.nlp.dcoref.CorefChain;
//import edu.stanford.nlp.ling.CoreAnnotations;
//import edu.stanford.nlp.ling.HasWord;
//import edu.stanford.nlp.ling.TaggedWord;
//import edu.stanford.nlp.parser.nndep.DependencyParser;
//import edu.stanford.nlp.pipeline.*;
//import edu.stanford.nlp.process.DocumentPreprocessor;
//import edu.stanford.nlp.semgraph.SemanticGraph;
//import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
//import edu.stanford.nlp.tagger.maxent.MaxentTagger;
//import edu.stanford.nlp.trees.*;
//import edu.stanford.nlp.util.CoreMap;
//import edu.stanford.nlp.util.PropertiesUtils;
//
//import java.io.StringReader;
//import java.util.*;
//
//public class StanfordCoreferencer {
//    public static void main(String[] args) {
//
//        // creates a StanfordCoreNLP object, with POS tagging, lemmatization, NER, parsing, and coreference resolution
//        Properties props = new Properties();
//        props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner, parse, mention, dcoref"); //dcoref
//        StanfordCoreNLP coreRefPipeline = new StanfordCoreNLP(props);
////                PropertiesUtils.asProperties(
////                "annotators", "tokenize, ssplit, pos, lemma, ner, parse, mention, dcoref",
////                "ssplit.isOneSentence", "true",
////                "tokenize.language", "en"));
//
//        StanfordCoreNLP pipeline = new StanfordCoreNLP(PropertiesUtils.asProperties(
//                "annotators", "tokenize, ssplit, pos, lemma, parse",
//                "ssplit.isOneSentence", "true",
//                "parse.model", "edu/stanford/nlp/models/srparser/englishSR.beam.ser.gz",
//                "tokenize.language", "en"));
//
//        // read some text in the text variable
//        String text = "Vehicle1, 2000 Ford Expedition was traveling south on the two lane undivided roadway when it impacted the rear of Vehicle2, a 2004 Dodge Ram 1500, with its front";
//
////        String modelPath = DependencyParser.DEFAULT_MODEL;
////        String taggerPath = "edu/stanford/nlp/models/pos-tagger/english-left3words/english-left3words-distsim.tagger";
////        MaxentTagger tagger = new MaxentTagger(taggerPath);
////        DependencyParser parser = DependencyParser.loadFromModelFile(modelPath);
////
////        DocumentPreprocessor tokenizer = new DocumentPreprocessor(new StringReader(text));
////        for (List<HasWord> sente: tokenizer) {
////            List<TaggedWord> tagged = tagger.tagSentence(sente);
////            ConsoleLogger.print('d',"Tagged Word List ");
////            ConsoleLogger.print('d',tagged);
////            GrammaticalStructure gs = parser.predict(tagged);
////
////            // Print typed dependencies
////            List<TypedDependency> dependenciesList = gs.typedDependenciesEnhancedPlusPlus();
////            for (TypedDependency dependency : dependenciesList)
////            {
////                ConsoleLogger.print('d',dependency);
////            }
////        }
//
//        // create an empty Annotation just with the given text
//        Annotation document = new Annotation(text);
//        Annotation documentCoref = new Annotation(text);
//        // run all Annotators on this text
////        pipeline.annotate(document);
//        coreRefPipeline.annotate(documentCoref);
//
//        ConsoleLogger.print('d',"CorefChain");
//        for (CorefChain cc : documentCoref.get(De.CorefChainAnnotation.class).values()) {
//            ConsoleLogger.print('d',"\t" + cc);
//        }
//
////        document = new Annotation(text);
////        pipeline.annotate(document);
////
////        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
////
////        for(CoreMap sentence: sentences) {
////            // traversing the words in the current sentence
////            // a CoreLabel is a CoreMap with additional token-specific methods
//////            for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
//////                // this is the text of the token
//////                String word = token.get(TextAnnotation.class);
//////                // this is the POS tag of the token
//////                String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
//////                // this is the NER label of the token
//////                String ne = token.get(NamedEntityTagAnnotation.class);
//////            }
////
////
////
////            // this is the parse tree of the current sentence
////            Tree tree = sentence.get(TreeCoreAnnotations.TreeAnnotation.class);
////            ConsoleLogger.print('d',"Print Tree");
////            tree.indentedListPrint();
//////
//////
//////            SemanticGraph dependencies = sentence.get(SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation.class);
////
////
////        }
////        Map<Integer, CorefChain> graph =
////                document.get(CorefCoreAnnotations.CorefChainAnnotation.class);
////        ConsoleLogger.print('d',"Graph ");
////        ConsoleLogger.print('d',graph);
//
//    }
//
//}
