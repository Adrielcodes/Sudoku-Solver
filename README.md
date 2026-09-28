# 🧩 Sudoku Solver

[![Java CI](https://github.com/Adrielcodes/Sudoku-Solver/actions/workflows/java.yml/badge.svg)](https://github.com/Adrielcodes/Sudoku-Solver/actions/workflows/java.yml)

A fast Sudoku solver in Java, with an interactive web version that animates the algorithm step by step.

**▶ [Try it in your browser](https://adrielcodes.github.io/Sudoku-Solver/)**

> **This was my first GitHub repo**, a backtracking solver I wrote in 2023 while learning to code in college. I came back to it in 2026 to see how much I'd grown: I made it faster, added input validation and tests, set up CI, and built a web visualizer. The original code is kept in [`original/`](original/SudokuSolver.java) for comparison.

## Features

- Solves any valid 9×9 puzzle, including Arto Inkala's "world's hardest sudoku", in milliseconds
- Accepts puzzles as an 81-character line, a pretty-printed grid, a file, or stdin
- Rejects bad input with clear messages: wrong length, invalid characters, or givens that already break the rules
- Reports puzzles with **no solution** and checks whether the solution is **unique**
- **Web visualizer:** watch every placement and backtrack, with adjustable speed, keyboard navigation, and paste-a-puzzle

## How It Works

The solver uses **backtracking**: place a digit that fits, move to the next cell, and when you get stuck, undo the last choice and try the next digit.

Two improvements over my original version make it much faster:

| | Original (2023) | Now |
|---|---|---|
| Checking whether a digit fits | Loop over the row, column, and box (27 cells) | One bitwise check against a precomputed mask for each row, column, and box |
| Choosing the next cell | The first empty cell | The empty cell with the **fewest** legal digits ("minimum remaining values") |
| Invalid input | Crashes or loops forever | Clear error message |
| Tests | None | 15 tests + GitHub Actions CI |

Filling the most constrained cell first means forced moves happen immediately and dead ends are found early, which cuts the search from millions of steps to thousands on hard puzzles.

## Running It

Requires JDK 17+.

```bash
javac -d out src/*.java

# Pass the puzzle directly (0 or . for blanks)
java -cp out Main 530070000600195000098000060800060003400803001700020006060000280000419005000080079

# ...or from a file
java -cp out Main -f puzzles/hard.txt
```

Example output:

```
Puzzle (21 givens):
8 . . | . . . | . . .
. . 3 | 6 . . | . . .
. 7 . | . 9 . | 2 . .
------+-------+------
...

Solution:
8 1 2 | 7 5 3 | 6 4 9
9 4 3 | 6 8 2 | 1 7 5
6 7 5 | 4 9 1 | 2 8 3
------+-------+------
...

Solved in 12.40 ms with 27,560 guesses.
This puzzle has a unique solution.
```

### Tests

```bash
javac -d out src/*.java test/*.java
java -cp out SudokuTest
```

## Project Structure

```
src/Sudoku.java        Board, parser, and solver
src/Main.java          Command-line interface
test/SudokuTest.java   Dependency-free test suite
puzzles/               Example puzzles
docs/                  Web visualizer (GitHub Pages)
original/              My original 2023 version
```
