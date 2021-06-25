package ptarau.iprolog;

public class Main {

    public static void run(final String fileName) {
        final var p = true;

        final var derivedFileName = fileName + ".nl";
        final var program = new Program(derivedFileName);

        prettyPrint("CODE");
        program.prettyPrintCode();

        prettyPrint("RUNNING");
        final var startTime = System.nanoTime();
        program.run();
        final var endTime = System.nanoTime();
        System.out.println("time: " + (endTime - startTime) / 1000000000.0);
    }

    public static void streamRun(final String fileName) {
        final var derivedFileName = fileName + ".nl";
        final var program = new Program(derivedFileName);

        prettyPrint("CODE");
        program.prettyPrintCode();

        prettyPrint("RUNNING");
        final var startTime = System.nanoTime();
        program.stream().forEach(x -> Main.prettyPrint(program.showTerm(x)));
        final var endTime = System.nanoTime();
        System.out.println("time: " + (endTime - startTime) / 1000000000.0);
    }

    public static void main(final String[] args) {
        String fileName = args[0];
        run(fileName);
    }

    public static void prettyPrint(final Object o) {
        System.out.println(o);
    }
}
