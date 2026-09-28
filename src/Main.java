import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Command-line entry point. */
public final class Main {
    private static final String USAGE = """
            Usage:
              java -cp out Main <puzzle>        81 characters, 0 or . for blanks
              java -cp out Main -f <file>       read the puzzle from a file
              java -cp out Main                 type or pipe the puzzle into stdin

            Example:
              java -cp out Main 530070000600195000098000060800060003400803001700020006060000280000419005000080079
            """;

    public static void main(String[] args) throws IOException {
        String input;
        if (args.length > 0 && (args[0].equals("-h") || args[0].equals("--help"))) {
            System.out.print(USAGE);
            return;
        } else if (args.length == 2 && args[0].equals("-f")) {
            input = Files.readString(Path.of(args[1]));
        } else if (args.length > 0) {
            input = String.join("", args);
        } else {
            System.out.println("Enter the puzzle (9 rows, 0 or . for blanks), then press Ctrl+D (Ctrl+Z then Enter on Windows):");
            input = new String(System.in.readAllBytes(), StandardCharsets.UTF_8);
        }

        Sudoku puzzle;
        try {
            puzzle = Sudoku.parse(input);
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            System.err.print("\n" + USAGE);
            System.exit(1);
            return;
        }

        System.out.println("Puzzle (" + puzzle.filledCount() + " givens):");
        System.out.println(puzzle);

        Sudoku solution = puzzle.copy();
        long start = System.nanoTime();
        boolean solved = solution.solve();
        double millis = (System.nanoTime() - start) / 1_000_000.0;

        if (!solved) {
            System.out.println("No solution exists for this puzzle.");
            System.exit(2);
            return;
        }

        System.out.println("Solution:");
        System.out.println(solution);
        System.out.printf("Solved in %.2f ms with %,d guesses.%n", millis, solution.guesses());
        System.out.println(puzzle.countSolutions(2) == 1
                ? "This puzzle has a unique solution."
                : "Heads up: this puzzle has more than one solution. Showing one of them.");
    }
}
