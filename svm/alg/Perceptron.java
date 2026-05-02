package alg;

import svm.SVM;

/**
 * Perceptron Simplu
 *
 * Algoritmul clasic Perceptron (Rosenblatt, 1958).
 * Lucreaza direct cu vectorul de ponderi w.
 *
 * Regula de decizie:
 *   f(x) = 1 daca w·x + b >= 0, altfel 0
 *
 * Actualizare la clasificare gresita a exemplului i:
 *   w[j] += eta * (yTrue - yPred) * x[i][j]
 *   b    += eta * (yTrue - yPred)
 *
 * Converge garantat in timp finit daca datele sunt separabile liniar.
 */
public class Perceptron extends Algorithm {

    private volatile boolean running   = false;
    private volatile boolean suspended = false;

    public Perceptron(SVM svm) {
        super(svm);
        if (svm.ind.V != null) {
            name = "Perceptron";
            svm.outd.algorithm = name;
            svm.outd.max_stages_count = P;
            svm.outd.showInputData();
        }
    }

    @Override
    public void suspend_() {
        suspended = true;
    }

    @Override
    public void resume_() {
        suspended = false;
        synchronized (this) { notify(); }
    }

    @Override
    public void stop_() {
        running   = false;
        suspended = false;
        synchronized (this) { notify(); }
    }

    @Override
    public void run() {
        t       = System.currentTimeMillis();
        running = true;

        float[] w = new float[dim + 1]; // w[0..dim-1] = ponderi, w[dim] = bias

        long    stage     = 0;
        boolean converged = false;

        while (running && stage < P && !converged) {

            // gestionare suspend/resume
            synchronized (this) {
                while (suspended) {
                    try { wait(); }
                    catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        running = false;
                        break;
                    }
                }
            }

            converged = true;
            stage++;

            for (int i = 0; i < N; i++) {
                // calculeaza scorul: w·x + b
                float s = w[dim];
                for (int j = 0; j < dim; j++)
                    s += w[j] * svm.ind.V[i].X[j];

                int yPred = (s >= 0) ? 1 : 0;
                int yTrue = svm.ind.V[i].cl.Y;

                // actualizare doar daca clasificat gresit
                if (yPred != yTrue) {
                    float delta = eta * (yTrue - yPred);
                    for (int j = 0; j < dim; j++)
                        w[j] += delta * svm.ind.V[i].X[j];
                    w[dim] += delta;
                    converged = false;
                }
            }

            svm.outd.stages_count   = stage;
            svm.outd.computing_time = System.currentTimeMillis() - t;
            svm.outd.w              = w;
            svm.outd.accuracy       = getAccuracy(w);
            svm.outd.showOutputData();

            svm.design.setPointsOfLine(w);
            svm.design.repaint();
        }

        // stare finala
        svm.outd.stages_count   = stage;
        svm.outd.computing_time = System.currentTimeMillis() - t;
        svm.outd.w              = w;
        svm.outd.accuracy       = getAccuracy(w);
        svm.outd.showInputData();
        svm.outd.showOutputData();
        svm.design.calculates = false;
        svm.design.repaint();
        svm.control.start.enable(false);
    }
}
