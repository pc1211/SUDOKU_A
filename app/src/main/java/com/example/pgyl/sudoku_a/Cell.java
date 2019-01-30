package com.example.pgyl.sudoku_a;

public class Cell {
    private enum STATUS {
        UNPROTECTED, PROTECTED_TEMPORARILY, PROTECTED_PERMANENTLY;

        public int getValue() {
            return ordinal();
        }
    }

    private final int CELL_DEFAULT_VALUE = 0;

    private int pointer;
    private int value;
    private int status;

    public Cell(int pointer) {
        this.pointer = pointer;
        delete();
    }

    public Cell(int pointer, int value, int status) {
        this.pointer = pointer;
        this.value = value;
        this.status = status;
    }

    public int getPointer() {
        return pointer;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public void delete() {
        setValue(CELL_DEFAULT_VALUE);
        unprotect();
    }

    public boolean isEmpty() {
        return ((value == CELL_DEFAULT_VALUE) && (status == STATUS.UNPROTECTED.getValue()));
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
