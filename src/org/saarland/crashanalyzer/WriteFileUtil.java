package org.saarland.crashanalyzer;

import org.saarland.accidentconstructor.ConsoleLogger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;

public class WriteFileUtil {



    public static void appendToFileAt(String pathToFile, String content)
    {
        try(
            FileWriter fw  = new FileWriter(pathToFile, true);
            BufferedWriter bw  = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw)
        )
        {
            out.write(content);
        }
        catch (Exception ex)
        {
            ConsoleLogger.print('d',"Error at calling WriteFileUtil.appendToFileAt() function, path " + pathToFile);
            ex.printStackTrace();
        }
    }

    public static void writeToFileAt(String pathToFile, String content)
    {
        try(
                FileWriter fw  = new FileWriter(pathToFile, false);
                BufferedWriter bw  = new BufferedWriter(fw);
                PrintWriter out = new PrintWriter(bw)
        )
        {
            out.write(content);
        }
        catch (Exception ex)
        {
            ConsoleLogger.print('d',"Error at calling WriteFileUtil.writeToFileAt() function, path " + pathToFile);
            ex.printStackTrace();
        }
    }

}
