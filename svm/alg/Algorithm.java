package alg;

import svm.SVM;
import io.*;

public abstract class Algorithm extends Thread {
    SVM svm;
    public String name;
    public float eta;
    public long P = 10000000;
    public int N;
    public int dim;
    public long t;

    /**
     * Constructor pentru utilizare cu GUI.
     * @param svm referinta la aplicatia principala (nenull)
     */
    public Algorithm(SVM svm) {
        this.svm = svm;
        eta = svm.settings.learning_rate;
        if (svm.ind.ALL != null) {
            N   = svm.ind.V.length;
            dim = svm.ind.V[0].getDimension();
            svm.outd.dataInputFile        = svm.ind.input_file;
            svm.outd.vectors_count        = N;
            svm.outd.attributes_count     = dim;
            svm.outd.max_stages_count     = P;
            svm.control.start.enable(true);
            svm.control.options.enable(true);
        }
    }

    /**
     * Constructor protejat pentru utilizare fara GUI (standalone).
     * Folosit de SMO.createStandalone() — nu acceseaza campurile svm.
     * Parametrul dummy previne conflictul cu constructorul principal.
     * @param dummy parametru fara semnificatie, doar pentru a diferentia constructorii
     */
    protected Algorithm(boolean dummy) {
        this.svm = null; // fara GUI
        this.eta = 0f;
        this.P   = 10000000;
        this.N   = 0;
        this.dim = 0;
    }

    public void suspend_() {}
    public void resume_()  {}
    public void stop_()    {}

    public void start_simulation() {
        svm.design.calculates = true;
        svm.design.repaint();
        start();
    }

    public abstract void run();

    public int getAccuracy(float[] w) {
        int accuracy = 100;
        if (svm != null && svm.ind.T != null && w != null) {
            svm.outd.testing_vectors_count = svm.ind.T.length;
            int hit = 0;
            for (int i = 0; i < svm.ind.T.length; i++) {
                float s = 0;
                for (int j = 0; j < dim; j++)
                    s += w[j] * svm.ind.T[i].X[j];
                s += w[dim];
                int y = 1;
                if (s < 0) y = 0;
                if (y == svm.ind.T[i].cl.Y) hit++;
            }
            accuracy = (hit * 100) / svm.ind.T.length;
        }
        return accuracy;
    }
}
