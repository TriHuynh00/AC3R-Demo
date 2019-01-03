package org.saarland.evaluation;

import org.saarland.accidentconstructor.ConsoleLogger;
import org.saarland.configparam.FilePathsConfig;
import org.saarland.configparam.FilterCaseConfig;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class UndefinedMMUCCGuidelineFilter {

    private List<String> stopWordList;

    public UndefinedMMUCCGuidelineFilter()
    {
        stopWordList = new ArrayList<String>();
    }

    public void filterCase(String caseName)
    {
        loadStopWordList();
    }

    private void loadStopWordList() {
        try {
            stopWordList = Files.readAllLines(Paths.get(
                    FilterCaseConfig.CATEGORY5_STOPWORD_LIST),
                    Charset.defaultCharset());

            for (String word : stopWordList) {
                ConsoleLogger.print('d', word);
            }
        } catch (Exception ex) {
            ConsoleLogger.print('e', "Cannot load stop word list\n" + ex.toString());
        }
    }

}
