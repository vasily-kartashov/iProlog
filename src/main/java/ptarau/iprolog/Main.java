package ptarau.iprolog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(final String[] args) {
        var fileName = args[0];
        run(fileName, true);
    }

    public static void run(final String programName, boolean streamMode) {
        logger.info("Processing [{}]", programName);

        var program = new Program(programName);

        logger.info("Program code");
        program.prettyPrintCode();

        logger.info("Executing program");
        var startTime = System.nanoTime();
        if (streamMode) {
            program.stream().map(program::showTerm).forEach(Main::prettyPrint);
        } else {
            program.run();
        }
        var endTime = System.nanoTime();
        logger.info("Done in {} seconds", (endTime - startTime) / 1000000000.0);
    }

    public static void prettyPrint(final Object o) {
        System.out.println(o);
    }
}
