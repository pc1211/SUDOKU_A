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
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

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
import static com.example.pgyl.pekislib_a.Constants.UNDEFINED;
import static com.example.pgyl.pekislib_a.HelpActivity.HELP_ACTIVITY_TITLE;
import static com.example.pgyl.pekislib_a.MiscUtils.msgBox;
import static com.example.pgyl.sudoku_a.Solver.SOLVE_STATES;
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

    private enum EDIT_TYPES {NONE, CELL, DIGIT}

    private enum SUDOKU_SHP_KEY_NAMES {KEEP_SCREEN, EDIT_TYPE, EDIT_INDEX}

    private final int SQUARE_ROWS = 3;
    private final int GRID_ROWS = SQUARE_ROWS * SQUARE_ROWS;
    private final int GRID_SIZE = GRID_ROWS * GRID_ROWS;
    private final int DELETE_DIGIT_BUTTON_INDEX = 0;
    private final String DELETE_DIGIT_BUTTON_VALUE = "x";
    //endregion
    //region Variables
    private CustomButton[] cellButtons;
    private CustomButton[] digitButtons;
    private CustomButton[] commandButtons;
    private Cell[] cells;
    private Solver solver;
    private CellsHandler cellsHandler;
    private SatsNodesHandler satsNodesHandler;
    private Menu menu;
    private MenuItem barMenuItemKeepScreen;
    private StringShelfDatabase stringShelfDatabase;
    private String shpFileName;
    private boolean keepScreen;
    private EDIT_TYPES editType;
    private int editIndex;
    private boolean needSolverReset;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String ACTIVITY_TITLE = "Sudoku";

        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        getActionBar().setTitle(ACTIVITY_TITLE);
        setContentView(R.layout.activity_main);
        setupCellButtons();
        setupDigitButtons();
        setupCommandButtons();
    }

    @Override
    protected void onPause() {
        super.onPause();

        saveCells(stringShelfDatabase, cellsToCellRows(cells));
        stringShelfDatabase.close();
        stringShelfDatabase = null;
        menu = null;
        savePreferences();
        solver.close();
        solver = null;
        satsNodesHandler.close();
        satsNodesHandler = null;
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
        setupSatsNodesHandler();
        setupSolver();
        setupCellButtonColors();
        setupDigitButtonColors();
        setupCommandButtonColors();

        editType = getSHPEditType();
        editIndex = getSHPEditIndex();
        needSolverReset = true;

        updateDisplayCellButtonTexts();
        updateDisplayCellButtonColors();
        updateDisplayDigitButtonColors();
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
        EDIT_TYPES oldEditType = editType;
        int oldEditIndex = editIndex;
        editType = EDIT_TYPES.CELL;
        editIndex = index;
        if (oldEditType.equals(EDIT_TYPES.CELL)) {  //  Click CELL puis CELL
            if (editIndex != oldEditIndex) {
                updateDisplayCellButtonColor(oldEditIndex);
            } else {
                editType = EDIT_TYPES.NONE;
            }
        }
        if (oldEditType.equals(EDIT_TYPES.DIGIT)) {  //  Click DIGIT puis CELL
            editType = EDIT_TYPES.NONE;
            String input = ((oldEditIndex == DELETE_DIGIT_BUTTON_INDEX) ? DELETE_DIGIT_BUTTON_VALUE : String.valueOf(oldEditIndex));
            cellsHandler.deleteAllUnprotectedCells();
            handleCellInput(index, input);
            updateDisplayDigitButtonColor(oldEditIndex);
            solver.resetSolveState();
            needSolverReset = true;
            updateDisplayCellButtonTexts();
            updateDisplayCommandButtonColors();
        }
        updateDisplayCellButtonColor(editIndex);
    }

    private void onDigitButtonClick(int index) {
        EDIT_TYPES oldEditType = editType;
        int oldEditIndex = editIndex;
        editType = EDIT_TYPES.DIGIT;
        editIndex = index;
        if (oldEditType.equals(EDIT_TYPES.DIGIT)) {   //  Click DIGIT puis DIGIT
            if (editIndex != oldEditIndex) {
                updateDisplayDigitButtonColor(oldEditIndex);
            } else {
                editType = EDIT_TYPES.NONE;
            }
        }
        if (oldEditType.equals(EDIT_TYPES.CELL)) {   //  Click CELL puis DIGIT
            editType = EDIT_TYPES.NONE;
            String input = ((editIndex == DELETE_DIGIT_BUTTON_INDEX) ? DELETE_DIGIT_BUTTON_VALUE : String.valueOf(editIndex));
            cellsHandler.deleteAllUnprotectedCells();
            handleCellInput(oldEditIndex, input);
            updateDisplayCellButtonColor(oldEditIndex);
            solver.resetSolveState();
            needSolverReset = true;
            updateDisplayCellButtonTexts();
            updateDisplayCommandButtonColors();
        }
        updateDisplayDigitButtonColor(editIndex);
    }

    private void onCommandButtonClick(COMMANDS command) {
        if ((command.equals(COMMANDS.RESET)) || (command.equals(COMMANDS.RESET_U))) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            String title = ((command.equals(COMMANDS.RESET)) ? "Delete all cells" : "Delete all unprotected cells");
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
                        cellsHandler.deleteAllUnprotectedCells();
                    }
                    solver.resetSolveState();
                    needSolverReset = true;
                }
            });
            builder.setNegativeButton("No", null);
            Dialog dialog = builder.create();
            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {    // OK pour modifier UI sous-jacente à la boîte de dialogue
                    updateDisplayCellButtonTexts();
                    updateDisplayCellButtonColors();
                    updateDisplayDigitButtonColors();
                    updateDisplayCommandButtonColors();
                }
            });
            dialog.show();
        }
        if (command.equals(COMMANDS.SOLVE)) {
            if (needSolverReset) {
                needSolverReset = false;
                solver.reset();
            }
            solver.solve();
        }
    }

    private void onSolverEnd() {
        updateDisplayCellButtonTexts();
        updateDisplayCommandButtonColors();
    }

    private void handleCellInput(int cellIndex, String input) {
        if (!input.equals(DELETE_DIGIT_BUTTON_VALUE)) {
            int value = Integer.parseInt(input);
            String errorMsg = reportUniqueCellValue(cellIndex, value);
            if (errorMsg.equals("")) {     //  cad pas de remarques
                cells[cellIndex].value = value;
                cells[cellIndex].protect();
            } else {
                msgBox(errorMsg, this);
            }
        } else {
            cells[cellIndex].empty();
        }
    }

    private String reportUniqueCellValue(int cellIndex, int cellValue) {
        String ret = "";
        if (!cellsHandler.isValueUniqueInRow(cellIndex, cellValue)) {
            ret = ret + ((!ret.equals("")) ? " and " : "") + "row";
        }
        if (!cellsHandler.isValueUniqueInCol(cellIndex, cellValue)) {
            ret = ret + ((!ret.equals("")) ? " and " : "") + "column";
        }
        if (!cellsHandler.isValueUniqueInSquare(cellIndex, cellValue)) {
            ret = ret + ((!ret.equals("")) ? " and " : "") + "square";
        }
        if (!ret.equals("")) {
            ret = "There is already a " + String.valueOf(cellValue) + " in the same " + ret;
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
        final String ENABLE_EDIT_UNPRESSED_COLOR = "3366FF";       //  Bleu
        final String ENABLE_EDIT_PRESSED_COLOR = "0033CC";

        if ((editType.equals(EDIT_TYPES.CELL)) && (index == editIndex)) {
            cellButtons[index].setUnpressedColor(ENABLE_EDIT_UNPRESSED_COLOR);
            cellButtons[index].setPressedColor(ENABLE_EDIT_PRESSED_COLOR);
        } else {
            if (cells[index].isProtected()) {
                cellButtons[index].setUnpressedColor(PROTECTED_UNPRESSED_COLOR);
                cellButtons[index].setPressedColor(PROTECTED_PRESSED_COLOR);
            } else {
                cellButtons[index].setUnpressedColor(BUTTON_STATES.UNPRESSED.DEFAULT_COLOR());
                cellButtons[index].setPressedColor(BUTTON_STATES.PRESSED.DEFAULT_COLOR());
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

    private void updateDisplayDigitButtonColors() {
        for (int i = 0; i <= (GRID_ROWS); i = i + 1) {    //  10 boutons: X, 1, 2, ... 9
            updateDisplayDigitButtonColor(i);
        }
    }

    private void updateDisplayDigitButtonColor(int index) {
        final String ENABLE_EDIT_UNPRESSED_COLOR = "3366FF";  //  Bleu
        final String ENABLE_EDIT_PRESSED_COLOR = "0033CC";
        final String NORMAL_UNPRESSED_COLOR = "4D4D4D";    // Gris
        final String NORMAL_PRESSED_COLOR = "999999";

        if ((editType.equals(EDIT_TYPES.DIGIT)) && (index == editIndex)) {
            digitButtons[index].setUnpressedColor(ENABLE_EDIT_UNPRESSED_COLOR);
            digitButtons[index].setPressedColor(ENABLE_EDIT_PRESSED_COLOR);
        } else {
            digitButtons[index].setUnpressedColor(NORMAL_UNPRESSED_COLOR);
            digitButtons[index].setPressedColor(NORMAL_PRESSED_COLOR);
        }
        digitButtons[index].updateColor();
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
        shpEditor.putString(SUDOKU_SHP_KEY_NAMES.EDIT_TYPE.toString(), getSHPEditType().toString());
        shpEditor.putInt(SUDOKU_SHP_KEY_NAMES.EDIT_INDEX.toString(), editIndex);
        shpEditor.putBoolean(SUDOKU_SHP_KEY_NAMES.KEEP_SCREEN.toString(), keepScreen);
        shpEditor.commit();
    }

    private EDIT_TYPES getSHPEditType() {
        SharedPreferences shp = getSharedPreferences(shpFileName, MODE_PRIVATE);
        return EDIT_TYPES.valueOf(shp.getString(SUDOKU_SHP_KEY_NAMES.EDIT_TYPE.toString(), EDIT_TYPES.NONE.toString()));
    }

    private int getSHPEditIndex() {
        SharedPreferences shp = getSharedPreferences(shpFileName, MODE_PRIVATE);
        return shp.getInt(SUDOKU_SHP_KEY_NAMES.EDIT_INDEX.toString(), UNDEFINED);
    }

    private boolean getSHPKeepScreen() {
        final boolean KEEP_SCREEN_DEFAULT_VALUE = false;

        SharedPreferences shp = getSharedPreferences(shpFileName, MODE_PRIVATE);
        return shp.getBoolean(SUDOKU_SHP_KEY_NAMES.KEEP_SCREEN.toString(), KEEP_SCREEN_DEFAULT_VALUE);
    }

    private void setupCellsHandler() {
        cellsHandler = new CellsHandler(cells);
    }

    private void setupSatsNodesHandler() {
        satsNodesHandler = new SatsNodesHandler(cellsHandler);
    }

    private void setupSolver() {
        solver = new Solver(satsNodesHandler);
        solver.setOnSolveEndListener(new Solver.onSolveEndListener() {
            @Override
            public void onSolveEnd() {
                onSolverEnd();
            }
        });
    }

    private void setupCellButtons() {
        final String BIG_LINE_XML_PREFIX = "BIG_LINE_";
        final String SQUARE_XML_PREFIX = "SQUARE_";
        final String SMALL_LINE_XML_PREFIX = "SMALL_LINE_";
        final String BUTTON_XML_PREFIX = "BTN_";
        final long BUTTON_MIN_CLICK_TIME_INTERVAL_MS = 500;

        cellButtons = new CustomButton[GRID_SIZE];
        Class rid = R.id.class;
        for (int i = 0; i <= (SQUARE_ROWS - 1); i = i + 1) {    //  3 grosses lignes dans la grille
            try {
                LinearLayout bigLineLayout = findViewById(rid.getField(BIG_LINE_XML_PREFIX + String.valueOf(i)).getInt(rid));
                for (int j = 0; j <= (SQUARE_ROWS - 1); j = j + 1) {    //  3 carrés par grosse ligne, disposés horizontalement
                    try {
                        LinearLayout squareLayout = bigLineLayout.findViewById(rid.getField(SQUARE_XML_PREFIX + String.valueOf(j)).getInt(rid));
                        for (int k = 0; k <= (SQUARE_ROWS - 1); k = k + 1) {    //  3 petites lignes par carré
                            try {
                                LinearLayout smallLineLayout = squareLayout.findViewById(rid.getField(SMALL_LINE_XML_PREFIX + String.valueOf(k)).getInt(rid));
                                for (int l = 0; l <= (SQUARE_ROWS - 1); l = l + 1) {   //  3 boutons par petite ligne, disposés horizontalement
                                    try {
                                        int cellIndex = i * SQUARE_ROWS * GRID_ROWS + k * GRID_ROWS + j * SQUARE_ROWS + l;
                                        cellButtons[cellIndex] = smallLineLayout.findViewById(rid.getField(BUTTON_XML_PREFIX + String.valueOf(l)).getInt(rid));
                                        cellButtons[cellIndex].setMinClickTimeInterval(BUTTON_MIN_CLICK_TIME_INTERVAL_MS);
                                        final int index = cellIndex;
                                        cellButtons[cellIndex].setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                onCellButtonClick(index);
                                            }
                                        });
                                    } catch (IllegalAccessException e) {
                                        e.printStackTrace();
                                    } catch (NoSuchFieldException e) {
                                        e.printStackTrace();
                                    }
                                }
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            } catch (NoSuchFieldException e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (NoSuchFieldException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }
    }

    private void setupDigitButtons() {
        final String DIGIT_LINE_XML_ID = "DIGIT_LINE";
        final String DIGIT_BUTTON_XML_PREFIX = "BTN_";
        final long BUTTON_MIN_CLICK_TIME_INTERVAL_MS = 500;

        digitButtons = new CustomButton[GRID_ROWS + 1];
        Class rid = R.id.class;
        try {
            LinearLayout digitLineLayout = findViewById(rid.getField(DIGIT_LINE_XML_ID).getInt(rid));
            for (int i = 0; i <= (GRID_ROWS); i = i + 1) {   //  10 boutons: X, 1, 2, ... 9
                try {
                    digitButtons[i] = digitLineLayout.findViewById(rid.getField(DIGIT_BUTTON_XML_PREFIX + String.valueOf(i)).getInt(rid));
                    final String value = ((i == DELETE_DIGIT_BUTTON_INDEX) ? DELETE_DIGIT_BUTTON_VALUE : String.valueOf(i));
                    digitButtons[i].setText(value);
                    digitButtons[i].setMinClickTimeInterval(BUTTON_MIN_CLICK_TIME_INTERVAL_MS);
                    final int index = i;
                    digitButtons[i].setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            onDigitButtonClick(index);
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
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
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
        final int CELL_BUTTON_TEXT_SIZE_SP = 28;

        for (int i = 0; i <= (GRID_SIZE - 1); i = i + 1) {
            cellButtons[i].setTextSize(TypedValue.COMPLEX_UNIT_SP, CELL_BUTTON_TEXT_SIZE_SP);
        }
    }

    private void setupDigitButtonColors() {
        final String DELETE_DIGIT_BUTTON_TEXT_COLOR = "FF0000";   //  Rouge
        final String NORMAL_DIGIT_BUTTON_TEXT_COLOR = "D9D9D9";   //  Gris
        final int DIGIT_BUTTON_TEXT_SIZE_SP = 26;

        for (int i = 0; i <= (GRID_ROWS); i = i + 1) {   //  10 boutons
            digitButtons[i].setTextColor(Color.parseColor(COLOR_PREFIX + ((i == DELETE_DIGIT_BUTTON_INDEX) ? DELETE_DIGIT_BUTTON_TEXT_COLOR : NORMAL_DIGIT_BUTTON_TEXT_COLOR)));
            digitButtons[i].setTextSize(TypedValue.COMPLEX_UNIT_SP, DIGIT_BUTTON_TEXT_SIZE_SP);
            digitButtons[i].setTypeface(digitButtons[i].getTypeface(), Typeface.BOLD);
        }
    }

    private void setupCommandButtonColors() {
        final int COMMAND_BUTTON_TEXT_SIZE_SP = 20;

        for (final COMMANDS command : COMMANDS.values()) {
            commandButtons[command.INDEX()].setTextSize(TypedValue.COMPLEX_UNIT_SP, COMMAND_BUTTON_TEXT_SIZE_SP);
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