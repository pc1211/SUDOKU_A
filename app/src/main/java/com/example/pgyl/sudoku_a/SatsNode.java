package com.example.pgyl.sudoku_a;

public class SatsNode {

    //region Variables
    public SatsNode right;      //  Nnoeud simple ou entête de ligne situé à sa droite
    public SatsNode down;       //  Noeud simple ou entête de colonne situé en-dessous de lui
    public SatsNode rowHeader;  //  Si Noeud simple: Entête de sa ligne
    public SatsNode colHeader;  //  Si Noeud simple: Entête de sa colonne
    public int level;           //  Si Noeud simple: niveau auquel le noeud simple a été retenu comme candidat (pour sa ligne, dans la colonne choisie)
    public int coverId;         //  Si Entête de ligne (ou de colonne): Identifiant utilisé pour la couverture de sa ligne (ou de sa colonne)
    public int satsRow;         //  Si Entête de ligne: Index de ligne dans la matrice-mère (satsMatrix)
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
        coverId = 0;
        satsRow = 0;
    }

}
