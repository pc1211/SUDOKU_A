package com.example.pgyl.sudoku_a;

import java.util.ArrayList;
import java.util.Arrays;

public class SatsNodesHandler {

    //region Constantes
    public static final int FREE = 0;
    //endregion

    //region Variables
    private ArrayList<SatsNode> nodes;
    private ArrayList<SatsNode> nodeStack;
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
        gridSize = cellsHandler.getGridSize();
        gridRows = cellsHandler.getGridRows();
        squareRows = cellsHandler.getSquareRows();
        cells = cellsHandler.getCells();
        nodeStack = new ArrayList<SatsNode>();
        nodes = new ArrayList<SatsNode>();
        level = 0;
    }

    public void close() {
        cells = null;
        nodes.clear();
        nodeStack.clear();
        nodes = null;
        nodeStack = null;
    }

    public void reset() {
        nodes.clear();
        nodeStack.clear();
        createCanonicalSatsMatrix();
        cellsToSatsMatrix();
        satsMatrixToSatsNodes();
        level = 0;
    }

    public void setNextPathNodes(SatsNode colHeader) {
        level = level + 1;
        SatsNode nc = colHeader.down;
        while (!nc.equals(colHeader)) {
            nc.level = level;
            push(nc);
            nc = nc.down;
        }
    }

    public SatsNode getNextPathNode() {
        SatsNode ret = pop();
        if (ret != null) {
            if (ret.level < level) {
                level = level - 1;
                unCover();
            }
            cover(ret);
        }
        return ret;
    }

    public SatsNode chooseColumn() {
        SatsNode ret = null;
        int min = Integer.MAX_VALUE;
        SatsNode ch = rootHeader.right;
        do {
            if (ch.level == FREE) {
                int rc = rowCount(ch);
                if (rc < min) {
                    min = rc;
                    ret = ch;
                }
            }
            ch = ch.right;
        } while (!ch.equals(rootHeader));
        return ret;
    }

    public int rowCount(SatsNode colHeader) {
        int ret = 0;
        SatsNode nc = colHeader.down;
        while (!nc.equals(colHeader)) {
            SatsNode rh = nc.rowHeader;
            if (rh.level == FREE) {
                ret = ret + 1;
            }
            nc = nc.down;
        }
        return ret;
    }

    public void cover(SatsNode node) {
        SatsNode rh = node.rowHeader;
        SatsNode nr = rh.right;
        while (!nr.equals(rh)) {
            SatsNode ch = nr.colHeader;
            if (ch.level == FREE) {
                ch.level = level;
                SatsNode nc = ch.down;
                while (!nc.equals(ch)) {
                    SatsNode rhc = nc.rowHeader;
                    if (rhc.level == FREE) {
                        rhc.level = level;
                    }
                    nc = nc.down;
                }
            }
            nr = nr.right;
        }
    }

    public void unCover() {
        SatsNode rh = rootHeader.down;
        do {
            if (rh.level == level) {
                rh.level = FREE;
            }
            rh = rh.down;
        } while (!rh.equals(rootHeader));
        SatsNode ch = rootHeader.right;
        do {
            if (ch.level == level) {
                ch.level = FREE;
            }
            ch = ch.right;
        } while (!ch.equals(rootHeader));
    }

    public void push(SatsNode node) {
        nodeStack.add(node);
    }

    public SatsNode pop() {
        SatsNode ret = null;
        int size = nodeStack.size();
        if (size > 0) {
            ret = nodeStack.get(size - 1);
            nodeStack.remove(size - 1);
        }
        return ret;
    }

    private void satsMatrixToSatsNodes() {

        boolean[][] sats = new boolean[6][7];
        for (int i = 0; i <= (sats.length - 1); i = i + 1) {
            for (int j = 0; j <= (sats[0].length - 1); j = j + 1) {
                sats[i][j] = false;
            }
        }
        sats[0][2] = true;
        sats[0][4] = true;
        sats[0][5] = true;
        sats[1][0] = true;
        sats[1][3] = true;
        sats[1][6] = true;
        sats[2][1] = true;
        sats[2][2] = true;
        sats[2][5] = true;
        sats[3][0] = true;
        sats[3][3] = true;
        sats[4][1] = true;
        sats[4][6] = true;
        sats[5][3] = true;
        sats[5][4] = true;
        sats[5][6] = true;


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
                        rowHeader = new SatsNode();   //  Le noeud simple était le 1er de sa ligne => Création Entête de ligne
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
                        colHeaders[j] = new SatsNode();    //  Le noeud simple était le 1er de sa colonne => Création Entête de colonne
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

    private void cellsToSatsMatrix() {
        for (int i = 0; i <= (gridSize - 1); i = i + 1) {
            int r = cellsRowColToSatsRow((i / 9), (i % 9), 0);   //  0 = 1e ligne (pour le chiffre 1)
            for (int k = 0; k <= (gridRows - 1); k = k + 1) {
                if (k != (cells[i].value - 1)) {
                    Arrays.fill(sats[r + k], false);   //  Ne garder que les contraintes concernant le chiffre cells[i]
                }
            }
        }
    }

    public void satsNodesToCells() {
        SatsNode rh = rootHeader.down;
        do {
            if (rh.level != FREE) {
                int sri = rh.satsRow;
                cells[satsRowToCellIndex(sri)].value = (sri % gridRows) + 1;
            }
            rh = rh.down;
        } while (!rh.equals(rootHeader));
    }

    private void createCanonicalSatsMatrix() {
        final int CONSTRAINT_TYPES = 4;    //  (RiCj#, Ri#, Ci#, Bi#)

        int candidates = gridSize * gridRows;                  //  729 candidats (R1C1#1 -> R9C9#9)
        int constraints = CONSTRAINT_TYPES * gridSize;         //  81 contraintes x 4 types de contrainte
        sats = new boolean[candidates][constraints];
        int satsColIndex = 0;
        for (int i = 0; i <= (gridRows - 1); i = i + 1) {
            for (int j = 0; j <= (gridRows - 1); j = j + 1) {
                for (int k = 0; k <= (gridRows - 1); k = k + 1) {
                    sats[cellsRowColToSatsRow(i, j, k)][satsColIndex + 0 * gridSize] = true;  //  RiCj#  valeur unique dans la cellule
                    sats[cellsRowColToSatsRow(i, k, j)][satsColIndex + 1 * gridSize] = true;  //  Ri#  valeur unique dans la ligne
                    sats[cellsRowColToSatsRow(k, i, j)][satsColIndex + 2 * gridSize] = true;  //  Ci#  valeur unique dans la colonne
                    int p = squareRows * (i / squareRows) + (k / squareRows);    //  i = n° de carré dans la grille
                    int q = squareRows * (i % squareRows) + (k % squareRows);    //  k = n° de cellule dans le carré
                    sats[cellsRowColToSatsRow(p, q, j)][satsColIndex + 3 * gridSize] = true;  //  Bi#  valeur unique dans le carré
                }
                satsColIndex = satsColIndex + 1;
            }
        }
    }

    private int cellsRowColToSatsRow(int cellsRow, int cellsCol, int cellValueIndex) {   //  cellValueIndex cad cell.value - 1
        return (gridSize * cellsRow + gridRows * cellsCol + cellValueIndex);
    }

    private int satsRowToCellIndex(int satsRow) {
        int row = (satsRow / gridSize);
        int col = (satsRow % gridSize) / gridRows;
        return (9 * row + col);
    }

}
