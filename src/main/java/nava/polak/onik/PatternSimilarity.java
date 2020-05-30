package nava.polak.onik;

import com.google.common.annotations.VisibleForTesting;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nava.polak.onik.model.KeyToSetMap;
import nava.polak.onik.model.PatternType;
import nava.polak.onik.model.Record;
import nava.polak.onik.model.ResultsPerType;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * This class finds the similarity between sentences and aggregate the results.
 * The algorithm to find similarity is run over all sentences and save the founded words into row indexes.
 * The sentences should be in format: [name] is [action] [doing something]. For example: Neta is eating an apple.
 * In order to run initiate constructor with input file path and out file path to create.
 *              PatternSimilarity patternSimilarity = new PatternSimilarity(input_file,output_file);
 *               patternSimilarity.findSentensesWith1Differnce();
 * There is test coverage in the test part
 */
@Slf4j
public class PatternSimilarity {

    /**
     * regular expression to match with each sentence
     */
    private static final String patternStr = "(\\d\\d-\\d\\d-\\d\\d\\d\\d \\d\\d:\\d\\d:\\d\\d) ([^\\s]+) is ([^\\s]+) (.*)";

    private final Pattern pattern;
    private final String outputFilePath;
    private final String inputFilePath;

    /**
     * Store all sentences read from file
     */
    private List<Record> sentences = new ArrayList<>();

    /**
     * Store sentences not according expected pattern
     */
    @Getter
    private final List<String> sentencesNotAccordingPattern = new ArrayList<>();

    /**
     * Store all the names in sentences mapped to the index in sentences array
     */
    private final KeyToSetMap<String,Integer> nameToIndexMap = new KeyToSetMap();

    /**
     * Store all the actions in sentences mapped to the index in sentences array
     */
    private final KeyToSetMap<String,Integer> actionToIndexMap = new KeyToSetMap();

    /**
     * Store all the rest in sentences mapped to the index in sentences array
     */
    private final KeyToSetMap<String,Integer> restToIndexMap = new KeyToSetMap();

    /**
     * store the reults of similar sentences
     * The key is string concatenations of the 2 parts which are identical.
     * The value is the sentence index in sentences array where this combination exists.
     */
    HashMap<String, ResultsPerType> keyToResults  = new HashMap<>();




    public PatternSimilarity(String inputFilePath, String outputFilePath) {
        this.inputFilePath = inputFilePath;
        this.outputFilePath = outputFilePath;
        this.pattern = Pattern.compile(patternStr);
    }

    public void printResults(){

        try (BufferedWriter bf = new BufferedWriter(new FileWriter(outputFilePath))) {
            keyToResults.values().stream().forEach(v->{
                try {
                    bf.write( v.getRecordsStr(sentences));
                    bf.newLine();
                    bf.flush();
                } catch (IOException e) {
                   log.error("Got error while writing record",e);
                }

            });

            if(keyToResults.isEmpty())
                log.warn("No match was found");
            else
                log.debug("Sentence similarity written into  " + outputFilePath);

        }catch(Exception e){
            throw new RuntimeException("Got error while writing results into file",e);
        }

        //print all sentences which  are not according the template
        if(!sentencesNotAccordingPattern.isEmpty()){
            StringBuilder message = new StringBuilder("The following sentences could not be parsed according pattern " + patternStr + ":");
            sentencesNotAccordingPattern.stream().forEach(s->message.append(s).append("\n"));
            String messageStr = message.toString();
            log.error(messageStr);
            System.out.println(messageStr);
        }
    }

    public void findSentencesSingleDifferenceAndPrint(){
        findSentensesWithSingleDifference();
        printResults();
    }


    @VisibleForTesting
    public HashMap<String, ResultsPerType> findSentensesWithSingleDifference(){
        //make sure the file contains any sentence
        final AtomicBoolean atLeast1Sentence = new AtomicBoolean(false);
        log.debug("Processing sentences from " + inputFilePath);
        //go over sentences from file
        try (Stream<String> stream = Files.lines(Paths.get(inputFilePath)) ) {
            stream.forEach(s-> {
                atLeast1Sentence.set(true);
                Record currRecord = null;
                try {
                    //match to pattern and save the parts
                    Matcher matcher = pattern.matcher(s.trim());
                    if(matcher.find()) {
                        currRecord = new Record(matcher.group(1),
                                matcher.group(2),
                                matcher.group(3),
                                matcher.group(4)
                        );
                    }else{
                        log.error("Failed in matching to pattern for " + s);
                        sentencesNotAccordingPattern.add(s);
                    }
                } catch (Exception e) {
                    log.error("Failed in matching to pattern for " + s + " " + e.getMessage());
                    sentencesNotAccordingPattern.add(s);
                }
                if(currRecord != null)
                    //analyze each record against indexed data
                    handleRecord(currRecord);

            });

        } catch (IOException e) {
           throw new RuntimeException("There was error during reading the input from file: " + e.getMessage(),e);
        }

        if(!atLeast1Sentence.get())
            log.warn("The input file is empty. No records exists");
        else
            log.debug("Finished processing sentences");

        return keyToResults;
    }

    /**
     * analyze each record against indexed data
     * @param currRecord
     */
    private void handleRecord(Record currRecord) {
        //save the data
        sentences.add(currRecord);

        int index = sentences.size() - 1;

        //get indexed data for each part of the sentence
        Set<Integer> sentencesWithThisName = nameToIndexMap.get(currRecord.getName());
        Set<Integer> sentensesWithThisAction = actionToIndexMap.get(currRecord.getAction());
        Set<Integer> sentensesWithThisRst = restToIndexMap.get(currRecord.getRestStr());

        //try to find records with same name, same action
        checkThirdPartAmongIdentical2Parts(currRecord,
                sentencesWithThisName,
                sentensesWithThisAction,
                currRecord.getRestStr(),
                PatternType.REST,
                index
        );

        //try to find records with same action, same restStr
        checkThirdPartAmongIdentical2Parts(currRecord,
                sentensesWithThisAction,
                sentensesWithThisRst,
                currRecord.getName(),
                PatternType.NAME,
                index);

        //try to find records with same action, same restStr
        checkThirdPartAmongIdentical2Parts(currRecord,
                sentencesWithThisName,
                sentensesWithThisRst,
                currRecord.getAction(),
                PatternType.ACTION,
                index);

        //save parts into indexed data
        nameToIndexMap.put(currRecord.getName(), index);
        actionToIndexMap.put(currRecord.getAction(), index);
        restToIndexMap.put(currRecord.getRestStr(), index);
    }

    public static void main(String[] args) {
        if(args== null || args.length != 2){
            System.out.println("Usage: PatternSimilarity [input file path] []output file path");
            System.exit(-1);

        }

        PatternSimilarity patternSimilarity = new PatternSimilarity(args[0],args[1]);
        patternSimilarity.findSentencesSingleDifferenceAndPrint();

    }

    /**
     * Example: we hava common name and common action and want to find all
     * these records. Upon then, we will find the records that in the rest part, they are different by one word.
     *
     * upon 2 set of records find records exists in both (i.e. these the sentences which may differ by one word)
     * having this list of records
     *  go over and find for third entity which is different in one word.
     * This should be added to the result list
     */
    private void checkThirdPartAmongIdentical2Parts(Record currRecord,
                                    Set<Integer> firstSet,
                                    Set<Integer> secondSet,
                                    String firstTestStr,
                                    PatternType type,
                                    int index) {

        if(firstSet == null || secondSet == null){
            //this is the first time the word exists - no match
            return;
        }

        //get the intersaction - i.e. records which both words exists
        Set<Integer> intersection = new HashSet<>(firstSet);
        intersection.retainAll(secondSet);

        //only in case there are elements in the intersection
        intersection.stream().forEach(i->{
            //if rest does not contains the string then,
            //it brobably the past with singele difference
           int otherSentIndex = i;

           //get the record found is common
           Record thatRecord = sentences.get(otherSentIndex);

           //this is the word which is different in that record
           String secTestStr = thatRecord.getWordAccorfingType(type);
           //in case it is not identical
           if(!firstTestStr.equals(secTestStr)){
               //make sure the difference in words is only one (actually we are saving parts)
               if(is1wordDifferent(firstTestStr,thatRecord.getWordAccorfingType(type))){

                   //The key is [first identical part]_[second identical part]
                   String key = currRecord.constructKeyAccordingType(type);
                    ResultsPerType currEntry = keyToResults.get(key);
                    //for first match
                    if(currEntry == null){
                        currEntry = new ResultsPerType(type);
                        keyToResults.put(key,currEntry);
                    }
                   currEntry.addRecord(index);
                    currEntry.addRecord(otherSentIndex);
               }
           }
        });
    }


    private boolean is1wordDifferent(String s1, String s2) {
            if (s1 == null || s2 == null)
                return false;

            if (s1.isEmpty() || s2.isEmpty())
                return false;

            String[] s1Arr = s1.split(" ");
            String[] s2Arr = s2.split(" ");

            if (s1Arr.length != s2Arr.length) {
                return false;
            }

            int numDifrrences = 0;
            for (int i = 0; i < s1Arr.length; i++) {
                if (!s1Arr[i].equals(s2Arr[i])) {
                    numDifrrences++;
                }
                if (numDifrrences > 1) {
                    return false;
                }
            }
            return (numDifrrences == 1);
        }
}
