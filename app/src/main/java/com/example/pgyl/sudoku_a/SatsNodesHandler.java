package com.example.pgyl.sudoku_a;

import java.util.ArrayList;
import java.util.Arrays;

public class SatsNodesHandler {

    //region Variables
    private ArrayList<SatsNode> nodes;
    private ArrayList<SatsNode> candidateStack;
    private ArrayList<Integer> solutionStack;
    private SatsNode rootHeader;
    private CellsHandler cellsHandler;
    private boolean[][] sats;
    private Cell[] cells;
    private int gridSize;
    private int gridRows;
    private int squareRows;
    private int level;
    //endregion

    public SatsNodesHandler(CellsHandler cellsHandler) {
        this.cellsHandler = cellsHandler;
        init();
    }

    private void init() {
        cells = cellsHandler.getCells();
        gridSize = cellsHandler.getGridSize();
        gridRows = cellsHandler.getGridRows();
        squareRows = cellsHandler.getSquareRows();
        candidateStack = new ArrayList<SatsNode>();
        solutionStack = new ArrayList<Integer>();
        nodes = new ArrayList<SatsNode>();
    }

    public void close() {
        candidateStack.clear();
        candidateStack = null;
        solutionStack.clear();
        solutionStack = null;
        nodes.clear();
        nodes = null;
        cells = null;
    }

    public void reset() {
        cellsHandler.deleteAllUnprotectedCells();
        nodes.clear();
        candidateStack.clear();
        solutionStack.clear();
        createSatsMatrix();
        mergeCellsIntoSatsMatrix();
        satsMatrixToSatsNodes();
        level = 0;
    }

    public void setNextCandidates(SatsNode colHeader) {
        level = level + 1;
        SatsNode nc = colHeader.down;
        while (!nc.equals(colHeader)) {
            if (nc.rowHeader.coverId == 0) {  //  Ligne non couverte
                nc.level = level;             //  On inscrit le niveau actuel dans le noeud simple
                pushCandidate(nc);
            }
            nc = nc.down;
        }
    }

    public SatsNode getNextCandidate() {
        SatsNode ret = popCandidate();
        if (ret != null) {
            if (ret.level < level) {      //  Le noeud simple date d'un niveau antérieur
                level = ret.level;
                discardLastSolution();    //  Enlever la dernière solution car on doit revenir à un niveau antérieur
                uncoverRowsAndCols();     //  Restaurer parfois plusieurs niveaux en une fois si (level - ret.level) > 1
            }
            coverRowsAndCols(ret);
        }
        return ret;
    }

    public SatsNode chooseColumn() {
        SatsNode ret = null;
        int min = Integer.MAX_VALUE;
        SatsNode ch = rootHeader.right;
        while (!ch.equals(rootHeader)) {
            if (ch.coverId == 0) {      //  Colonne non couverte
                int rowCount = 0;
                SatsNode nc = ch.down;
                while (!nc.equals(ch)) {
                    SatsNode rhc = nc.rowHeader;
                    if (rhc.coverId == 0) {      //  Ligne non couverte
                        rowCount = rowCount + 1;
                    }
                    nc = nc.down;
                }
                ch.rowCount = rowCount;
                if (rowCount < min) {    //  Chercher la colonne avec le minimum de lignes non couvertes
                    min = rowCount;
                    ret = ch;
                    if (min == 0) {     //  Plus bas impossible
                        break;
                    }
                }
            }
            ch = ch.right;
        }
        return ret;
    }

    public void appendSolution(SatsNode satsNode) {
        solutionStack.add(satsNode.rowHeader.satsRow);
    }

    public void discardLastSolution() {
        int size = solutionStack.size();
        if (size > 0) {
            solutionStack.remove(size - 1);
        }
    }

    public void solutionsToCells() {
        int size = solutionStack.size();
        if (size > 0) {
            for (int i = 0; i <= (size - 1); i = i + 1) {
                int satsRow = solutionStack.get(i);
                int row = (satsRow / gridSize);
                int col = (satsRow % gridSize) / gridRows;
                int cellIndex = gridRows * row + col;
                cells[cellIndex].value = (satsRow % gridRows) + 1;
            }
        }
    }

    public void uncoverRowsAndCols() {     //  Découvrir les lignes et colonnes nécessaires pour revenir à un niveau antérieur
        SatsNode rh = rootHeader.down;
        while (!rh.equals(rootHeader)) {
            if (rh.coverId >= level) {
                rh.coverId = 0;            //  Découvrir la ligne
            }
            rh = rh.down;
        }
        SatsNode ch = rootHeader.right;
        while (!ch.equals(rootHeader)) {
            if (ch.coverId >= level) {
                ch.coverId = 0;           //  Découvrir la colonne
            }
            ch = ch.right;
        }
    }

    private void coverRowsAndCols(SatsNode satsNode) {  //  Couvrir les lignes et colonnes liées au noeud
        SatsNode rh = satsNode.rowHeader;
        SatsNode nr = rh.right;
        while (!nr.equals(rh)) {
            SatsNode ch = nr.colHeader;
            if (ch.coverId == 0) {      //  Colonne non couverte
                ch.coverId = level;     //  Couvrir la colonne (en utilisant le niveau actuel comme Id)
                SatsNode nc = ch.down;
                while (!nc.equals(ch)) {
                    SatsNode rhc = nc.rowHeader;
                    if (rhc.coverId == 0) {    //  Ligne non couverte
                        rhc.coverId = level;   //  Couvrir la ligne (en utilisant le niveau actuel comme Id)
                    }
                    nc = nc.down;
                }
            }
            nr = nr.right;
        }
    }

    private void pushCandidate(SatsNode SatsNode) {
        candidateStack.add(SatsNode);
    }

    private SatsNode popCandidate() {
        SatsNode ret = null;
        int size = candidateStack.size();
        if (size > 0) {
            ret = candidateStack.get(size - 1);
            candidateStack.remove(size - 1);
        }
        return ret;
    }

    private void satsMatrixToSatsNodes() {
        int candidates = sats.length;
        int constraints = sats[0].length;

        SatsNode[] colHeaders = new SatsNode[constraints];
        SatsNode[] lastRowNodes = new SatsNode[constraints];
        SatsNode lastColNode = new SatsNode();

        rootHeader = new SatsNode();
        nodes.add(rootHeader);
        rootHeader.right = rootHeader;    //  Pas encore d'entêtes de colonne => L'entête des entêtes pointe vers elle-même
        rootHeader.down = rootHeader;     //  Pas encore d'entêtes de ligne => L'entête des entêtes pointe vers elle-même
        SatsNode lastRowHeader = rootHeader;
        SatsNode lastColHeader = rootHeader;

        for (int i = 0; i <= (candidates - 1); i = i + 1) {
            SatsNode rowHeader = null;
            for (int j = 0; j <= (constraints - 1); j = j + 1) {
                if (sats[i][j]) {
                    SatsNode node = new SatsNode();
                    nodes.add(node);                  //  Création noeud simple
                    if (rowHeader == null) {
                        rowHeader = new SatsNode();   //  Le noeud simple est le 1er de sa ligne => Création Entête de ligne
                        nodes.add(rowHeader);
                        rowHeader.right = node;       //  L'entête de ligne pointe vers le 1er noeud simple de sa ligne
                        rowHeader.satsRow = i;
                        if (rootHeader.down.equals(rootHeader)) {
                            rootHeader.down = rowHeader;   //  L'entête des entêtes pointe vers la 1e entête de ligne
                        }
                    } else {
                        lastColNode.right = node;     //  Le noeud simple précédent (de la même ligne) pointe vers celui-ci
                    }
                    if (colHeaders[j] == null) {
                        colHeaders[j] = new SatsNode();    //  Le noeud simple est le 1er de sa colonne => Création Entête de colonne
                        nodes.add(colHeaders[j]);
                        colHeaders[j].down = node;         //  L'entête de colonne pointe vers le 1er noeud simple de sa colonne
                        if (rootHeader.right.equals(rootHeader)) {
                            rootHeader.right = colHeaders[j];   //  L'entête des entêtes pointe vers la 1e entête de colonne
                        }
                        lastRowNodes[j] = new SatsNode();
                    } else {
                        lastRowNodes[j].down = node;       //  Le noeud simple précédent (de la même colonne) pointe vers celui-ci
                    }
                    node.rowHeader = rowHeader;
                    node.colHeader = colHeaders[j];
                    lastColNode = node;                //  Le dernier noeud simple (de la même ligne) est celui-ci
                    lastRowNodes[j] = node;            //  Le dernier noeud simple (de la même colonne) est celui-ci
                }
            }
            if (rowHeader != null) {
                lastColNode.right = rowHeader;         //  Le dernier noeud simple de la ligne pointe vers l'entête de ligne
                lastRowHeader.down = rowHeader;        //  La précédente entête de ligne pointe vers celle-ci
                lastRowHeader = rowHeader;             //  La dernière entête de ligne est celle-ci
            }
        }
        for (int j = 0; j <= (constraints - 1); j = j + 1) {
            if (colHeaders[j] != null) {
                lastRowNodes[j].down = colHeaders[j];   //  Le dernier noeud simple de la colonne pointe vers l'entête de colonne
                lastColHeader.right = colHeaders[j];    //  La précédente entête de colonne pointe vers celle-ci
                lastColHeader = colHeaders[j];          //  La dernière entête de colonne est celle-ci
            }
        }
        if (lastRowHeader != rootHeader) {
            lastRowHeader.down = rootHeader;            //  La dernière entête de ligne pointe vers l'entête des entêtes
        }
        if (lastColHeader != rootHeader) {
            lastColHeader.right = rootHeader;           //  La dernière entête de colonne pointe vers l'entête des entêtes
        }
        lastColNode = null;
        colHeaders = null;
        lastRowNodes = null;
        sats = null;
    }

    private void mergeCellsIntoSatsMatrix() {
        for (int i = 0; i <= (gridSize - 1); i = i + 1) {
            if (!cells[i].isEmpty()) {
                int r = cellsRowColValueToSatsRow((i / gridRows), (i % gridRows), 0);   //  0 = 1e ligne (pour le chiffre 1)
                for (int k = 0; k <= (gridRows - 1); k = k + 1) {
                    if (k != (cells[i].value - 1)) {
                        Arrays.fill(sats[r + k], false);   //  Ne garder que les contraintes concernant le chiffre cells[i]
                    }
                }
            }
        }
    }

    private void createSatsMatrix() {
        final int CONSTRAINT_TYPES = 4;    //  (RiCj#, Ri#, Ci#, Bi#)

        int candidates = gridSize * gridRows;                  //  729 candidats (R1C1#1 -> R9C9#9)
        int constraints = CONSTRAINT_TYPES * gridSize;         //  81 contraintes x 4 types de contrainte
        sats = new boolean[candidates][constraints];
        int satsCol = 0;
        for (int i = 0; i <= (gridRows - 1); i = i + 1) {
            for (int j = 0; j <= (gridRows - 1); j = j + 1) {
                for (int k = 0; k <= (gridRows - 1); k = k + 1) {
                    sats[cellsRowColValueToSatsRow(i, j, k)][satsCol + 0 * gridSize] = true;  //  RiCj#  valeur unique dans la cellule
                    sats[cellsRowColValueToSatsRow(i, k, j)][satsCol + 1 * gridSize] = true;  //  Ri#  valeur unique dans la ligne
                    sats[cellsRowColValueToSatsRow(k, i, j)][satsCol + 2 * gridSize] = true;  //  Ci#  valeur unique dans la colonne
                    int p = squareRows * (i / squareRows) + (k / squareRows);    //  i = n° de carré dans la grille
                    int q = squareRows * (i % squareRows) + (k % squareRows);    //  k = n° de cellule dans le carré
                    sats[cellsRowColValueToSatsRow(p, q, j)][satsCol + 3 * gridSize] = true;  //  Bi#  valeur unique dans le carré
                }
                satsCol = satsCol + 1;
            }
        }
    }

    private int cellsRowColValueToSatsRow(int cellsRow, int cellsCol, int cellValueIndex) {   //  cellValueIndex cad cell.value - 1
        return (gridSize * cellsRow + gridRows * cellsCol + cellValueIndex);
    }

}
