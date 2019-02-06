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
    private final boolean DIGIT_ROOM_FREE = false;
    //endregion

    //region Variables
    private Cell[] cells;
    private CellsHandler cellsHandler;
    private int pointer;
    private SOLVE_STATES solveState;
    private boolean[][] digitRooms;
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
        digitRooms = null;
        cells = null;
    }

    public void reset() {
        setupDigitRooms();
        populateDigitRooms();
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
        boolean pointerOverflow;

        if (!solveState.equals(SOLVE_STATES.IMPOSSIBLE)) {
            solveState = SOLVE_STATES.UNKNOWN;
            int lastUnprotectedCellIndex = cellsHandler.getLastUnprotectedCellIndex();
            int firstUnprotectedCellIndex = cellsHandler.getFirstUnprotectedCellIndex();
            do {
                pointerOverflow = false;
                Cell currentCell = cells[pointer];
                if (!currentCell.isEmpty()) {
                    freeDigitRoom(currentCell);
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
                    bookDigitRoom(currentCell);
                    if (pointer != lastUnprotectedCellIndex) {
                        pointer = currentCell.nextUnprotectedCellIndex;
                    } else {
                        pointerOverflow = true;
                    }
                } else {
                    currentCell.empty();
                    if (pointer != firstUnprotectedCellIndex) {
                        pointer = currentCell.previousUnprotectedCellIndex;
                    } else {
                        solveState = SOLVE_STATES.IMPOSSIBLE;
                    }
                }
            } while ((!pointerOverflow) && (!solveState.equals(SOLVE_STATES.IMPOSSIBLE)));
            if (pointerOverflow) {
                pointer = lastUnprotectedCellIndex;
                solveState = SOLVE_STATES.SOLUTION_FOUND;
            }
        } else {
            solveState = SOLVE_STATES.UNKNOWN;
        }
        if (mOnSolveEndListener != null) {
            mOnSolveEndListener.onSolveEnd();
        }
    }

    public boolean isCellUniqueInRow(Cell cell) {
        return (!digitRooms[cell.rowDigitRoomIndex][cell.value]);
    }

    public boolean isCellUniqueInColumn(Cell cell) {
        return (!digitRooms[cell.colDigitRoomIndex][cell.value]);
    }

    public boolean isCellUniqueInSquare(Cell cell) {
        return (!digitRooms[cell.squareDigitRoomIndex][cell.value]);
    }

    public void bookDigitRoom(Cell cell) {
        digitRooms[cell.rowDigitRoomIndex][cell.value] = !DIGIT_ROOM_FREE;
        digitRooms[cell.colDigitRoomIndex][cell.value] = !DIGIT_ROOM_FREE;
        digitRooms[cell.squareDigitRoomIndex][cell.value] = !DIGIT_ROOM_FREE;
    }

    public void freeDigitRoom(Cell cell) {
        digitRooms[cell.rowDigitRoomIndex][cell.value] = DIGIT_ROOM_FREE;
        digitRooms[cell.colDigitRoomIndex][cell.value] = DIGIT_ROOM_FREE;
        digitRooms[cell.squareDigitRoomIndex][cell.value] = DIGIT_ROOM_FREE;
    }

    private void setupDigitRooms() {
        digitRooms = new boolean[3 * gridRows][1 + gridRows];
        for (int i = 0; i <= (digitRooms.length - 1); i = i + 1) {   //  En ligne: (n° de ligne (0..8)), (9 + n°de colonne (0..8)) ou (18 + n° de carré (0..8)) dans la grille
            for (int j = 0; j <= (digitRooms[0].length - 1); j = j + 1) {   //  En colonne: (1 + cell.value (1..9))
                digitRooms[i][j] = DIGIT_ROOM_FREE;
            }
        }
    }

    private void populateDigitRooms() {
        for (int i = 0; i <= (gridSize - 1); i = i + 1) {
            if (!cells[i].isEmpty()) {
                bookDigitRoom(cells[i]);
            }
        }
    }

}
