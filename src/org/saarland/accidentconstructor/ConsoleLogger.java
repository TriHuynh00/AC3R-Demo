package org.saarland.accidentconstructor;

import org.saarland.configparam.AccidentParam;

public class ConsoleLogger {
    private static final char DEBUGMODE = 'd';
    private static final char DEBUGNONEWLINE = 'n';
    private static final byte RELEASEMODE = 'r';
    private static final byte ERRORMODE = 'e';
    public static void print(char mode, Object message)
    {
        if (mode == RELEASEMODE)
            System.out.println(message);

        else if (AccidentParam.DEBUG)
        {
            if (mode == DEBUGMODE)
                System.out.println(message);
            else if ( mode == DEBUGNONEWLINE)
                System.out.print(message);
            else if ( mode == ERRORMODE)
                System.err.println(message);
        }
    }

}
