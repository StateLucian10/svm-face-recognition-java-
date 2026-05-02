package alg;

import svm.SVM;

/**
 * Perceptron Dual - Varianta pentru date NESEPARABILE LINIAR (Pocket)
 *
 * Ideea Pocket aplicata formei duale:
 *   - Se ruleaza algoritmul Dual Perceptron standard
 *   - Dupa fiecare epoca se reconstruieste w si se calculeaza acuratetea
 *   - Se pastreaza cel mai bun w gasit pana la momentul respectiv
 *     (ca si cum l-am "baga in buzunar" / pocket)
 *   - La final se returneaza cel mai bun w, nu ultimul
 *
 * Aceasta strategie garanteaza ca pentru date neseparabile
 * nu "uitam" solutia buna din cauza actualizarilor ulterioare.
 */
public class DualPerceptronNS extends Algorithm {

    private volatile boolean running   = false;
    private volatile boolean suspended = false;

    public DualPerceptronNS(SVM svm) {
        super(svm);
        if (svm.ind.V != null) {
            name = "Dual Perceptron NS";
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

    /** Reconstruieste w din alpha si b */
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

    /** Copiaza un vector de ponderi */
    private float[] copyW(float[] w) {
        float[] copy = new float[w.length];
        for (int i = 0; i < w.length; i++) copy[i] = w[i];
        return copy;
    }

    @Override
    public void run() {
        t       = System.currentTimeMillis();
        running = true;

        float[] alpha    = new float[N];
        float   b        = 0f;

        // Pocket: cel mai bun w si acuratetea lui
        float[] bestW        = buildW(alpha, b);
        int     bestAccuracy = getAccuracy(bestW);

        long stage = 0;

        while (running && stage < P) {

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

            stage++;
            boolean allCorrect = true;

            for (int i = 0; i < N; i++) {
                // calculeaza scorul dual
                float s = b;
                for (int k = 0; k < N; k++) {
                    if (alpha[k] == 0f) continue;
                    float dot = 0f;
                    for (int j = 0; j < dim; j++)
                        dot += svm.ind.V[k].X[j] * svm.ind.V[i].X[j];
                    s += alpha[k] * label(svm.ind.V[k].cl.Y) * dot;
                }

                int yTrue = label(svm.ind.V[i].cl.Y);
                if (yTrue * s <= 0) {
                    alpha[i] += 1f;
                    b        += eta * yTrue;
                    allCorrect = false;
                }
            }

            // reconstruieste w curent si verifica daca e mai bun (Pocket)
            float[] currentW        = buildW(alpha, b);
            int     currentAccuracy = getAccuracy(currentW);

            if (currentAccuracy >= bestAccuracy) {
                bestW        = copyW(currentW);
                bestAccuracy = currentAccuracy;
            }

            svm.outd.stages_count   = stage;
            svm.outd.computing_time = System.currentTimeMillis() - t;
            svm.outd.w              = bestW;        // afisam intotdeauna cel mai bun w
            svm.outd.accuracy       = bestAccuracy;
            svm.outd.showOutputData();

            svm.design.setPointsOfLine(bestW);
            svm.design.repaint();

            // daca datele sunt de fapt separabile, ne oprim devreme
            if (allCorrect) break;
        }

        // stare finala - afisam cel mai bun w gasit
        svm.outd.stages_count   = stage;
        svm.outd.computing_time = System.currentTimeMillis() - t;
        svm.outd.w              = bestW;
        svm.outd.accuracy       = bestAccuracy;
        svm.outd.showInputData();
        svm.outd.showOutputData();
        svm.design.calculates = false;
        svm.design.repaint();
        svm.control.start.enable(false);
    }
}
