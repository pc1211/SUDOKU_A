package com.example.pgyl.sudoku_a;

import com.example.pgyl.pekislib_a.StringShelfDatabase;

import static com.example.pgyl.pekislib_a.StringShelfDatabase.TABLE_ID_INDEX;

public class StringShelfDatabaseUtils {
    //region Constantes
    private enum SUDOKU_TABLES {
        CELLS;
    }

    private enum TABLE_CELLS_DATA_FIELDS {
        VALUE, STATUS;

        public int INDEX() {
            return ordinal() + 1;
        }   //  INDEX 0 pour identifiant utilisateur (cellPointer (0..80))
    }
    //endregion

    //region TABLES
    public static boolean tableCellsExists(StringShelfDatabase stringShelfDatabase) {
        return stringShelfDatabase.tableExists(SUDOKU_TABLES.CELLS.toString());
    }

    public static void createTableCellsIfNotExists(StringShelfDatabase stringShelfDatabase) {
        stringShelfDatabase.createTableIfNotExists(SUDOKU_TABLES.CELLS.toString(), 1 + TABLE_CELLS_DATA_FIELDS.values().length);   //  Champ ID + Données
    }

    public static void initializeTableCells(StringShelfDatabase stringShelfDatabase, int gridSize) {
        String[][] sa = new String[gridSize][];
        for (int i = 0; i <= (gridSize - 1); i = i + 1) {
            sa[i] = cellToCellRow(new Cell(i));
        }
        stringShelfDatabase.insertOrReplaceRows(SUDOKU_TABLES.CELLS.toString(), sa);
    }
    //endregion

    //region CELLS
    public static String[][] getCells(StringShelfDatabase stringShelfDatabase) {
        return stringShelfDatabase.selectRows(SUDOKU_TABLES.CELLS.toString(), null);
    }

    public static void saveCells(StringShelfDatabase stringShelfDatabase, String[][] values) {
        stringShelfDatabase.insertOrReplaceRows(SUDOKU_TABLES.CELLS.toString(), values);
    }

    public static Cell cellRowToCell(String[] cellRow) {
        return new Cell(
                Integer.parseInt(cellRow[TABLE_ID_INDEX]),
                Integer.parseInt(cellRow[TABLE_CELLS_DATA_FIELDS.VALUE.INDEX()]),
                Integer.parseInt(cellRow[TABLE_CELLS_DATA_FIELDS.STATUS.INDEX()]));
    }

    public static String[] cellToCellRow(Cell cell) {
        String[] ret = new String[1 + TABLE_CELLS_DATA_FIELDS.values().length];  //  Champ ID + Données
        ret[TABLE_ID_INDEX] = String.valueOf(cell.pointer);
        ret[TABLE_CELLS_DATA_FIELDS.VALUE.INDEX()] = String.valueOf(cell.value);
        ret[TABLE_CELLS_DATA_FIELDS.STATUS.INDEX()] = String.valueOf(cell.getStatus());
        return ret;
    }

    public static Cell[] cellRowsToCells(String[][] cellRows) {
        Cell cell;

        Cell[] ret = new Cell[cellRows.length];
        if (cellRows != null) {
            for (int i = 0; i <= (cellRows.length - 1); i = i + 1) {
                cell = cellRowToCell(cellRows[i]);
                ret[cell.pointer] = cell;    //   la position de cell dans ret doit être celle fixée par cell.getPointer()
            }
            cell = null;
        }
        return ret;
    }

    public static String[][] cellsToCellRows(Cell[] cells) {
        String[][] ret = null;
        if (cells.length != 0) {
            ret = new String[cells.length][];
            for (int i = 0; i <= (cells.length - 1); i = i + 1) {
                ret[i] = cellToCellRow(cells[i]);
            }
        }
        return ret;
    }
    //endregion

}
