package com.example.pgyl.sudoku_a;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.example.pgyl.pekislib_a.Constants.ACTIVITY_EXTRA_KEYS;
import com.example.pgyl.pekislib_a.CustomButton;
import com.example.pgyl.pekislib_a.HelpActivity;
import com.example.pgyl.pekislib_a.HelpActivity.HELP_ACTIVITY_EXTRA_KEYS;
import com.example.pgyl.pekislib_a.StringShelfDatabase;

import java.util.logging.Level;
import java.util.logging.Logger;

import static com.example.pgyl.pekislib_a.Constants.BUTTON_STATES;
import static com.example.pgyl.pekislib_a.Constants.COLOR_PREFIX;
import static com.example.pgyl.pekislib_a.Constants.SHP_FILE_NAME_SUFFIX;
import static com.example.pgyl.pekislib_a.HelpActivity.HELP_ACTIVITY_TITLE;
import static com.example.pgyl.pekislib_a.MiscUtils.msgBox;
import static com.example.pgyl.sudoku_a.StringShelfDatabaseUtils.cellRowsToCells;
import static com.example.pgyl.sudoku_a.StringShelfDatabaseUtils.cellsToCellRows;
import static com.example.pgyl.sudoku_a.StringShelfDatabaseUtils.createTableCellsIfNotExists;
import static com.example.pgyl.sudoku_a.StringShelfDatabaseUtils.initializeTableCells;
import static com.example.pgyl.sudoku_a.StringShelfDatabaseUtils.saveCells;
import static com.example.pgyl.sudoku_a.StringShelfDatabaseUtils.tableCellsExists;

public class MainActivity extends Activity {
    //region Constantes
    private enum COMMANDS {
        RESET, RESET_U, SOLVE;

        public int INDEX() {
            return ordinal();
        }
    }

    public enum SUDOKU_SHP_KEY_NAMES {KEEP_SCREEN, SOLVE_STATE, POINTER, EDIT_POINTER}

    public enum SOLVE_STATES {UNKNOWN, SOLUTION_FOUND, IMPOSSIBLE}

    private final int SQUARE_ROWS = 3;
    private final int GRID_ROWS = SQUARE_ROWS * SQUARE_ROWS;
    private final int GRID_SIZE = GRID_ROWS * GRID_ROWS;
    private final int DELETE_DIGIT_KEYBOARD_BUTTON_INDEX = 0;
    private final String DELETE_DIGIT_KEYBOARD_BUTTON_VALUE = "X";
    private final int NO_EDIT_POINTER = -1;
    //endregion
    //region Variables
    private CustomButton[] cellButtons;
    private CustomButton[] keyboardButtons;
    private CustomButton[] commandButtons;
    private Cell[] cells;
    private Solver solver;
    private CellsHandler cellsHandler;
    private Menu menu;
    private MenuItem barMenuItemKeepScreen;
    private StringShelfDatabase stringShelfDatabase;
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
        setupCellButtons();
        setupKeyboardButtons();
        setupCommandButtons();
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
        setupCellButtonColors();
        setupKeyboardButtonColors();
        setupCommandButtonColors();

        solveState = getSHPSolveState();
        pointer = getSHPPointer();
        editPointer = getSHPEditPointer();
        solver.setPointer(pointer);
        solver.setSolveState(solveState);

        updateDisplayCellButtonTexts();
        updateDisplayCellButtonColors();
        updateDisplayCommandButtonColors();
        updateDisplayKeepScreen();
        invalidateOptionsMenu();
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

    private void onCellButtonClick(int index) {
        if (editPointer != NO_EDIT_POINTER) {
            if (index != editPointer) {
                int oldEditPointer = editPointer;
                editPointer = index;
                updateDisplayCellButtonColor(oldEditPointer);
            } else {
                editPointer = NO_EDIT_POINTER;
            }
        } else {
            editPointer = index;
        }
        updateDisplayCellButtonColor(index);
    }

    private void onKeyboardButtonClick(String input) {
        if (editPointer != NO_EDIT_POINTER) {
            handleCellInput(input);
            updateDisplayCellButtonText(editPointer);
            int oldEditPointer = editPointer;
            editPointer = NO_EDIT_POINTER;
            updateDisplayCellButtonColor(oldEditPointer);     //  Cellule Rouge -> Normal
        }
    }

    private void onCommandButtonClick(COMMANDS command) {
        if ((command.equals(COMMANDS.RESET)) || (command.equals(COMMANDS.RESET_U))) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            String title = ((command.equals(COMMANDS.RESET)) ? "Delete all cells" : "Delete all Unprotected cells");
            builder.setTitle(title);
            builder.setMessage("Are you sure ?");
            builder.setCancelable(false);
            final COMMANDS cmd = command;
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int id) {
                    if (cmd.equals(COMMANDS.RESET)) {
                        cellsHandler.deleteAllCells();
                    }
                    if (cmd.equals(COMMANDS.RESET_U)) {
                        cellsHandler.deleteAllExceptProtectedCells();
                    }
                    cellsHandler.linkCells();
                    solver.reset();
                }
            });
            builder.setNegativeButton("No", null);
            Dialog dialog = builder.create();
            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {    // OK pour modifier UI sous-jacente à la boîte de dialogue
                    updateDisplayCellButtonTexts();
                    updateDisplayCellButtonColors();
                    updateDisplayCommandButtonColors();
                }
            });
            dialog.show();
        }
        if (command.equals(COMMANDS.SOLVE)) {
            solver.solve();
        }
    }

    private void onSolverEnd() {
        updateDisplayCellButtonTexts();
        updateDisplayCommandButtonColors();
    }

    private void handleCellInput(String input) {
        Cell editCell = cells[editPointer];
        editCell.empty();
        cellsHandler.deleteAllExceptProtectedCells();
        cellsHandler.linkCells();
        solver.reset();
        if (!input.equals(DELETE_DIGIT_KEYBOARD_BUTTON_VALUE)) {
            editCell.value = Integer.parseInt(input);
            String errorMsg = reportUniqueCellValue(editCell);
            if (errorMsg.equals("")) {     //  cas pas de remarques
                editCell.protect();
            } else {
                msgBox(errorMsg, this);
                editCell.empty();
            }
            cellsHandler.linkCells();
            solver.reset();
        }
    }

    private String reportUniqueCellValue(Cell cell) {
        String ret = "";
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
            ret = "There is already a " + String.valueOf(cell.value) + " in the same " + ret;
        }
        return ret;
    }

    private void updateDisplayCommandButtonColors() {
        final String SOLUTION_FOUND_UNPRESSED_COLOR = "00B777";   //  Vert
        final String SOLUTION_FOUND_PRESSED_COLOR = "006944";
        final String IMPOSSIBLE_UNPRESSED_COLOR = "FF0000";       //  Rouge
        final String IMPOSSIBLE_PRESSED_COLOR = "940000";

        for (final COMMANDS command : COMMANDS.values()) {
            if (command.equals(COMMANDS.SOLVE)) {
                if (solver.getSolveState().equals(SOLVE_STATES.SOLUTION_FOUND)) {
                    commandButtons[command.INDEX()].setUnpressedColor(SOLUTION_FOUND_UNPRESSED_COLOR);
                    commandButtons[command.INDEX()].setPressedColor(SOLUTION_FOUND_PRESSED_COLOR);
                }
                if (solver.getSolveState().equals(SOLVE_STATES.IMPOSSIBLE)) {
                    commandButtons[command.INDEX()].setUnpressedColor(IMPOSSIBLE_UNPRESSED_COLOR);
                    commandButtons[command.INDEX()].setPressedColor(IMPOSSIBLE_PRESSED_COLOR);
                }
                if (solver.getSolveState().equals(SOLVE_STATES.UNKNOWN)) {
                    commandButtons[command.INDEX()].setUnpressedColor(BUTTON_STATES.UNPRESSED.DEFAULT_COLOR());
                    commandButtons[command.INDEX()].setPressedColor(BUTTON_STATES.PRESSED.DEFAULT_COLOR());
                }
            }
            commandButtons[command.INDEX()].updateColor();
        }
    }

    private void updateDisplayCellButtonColors() {
        for (int i = 0; i <= (GRID_SIZE - 1); i = i + 1) {
            updateDisplayCellButtonColor(i);
        }
    }

    private void updateDisplayCellButtonColor(int index) {
        final String PROTECTED_UNPRESSED_COLOR = "FF9A22";         //  Orange
        final String PROTECTED_PRESSED_COLOR = "995400";
        final String ENABLE_EDIT_UNPRESSED_COLOR = "FF0000";       //  Rouge
        final String ENABLE_EDIT_PRESSED_COLOR = "940000";

        if (index == editPointer) {
            cellButtons[index].setUnpressedColor(ENABLE_EDIT_UNPRESSED_COLOR);
            cellButtons[index].setPressedColor(ENABLE_EDIT_PRESSED_COLOR);
        } else {
            if (!cells[index].isProtected()) {
                cellButtons[index].setUnpressedColor(BUTTON_STATES.UNPRESSED.DEFAULT_COLOR());
                cellButtons[index].setPressedColor(BUTTON_STATES.PRESSED.DEFAULT_COLOR());
            } else {
                cellButtons[index].setUnpressedColor(PROTECTED_UNPRESSED_COLOR);
                cellButtons[index].setPressedColor(PROTECTED_PRESSED_COLOR);
            }
        }
        cellButtons[index].updateColor();
    }

    private void updateDisplayCellButtonTexts() {
        for (int i = 0; i <= (GRID_SIZE - 1); i = i + 1) {
            updateDisplayCellButtonText(i);
        }
    }

    private void updateDisplayCellButtonText(int index) {
        if (cells[index].isEmpty()) {
            cellButtons[index].setText("");
        } else {
            cellButtons[index].setText(String.valueOf(cells[index].value));
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
        return shp.getInt(SUDOKU_SHP_KEY_NAMES.EDIT_POINTER.toString(), -1);
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

    private void setupCellButtons() {
        final String CELL_BUTTON_XML_PREFIX = "BTN_P";
        final long BUTTON_MIN_CLICK_TIME_INTERVAL_MS = 500;

        cellButtons = new CustomButton[GRID_SIZE];
        Class rid = R.id.class;
        for (int i = 0; i <= (GRID_SIZE - 1); i = i + 1) {
            try {
                cellButtons[i] = findViewById(rid.getField(CELL_BUTTON_XML_PREFIX + (i + 1)).getInt(rid));  // BTN_P1, BTN_P2, ...
                cellButtons[i].setMinClickTimeInterval(BUTTON_MIN_CLICK_TIME_INTERVAL_MS);
                final int index = i;
                cellButtons[i].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onCellButtonClick(index);
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

    private void setupKeyboardButtons() {
        final String KEYBOARD_BUTTON_XML_PREFIX = "BTN_K";
        final long BUTTON_MIN_CLICK_TIME_INTERVAL_MS = 500;

        keyboardButtons = new CustomButton[GRID_ROWS + 1];
        Class rid = R.id.class;
        for (int i = 0; i <= (GRID_ROWS); i = i + 1) {   //  10 boutons: X, 1, 2, ... 9
            try {
                keyboardButtons[i] = findViewById(rid.getField(KEYBOARD_BUTTON_XML_PREFIX + (i + 1)).getInt(rid));  // BTN_K1, BTN_K2, ...
                final String value = ((i == DELETE_DIGIT_KEYBOARD_BUTTON_INDEX) ? DELETE_DIGIT_KEYBOARD_BUTTON_VALUE : String.valueOf(i));
                keyboardButtons[i].setText(value);
                keyboardButtons[i].setMinClickTimeInterval(BUTTON_MIN_CLICK_TIME_INTERVAL_MS);
                final int index = i;
                keyboardButtons[i].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onKeyboardButtonClick(value);
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

    private void setupCommandButtons() {
        final String BUTTON_COMMAND_XML_PREFIX = "BTN_";
        final long BUTTON_MIN_CLICK_TIME_INTERVAL_MS = 500;

        commandButtons = new CustomButton[COMMANDS.values().length];
        Class rid = R.id.class;
        for (COMMANDS command : COMMANDS.values())
            try {
                commandButtons[command.INDEX()] = findViewById(rid.getField(BUTTON_COMMAND_XML_PREFIX + command.toString()).getInt(rid));
                commandButtons[command.INDEX()].setMinClickTimeInterval(BUTTON_MIN_CLICK_TIME_INTERVAL_MS);
                final COMMANDS fcommand = command;
                commandButtons[command.INDEX()].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onCommandButtonClick(fcommand);
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

    private void setupCellButtonColors() {
        for (int i = 0; i <= (GRID_SIZE - 1); i = i + 1) {
            cellButtons[i].setUnpressedColor(BUTTON_STATES.UNPRESSED.DEFAULT_COLOR());
            cellButtons[i].setPressedColor(BUTTON_STATES.PRESSED.DEFAULT_COLOR());
        }
    }

    private void setupKeyboardButtonColors() {
        final String DELETE_DIGIT_KEYBOARD_BUTTON_TEXT_COLOR = "FF0000";   //  Rouge

        for (int i = 0; i <= (GRID_ROWS); i = i + 1) {   //  10 boutons
            keyboardButtons[i].setUnpressedColor(BUTTON_STATES.UNPRESSED.DEFAULT_COLOR());
            keyboardButtons[i].setPressedColor(BUTTON_STATES.PRESSED.DEFAULT_COLOR());
        }
        keyboardButtons[DELETE_DIGIT_KEYBOARD_BUTTON_INDEX].setTextColor(Color.parseColor(COLOR_PREFIX + DELETE_DIGIT_KEYBOARD_BUTTON_TEXT_COLOR));
        keyboardButtons[DELETE_DIGIT_KEYBOARD_BUTTON_INDEX].setTypeface(keyboardButtons[DELETE_DIGIT_KEYBOARD_BUTTON_INDEX].getTypeface(), Typeface.BOLD);
    }

    private void setupCommandButtonColors() {
        for (final COMMANDS command : COMMANDS.values()) {
            commandButtons[command.INDEX()].setUnpressedColor(BUTTON_STATES.UNPRESSED.DEFAULT_COLOR());
            commandButtons[command.INDEX()].setPressedColor((BUTTON_STATES.PRESSED.DEFAULT_COLOR()));
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

}