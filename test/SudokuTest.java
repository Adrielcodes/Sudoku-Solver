import java.util.function.Supplier;

/**
 * Dependency-free test runner, so the project builds with nothing but a JDK.
 * Run:  javac -d out src/*.java test/*.java && java -cp out SudokuTest
 */
public final class SudokuTest {
    private static final String EASY =
            "530070000600195000098000060800060003400803001700020006060000280000419005000080079";
    private static final String EASY_SOLUTION =
            "534678912672195348198342567859761423426853791713924856961537284287419635345286179";
    private static final String HARD = // Arto Inkala's "world's hardest sudoku"
            "800000000003600000070090200050007000000045700000100030001000068008500010090000400";
    private static final String HARD_SOLUTION =
            "812753649943682175675491283154237896369845721287169534521974368438526917796318452";

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        test("solves an easy puzzle", () -> solve(EASY).toLine().equals(EASY_SOLUTION));
        test("solves the 'world's hardest' puzzle", () -> solve(HARD).toLine().equals(HARD_SOLUTION));
        test("solution obeys every rule", () -> isValidSolution(solve(HARD)));
        test("keeps the original givens", () -> {
            Sudoku solved = solve(EASY);
            for (int i = 0; i < 81; i++) {
                char given = EASY.charAt(i);
                if (given != '0' && solved.get(i / 9, i % 9) != given - '0') {
                    return false;
                }
            }
            return true;
        });
        test("parses a pretty-printed grid with comments", () -> Sudoku.parse("""
                # comment line
                5 3 . | . 7 . | . . .
                6 . . | 1 9 5 | . . .
                . 9 8 | . . . | . 6 .
                ------+-------+------
                8 . . | . 6 . | . . 3
                4 . . | 8 . 3 | . . 1
                7 . . | . 2 . | . . 6
                ------+-------+------
                . 6 . | . . . | 2 8 .
                . . . | 4 1 9 | . . 5
                . . . | . 8 . | . 7 9
                """).toLine().equals(EASY.replace('0', '.')));
        test("toString round-trips through parse", () -> {
            Sudoku board = Sudoku.parse(EASY);
            return Sudoku.parse(board.toString()).toLine().equals(board.toLine());
        });
        test("rejects too few cells", () -> throwsError(() -> Sudoku.parse("123")));
        test("rejects too many cells", () -> throwsError(() -> Sudoku.parse(EASY + "1")));
        test("rejects letters", () -> throwsError(() -> Sudoku.parse(EASY.replaceFirst("5", "x"))));
        test("rejects duplicate givens in a row", () -> throwsError(() -> Sudoku.parse("55" + EASY.substring(2))));
        test("reports unsolvable puzzles", () -> {
            // Row 1 needs a 9 in its last cell, but column 9 already has a 9 in row 2
            String unsolvable = "123456780" + "000000009" + "0".repeat(63);
            Sudoku board = Sudoku.parse(unsolvable);
            return !board.solve() && board.toLine().equals(unsolvable.replace('0', '.'));
        });
        test("detects a unique solution", () -> Sudoku.parse(HARD).countSolutions(2) == 1);
        test("detects multiple solutions", () -> Sudoku.parse("0".repeat(81)).countSolutions(2) == 2);
        test("countSolutions doesn't modify the board", () -> {
            Sudoku board = Sudoku.parse(EASY);
            board.countSolutions(2);
            return board.toLine().equals(EASY.replace('0', '.'));
        });
        test("solves an empty board", () -> isValidSolution(solve("0".repeat(81))));

        System.out.printf("%n%d passed, %d failed%n", passed, failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static Sudoku solve(String puzzle) {
        Sudoku board = Sudoku.parse(puzzle);
        if (!board.solve()) {
            throw new AssertionError("expected a solution");
        }
        return board;
    }

    private static boolean isValidSolution(Sudoku board) {
        for (int unit = 0; unit < 9; unit++) {
            int row = 0;
            int col = 0;
            int box = 0;
            for (int k = 0; k < 9; k++) {
                row |= 1 << board.get(unit, k);
                col |= 1 << board.get(k, unit);
                box |= 1 << board.get((unit / 3) * 3 + k / 3, (unit % 3) * 3 + k % 3);
            }
            int allDigits = 0b11_1111_1110;
            if (row != allDigits || col != allDigits || box != allDigits) {
                return false;
            }
        }
        return true;
    }

    private static boolean throwsError(Runnable action) {
        try {
            action.run();
            return false;
        } catch (IllegalArgumentException expected) {
            return true;
        }
    }

    private static void test(String name, Supplier<Boolean> check) {
        boolean ok;
        try {
            ok = check.get();
        } catch (RuntimeException | AssertionError e) {
            ok = false;
            System.out.println("  error: " + e);
        }
        if (ok) {
            passed++;
            System.out.println("PASS  " + name);
        } else {
            failed++;
            System.out.println("FAIL  " + name);
        }
    }
}
