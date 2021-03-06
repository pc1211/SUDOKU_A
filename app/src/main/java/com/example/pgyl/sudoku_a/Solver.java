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
    private SatsNode candidate;
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
        candidate = null;
        resetSolveState();
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
            if (solveState.equals(SOLVE_STATES.SOLUTION_FOUND)) {   //  Pour continuer
                satsNodesHandler.discardLastSolution();
                satsNodesHandler.uncoverRowsAndCols();
                candidate = satsNodesHandler.getNextCandidate();
                if (candidate != null) {
                    solveState = SOLVE_STATES.UNKNOWN;
                } else {
                    solveState = SOLVE_STATES.IMPOSSIBLE;
                }
            }
            while (solveState.equals(SOLVE_STATES.UNKNOWN)) {
                SatsNode colHeader = satsNodesHandler.chooseColumn();
                if (colHeader != null) {
                    if (colHeader.rowCount > 0) {
                        if (candidate != null) {
                            satsNodesHandler.appendSolution(candidate);
                        }
                        satsNodesHandler.setNextCandidates(colHeader);
                    } else {
                        satsNodesHandler.uncoverRowsAndCols();
                    }
                    candidate = satsNodesHandler.getNextCandidate();
                    if (candidate == null) {
                        solveState = SOLVE_STATES.IMPOSSIBLE;
                    }
                } else {
                    satsNodesHandler.appendSolution(candidate);
                    satsNodesHandler.solutionsToCells();
                    solveState = SOLVE_STATES.SOLUTION_FOUND;
                }
            }
        }
        if (mOnSolveEndListener != null) {
            mOnSolveEndListener.onSolveEnd();
        }
    }

}
