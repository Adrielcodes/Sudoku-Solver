/**
 * A 9x9 Sudoku board with a fast backtracking solver.
 *
 * <p>Each row, column, and 3x3 box keeps a bitmask of the digits it already contains,
 * so checking whether a digit fits is a single bitwise operation instead of a loop.
 * The solver always fills the empty cell with the fewest candidates first
 * ("minimum remaining values"), which cuts the search down dramatically on hard puzzles.
 */
public final class Sudoku {
    public static final int SIZE = 9;
    private static final int CELLS = SIZE * SIZE;
    private static final int ALL_DIGITS = 0b11_1111_1110; // bits 1..9

    private final int[] cells = new int[CELLS];
    private final int[] rowUsed = new int[SIZE];
    private final int[] colUsed = new int[SIZE];
    private final int[] boxUsed = new int[SIZE];
    private long guesses;

    private Sudoku() {
    }

    /**
     * Parses a puzzle from text. Digits 1-9 are givens; '0', '.' or '_' are blanks.
     * Whitespace, grid separators (| - +) and lines starting with '#' are ignored,
     * so both "53..7...." one-liners and pretty-printed grids work.
     *
     * @throws IllegalArgumentException if the text isn't 81 cells or the givens break the rules
     */
    public static Sudoku parse(String text) {
        Sudoku board = new Sudoku();
        int index = 0;
        for (String line : text.split("\\R")) {
            if (line.strip().startsWith("#")) {
                continue;
            }
            for (char ch : line.toCharArray()) {
                if (Character.isWhitespace(ch) || ch == '|' || ch == '-' || ch == '+') {
                    continue;
                }
                if (index >= CELLS) {
                    throw new IllegalArgumentException("Puzzle has more than 81 cells");
                }
                if (ch == '0' || ch == '.' || ch == '_') {
                    index++;
                    continue;
                }
                if (ch < '1' || ch > '9') {
                    throw new IllegalArgumentException(
                            "Unexpected character '" + ch + "' at row " + (index / SIZE + 1) + ", column " + (index % SIZE + 1));
                }
                int digit = ch - '0';
                if (!board.canPlace(index, digit)) {
                    throw new IllegalArgumentException(
                            "Invalid puzzle: " + digit + " appears twice (row " + (index / SIZE + 1) + ", column " + (index % SIZE + 1) + ")");
                }
                board.place(index, digit);
                index++;
            }
        }
        if (index != CELLS) {
            throw new IllegalArgumentException("Puzzle must have 81 cells but found " + index);
        }
        return board;
    }

    /** Solves the puzzle in place. Returns false (leaving the board unchanged) if there is no solution. */
    public boolean solve() {
        return backtrack();
    }

    /**
     * Counts solutions, stopping once {@code limit} is reached. A well-formed puzzle has exactly one,
     * so {@code countSolutions(2) == 1} checks uniqueness. The board itself is not modified.
     */
    public int countSolutions(int limit) {
        return copy().count(limit);
    }

    public int get(int row, int col) {
        return cells[row * SIZE + col];
    }

    public int filledCount() {
        int count = 0;
        for (int value : cells) {
            if (value != 0) {
                count++;
            }
        }
        return count;
    }

    public boolean isSolved() {
        return filledCount() == CELLS;
    }

    /** Number of digits the solver tried placing on the last solve (a rough measure of difficulty). */
    public long guesses() {
        return guesses;
    }

    public Sudoku copy() {
        Sudoku other = new Sudoku();
        System.arraycopy(cells, 0, other.cells, 0, CELLS);
        System.arraycopy(rowUsed, 0, other.rowUsed, 0, SIZE);
        System.arraycopy(colUsed, 0, other.colUsed, 0, SIZE);
        System.arraycopy(boxUsed, 0, other.boxUsed, 0, SIZE);
        return other;
    }

    /** The board as 81 characters, with '.' for blanks. */
    public String toLine() {
        StringBuilder sb = new StringBuilder(CELLS);
        for (int value : cells) {
            sb.append(value == 0 ? '.' : (char) ('0' + value));
        }
        return sb.toString();
    }

    /** A human-friendly grid with 3x3 box separators. */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int row = 0; row < SIZE; row++) {
            if (row > 0 && row % 3 == 0) {
                sb.append("------+-------+------\n");
            }
            for (int col = 0; col < SIZE; col++) {
                if (col > 0 && col % 3 == 0) {
                    sb.append("| ");
                }
                int value = get(row, col);
                sb.append(value == 0 ? '.' : (char) ('0' + value));
                sb.append(col == SIZE - 1 ? "\n" : " ");
            }
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ solver internals

    private boolean backtrack() {
        int cell = mostConstrainedCell();
        if (cell == -1) {
            return true; // no empty cells left
        }
        if (cell == -2) {
            return false; // some empty cell has no legal digit
        }
        int options = candidates(cell);
        for (int digit = 1; digit <= SIZE; digit++) {
            if ((options & (1 << digit)) != 0) {
                place(cell, digit);
                guesses++;
                if (backtrack()) {
                    return true;
                }
                remove(cell, digit);
            }
        }
        return false;
    }

    private int count(int limit) {
        int cell = mostConstrainedCell();
        if (cell == -1) {
            return 1;
        }
        if (cell == -2) {
            return 0;
        }
        int options = candidates(cell);
        int total = 0;
        for (int digit = 1; digit <= SIZE && total < limit; digit++) {
            if ((options & (1 << digit)) != 0) {
                place(cell, digit);
                total += count(limit - total);
                remove(cell, digit);
            }
        }
        return total;
    }

    /** Returns the empty cell with the fewest candidates, -1 if the board is full, or -2 if a cell is stuck. */
    private int mostConstrainedCell() {
        int best = -1;
        int bestCount = SIZE + 1;
        for (int i = 0; i < CELLS; i++) {
            if (cells[i] != 0) {
                continue;
            }
            int count = Integer.bitCount(candidates(i));
            if (count == 0) {
                return -2;
            }
            if (count < bestCount) {
                best = i;
                bestCount = count;
                if (count == 1) {
                    break; // can't do better than a forced move
                }
            }
        }
        return best;
    }

    private int candidates(int i) {
        return ~(rowUsed[i / SIZE] | colUsed[i % SIZE] | boxUsed[box(i)]) & ALL_DIGITS;
    }

    private boolean canPlace(int i, int digit) {
        return (candidates(i) & (1 << digit)) != 0;
    }

    private void place(int i, int digit) {
        int bit = 1 << digit;
        cells[i] = digit;
        rowUsed[i / SIZE] |= bit;
        colUsed[i % SIZE] |= bit;
        boxUsed[box(i)] |= bit;
    }

    private void remove(int i, int digit) {
        int mask = ~(1 << digit);
        cells[i] = 0;
        rowUsed[i / SIZE] &= mask;
        colUsed[i % SIZE] &= mask;
        boxUsed[box(i)] &= mask;
    }

    private static int box(int i) {
        return (i / SIZE / 3) * 3 + (i % SIZE) / 3;
    }
}
