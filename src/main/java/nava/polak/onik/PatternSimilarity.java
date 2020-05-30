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

@Slf4j
public class PatternSimilarity {
    private static final String patternStr = "(\\d\\d-\\d\\d-\\d\\d\\d\\d \\d\\d:\\d\\d:\\d\\d) ([^\\s]+) is ([^\\s]+) (.*)";
    private final Pattern pattern;
    private final String outputFile;
    private final String inputFile;

    List<Record> sentences = new ArrayList<>();

    @Getter
    private final List<String> sentencesNotAccordingPattern = new ArrayList<>();


    private final KeyToSetMap<String,Integer> nameToIndexMap = new KeyToSetMap();
    private final KeyToSetMap<String,Integer> actionToIndexMap = new KeyToSetMap();
    private final KeyToSetMap<String,Integer> restToIndexMap = new KeyToSetMap();

    HashMap<String, ResultsPerType> keyToResults  = new HashMap<>();


    public PatternSimilarity(String inputFile, String outputFile) {
        this.inputFile = inputFile;
        this.outputFile = outputFile;
        this.pattern = Pattern.compile(patternStr);
    }

    public void printResults(){

        try (BufferedWriter bf = new BufferedWriter(new FileWriter(outputFile))) {
            keyToResults.values().stream().forEach(v->{
                try {
                    bf.write( v.getRecordsStr(sentences));
                    bf.newLine();
                    bf.flush();
                } catch (IOException e) {
                   log.error("Got error while writint record",e);
                }

            });

            if(keyToResults.isEmpty())
                log.warn("No match was found");

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

    public void findSentences1DifferenceAndPrint(){
        findSentensesWith1Differnce();
        printResults();
    }


    @VisibleForTesting
    public HashMap<String, ResultsPerType> findSentensesWith1Differnce(){
        final AtomicBoolean atLeast1Sentence = new AtomicBoolean(false);
        try (Stream<String> stream = Files.lines(Paths.get(inputFile)) ) {
            stream.forEach(s-> {
                atLeast1Sentence.set(true);
                Record currRecord = null;
                try {
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
                    handleRecord(currRecord);

            });

        } catch (IOException e) {
           throw new RuntimeException("There was error during reading the input from file: " + e.getMessage(),e);
        }

        if(!atLeast1Sentence.get())
            log.warn("The input file is empty. No records exists");

        return keyToResults;
    }

    private void handleRecord(Record currRecord) {
        sentences.add(currRecord);
        int index = sentences.size() - 1;

        //take name and try to find same name with same action
        Set<Integer> sentencesWithThisName = nameToIndexMap.get(currRecord.getName());
        Set<Integer> sentensesWithThisAction = actionToIndexMap.get(currRecord.getAction());
        Set<Integer> sentensesWithThisRst = restToIndexMap.get(currRecord.getRestStr());
        boolean foundAction = false;

        //try to find same name with same action
        checkThirdPartAmongIdentical2Parts(currRecord,
                sentencesWithThisName,
                sentensesWithThisAction,
                currRecord.getRestStr(),
                PatternType.REST,
                index
        );

        //try to find same action with same restStr
        checkThirdPartAmongIdentical2Parts(currRecord,
                sentensesWithThisAction,
                sentensesWithThisRst,
                currRecord.getName(),
                PatternType.NAME,
                index);

        //try to find same action with same restStr
        checkThirdPartAmongIdentical2Parts(currRecord,
                sentencesWithThisName,
                sentensesWithThisRst,
                currRecord.getAction(),
                PatternType.ACTION,
                index);


        nameToIndexMap.put(currRecord.getName(), index);
        actionToIndexMap.put(currRecord.getAction(), index);
        restToIndexMap.put(currRecord.getRestStr(), index);
    }

    public static void main(String[] args) {
        if(args== null || args.length == 0){

        }
        String fileName = args[0];
        PatternSimilarity patternSimilarity = new PatternSimilarity(args[0],args[1]);
        patternSimilarity.findSentensesWith1Differnce();

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
            //this is the first time the word exists
            //no match
            return;
        }

        Set<Integer> intersection = new HashSet<>(firstSet);
        intersection.retainAll(secondSet);

        //only in case there are lements in the intersection
        intersection.stream().forEach(i->{
            //if rest does not contains the string then,
            //it brobably the past with singele diggerence
           int otherSentIndex = i;
           Record thatRecord = sentences.get(otherSentIndex);
           String secTestStr = thatRecord.getWordAccorfingType(type);
           //in case it is not identical
           if(!firstTestStr.equals(secTestStr)){
               //make sure the difference in words is only one
               if(is1wordDifferent(firstTestStr,thatRecord.getWordAccorfingType(type))){
                   String key = currRecord.constructKeyAccordingType(type);
                    ResultsPerType currEntry = keyToResults.get(key);

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


    public class SimilarRecords{
        Record firstRecord;
        Record secondRecord;
        PatternType type;

        public SimilarRecords(Record firstRecord, Record secondRecord, PatternType type) {
            this.firstRecord = firstRecord;
            this.secondRecord = secondRecord;
            this.type = type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SimilarRecords that = (SimilarRecords) o;
            return Objects.equals(firstRecord, that.firstRecord) &&
                    Objects.equals(secondRecord, that.secondRecord) &&
                    type == that.type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(firstRecord, secondRecord, type);
        }




    }


}
