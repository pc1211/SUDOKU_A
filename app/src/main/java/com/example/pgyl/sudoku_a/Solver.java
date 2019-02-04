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
    private final int DIGIT_ROOM_FREE = 0;
    private final int DIGIT_ROOM_BOOKED = 1;
    //endregion

    //region Variables
    private Cell[] cells;
    private CellsHandler cellsHandler;
    private int pointer;
    private SOLVE_STATES solveState;
    private int[][] digitRooms;
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
        cells = null;
        cellsHandler = null;
        digitRooms = null;
    }

    public void reset() {
        setupDigitRooms();
        populateDigitRooms();
        pointer = cellsHandler.getFirstUnprotectedCellIndex();
        solveState = SOLVE_STATES.UNKNOWN;
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
        boolean cellIndexOverflow;

        if (!solveState.equals(SOLVE_STATES.IMPOSSIBLE)) {
            solveState = SOLVE_STATES.UNKNOWN;
            do {
                cellIndexOverflow = false;
                if (!cells[pointer].isEmpty()) {
                    freeDigitRoom(pointer);
                }
                do {
                    cellUnique = false;
                    digitOverflow = false;
                    cells[pointer].value = cells[pointer].value + 1;
                    if (cells[pointer].value <= gridRows) {
                        if (isCellUniqueInRow(pointer)) {
                            if (isCellUniqueInColumn(pointer)) {
                                if (isCellUniqueInSquare(pointer)) {
                                    cellUnique = true;
                                }
                            }
                        }
                    } else {
                        digitOverflow = true;
                    }
                } while ((!cellUnique) && (!digitOverflow));
                if (cellUnique) {
                    bookDigitRoom(pointer);
                    if (pointer != cellsHandler.getLastUnprotectedCellIndex()) {
                        pointer = cells[pointer].nextUnprotectedCellIndex;
                    } else {
                        cellIndexOverflow = true;
                    }
                } else {
                    cellsHandler.emptyCell(pointer);
                    if (pointer != cellsHandler.getFirstUnprotectedCellIndex()) {
                        pointer = cells[pointer].previousUnprotectedCellIndex;
                    } else {
                        solveState = SOLVE_STATES.IMPOSSIBLE;
                    }
                }
            } while ((!cellIndexOverflow) && (!solveState.equals(SOLVE_STATES.IMPOSSIBLE)));
            if (cellIndexOverflow) {
                pointer = cellsHandler.getLastUnprotectedCellIndex();
                solveState = SOLVE_STATES.SOLUTION_FOUND;
            }
        } else {
            solveState = SOLVE_STATES.UNKNOWN;
        }
        if (mOnSolveEndListener != null) {
            mOnSolveEndListener.onSolveEnd();
        }
    }

    private void bookDigitRoom(int index) {
        digitRooms[cells[index].rowDigitRoomIndex][cells[index].value] = DIGIT_ROOM_BOOKED;
        digitRooms[cells[index].colDigitRoomIndex][cells[index].value] = DIGIT_ROOM_BOOKED;
        digitRooms[cells[index].squareDigitRoomIndex][cells[index].value] = DIGIT_ROOM_BOOKED;
    }

    private void freeDigitRoom(int index) {
        digitRooms[cells[index].rowDigitRoomIndex][cells[index].value] = DIGIT_ROOM_FREE;
        digitRooms[cells[index].colDigitRoomIndex][cells[index].value] = DIGIT_ROOM_FREE;
        digitRooms[cells[index].squareDigitRoomIndex][cells[index].value] = DIGIT_ROOM_FREE;
    }

    private boolean isCellUniqueInRow(int index) {
        return (digitRooms[cells[index].rowDigitRoomIndex][cells[index].value] == DIGIT_ROOM_FREE);
    }

    private boolean isCellUniqueInColumn(int index) {
        return (digitRooms[cells[index].colDigitRoomIndex][cells[index].value] == DIGIT_ROOM_FREE);
    }

    private boolean isCellUniqueInSquare(int index) {
        return (digitRooms[cells[index].squareDigitRoomIndex][cells[index].value] == DIGIT_ROOM_FREE);
    }

    private void setupDigitRooms() {
        digitRooms = new int[3 * gridRows][1 + gridRows];
        for (int i = 0; i <= (digitRooms.length - 1); i = i + 1) {   //  En ligne: (n° de ligne (0..8)), (9 + n°de colonne (0..8)) ou (18 + n° de carré (0..8)) dans la grille
            for (int j = 0; j <= (digitRooms[0].length - 1); j = j + 1) {   //  En colonne: (1 + cell.value (1..9))
                digitRooms[i][j] = DIGIT_ROOM_FREE;
            }
        }
    }

    private void populateDigitRooms() {
        for (int i = 0; i <= (gridSize - 1); i = i + 1) {
            if (!cells[i].isEmpty()) {
                bookDigitRoom(i);
            }
        }
    }

}
