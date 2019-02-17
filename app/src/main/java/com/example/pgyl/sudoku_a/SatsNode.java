package com.example.pgyl.sudoku_a;

public class SatsNode {

    //region Variables
    public SatsNode right;
    public SatsNode down;
    public SatsNode rowHeader;
    public SatsNode colHeader;
    public int level;         //  Niveau lors duquel soit ce noeud simple a été retenu comme candidat, soit cette entête a vu sa ligne ou colonne couverte
    public int satsRow;       //  Si entête de ligne: Index de ligne dans la matrice-mère
    //endregion

    public SatsNode() {
        init();
    }

    private void init() {
        right = null;
        down = null;
        rowHeader = null;
        colHeader = null;
        level = 0;
        satsRow = 0;
    }

}
