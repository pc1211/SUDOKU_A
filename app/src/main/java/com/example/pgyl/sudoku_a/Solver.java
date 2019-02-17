package com.example.pgyl.sudoku_a;

import com.example.pgyl.sudoku_a.MainActivity.SOLVE_STATES;


public class Solver {
    public interface onSolveEndListener {
        void onSolveEnd();
    }

    public void setOnSolveEndListener(onSolveEndListener listener) {
        mOnSolveEndListener = listener;
    }

    private onSolveEndListener mOnSolveEndListener;

    //region Variables
    private SatsNodesHandler satsNodesHandler;
    private SOLVE_STATES solveState;
    private SatsNode pathNode;
    //endregion

    public Solver(SatsNodesHandler satsNodesHandler) {
        this.satsNodesHandler = satsNodesHandler;
        init();
    }

    private void init() {
        pathNode = null;
        solveState = SOLVE_STATES.UNKNOWN;
    }

    public void close() {
        satsNodesHandler = null;
    }

    public void reset() {
        satsNodesHandler.reset();
        init();
    }

    public SOLVE_STATES getSolveState() {
        return solveState;
    }

    public void solve() {
        if (solveState.equals(SOLVE_STATES.SOLUTION_FOUND)) {
            satsNodesHandler.unCover();
            pathNode = satsNodesHandler.getNextPathNode();
            if (pathNode == null) {
                solveState = SOLVE_STATES.IMPOSSIBLE;
            }
        }
        while (solveState.equals(SOLVE_STATES.UNKNOWN)) {
            SatsNode colHeader = satsNodesHandler.chooseColumn();
            if (colHeader != null) {
                if (satsNodesHandler.rowCount(colHeader) > 0) {
                    satsNodesHandler.setNextPathNodes(colHeader);
                } else {
                    satsNodesHandler.unCover();
                }
                pathNode = satsNodesHandler.getNextPathNode();
                if (pathNode == null) {
                    solveState = SOLVE_STATES.IMPOSSIBLE;
                }
            } else {
                solveState = SOLVE_STATES.SOLUTION_FOUND;
            }
        }
        if (solveState.equals(SOLVE_STATES.SOLUTION_FOUND)) {
            satsNodesHandler.satsNodesToCells();
        }
        if (mOnSolveEndListener != null) {
            mOnSolveEndListener.onSolveEnd();
        }
    }

}
