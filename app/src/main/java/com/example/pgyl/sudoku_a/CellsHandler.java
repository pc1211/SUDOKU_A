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

    public void linkPreviouslyProtectedCell(int index) {
        int i = index;
        while (cells[i].isProtected()) {
            i = i - 1;
        }
        if (i >= 0) {
            cells[i].nextUnprotectedCellIndex = index;
            cells[index].previousUnprotectedCellIndex = i;
        } else {
            firstUnprotectedCellIndex = index;
        }
        i = index;
        while (cells[i].isProtected()) {
            i = i + 1;
        }
        if (i < gridSize) {
            cells[i].previousUnprotectedCellIndex = index;
            cells[index].previousUnprotectedCellIndex = i;
        } else {
            lastUnprotectedCellIndex = index;
        }
    }

    public void unlinkPreviouslyUnprotectedCell(int index) {
        if (index != lastUnprotectedCellIndex) {
            int j = cells[index].nextUnprotectedCellIndex;
            if (index != firstUnprotectedCellIndex) {
                int i = cells[index].previousUnprotectedCellIndex;
                cells[i].nextUnprotectedCellIndex = j;
                cells[j].previousUnprotectedCellIndex = i;
            } else {
                firstUnprotectedCellIndex = j;
            }
        } else {
            if (index != firstUnprotectedCellIndex) {
                int i = cells[index].previousUnprotectedCellIndex;
                lastUnprotectedCellIndex = i;
            } else {
                firstUnprotectedCellIndex = -1;
                lastUnprotectedCellIndex = gridSize;
            }
        }
        cells[index].previousUnprotectedCellIndex = 0;   //  Pas nécessaire mais plus propre
        cells[index].nextUnprotectedCellIndex = 0;
    }

    public void emptyCell(int index) {
        if (cells[index].isProtected()) {
            linkPreviouslyProtectedCell(index);
        }
        cells[index].empty();
    }

    public void deleteAllExceptProtectedCells() {
        for (int i = 0; i <= (gridSize - 1); i = i + 1) {
            if (!cells[i].isProtected()) {
                emptyCell(i);
            }
        }
    }

    public void deleteAllExceptPermanentCells() {
        for (int i = 0; i <= (gridSize - 1); i = i + 1) {
            if (!cells[i].isProtectedPermanently()) {
                emptyCell(i);
            }
        }
    }

    public void deleteAllCells() {
        for (int i = 0; i <= (gridSize - 1); i = i + 1) {
            emptyCell(i);
        }
    }

    public void setTemporaryCellsToPermanent() {
        for (int i = 0; i <= (gridSize - 1); i = i + 1) {
            if (cells[i].isProtectedTemporarily()) {
                cells[i].protectPermanently();
            }
        }
    }

    private void prepareCellsForSolver() {
        for (int i = 0; i <= (gridSize - 1); i = i + 1) {
            cells[i].rowDigitRoomIndex = i / gridRows;   //  (n° de ligne (0..8)) dans la grille
            cells[i].colDigitRoomIndex = i % gridRows + gridRows;     //  (9 + n° de colonne (0..8)) dans la grille
            cells[i].squareDigitRoomIndex = squareRows * ((i / gridRows) / squareRows) + ((i % gridRows) / squareRows) + 2 * gridRows;   //  (18 + n° de carré (0..8)) dans la grille
        }
    }

    private void linkCells() {
        int lastCellIndex = -1;
        firstUnprotectedCellIndex = -1;
        lastUnprotectedCellIndex = gridSize;
        int i = 0;
        do {
            while (cells[i].isProtected()) {
                i = i + 1;
            }
            if (i < gridSize) {
                if (firstUnprotectedCellIndex == -1) {
                    firstUnprotectedCellIndex = i;
                }
                if (lastCellIndex != -1) {
                    cells[lastCellIndex].nextUnprotectedCellIndex = i;
                    cells[i].previousUnprotectedCellIndex = lastCellIndex;
                }
                lastCellIndex = i;
                i = i + 1;
            }
        } while (i < gridSize);
        if (lastCellIndex != -1) {
            lastUnprotectedCellIndex = lastCellIndex;
        }
    }

}
