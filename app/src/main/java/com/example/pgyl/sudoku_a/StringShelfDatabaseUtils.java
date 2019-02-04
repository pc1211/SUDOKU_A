package com.example.pgyl.sudoku_a;

import com.example.pgyl.pekislib_a.InputButtonsActivity.KEYBOARDS;
import com.example.pgyl.pekislib_a.StringShelfDatabase;
import com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.ACTIVITY_START_STATUS;
import com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.TABLE_IDS;
import com.example.pgyl.sudoku_a.Constants.SUDOKU_ACTIVITIES;

import static com.example.pgyl.pekislib_a.StringShelfDatabase.TABLE_ID_INDEX;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.getActivityInfosStartStatusIndex;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.getActivityInfosTableName;

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

    private static final String CELL_ID = "CELL";   //  => CELL1, CELL2, CELL3, ...
    //endregion

    //region TABLES
    public static boolean tableCellsExists(StringShelfDatabase stringShelfDatabase) {
        return stringShelfDatabase.tableExists(SUDOKU_TABLES.CELLS.toString());
    }

    public static void createTableCellsIfNotExists(StringShelfDatabase stringShelfDatabase) {
        stringShelfDatabase.createTableIfNotExists(SUDOKU_TABLES.CELLS.toString(), 1 + TABLE_CELLS_DATA_FIELDS.values().length);   //  Champ ID + Données
    }

    public static void initializeTableCells(StringShelfDatabase stringShelfDatabase, int gridSize) {
        final String[][] TABLE_CELLS_INITS = {
                {TABLE_IDS.KEYBOARD.toString(), KEYBOARDS.POSINT.toString(), null},
                {TABLE_IDS.REGEXP.toString(), "[1-9]?", null}   //  0 non accepté, <vide> pour "effacer" la valeur
        };
        String[][] sa = new String[gridSize][];
        for (int i = 0; i <= (gridSize - 1); i = i + 1) {
            sa[i] = cellToCellRow(new Cell(i));
        }
        stringShelfDatabase.insertOrReplaceRows(SUDOKU_TABLES.CELLS.toString(), sa);
        stringShelfDatabase.insertOrReplaceRows(SUDOKU_TABLES.CELLS.toString(), TABLE_CELLS_INITS);
    }

    public static String getCellsTableName() {
        return SUDOKU_TABLES.CELLS.toString();
    }

    public static int getCellValueIndex() {
        return TABLE_CELLS_DATA_FIELDS.VALUE.INDEX();
    }
    //endregion

    //region CELLS
    public static String whereConditionForCells(StringShelfDatabase stringShelfDatabase) {
        return stringShelfDatabase.getFieldName(TABLE_ID_INDEX) + " LIKE '" + CELL_ID + "%'";
    }

    public static String[][] getCells(StringShelfDatabase stringShelfDatabase) {
        return stringShelfDatabase.selectRows(SUDOKU_TABLES.CELLS.toString(), whereConditionForCells(stringShelfDatabase));
    }

    public static void saveCells(StringShelfDatabase stringShelfDatabase, String[][] values) {
        stringShelfDatabase.deleteRows(SUDOKU_TABLES.CELLS.toString(), whereConditionForCells(stringShelfDatabase));
        stringShelfDatabase.insertOrReplaceRows(SUDOKU_TABLES.CELLS.toString(), values);
    }

    public static Cell cellRowToCell(String[] cellRow) {
        return new Cell(
                Integer.parseInt(cellRow[TABLE_ID_INDEX].substring(CELL_ID.length())),   //  Skip "CELL" prefix
                Integer.parseInt(cellRow[TABLE_CELLS_DATA_FIELDS.VALUE.INDEX()]),
                Integer.parseInt(cellRow[TABLE_CELLS_DATA_FIELDS.STATUS.INDEX()]));
    }

    public static String[] cellToCellRow(Cell cell) {
        String[] ret = new String[1 + TABLE_CELLS_DATA_FIELDS.values().length];  //  Champ ID + Données
        ret[TABLE_ID_INDEX] = CELL_ID + String.valueOf(cell.pointer);   //  => CELL1, CELL2, CELL3, ...
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

    //region START_STATUS
    public static boolean isColdStartStatusInMainActivity(StringShelfDatabase stringShelfDatabase) {
        return stringShelfDatabase.selectFieldByIdOrCreate(getActivityInfosTableName(), SUDOKU_ACTIVITIES.MAIN.toString(), getActivityInfosStartStatusIndex()).equals(ACTIVITY_START_STATUS.COLD.toString());
    }

    public static void setStartStatusInMainActivity(StringShelfDatabase stringShelfDatabase, ACTIVITY_START_STATUS activityStartStatus) {
        stringShelfDatabase.insertOrReplaceFieldById(getActivityInfosTableName(), SUDOKU_ACTIVITIES.MAIN.toString(), getActivityInfosStartStatusIndex(), activityStartStatus.toString());
    }
    //endregion

}
