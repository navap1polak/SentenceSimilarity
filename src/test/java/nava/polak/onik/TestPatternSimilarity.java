package nava.polak.onik;

import lombok.extern.slf4j.Slf4j;
import nava.polak.onik.PatternSimilarity;
import nava.polak.onik.model.ResultsPerType;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.testng.FileAssert.fail;

/**
 * Tests for SentenceSimilarityclass.
 * The test name is self explanatory
 */
@Slf4j
public class TestPatternSimilarity {
    String DIR_PATH = System.getProperty("user.dir") + File.separator + "output";

    String[] EXAMPLE_1 = new String[]{"01-01-2012 19:45:00 Naomi is getting into the car",
            "01-01-2012 20:12:39 Naomi is eating at a restaurant",
            "02-01-2012 09:13:15 George is getting into the car",
            "02-01-2012 10:14:00 George is eating at a diner",
            "03-01-2012 10:14:00 Naomi is eating at a diner"};

    String[] EXPECTED_RESULTS = new String[]{ "01-01-2012 19:45:00 Naomi is getting into the car",
            "02-01-2012 09:13:15 George is getting into the car",
            "The changing word was: Naomi , George",
            "01-01-2012 20:12:39 Naomi is eating at a restaurant",
            "03-01-2012 10:14:00 Naomi is eating at a diner",
            "The changing word was: at a restaurant , at a diner",
            "02-01-2012 10:14:00 George is eating at a diner",
            "03-01-2012 10:14:00 Naomi is eating at a diner",
            "The changing word was: George , Naomi"};

    //test no line
    //test 1 line
//test input not according pattern
    //test input according but rest change by more than 1 word

    @Test
    public void testSentencesNotAccordingPattern(){
        log.debug("run test testSentencesNotAccordingPattern");
        //the third sentence is not according the pattern
        String [] sentences = new String[]{ "01-01-2012 19:45:00 Naomi is getting into the car",
                "01-01-2012 20:12:39 George is getting into the restaurant",
                "01-01-2012 20:12:40 George and Neomi are getting into the restaurant",
                "01-01-2012 20:12:39 George is getting into the car"};

        //run compare
        String inputFile = "testSentencesNotAccordingPattern";
        String outputFile = DIR_PATH + File.separator + inputFile + "_output.txt";
        String testFilePath = createInputFile(inputFile,sentences);
        PatternSimilarity patternSimilarity = new PatternSimilarity(testFilePath, outputFile);
        HashMap<String, ResultsPerType> results =  patternSimilarity.findSentensesWithSingleDifference();

        //the sentence not according pattern should be in the list of unmatched pattern
        Assert.assertTrue(!patternSimilarity.getSentencesNotAccordingPattern().isEmpty());

        //There should still be results
        Assert.assertTrue(results.size() == 2);
    }

    @Test
    public void testNoLineInInput(){
        log.debug("run test testNoLineInInput");
        String[] examples = new String[]{};
        testScenario("testNoLine",examples,new String[]{});
    }

    @Test
    public void testFullExampleScenario() {
        log.debug("run test testFullExampleScenario");
        testScenario("testFullExampleScenario",EXAMPLE_1,EXPECTED_RESULTS);

    }

    /**
     * The third sentence should match both the first and second  - it will create 2
     * matches
     */
    @Test
    public void testDoubleMatchOf1Sentence(){
        log.debug("run test testDoubleMatchOf1Sentence");
        String[] examples = new String[]{
                "01-01-2012 19:45:00 Naomi is getting into the car",
                "01-01-2012 20:12:39 George is getting into the restaurant",
                "01-01-2012 20:12:39 George is getting into the car"};

        String[] EXPECTED = new String[]{"01-01-2012 19:45:00 Naomi is getting into the car",
                "01-01-2012 20:12:39 George is getting into the car",
                "The changing word was: Naomi , George",
                "01-01-2012 20:12:39 George is getting into the restaurant",
                "01-01-2012 20:12:39 George is getting into the car",
                "The changing word was: into the restaurant , into the car"};

        testScenario("testDoubleMatchOf1Sentence",examples,EXPECTED);
    }

    @Test
    public void testNoMatchScenario(){
        log.debug("run test testNoMatchScenario");
        String[] examples = new String[]{
                "01-01-2012 19:45:00 Naomi is getting into the car",
                "01-01-2012 20:12:39 George is eating apple",
                "01-01-2012 20:12:39 George is getting into the church"};
        testScenario("testNoMatchScenario",examples,new String[]{});
    }

    @Test
    public void testSingleSentenceScenario(){
        log.debug("run test testSingleSentenceScenario");
        String[] examples = new String[]{
                "01-01-2012 19:45:00 Naomi is getting into the car"};
        testScenario("testSingleSentenceScenario",examples,new String[]{});
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testInputFileNotExistsScenario(){
        log.debug("run test testInputFileNotExistsScenario");
        PatternSimilarity patternSimilarity = new PatternSimilarity("c:\\noExistsFile.txt", "c:\\noExistsFile_output.txt");
        patternSimilarity.findSentencesSingleDifferenceAndPrint();
    }

    /**
     *
     * @param inputFile - the file input to create
     * @param data data to write into file
     * @param expected the expected results
     */
    private void testScenario(String inputFile, String[] data,String[] expected) {
        String outputFile = DIR_PATH + File.separator + inputFile + "_output.txt";
        String testFilePath = createInputFile(inputFile,data);
        PatternSimilarity patternSimilarity = new PatternSimilarity(testFilePath, outputFile);
        patternSimilarity.findSentencesSingleDifferenceAndPrint();

        //read file
        try {
            Path filePath = new File(outputFile).toPath();
            Charset charset = Charset.defaultCharset();
            List<String> stringList = Files.readAllLines(filePath, charset);
            String[] stringArray = stringList.toArray(new String[]{});
            for (int index = 0, i = 0; i < stringArray.length; i++) {
                String line = stringArray[i];
                if (line.isEmpty())
                    continue;
                Assert.assertEquals(stringArray[i], expected[index++]);
            }
        } catch (IOException e) {
            fail("Fail to validate results");
        }
    }



    private String createInputFile(String name,String[] data){

        File dirFile = new File(DIR_PATH);
        if (!dirFile.exists())
            dirFile.mkdirs();
        String testFilePath = DIR_PATH + File.separator + name + ".txt";

        try (BufferedWriter bf = new BufferedWriter(new FileWriter(testFilePath))) {
            Arrays.stream(data).forEach(l -> {
                try {
                    bf.write(l);
                    bf.newLine();
                    bf.flush();
                } catch (IOException e) {
                    throw new RuntimeException("Error writing the input file");
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Error writing the input file");
        }
        return testFilePath;
    }
}