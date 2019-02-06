package com.example.pgyl.sudoku_a;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.example.pgyl.pekislib_a.Constants.ACTIVITY_EXTRA_KEYS;
import com.example.pgyl.pekislib_a.Constants.PEKISLIB_ACTIVITIES;
import com.example.pgyl.pekislib_a.CustomButton;
import com.example.pgyl.pekislib_a.HelpActivity;
import com.example.pgyl.pekislib_a.HelpActivity.HELP_ACTIVITY_EXTRA_KEYS;
import com.example.pgyl.pekislib_a.InputButtonsActivity;
import com.example.pgyl.pekislib_a.StringShelfDatabase;
import com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.ACTIVITY_START_STATUS;
import com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.TABLE_EXTRA_KEYS;

import java.util.logging.Level;
import java.util.logging.Logger;

import static com.example.pgyl.pekislib_a.Constants.BUTTON_STATES;
import static com.example.pgyl.pekislib_a.Constants.SHP_FILE_NAME_SUFFIX;
import static com.example.pgyl.pekislib_a.HelpActivity.HELP_ACTIVITY_TITLE;
import static com.example.pgyl.pekislib_a.MiscUtils.msgBox;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.createTableActivityInfosIfNotExists;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.getCurrentStringInInputButtonsActivity;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.setCurrentStringInInputButtonsActivity;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.setStartStatusInInputButtonsActivity;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.tableActivityInfosExists;
import static com.example.pgyl.sudoku_a.StringShelfDatabaseUtils.cellRowsToCells;
import static com.example.pgyl.sudoku_a.StringShelfDatabaseUtils.cellsToCellRows;
import static com.example.pgyl.sudoku_a.StringShelfDatabaseUtils.createTableCellsIfNotExists;
import static com.example.pgyl.sudoku_a.StringShelfDatabaseUtils.getCellValueIndex;
import static com.example.pgyl.sudoku_a.StringShelfDatabaseUtils.getCellsTableName;
import static com.example.pgyl.sudoku_a.StringShelfDatabaseUtils.initializeTableCells;
import static com.example.pgyl.sudoku_a.StringShelfDatabaseUtils.isColdStartStatusInMainActivity;
import static com.example.pgyl.sudoku_a.StringShelfDatabaseUtils.saveCells;
import static com.example.pgyl.sudoku_a.StringShelfDatabaseUtils.setStartStatusInMainActivity;
import static com.example.pgyl.sudoku_a.StringShelfDatabaseUtils.tableCellsExists;

public class MainActivity extends Activity {
    //region Constantes
    private enum COMMANDS {
        PERMANENT, SOLVE;

        public int INDEX() {
            return ordinal();
        }
    }

    public enum SUDOKU_SHP_KEY_NAMES {KEEP_SCREEN, SOLVE_STATE, POINTER, EDIT_POINTER}

    public enum SOLVE_STATES {UNKNOWN, SOLUTION_FOUND, IMPOSSIBLE}

    private final int SQUARE_ROWS = 3;
    private final int GRID_ROWS = SQUARE_ROWS * SQUARE_ROWS;
    private final int GRID_SIZE = GRID_ROWS * GRID_ROWS;
    //endregion
    //region Variables
    private CustomButton[] buttons;
    private CustomButton[] gridButtons;
    private Cell[] cells;
    private Solver solver;
    private CellsHandler cellsHandler;
    private Menu menu;
    private MenuItem barMenuItemKeepScreen;
    private StringShelfDatabase stringShelfDatabase;
    private boolean validReturnFromCalledActivity;
    private String calledActivity;
    private String shpFileName;
    private boolean keepScreen;
    private SOLVE_STATES solveState;
    private int pointer;
    private int editPointer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String ACTIVITY_TITLE = "Sudoku";

        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        getActionBar().setTitle(ACTIVITY_TITLE);
        setContentView(R.layout.activity_main);
        setupButtons();
        setupGridButtons();
        validReturnFromCalledActivity = false;
    }

    @Override
    protected void onPause() {
        super.onPause();

        saveCells(stringShelfDatabase, cellsToCellRows(cells));
        stringShelfDatabase.close();
        stringShelfDatabase = null;
        menu = null;
        pointer = solver.getPointer();
        solveState = solver.getSolveState();
        savePreferences();
        solver.close();
        solver = null;
        cellsHandler.close();
        cellsHandler = null;
        cells = null;
    }
    //endregion

    @Override
    protected void onResume() {
        super.onResume();

        shpFileName = getPackageName() + "." + getClass().getSimpleName() + SHP_FILE_NAME_SUFFIX;
        keepScreen = getSHPKeepScreen();
        setupStringShelfDatabase();
        cells = cellRowsToCells(StringShelfDatabaseUtils.getCells(stringShelfDatabase));
        setupCellsHandler();
        setupSolver();
        setupButtonColors();
        setupGridButtonColors();

        if (isColdStartStatusInMainActivity(stringShelfDatabase)) {
            setStartStatusInMainActivity(stringShelfDatabase, ACTIVITY_START_STATUS.HOT);
        } else {
            solveState = getSHPSolveState();
            pointer = getSHPPointer();
            editPointer = getSHPEditPointer();
            solver.setPointer(pointer);
            solver.setSolveState(solveState);
            if (validReturnFromCalledActivity) {
                validReturnFromCalledActivity = false;
                if (returnsFromInputButtonsActivity()) {
                    String input = getCurrentStringInInputButtonsActivity(stringShelfDatabase, getCellsTableName(), getCellValueIndex());
                    Cell editCell = cells[editPointer];
                    String errorMsg = "";
                    if (input.length() >= 1) {
                        int newValue = Integer.parseInt(input);
                        errorMsg = testCellContents(editCell, newValue);
                        if (errorMsg.equals("")) {     //  cad pas de remarques
                            editCell.value = newValue;
                            if (!editCell.isProtected()) {
                                editCell.protectTemporarily();
                            }
                        } else {
                            msgBox(errorMsg, this);
                        }
                    } else {
                        editCell.empty();
                    }
                    if (errorMsg.equals("")) {
                        cellsHandler.deleteAllExceptProtectedCells();
                        solver.reset();
                    }
                }
            }
        }
        updateDisplayGridButtonTexts();
        updateDisplayGridButtonColors();
        updateDisplayButtonColors();
        updateDisplayKeepScreen();
        invalidateOptionsMenu();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent returnIntent) {
        validReturnFromCalledActivity = false;
        if (requestCode == PEKISLIB_ACTIVITIES.INPUT_BUTTONS.INDEX()) {
            calledActivity = PEKISLIB_ACTIVITIES.INPUT_BUTTONS.toString();
            if (resultCode == RESULT_OK) {
                validReturnFromCalledActivity = true;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        this.menu = menu;
        setupBarMenuItems();
        updateDisplayKeepScreenBarMenuItemIcon(keepScreen);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {  // appelé par invalideOptionsMenu après changement d'orientation
        updateDisplayKeepScreenBarMenuItemIcon(keepScreen);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.BAR_MENU_ITEM_KEEP_SCREEN) {
            keepScreen = !keepScreen;
            updateDisplayKeepScreen();
            updateDisplayKeepScreenBarMenuItemIcon(keepScreen);
        }
        if ((item.getItemId() == R.id.DELETE_ALL_EXCEPT_PROTECTED) || (item.getItemId() == R.id.DELETE_ALL_EXCEPT_PERMANENT) || (item.getItemId() == R.id.DELETE_ALL)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(item.getTitle());
            builder.setMessage("Are you sure ?");
            builder.setCancelable(false);
            final MenuItem it = item;
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int id) {
                    if (it.getItemId() == R.id.DELETE_ALL_EXCEPT_PROTECTED) {
                        cellsHandler.deleteAllExceptProtectedCells();
                    }
                    if (it.getItemId() == R.id.DELETE_ALL_EXCEPT_PERMANENT) {
                        cellsHandler.deleteAllExceptPermanentCells();
                    }
                    if (it.getItemId() == R.id.DELETE_ALL) {
                        cellsHandler.deleteAllCells();
                    }
                    solver.reset();
                }
            });
            builder.setNegativeButton("No", null);
            Dialog dialog = builder.create();
            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {    // OK pour modifier UI sous-jacente à la boîte de dialogue
                    updateDisplayGridButtonTexts();
                    updateDisplayGridButtonColors();
                    updateDisplayButtonColors();
                }
            });
            dialog.show();
        }
        if (item.getItemId() == R.id.HELP) {
            launchHelpActivity();
            return true;
        }
        if (item.getItemId() == R.id.ABOUT) {
            msgBox("Version: " + BuildConfig.VERSION_NAME, this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onButtonClick(COMMANDS command) {
        if (command.equals(COMMANDS.PERMANENT)) {
            cellsHandler.setTemporaryCellsToPermanent();
            updateDisplayGridButtonColors();
        }
        if (command.equals(COMMANDS.SOLVE)) {
            solver.solve();
        }
    }

    private void onGridButtonClick(int index) {
        editPointer = index;
        setCurrentStringInInputButtonsActivity(stringShelfDatabase, getCellsTableName(), getCellValueIndex(), String.valueOf(cells[editPointer].value));
        launchInputButtonsActivity();
    }

    private void onSolverEnd() {
        updateDisplayGridButtonTexts();
        updateDisplayButtonColors();
    }

    private String testCellContents(Cell cell, int newValue) {
        String ret = "";
        int oldCellValue = cell.value;
        boolean oldEmpty = cell.isEmpty();
        if (!cell.isEmpty()) {
            solver.freeDigitRoom(cell);
        }
        cell.value = newValue;
        if (!solver.isCellUniqueInRow(cell)) {
            ret = ret + ((!ret.equals("")) ? " and " : "") + "row";
        }
        if (!solver.isCellUniqueInColumn(cell)) {
            ret = ret + ((!ret.equals("")) ? " and " : "") + "column";
        }
        if (!solver.isCellUniqueInSquare(cell)) {
            ret = ret + ((!ret.equals("")) ? " and " : "") + "square";
        }
        if (!ret.equals("")) {
            ret = "There is already a " + String.valueOf(newValue) + " in the same " + ret;
        }

        cell.value = oldCellValue;       //  Remettre tout en état
        if (!oldEmpty) {
            solver.bookDigitRoom(cell);
        }
        return ret;
    }

    private void updateDisplayButtonColors() {
        final String SOLUTION_FOUND_UNPRESSED_COLOR = "00B777";   //  Vert
        final String SOLUTION_FOUND_PRESSED_COLOR = "006944";
        final String IMPOSSIBLE_UNPRESSED_COLOR = "FF0000";       //  Rouge
        final String IMPOSSIBLE_PRESSED_COLOR = "940000";

        for (final COMMANDS command : COMMANDS.values()) {
            if (command.equals(COMMANDS.SOLVE)) {
                if (solver.getSolveState().equals(SOLVE_STATES.SOLUTION_FOUND)) {
                    buttons[command.INDEX()].setUnpressedColor(SOLUTION_FOUND_UNPRESSED_COLOR);
                    buttons[command.INDEX()].setPressedColor(SOLUTION_FOUND_PRESSED_COLOR);
                }
                if (solver.getSolveState().equals(SOLVE_STATES.IMPOSSIBLE)) {
                    buttons[command.INDEX()].setUnpressedColor(IMPOSSIBLE_UNPRESSED_COLOR);
                    buttons[command.INDEX()].setPressedColor(IMPOSSIBLE_PRESSED_COLOR);
                }
                if (solver.getSolveState().equals(SOLVE_STATES.UNKNOWN)) {
                    buttons[command.INDEX()].setUnpressedColor(BUTTON_STATES.UNPRESSED.DEFAULT_COLOR());
                    buttons[command.INDEX()].setPressedColor(BUTTON_STATES.PRESSED.DEFAULT_COLOR());
                }
            }
            buttons[command.INDEX()].updateColor();
        }
    }

    private void updateDisplayGridButtonColors() {
        for (int i = 0; i <= (GRID_SIZE - 1); i = i + 1) {
            updateDisplayGridButtonColor(i);
        }
    }

    private void updateDisplayGridButtonColor(int index) {
        final String TEMPORARY_UNPRESSED_COLOR = "FF9A22";            //  Orange
        final String TEMPORARY_PRESSED_COLOR = "995400";
        final String PERMANENT_UNPRESSED_COLOR_DEFAULT = "668CFF";    // Bleu
        final String PERMANENT_PRESSED_COLOR_DEFAULT = "0040FF";

        if (!cells[index].isProtected()) {
            gridButtons[index].setUnpressedColor(BUTTON_STATES.UNPRESSED.DEFAULT_COLOR());
            gridButtons[index].setPressedColor(BUTTON_STATES.PRESSED.DEFAULT_COLOR());
        } else {
            if (cells[index].isProtectedPermanently()) {
                gridButtons[index].setUnpressedColor(PERMANENT_UNPRESSED_COLOR_DEFAULT);
                gridButtons[index].setPressedColor(PERMANENT_PRESSED_COLOR_DEFAULT);
            }
            if (cells[index].isProtectedTemporarily()) {
                gridButtons[index].setUnpressedColor(TEMPORARY_UNPRESSED_COLOR);
                gridButtons[index].setPressedColor(TEMPORARY_PRESSED_COLOR);
            }
        }
        gridButtons[index].updateColor();
    }

    private void updateDisplayGridButtonTexts() {
        for (int i = 0; i <= (GRID_SIZE - 1); i = i + 1) {
            updateDisplayGridButtonText(i);
        }
    }

    private void updateDisplayGridButtonText(int index) {
        if (cells[index].isEmpty()) {
            gridButtons[index].setText("");
        } else {
            gridButtons[index].setText(String.valueOf(cells[index].value));
        }
    }

    private void updateDisplayKeepScreenBarMenuItemIcon(boolean keepScreen) {
        barMenuItemKeepScreen.setIcon((keepScreen ? R.drawable.main_light_on : R.drawable.main_light_off));
    }

    private void updateDisplayKeepScreen() {
        if (keepScreen) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void savePreferences() {
        SharedPreferences shp = getSharedPreferences(shpFileName, MODE_PRIVATE);
        SharedPreferences.Editor shpEditor = shp.edit();
        shpEditor.putString(SUDOKU_SHP_KEY_NAMES.SOLVE_STATE.toString(), solveState.toString());
        shpEditor.putInt(SUDOKU_SHP_KEY_NAMES.POINTER.toString(), pointer);
        shpEditor.putInt(SUDOKU_SHP_KEY_NAMES.EDIT_POINTER.toString(), editPointer);
        shpEditor.putBoolean(SUDOKU_SHP_KEY_NAMES.KEEP_SCREEN.toString(), keepScreen);
        shpEditor.commit();
    }

    private SOLVE_STATES getSHPSolveState() {
        SharedPreferences shp = getSharedPreferences(shpFileName, MODE_PRIVATE);
        return SOLVE_STATES.valueOf(shp.getString(SUDOKU_SHP_KEY_NAMES.SOLVE_STATE.toString(), SOLVE_STATES.UNKNOWN.toString()));
    }

    private int getSHPPointer() {
        SharedPreferences shp = getSharedPreferences(shpFileName, MODE_PRIVATE);
        return shp.getInt(SUDOKU_SHP_KEY_NAMES.POINTER.toString(), 0);
    }

    private int getSHPEditPointer() {
        SharedPreferences shp = getSharedPreferences(shpFileName, MODE_PRIVATE);
        return shp.getInt(SUDOKU_SHP_KEY_NAMES.EDIT_POINTER.toString(), 0);
    }

    private boolean getSHPKeepScreen() {
        final boolean KEEP_SCREEN_DEFAULT_VALUE = false;

        SharedPreferences shp = getSharedPreferences(shpFileName, MODE_PRIVATE);
        return shp.getBoolean(SUDOKU_SHP_KEY_NAMES.KEEP_SCREEN.toString(), KEEP_SCREEN_DEFAULT_VALUE);
    }

    private void setupCellsHandler() {
        cellsHandler = new CellsHandler(cells);
    }

    private void setupSolver() {
        solver = new Solver(cellsHandler);
        solver.setOnSolveEndListener(new Solver.onSolveEndListener() {
            @Override
            public void onSolveEnd() {
                onSolverEnd();
            }
        });
    }

    private void setupButtons() {
        final String BUTTON_COMMAND_XML_PREFIX = "BTN_";
        final long BUTTON_MIN_CLICK_TIME_INTERVAL_MS = 500;

        buttons = new CustomButton[COMMANDS.values().length];
        Class rid = R.id.class;
        for (COMMANDS command : COMMANDS.values())
            try {
                buttons[command.INDEX()] = findViewById(rid.getField(BUTTON_COMMAND_XML_PREFIX + command.toString()).getInt(rid));
                buttons[command.INDEX()].setMinClickTimeInterval(BUTTON_MIN_CLICK_TIME_INTERVAL_MS);
                final COMMANDS fcommand = command;
                buttons[command.INDEX()].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onButtonClick(fcommand);
                    }
                });
            } catch (IllegalAccessException ex) {
                Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NoSuchFieldException ex) {
                Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
            } catch (SecurityException ex) {
                Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
            }
    }

    private void setupGridButtons() {
        final String BUTTON_GRID_XML_PREFIX = "BTN_P";
        final long BUTTON_MIN_CLICK_TIME_INTERVAL_MS = 500;

        gridButtons = new CustomButton[GRID_SIZE];
        Class rid = R.id.class;
        for (int i = 0; i <= (GRID_SIZE - 1); i = i + 1) {
            try {
                gridButtons[i] = findViewById(rid.getField(BUTTON_GRID_XML_PREFIX + (i + 1)).getInt(rid));  // BTN_P1, BTN_P2, ...
                gridButtons[i].setMinClickTimeInterval(BUTTON_MIN_CLICK_TIME_INTERVAL_MS);
                final int index = i;
                gridButtons[i].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onGridButtonClick(index);
                    }
                });
            } catch (IllegalAccessException ex) {
                Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NoSuchFieldException ex) {
                Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
            } catch (SecurityException ex) {
                Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void setupButtonColors() {
        for (final COMMANDS command : COMMANDS.values()) {
            buttons[command.INDEX()].setUnpressedColor(BUTTON_STATES.UNPRESSED.DEFAULT_COLOR());
            buttons[command.INDEX()].setPressedColor((BUTTON_STATES.PRESSED.DEFAULT_COLOR()));
        }
    }

    private void setupGridButtonColors() {
        for (int i = 0; i <= (GRID_SIZE - 1); i = i + 1) {
            gridButtons[i].setUnpressedColor(BUTTON_STATES.UNPRESSED.DEFAULT_COLOR());
            gridButtons[i].setPressedColor(BUTTON_STATES.PRESSED.DEFAULT_COLOR());
        }
    }

    private void setupBarMenuItems() {
        final String BAR_MENU_ITEM_KEEP_SCREEN_NAME = "BAR_MENU_ITEM_KEEP_SCREEN";

        Class rid = R.id.class;
        try {
            barMenuItemKeepScreen = menu.findItem(rid.getField(BAR_MENU_ITEM_KEEP_SCREEN_NAME).getInt(rid));
        } catch (IllegalAccessException ex) {
            Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchFieldException ex) {
            Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void setupStringShelfDatabase() {
        stringShelfDatabase = new StringShelfDatabase(this);
        stringShelfDatabase.open();
        if (!tableActivityInfosExists(stringShelfDatabase)) {
            createTableActivityInfosIfNotExists(stringShelfDatabase);
            setStartStatusInMainActivity(stringShelfDatabase, ACTIVITY_START_STATUS.COLD);
        }
        if (!tableCellsExists(stringShelfDatabase)) {
            createTableCellsIfNotExists(stringShelfDatabase);
            initializeTableCells(stringShelfDatabase, GRID_SIZE);
        }
    }

    private void launchHelpActivity() {
        Intent callingIntent = new Intent(this, HelpActivity.class);
        callingIntent.putExtra(ACTIVITY_EXTRA_KEYS.TITLE.toString(), HELP_ACTIVITY_TITLE);
        callingIntent.putExtra(HELP_ACTIVITY_EXTRA_KEYS.HTML_ID.toString(), R.raw.helpmainactivity);
        startActivity(callingIntent);
    }

    private void launchInputButtonsActivity() {
        setStartStatusInInputButtonsActivity(stringShelfDatabase, ACTIVITY_START_STATUS.COLD);
        Intent callingIntent = new Intent(this, InputButtonsActivity.class);
        callingIntent.putExtra(TABLE_EXTRA_KEYS.TABLE.toString(), getCellsTableName());
        callingIntent.putExtra(TABLE_EXTRA_KEYS.INDEX.toString(), getCellValueIndex());
        callingIntent.putExtra(ACTIVITY_EXTRA_KEYS.TITLE.toString(), "Cell value");
        startActivityForResult(callingIntent, PEKISLIB_ACTIVITIES.INPUT_BUTTONS.INDEX());
    }

    private boolean returnsFromInputButtonsActivity() {
        return (calledActivity.equals(PEKISLIB_ACTIVITIES.INPUT_BUTTONS.toString()));
    }

}