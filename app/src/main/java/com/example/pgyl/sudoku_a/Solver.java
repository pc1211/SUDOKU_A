package com.example.pgyl.sudoku_a;

import com.example.pgyl.sudoku_a.MainActivity.SOLVE_STATES;

public class Solver {
    public interface onSolveEndListener {
        void onSolveEnd();
    }

    public void setOnSolveEndListener(onSolveEndListener listener) {
        mOnSolveEndListener = listener;
    }

    private onSolveEndListener mOnSolveEndListener;

    //region Constantes
    private final boolean DIGIT_BOX_FREE = false;
    //endregion

    //region Variables
    private Cell[] cells;
    private CellsHandler cellsHandler;
    private int pointer;
    private SOLVE_STATES solveState;
    private boolean[][] digitBoxes;
    private int gridRows;
    private int gridSize;
    //endregion

    public Solver(CellsHandler cellsHandler) {
        this.cellsHandler = cellsHandler;
        init();
    }

    private void init() {
        cells = cellsHandler.getCells();
        gridSize = cells.length;
        gridRows = (int) Math.sqrt(gridSize);
        reset();
    }

    public void close() {
        cellsHandler = null;
        digitBoxes = null;
        cells = null;
    }

    public void reset() {
        setupDigitBoxes();
        populateDigitBoxes();
        pointer = cellsHandler.getFirstUnprotectedCellIndex();
        if ((pointer >= 0) && (pointer < gridSize)) {
            solveState = SOLVE_STATES.UNKNOWN;
        } else {
            solveState = SOLVE_STATES.IMPOSSIBLE;
        }
    }

    public int getPointer() {
        return pointer;
    }

    public void setPointer(int pointer) {
        this.pointer = pointer;
    }

    public SOLVE_STATES getSolveState() {
        return solveState;
    }

    public void setSolveState(SOLVE_STATES solveState) {
        this.solveState = solveState;
    }

    public void solve() {
        boolean cellUnique;
        boolean digitOverflow;

        solveState = SOLVE_STATES.UNKNOWN;
        int lastUnprotectedCellIndex = cellsHandler.getLastUnprotectedCellIndex();
        int firstUnprotectedCellIndex = cellsHandler.getFirstUnprotectedCellIndex();
        do {
            Cell currentCell = cells[pointer];
            if (!currentCell.isEmpty()) {
                freeDigitBox(currentCell);
            }
            do {
                cellUnique = false;
                digitOverflow = false;
                currentCell.value = currentCell.value + 1;
                if (currentCell.value <= gridRows) {
                    if (isCellUniqueInRow(currentCell)) {
                        if (isCellUniqueInColumn(currentCell)) {
                            if (isCellUniqueInSquare(currentCell)) {
                                cellUnique = true;
                            }
                        }
                    }
                } else {
                    digitOverflow = true;
                }
            } while ((!cellUnique) && (!digitOverflow));
            if (cellUnique) {
                bookDigitBox(currentCell);
                if (pointer != lastUnprotectedCellIndex) {
                    pointer = currentCell.nextUnprotectedCellIndex;
                } else {
                    solveState = SOLVE_STATES.SOLUTION_FOUND;
                }
            } else {
                currentCell.empty();
                if (pointer != firstUnprotectedCellIndex) {
                    pointer = currentCell.previousUnprotectedCellIndex;
                } else {
                    solveState = SOLVE_STATES.IMPOSSIBLE;
                }
            }
        } while (solveState.equals(SOLVE_STATES.UNKNOWN));
        if (mOnSolveEndListener != null) {
            mOnSolveEndListener.onSolveEnd();
        }
    }

    public boolean isCellUniqueInRow(Cell cell) {
        return (!digitBoxes[cell.rowDigitBoxIndex][cell.value]);
    }

    public boolean isCellUniqueInColumn(Cell cell) {
        return (!digitBoxes[cell.colDigitBoxIndex][cell.value]);
    }

    public boolean isCellUniqueInSquare(Cell cell) {
        return (!digitBoxes[cell.squareDigitBoxIndex][cell.value]);
    }

    private void bookDigitBox(Cell cell) {
        digitBoxes[cell.rowDigitBoxIndex][cell.value] = !DIGIT_BOX_FREE;
        digitBoxes[cell.colDigitBoxIndex][cell.value] = !DIGIT_BOX_FREE;
        digitBoxes[cell.squareDigitBoxIndex][cell.value] = !DIGIT_BOX_FREE;
    }

    private void freeDigitBox(Cell cell) {
        digitBoxes[cell.rowDigitBoxIndex][cell.value] = DIGIT_BOX_FREE;
        digitBoxes[cell.colDigitBoxIndex][cell.value] = DIGIT_BOX_FREE;
        digitBoxes[cell.squareDigitBoxIndex][cell.value] = DIGIT_BOX_FREE;
    }

    private void setupDigitBoxes() {
        digitBoxes = new boolean[3 * gridRows][1 + gridRows];
        for (int i = 0; i <= (digitBoxes.length - 1); i = i + 1) {   //  En ligne: (n° de ligne (0..8)), (9 + n°de colonne (0..8)) ou (18 + n° de carré (0..8)) dans la grille
            for (int j = 0; j <= (digitBoxes[0].length - 1); j = j + 1) {   //  En colonne: (1 + cell.value (1..9))
                digitBoxes[i][j] = DIGIT_BOX_FREE;
            }
        }
    }

    private void populateDigitBoxes() {
        for (int i = 0; i <= (gridSize - 1); i = i + 1) {
            if (!cells[i].isEmpty()) {
                bookDigitBox(cells[i]);
            }
        }
    }

}
