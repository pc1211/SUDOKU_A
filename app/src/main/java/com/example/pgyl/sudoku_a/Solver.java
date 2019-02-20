package com.example.pgyl.sudoku_a;

public class Solver {
    public interface onSolveEndListener {
        void onSolveEnd();
    }

    public void setOnSolveEndListener(onSolveEndListener listener) {
        mOnSolveEndListener = listener;
    }

    private onSolveEndListener mOnSolveEndListener;

    //region Constantes
    public enum SOLVE_STATES {
        UNKNOWN, SOLUTION_FOUND, IMPOSSIBLE
    }
    //endregion

    //region Variables
    private SatsNodesHandler satsNodesHandler;
    private SOLVE_STATES solveState;
    private SatsNode tryRow;
    //endregion

    public Solver(SatsNodesHandler satsNodesHandler) {
        this.satsNodesHandler = satsNodesHandler;
        init();
    }

    private void init() {
        solveState = SOLVE_STATES.UNKNOWN;
    }

    public void close() {
        satsNodesHandler = null;
    }

    public void reset() {
        satsNodesHandler.reset();
        tryRow = null;
        solveState = SOLVE_STATES.UNKNOWN;
    }

    public void resetSolveState() {
        solveState = SOLVE_STATES.UNKNOWN;
    }

    public SOLVE_STATES getSolveState() {
        return solveState;
    }

    public void solve() {
        if (solveState.equals(SOLVE_STATES.IMPOSSIBLE)) {
            reset();
        } else {
            if (solveState.equals(SOLVE_STATES.SOLUTION_FOUND)) {
                satsNodesHandler.discardLastSolution();
                satsNodesHandler.unCover();
                tryRow = satsNodesHandler.getNextCandidate();
                if (tryRow != null) {
                    solveState = SOLVE_STATES.UNKNOWN;
                } else {
                    solveState = SOLVE_STATES.IMPOSSIBLE;
                }
            }
            while (solveState.equals(SOLVE_STATES.UNKNOWN)) {
                SatsNode colHeader = satsNodesHandler.chooseColumn();
                if (colHeader != null) {
                    if (satsNodesHandler.rowCount(colHeader) > 0) {
                        if (tryRow != null) {
                            satsNodesHandler.addSolution(tryRow);
                        }
                        satsNodesHandler.setNextCandidates(colHeader);
                    } else {
                        satsNodesHandler.unCover();
                    }
                    tryRow = satsNodesHandler.getNextCandidate();
                    if (tryRow == null) {
                        solveState = SOLVE_STATES.IMPOSSIBLE;
                    }
                } else {
                    satsNodesHandler.addSolution(tryRow);
                    solveState = SOLVE_STATES.SOLUTION_FOUND;
                }
            }
            satsNodesHandler.satsNodesToCells();
        }
        if (mOnSolveEndListener != null) {
            mOnSolveEndListener.onSolveEnd();
        }
    }

}
