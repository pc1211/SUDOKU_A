package com.example.pgyl.sudoku_a;

public class Cell {
    //region Constantes
    private enum STATUS {
        UNPROTECTED, PROTECTED_TEMPORARILY, PROTECTED_PERMANENTLY;

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
    public int rowDigitRoomIndex;
    public int colDigitRoomIndex;
    public int squareDigitRoomIndex;
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

        rowDigitRoomIndex = INDEX_DEFAULT_VALUE;
        colDigitRoomIndex = INDEX_DEFAULT_VALUE;
        squareDigitRoomIndex = INDEX_DEFAULT_VALUE;
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

    public boolean isProtectedTemporarily() {
        return (status == STATUS.PROTECTED_TEMPORARILY.getValue());
    }

    public boolean isProtectedPermanently() {
        return (status == STATUS.PROTECTED_PERMANENTLY.getValue());
    }

    public void unprotect() {
        status = STATUS.UNPROTECTED.getValue();
    }

    public void protectTemporarily() {
        status = STATUS.PROTECTED_TEMPORARILY.getValue();
    }

    public void protectPermanently() {
        status = STATUS.PROTECTED_PERMANENTLY.getValue();
    }

}
