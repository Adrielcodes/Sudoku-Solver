// Same algorithm as src/Sudoku.java: bitmask bookkeeping + "fewest candidates first" backtracking.
// Written as a generator so the page can animate every placement and backtrack.

const ALL = 0b1111111110; // bits 1..9

const boxOf = (i) => Math.floor(i / 27) * 3 + Math.floor((i % 9) / 3);

function masks(cells) {
  const row = new Array(9).fill(0);
  const col = new Array(9).fill(0);
  const box = new Array(9).fill(0);
  for (let i = 0; i < 81; i++) {
    const d = cells[i];
    if (!d) continue;
    const bit = 1 << d;
    if ((row[(i / 9) | 0] | col[i % 9] | box[boxOf(i)]) & bit) {
      return { conflict: i };
    }
    row[(i / 9) | 0] |= bit;
    col[i % 9] |= bit;
    box[boxOf(i)] |= bit;
  }
  return { row, col, box };
}

function bitCount(n) {
  let c = 0;
  while (n) {
    n &= n - 1;
    c++;
  }
  return c;
}

/** Returns the index of a cell that breaks the rules, or -1 if the givens are consistent. */
export function findConflict(cells) {
  const m = masks(cells);
  return m.conflict ?? -1;
}

/**
 * Yields { type: "place" | "remove", index, digit } for every step.
 * The generator's return value is true when solved, false when there is no solution.
 * `cells` (length 81, 0 = blank) is filled in place.
 */
export function* solveSteps(cells) {
  const m = masks(cells);
  if (m.conflict !== undefined) return false;
  const { row, col, box } = m;
  const candidates = (i) => ~(row[(i / 9) | 0] | col[i % 9] | box[boxOf(i)]) & ALL;

  function* backtrack() {
    let best = -1;
    let bestCount = 10;
    for (let i = 0; i < 81; i++) {
      if (cells[i]) continue;
      const count = bitCount(candidates(i));
      if (count === 0) return false;
      if (count < bestCount) {
        best = i;
        bestCount = count;
        if (count === 1) break;
      }
    }
    if (best === -1) return true;

    const options = candidates(best);
    for (let d = 1; d <= 9; d++) {
      if (!(options & (1 << d))) continue;
      const bit = 1 << d;
      cells[best] = d;
      row[(best / 9) | 0] |= bit;
      col[best % 9] |= bit;
      box[boxOf(best)] |= bit;
      yield { type: "place", index: best, digit: d };

      if (yield* backtrack()) return true;

      cells[best] = 0;
      row[(best / 9) | 0] &= ~bit;
      col[best % 9] &= ~bit;
      box[boxOf(best)] &= ~bit;
      yield { type: "remove", index: best, digit: d };
    }
    return false;
  }

  return yield* backtrack();
}

/** Solves instantly. Returns { solved, steps }. */
export function solve(cells) {
  const it = solveSteps(cells);
  let steps = 0;
  let result = it.next();
  while (!result.done) {
    steps++;
    result = it.next();
  }
  return { solved: result.value, steps };
}
