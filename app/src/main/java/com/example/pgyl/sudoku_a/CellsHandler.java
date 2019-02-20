package com.example.pgyl.sudoku_a;

public class CellsHandler {
    //region Variables
    private Cell[] cells;
    private int gridSize;
    private int gridRows;
    private int squareRows;
    //endregion

    public CellsHandler(Cell[] cells) {
        this.cells = cells;
        init();
    }

    private void init() {
        gridSize = cells.length;
        gridRows = (int) Math.sqrt(gridSize);
        squareRows = (int) Math.sqrt(gridRows);
    }

    public void close() {
        cells = null;
    }

    public Cell[] getCells() {
        return cells;
    }

    public int getGridSize() {
        return gridSize;
    }

    public int getGridRows() {
        return gridRows;
    }

    public int getSquareRows() {
        return squareRows;
    }

    public void deleteAllExceptProtectedCells() {
        for (int i = 0; i <= (gridSize - 1); i = i + 1) {
            if (!cells[i].isProtected()) {
                cells[i].empty();
            }
        }
    }

    public void deleteAllCells() {
        for (int i = 0; i <= (gridSize - 1); i = i + 1) {
            cells[i].empty();
        }
    }

    public boolean isValueUniqueInRow(int cellIndex, int cellValue) {
        int row = cellIndex / gridRows;
        for (int i = 0; i <= (gridRows - 1); i = i + 1) {   //  i = n° de colonne dans la grille
            int cind = gridRows * row + i;
            if (cells[cind].value == cellValue) {
                return false;
            }
        }
        return true;
    }

    public boolean isValueUniqueInCol(int cellIndex, int cellValue) {
        int col = cellIndex % gridRows;
        for (int i = 0; i <= (gridRows - 1); i = i + 1) {    //  i = n° de ligne dans la grille
            int cind = i * gridRows + col;
            if (cells[cind].value == cellValue) {
                return false;
            }
        }
        return true;
    }

    public boolean isValueUniqueInSquare(int cellIndex, int cellValue) {
        int row = cellIndex / gridRows;
        int col = cellIndex % gridRows;
        int squareFirstRow = (row / squareRows) * squareRows;      //  n° de la 1e ligne du carré dans la grille
        int squareFirstCol = (col / squareRows) * squareRows;      //  n° de la 1e colonne du carré dans la grille
        for (int i = 0; i <= (squareRows - 1); i = i + 1) {        //  i = n° de ligne dans le carré
            for (int j = 0; j <= (squareRows - 1); j = j + 1) {    //  j = n° de colonne dans le carré
                int cind = (squareFirstRow + i) * gridRows + (squareFirstCol + j);
                if (cells[cind].value == cellValue) {
                    return false;
                }
            }
        }
        return true;
    }

}
