package com.example.pgyl.sudoku_a;

public class CellsHandler {
    //region Variables
    private Cell[] cells;
    private int gridSize;
    private int gridRows;
    private int squareRows;
    private int firstUnprotectedCellIndex;
    private int lastUnprotectedCellIndex;
    //endregion

    public CellsHandler(Cell[] cells) {
        this.cells = cells;
        init();
    }

    private void init() {
        gridSize = cells.length;
        gridRows = (int) Math.sqrt(gridSize);
        squareRows = (int) Math.sqrt(gridRows);
        reset();
    }

    public void close() {
        cells = null;
    }

    public void reset() {
        prepareCellsForSolver();
        linkCells();
    }

    public Cell[] getCells() {
        return cells;
    }

    public int getFirstUnprotectedCellIndex() {
        return firstUnprotectedCellIndex;
    }

    public int getLastUnprotectedCellIndex() {
        return lastUnprotectedCellIndex;
    }

    public void deleteAllExceptProtectedCells() {
        for (int i = 0; i <= (gridSize - 1); i = i + 1) {
            if (!cells[i].isProtected()) {
                cells[i].empty();
            }
        }
        linkCells();
    }

    public void deleteAllExceptProtectedSpecialCells() {
        for (int i = 0; i <= (gridSize - 1); i = i + 1) {
            if (!cells[i].isProtectedSpecial()) {
                cells[i].empty();
            }
        }
        linkCells();
    }

    public void deleteAllCells() {
        for (int i = 0; i <= (gridSize - 1); i = i + 1) {
            cells[i].empty();
        }
        linkCells();
    }

    public void setProtectedNormalCellsToProtectedSpecial() {
        for (int i = 0; i <= (gridSize - 1); i = i + 1) {
            if (cells[i].isProtectedNormal()) {
                cells[i].protectSpecial();
            }
        }
    }

    public void linkCells() {
        boolean isProtected;

        int lastCellIndex = -1;
        firstUnprotectedCellIndex = -1;
        lastUnprotectedCellIndex = -1;
        int i = 0;
        do {
            do {
                isProtected = cells[i].isProtected();
                if (isProtected) {
                    i = i + 1;
                }
            }
            while ((i < gridSize) & (isProtected));
            if (i < gridSize) {
                if (lastCellIndex != -1) {
                    cells[lastCellIndex].nextUnprotectedCellIndex = i;
                    cells[i].previousUnprotectedCellIndex = lastCellIndex;
                } else {
                    firstUnprotectedCellIndex = i;
                }
                lastCellIndex = i;
                i = i + 1;
            }
        } while (i < gridSize);
        if (lastCellIndex != -1) {
            lastUnprotectedCellIndex = lastCellIndex;
        }
    }

    private void prepareCellsForSolver() {
        for (int i = 0; i <= (gridSize - 1); i = i + 1) {
            cells[i].rowDigitRoomIndex = i / gridRows;   //  (n° de ligne (0..8)) dans la grille
            cells[i].colDigitRoomIndex = (i % gridRows) + gridRows;     //  (9 + n° de colonne (0..8)) dans la grille
            cells[i].squareDigitRoomIndex = squareRows * ((i / gridRows) / squareRows) + ((i % gridRows) / squareRows) + 2 * gridRows;   //  (18 + n° de carré (0..8)) dans la grille
        }
    }

}