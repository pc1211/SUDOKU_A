package com.example.pgyl.sudoku_a;

public class Cell {
    //region Constantes
    private enum STATUS {
        UNPROTECTED, PROTECTED;

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
        //   NOP;
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
        return (status == STATUS.PROTECTED.getValue());
    }

    public void unprotect() {
        status = STATUS.UNPROTECTED.getValue();
    }

    public void protect() {
        status = STATUS.PROTECTED.getValue();
    }

}
