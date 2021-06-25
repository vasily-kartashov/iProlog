package ptarau.iprolog;

public class Main {

    public static void run(final String fileName, boolean streamMode) {
        final var derivedFileName = fileName + ".nl";
        final var program = new Program(derivedFileName);

        prettyPrint("CODE");
        program.prettyPrintCode();

        prettyPrint("RUNNING");
        final var startTime = System.nanoTime();
        if (streamMode) {
            program.stream().map(program::showTerm).forEach(Main::prettyPrint);
        } else {
            program.run();
        }
        final var endTime = System.nanoTime();
        System.out.println("time: " + (endTime - startTime) / 1000000000.0);
    }

    public static void main(final String[] args) {
        String fileName = args[0];
        run(fileName, true);
    }

    public static void prettyPrint(final Object o) {
        System.out.println(o);
    }
}
