package com.example.pgyl.sudoku_a;

public class Cell {
    //region Constantes
    private enum STATUS {
        UNPROTECTED, PROTECTED_NORMAL, PROTECTED_DISTINCT;

        public int getValue() {
            return ordinal();
        }
    }

    private final int CELL_DEFAULT_VALUE = 0;
    //endregion

    //region Variables
    public int pointer;
    public int value;
    private int status;
    public int rowDigitBoxIndex;
    public int colDigitBoxIndex;
    public int squareDigitBoxIndex;
    public int nextUnprotectedCellIndex;
    public int previousUnprotectedCellIndex;
    //endregion

    public Cell(int pointer) {
        this.pointer = pointer;
        empty();
        init();
    }

    public Cell(int pointer, int value, int status) {
        this.pointer = pointer;
        this.value = value;
        this.status = status;
        init();
    }

    private void init() {
        final int INDEX_DEFAULT_VALUE = 0;

        rowDigitBoxIndex = INDEX_DEFAULT_VALUE;
        colDigitBoxIndex = INDEX_DEFAULT_VALUE;
        squareDigitBoxIndex = INDEX_DEFAULT_VALUE;
        nextUnprotectedCellIndex = INDEX_DEFAULT_VALUE;
        previousUnprotectedCellIndex = INDEX_DEFAULT_VALUE;
    }

    public void empty() {
        value = CELL_DEFAULT_VALUE;
        unprotect();
    }

    public boolean isEmpty() {
        return ((value == CELL_DEFAULT_VALUE) && (!isProtected()));
    }

    public int getStatus() {
        return status;
    }

    public boolean isProtected() {
        return (status != STATUS.UNPROTECTED.getValue());
    }

    public boolean isProtectedNormal() {
        return (status == STATUS.PROTECTED_NORMAL.getValue());
    }

    public boolean isProtectedDistinct() {
        return (status == STATUS.PROTECTED_DISTINCT.getValue());
    }

    public void unprotect() {
        status = STATUS.UNPROTECTED.getValue();
    }

    public void protectNormal() {
        status = STATUS.PROTECTED_NORMAL.getValue();
    }

    public void protectDistinct() {
        status = STATUS.PROTECTED_DISTINCT.getValue();
    }

}
