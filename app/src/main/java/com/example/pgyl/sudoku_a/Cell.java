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
        rowDigitRoomIndex = 0;
        colDigitRoomIndex = 0;
        squareDigitRoomIndex = 0;
        nextUnprotectedCellIndex = 0;
        previousUnprotectedCellIndex = 0;
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
