package alg;

import svm.SVM;

public class MPerceptron extends Algorithm {

    private volatile boolean running = false;
    private volatile boolean suspended = false;

    public MPerceptron(SVM svm) {
        super(svm);
        if (svm.ind.V != null) {
            name = "Median-Perceptron";
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
        synchronized (this) {
            notify();
        }
    }

    @Override
    public void stop_() {
        running = false;
        suspended = false;
        synchronized (this) {
            notify();
        }
    }

    private float[] computeMedianWeights() {
        float[] M0 = new float[dim];
        float[] M1 = new float[dim];
        float[] w  = new float[dim + 1];
        int k0 = 0, k1 = 0;

        for (int i = 0; i < N; i++)
            if (svm.ind.V[i].cl.Y == 0) k0++; else k1++;

        if (k0 == 0 || k1 == 0) return null; 

        for (int j = 0; j < dim; j++)
            for (int i = 0; i < N; i++)
                if (svm.ind.V[i].cl.Y == 0) M0[j] += svm.ind.V[i].X[j];
                else                         M1[j] += svm.ind.V[i].X[j];

        for (int j = 0; j < dim; j++) {
            M0[j] /= k0;
            M1[j] /= k1;
        }

        for (int j = 0; j < dim; j++) {
            float X0j = (M0[j] + M1[j]) / 2f;
            w[j]    = M1[j] - M0[j];
            w[dim] -= w[j] * X0j;
        }
        return w;
    }

    @Override
    public void run() {
        t = System.currentTimeMillis();
        running = true;


        float[] w = computeMedianWeights();
        if (w == null) w = new float[dim + 1]; 

        long stage = 0;
        boolean converged = false;

        while (running && stage < P && !converged) {
            synchronized (this) {
                while (suspended) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        running = false;
                        break;
                    }
                }
            }

            converged = true;
            stage++;

            for (int i = 0; i < N; i++) {
                float s = 0;
                for (int j = 0; j < dim; j++)
                    s += w[j] * svm.ind.V[i].X[j];
                s += w[dim];

                int yPred = (s >= 0) ? 1 : 0;
                int yTrue = svm.ind.V[i].cl.Y;

                if (yPred != yTrue) {
                    float delta = eta * (yTrue - yPred);
                    for (int j = 0; j < dim; j++)
                        w[j] += delta * svm.ind.V[i].X[j];
                    w[dim] += delta;
                    converged = false;
                }
            }

            svm.outd.stages_count = stage;
            svm.outd.computing_time = System.currentTimeMillis() - t;
            svm.outd.w = w;
            svm.outd.accuracy = getAccuracy(w);
            svm.outd.showOutputData();

    
            svm.design.setPointsOfLine(w);
            svm.design.repaint();
        }

        svm.outd.stages_count = stage;
        svm.outd.computing_time = System.currentTimeMillis() - t;
        svm.outd.w = w;
        svm.outd.accuracy = getAccuracy(w);
        svm.outd.showInputData();
        svm.outd.showOutputData();
        svm.design.calculates = false;
        svm.design.repaint();
        svm.control.start.enable(false);
    }
}
