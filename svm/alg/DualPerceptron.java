package alg;

import svm.SVM;

/**
 * Perceptron Dual
 *
 * In loc sa lucreze direct cu vectorul de ponderi w,
 * algoritmul pastreaza un vector de multiplicatori alpha[i] >= 0,
 * cate unul per exemplu de antrenament.
 *
 * Regula de decizie:
 *   f(x) = sign( sum_i( alpha[i] * y[i] * <x[i], x> ) + b )
 *
 * Actualizare la clasificare gresita a exemplului i:
 *   alpha[i] += 1
 *   b        += eta * y[i]   (y[i] in {-1, +1})
 *
 * Reconstituirea lui w (pentru vizualizare si acuratete):
 *   w[j] = sum_i( alpha[i] * y[i] * x[i][j] )
 *   w[dim] = b
 */
public class DualPerceptron extends Algorithm {

    private volatile boolean running   = false;
    private volatile boolean suspended = false;

    public DualPerceptron(SVM svm) {
        super(svm);
        if (svm.ind.V != null) {
            name = "Dual Perceptron";
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

    /** Converteste etichetele {0,1} -> {-1,+1} */
    private int label(int Y) { return (Y == 0) ? -1 : 1; }

    /** Reconstruieste w din alpha si b pentru vizualizare */
    private float[] buildW(float[] alpha, float b) {
        float[] w = new float[dim + 1];
        for (int i = 0; i < N; i++) {
            float coef = alpha[i] * label(svm.ind.V[i].cl.Y);
            for (int j = 0; j < dim; j++)
                w[j] += coef * svm.ind.V[i].X[j];
        }
        w[dim] = b;
        return w;
    }

    @Override
    public void run() {
        t       = System.currentTimeMillis();
        running = true;

        float[] alpha = new float[N];   // multiplicatori duali, init 0
        float   b     = 0f;             // bias

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
                // calculeaza scorul dual: sum_k( alpha[k]*y[k]*<x[k],x[i]> ) + b
                float s = b;
                for (int k = 0; k < N; k++) {
                    if (alpha[k] == 0f) continue;
                    float dot = 0f;
                    for (int j = 0; j < dim; j++)
                        dot += svm.ind.V[k].X[j] * svm.ind.V[i].X[j];
                    s += alpha[k] * label(svm.ind.V[k].cl.Y) * dot;
                }

                int yTrue = label(svm.ind.V[i].cl.Y);
                // clasificat gresit daca y * s <= 0
                if (yTrue * s <= 0) {
                    alpha[i] += 1f;
                    b        += eta * yTrue;
                    converged = false;
                }
            }

            // reconstruieste w pentru output si vizualizare
            float[] w = buildW(alpha, b);

            svm.outd.stages_count    = stage;
            svm.outd.computing_time  = System.currentTimeMillis() - t;
            svm.outd.w               = w;
            svm.outd.accuracy        = getAccuracy(w);
            svm.outd.showOutputData();

            svm.design.setPointsOfLine(w);
            svm.design.repaint();
        }

        // stare finala
        float[] w = buildW(alpha, b);
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
