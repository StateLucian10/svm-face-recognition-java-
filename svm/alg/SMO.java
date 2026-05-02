package alg;

import svm.SVM;
import io.Vector;

/**
 * SMO - Sequential Minimal Optimization cu kernel Sigmoid
 *
 * Rezolva problema duala a SVM-ului:
 *   Maximizeaza: sum_i(alpha[i]) - 0.5 * sum_i sum_j (alpha[i]*alpha[j]*y[i]*y[j]*K(x[i],x[j]))
 *   Subject to:  0 <= alpha[i] <= C
 *                sum_i(alpha[i]*y[i]) = 0
 *
 * Kernel Sigmoid:
 *   K(x[i], x[j]) = tanh(gamma * dot(x[i],x[j]) + coef0)
 *
 * La fiecare pas SMO alege 2 multiplicatori alpha si ii optimizeaza analitic.
 * Astfel evita rezolvarea unui sistem mare de ecuatii simultan.
 */
public class SMO extends Algorithm {

    // --- Parametrii kernel-ului Sigmoid ---
    /** Coeficientul de scalare al produsului scalar in kernel */
    public double gamma;

    /** Termenul liber adunat inainte de tanh */
    public double coef0;

    // --- Parametrul de regularizare ---
    /**
     * C controleaza penalizarea erorilor de clasificare.
     * C mic = margine mai larga, mai multe erori acceptate.
     * C mare = margine mai mica, mai putine erori acceptate.
     */
    public double C;

    /** Toleranta pentru verificarea conditiilor KKT */
    public double tol;

    /** Numarul maxim de iteratii — previne bucla infinita pe date neseparabile */
    public long maxIter;

    // --- Variabile interne ---
    /** Multiplicatorii duali, cate unul per exemplu de antrenament */
    private double[] alpha;

    /** Bias-ul clasificatorului */
    private double b;

    /** Cache erori: errors[i] = f(x[i]) - y[i] */
    private double[] errors;

    /** Etichetele in {-1, +1} */
    private int[] labels;

    /** Vectorii de antrenament salvati pentru classify() si score() */
    private Vector[] trainVectors;

    /** Flag pentru controlul executiei thread-ului */
    private volatile boolean running = false;

    /** Flag pentru suspendarea thread-ului */
    private volatile boolean suspended = false;

    // -----------------------------------------------------------------------
    // Constructori
    // -----------------------------------------------------------------------

    /**
     * Constructor pentru utilizare cu GUI.
     * @param svm referinta la aplicatia principala
     */
    public SMO(SVM svm) {
        super(svm);
        this.gamma   = 0.01;
        this.coef0   = -1.0;
        this.C       = 1.0;
        this.tol     = 0.001;
        this.maxIter = P; // P mostenita din Algorithm = 10.000.000

        if (svm.ind.V != null) {
            name = "SMO Sigmoid";
            svm.outd.algorithm = name;
            svm.outd.max_stages_count = P;
            svm.outd.showInputData();
        }
    }

    /**
     * Constructor cu parametrii expliciti, pentru utilizare cu GUI.
     * @param svm   referinta la aplicatia principala
     * @param C     parametrul de regularizare
     * @param gamma coeficientul kernel-ului sigmoid
     * @param coef0 termenul liber al kernel-ului sigmoid
     * @param tol   toleranta KKT
     */
    public SMO(SVM svm, double C, double gamma, double coef0, double tol) {
        super(svm);
        this.C       = C;
        this.gamma   = gamma;
        this.coef0   = coef0;
        this.tol     = tol;
        this.maxIter = P;

        if (svm.ind.V != null) {
            name = "SMO Sigmoid";
            svm.outd.algorithm = name;
            svm.outd.max_stages_count = P;
            svm.outd.showInputData();
        }
    }

    /**
     * Constructor privat fara argumente — folosit de createStandalone().
     * Nu apeleaza super(svm) pentru a evita NullPointerException
     * (Algorithm acceseaza svm.settings in constructor).
     * Campurile N, dim, eta sunt setate manual de createStandalone().
     */
    private SMO(boolean standalone) {
        // Apelam constructorul protejat din Algorithm(boolean dummy)
        // care nu acceseaza campurile svm — evita NullPointerException
        super(standalone);
    }

    /**
     * Factory method: creeaza un SMO independent de GUI.
     * Folosit de FaceDetector si clasificatoarele de recunoastere faciala.
     * @param C       parametrul de regularizare
     * @param gamma   coeficientul kernel-ului
     * @param coef0   termenul liber al kernel-ului
     * @param tol     toleranta KKT
     * @param maxIter numarul maxim de iteratii
     * @return instanta SMO configurata, gata de antrenament
     */
    public static SMO createStandalone(double C, double gamma, double coef0,
                                       double tol, long maxIter) {
        SMO smo      = new SMO(true); // constructor privat, fara GUI
        smo.C        = C;
        smo.gamma    = gamma;
        smo.coef0    = coef0;
        smo.tol      = tol;
        smo.maxIter  = maxIter;
        smo.N        = 0;   // va fi setat in train()
        smo.dim      = 0;   // va fi setat in train()
        smo.eta      = 0f;  // neutilizat in modul standalone
        return smo;
    }

    // -----------------------------------------------------------------------
    // Controlul thread-ului (GUI)
    // -----------------------------------------------------------------------

    @Override public void suspend_() { suspended = true; }

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

    // -----------------------------------------------------------------------
    // Kernel si functia de decizie
    // -----------------------------------------------------------------------

    /**
     * Calculeaza kernel-ul Sigmoid intre doi vectori x si z:
     *   K(x, z) = tanh(gamma * dot(x,z) + coef0)
     * unde dot(x,z) = sum_j(x[j]*z[j]) este produsul scalar euclidian.
     * @param x primul vector de trasaturi
     * @param z al doilea vector de trasaturi
     * @return valoarea scalara a kernel-ului in (-1, 1)
     */
    public double kernel(float[] x, float[] z) {
        double dot = 0.0;
        for (int j = 0; j < x.length; j++)
            dot += (double)x[j] * (double)z[j]; // produs scalar componenta cu componenta
        return Math.tanh(gamma * dot + coef0);   // aplicam tanh pe combinatia liniara
    }

    /**
     * Calculeaza functia de decizie duala pentru exemplul i:
     *   f(x[i]) = sum_k( alpha[k] * y[k] * K(x[k], x[i]) ) + b
     * Contribuie doar vectorii cu alpha[k] > 0 (vectorii suport).
     * @param i indexul exemplului
     * @param V vectorii de antrenament
     * @return valoarea reala a functiei (nu semnul)
     */
    private double decisionFunction(int i, Vector[] V) {
        double s = b; // pornim cu bias-ul
        for (int k = 0; k < N; k++) {
            if (alpha[k] == 0.0) continue; // non-vectorii suport nu contribuie
            s += alpha[k] * labels[k] * kernel(V[k].X, V[i].X);
        }
        return s;
    }

    // -----------------------------------------------------------------------
    // Pasii SMO
    // -----------------------------------------------------------------------

    /**
     * Pasul 2 SMO: optimizeaza analitic perechea (i1, i2) de multiplicatori.
     * Deriva formulele din conditia de stationaritate a lagrangianului dual.
     * @param i1 indexul primului multiplicator ales
     * @param i2 indexul celui de-al doilea multiplicator
     * @param V  vectorii de antrenament
     * @return 1 daca s-a realizat o actualizare semnificativa, 0 altfel
     */
    private int step(int i1, int i2, Vector[] V) {
        if (i1 == i2) return 0; // nu putem optimiza un singur multiplicator

        double alpha1 = alpha[i1];  // alpha curent pentru i1
        double alpha2 = alpha[i2];  // alpha curent pentru i2
        int    y1     = labels[i1]; // eticheta i1 in {-1,+1}
        int    y2     = labels[i2]; // eticheta i2 in {-1,+1}
        double E1     = errors[i1]; // eroarea curenta pentru i1: f(x[i1]) - y[i1]
        double E2     = errors[i2]; // eroarea curenta pentru i2: f(x[i2]) - y[i2]
        int    s      = y1 * y2;   // +1 daca aceeasi clasa, -1 daca clase diferite

        // Calculam limitele [L, H] pentru alpha2 nou
        // Derivate din: 0 <= alpha[i] <= C si suma(alpha*y) = const
        double L, H;
        if (s == -1) {
            // Clase diferite: alpha2 - alpha1 = const
            L = Math.max(0.0, alpha2 - alpha1);
            H = Math.min(C,   C + alpha2 - alpha1);
        } else {
            // Aceeasi clasa: alpha1 + alpha2 = const
            L = Math.max(0.0, alpha1 + alpha2 - C);
            H = Math.min(C,   alpha1 + alpha2);
        }
        if (L >= H) return 0; // intervalul feasibil e gol, renuntam

        // Calculam valorile kernel-ului necesare formulei de actualizare
        double k11 = kernel(V[i1].X, V[i1].X); // K(x[i1], x[i1])
        double k12 = kernel(V[i1].X, V[i2].X); // K(x[i1], x[i2])
        double k22 = kernel(V[i2].X, V[i2].X); // K(x[i2], x[i2])

        // eta = curvatura (a doua derivata negata a obiectivului in directia alpha2)
        // Trebuie sa fie pozitiv pentru ca functia sa fie concava (maximizabila)
        double eta = k11 + k22 - 2.0 * k12;

        double alpha2New;
        if (eta > 1e-12) {
            // Cazul normal: pasul optim e alpha2 + y2*(E1-E2)/eta, clipat in [L,H]
            alpha2New = alpha2 + y2 * (E1 - E2) / eta;
            if      (alpha2New < L) alpha2New = L;
            else if (alpha2New > H) alpha2New = H;
        } else {
            // Cazul degenerat: eta <= 0, evaluam obiectivul la capetele [L,H]
            double a1L = alpha1 + s * (alpha2 - L); // alpha1 cand alpha2=L
            double a1H = alpha1 + s * (alpha2 - H); // alpha1 cand alpha2=H
            // Functia obiectiv simplificata la L si H
            double objL = a1L + L
                        - 0.5*k11*a1L*a1L - 0.5*k22*L*L - s*k12*a1L*L
                        - y1*a1L*E1 - y2*L*E2;
            double objH = a1H + H
                        - 0.5*k11*a1H*a1H - 0.5*k22*H*H - s*k12*a1H*H
                        - y1*a1H*E1 - y2*H*E2;
            if      (objL > objH + tol) alpha2New = L;
            else if (objH > objL + tol) alpha2New = H;
            else                        alpha2New = alpha2; // nicio imbunatatire
        }

        // Daca schimbarea e prea mica, nu actualizam (evitam drift numeric)
        if (Math.abs(alpha2New - alpha2) < tol * (alpha2New + alpha2 + tol))
            return 0;

        // Calculam alpha1 nou din constrangerea liniara: alpha1*y1 + alpha2*y2 = const
        double alpha1New = alpha1 + s * (alpha2 - alpha2New);

        // Actualizam bias-ul din conditiile KKT pentru vectorii suport
        // b1: din conditia f(x[i1]) = y[i1], valabila cand 0 < alpha1New < C
        double b1 = b - E1
                    - y1 * (alpha1New - alpha1) * k11
                    - y2 * (alpha2New - alpha2) * k12;
        // b2: din conditia f(x[i2]) = y[i2], valabila cand 0 < alpha2New < C
        double b2 = b - E2
                    - y1 * (alpha1New - alpha1) * k12
                    - y2 * (alpha2New - alpha2) * k22;

        // Alegem b-ul final in functie de care alpha e strict in (0, C)
        if      (alpha1New > 0 && alpha1New < C) b = b1;
        else if (alpha2New > 0 && alpha2New < C) b = b2;
        else                                     b = (b1 + b2) / 2.0;

        // Salvam noile valori ale alpha
        alpha[i1] = alpha1New;
        alpha[i2] = alpha2New;

        // Actualizam cache-ul de erori DOAR pentru i1 si i2 (nu pentru toti!)
        // Erorile celorlalti se vor actualiza lazy la urmatoarea examinare
        errors[i1] = decisionFunction(i1, V) - labels[i1];
        errors[i2] = decisionFunction(i2, V) - labels[i2];

        return 1; // actualizare realizata
    }

    /**
     * Pasul 1 SMO: examineaza alpha[i2] si cauta un alpha[i1] potrivit.
     * Aplica 3 heuristici in cascada pentru selectia lui i1.
     * @param i2 indexul multiplicatorului de examinat
     * @param V  vectorii de antrenament
     * @return 1 daca s-a realizat o actualizare, 0 altfel
     */
    private int examineExample(int i2, Vector[] V) {
        int    y2     = labels[i2];
        double alpha2 = alpha[i2];
        double E2     = errors[i2];
        double r2     = E2 * y2; // r2 = y[i2]*f(x[i2]) - 1, masura a violarii KKT

        // Conditii KKT (cu toleranta tol):
        //   alpha=0  => r2 >= 0  (exemplu corect clasificat, dincolo de margine)
        //   alpha=C  => r2 <= 0  (exemplu gresit clasificat sau pe margine)
        //   0<alpha<C => r2 = 0  (exemplu exact pe margine — vector suport)
        // Daca niciuna nu e violata, nu facem nimic
        if (!((r2 < -tol && alpha2 < C) || (r2 > tol && alpha2 > 0)))
            return 0;

        // Heuristica 1: i1 cu eroarea maxima |E1 - E2| (maximizeaza pasul)
        int    i1Best   = -1;
        double maxDiff  = 0.0;
        for (int k = 0; k < N; k++) {
            if (alpha[k] > 0 && alpha[k] < C) {
                double diff = Math.abs(errors[k] - E2);
                if (diff > maxDiff) { maxDiff = diff; i1Best = k; }
            }
        }
        if (i1Best >= 0 && step(i1Best, i2, V) == 1) return 1;

        // Heuristica 2: toti vectorii suport activi, ordinea random
        int start = (int)(Math.random() * N);
        for (int k = 0; k < N; k++) {
            int i1 = (start + k) % N;
            if (alpha[i1] > 0 && alpha[i1] < C)
                if (step(i1, i2, V) == 1) return 1;
        }

        // Heuristica 3: toti vectorii, indiferent de alpha, ordinea random
        start = (int)(Math.random() * N);
        for (int k = 0; k < N; k++) {
            int i1 = (start + k) % N;
            if (step(i1, i2, V) == 1) return 1;
        }

        return 0; // nicio pereche utila gasita
    }

    // -----------------------------------------------------------------------
    // Antrenament
    // -----------------------------------------------------------------------

    /**
     * Antreneaza clasificatorul SMO pe setul de date V.
     * Poate fi apelata si fara GUI (de ex. de FaceDetector).
     * @param V vectorii de antrenament cu etichete
     */
    public void train(Vector[] V) {
        this.N            = V.length;
        this.dim          = V[0].getDimension();
        this.trainVectors = V; // salvam pentru classify() si score()

        alpha  = new double[N]; // toti alpha pornesc din 0
        b      = 0.0;
        labels = new int[N];
        errors = new double[N];

        // Convertim etichetele {0,1} -> {-1,+1} cum cere formularea SVM
        for (int i = 0; i < N; i++)
            labels[i] = (V[i].cl.Y == 1) ? 1 : -1;

        // Initial f(x[i]) = b = 0 => errors[i] = 0 - y[i] = -y[i]
        for (int i = 0; i < N; i++)
            errors[i] = -labels[i];

        // Bucla principala SMO cu limita de iteratii (Platt 1998)
        int     numChanged = 0;
        boolean examineAll = true;
        long    iter       = 0;

        while ((numChanged > 0 || examineAll) && iter < maxIter) {
            iter++;
            numChanged = 0;

            if (examineAll) {
                // Parcurgem toti vectorii
                for (int i = 0; i < N; i++)
                    numChanged += examineExample(i, V);
            } else {
                // Parcurgem doar vectorii suport activi (0 < alpha < C)
                for (int i = 0; i < N; i++)
                    if (alpha[i] > 0 && alpha[i] < C)
                        numChanged += examineExample(i, V);
            }

            // Alternam strategia conform SMO original
            if      (examineAll)      examineAll = false;
            else if (numChanged == 0) examineAll = true;
        }
    }

    // -----------------------------------------------------------------------
    // Run (GUI thread)
    // -----------------------------------------------------------------------

    /**
     * Metoda run() — apelata cand SMO ruleaza ca Thread in GUI.
     */
    @Override
    public void run() {
        t       = System.currentTimeMillis();
        running = true;

        Vector[] V        = svm.ind.V;
        this.N            = V.length;
        this.dim          = V[0].getDimension();
        this.trainVectors = V;

        alpha  = new double[N];
        b      = 0.0;
        labels = new int[N];
        errors = new double[N];

        for (int i = 0; i < N; i++) {
            labels[i] = (V[i].cl.Y == 1) ? 1 : -1;
            errors[i] = -labels[i];
        }

        int     numChanged = 0;
        boolean examineAll = true;
        long    stage      = 0;

        while (running && (numChanged > 0 || examineAll) && stage < P) {

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
            numChanged = 0;

            if (examineAll) {
                for (int i = 0; i < N && running; i++)
                    numChanged += examineExample(i, V);
            } else {
                for (int i = 0; i < N && running; i++)
                    if (alpha[i] > 0 && alpha[i] < C)
                        numChanged += examineExample(i, V);
            }

            if      (examineAll)      examineAll = false;
            else if (numChanged == 0) examineAll = true;

            // Actualizam GUI
            float[] w = buildW(V);
            svm.outd.stages_count   = stage;
            svm.outd.computing_time = System.currentTimeMillis() - t;
            svm.outd.w              = w;
            svm.outd.accuracy       = getAccuracy(w);
            svm.outd.showOutputData();
            svm.design.setPointsOfLine(w);
            svm.design.repaint();
        }

        float[] w = buildW(V);
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

    // -----------------------------------------------------------------------
    // Utilitare
    // -----------------------------------------------------------------------

    /**
     * Reconstruieste un vector w aproximativ pentru vizualizare in GUI.
     * In spatiul kernel, w nu exista explicit — aceasta e doar o aproximatie
     * in spatiul de intrare, utila vizual dar nu pentru clasificare corecta.
     * @param V vectorii de antrenament
     * @return vector de ponderi (dim elemente + bias la pozitia dim)
     */
    private float[] buildW(Vector[] V) {
        float[] w = new float[dim + 1];
        for (int i = 0; i < N; i++) {
            double coef = alpha[i] * labels[i];
            for (int j = 0; j < dim; j++)
                w[j] += (float)(coef * V[i].X[j]);
        }
        w[dim] = (float) b;
        return w;
    }

    /**
     * Clasifica vectorul x folosind clasificatorul antrenat.
     * @param x vectorul de clasificat (dim elemente)
     * @return +1 sau -1
     */
    public int classify(float[] x) {
        return score(x) >= 0.0 ? 1 : -1;
    }

    /**
     * Returneaza scorul brut al functiei de decizie pentru x.
     * Scor pozitiv = clasa +1, negativ = clasa -1.
     * Magnitudinea scorului indica nivelul de incredere.
     * @param x vectorul de clasificat
     * @return valoarea reala a functiei de decizie
     */
    public double score(float[] x) {
        if (trainVectors == null || alpha == null)
            throw new IllegalStateException("SMO: clasificatorul nu a fost antrenat!");
        double s = b;
        for (int i = 0; i < N; i++) {
            if (alpha[i] == 0.0) continue;
            s += alpha[i] * labels[i] * kernel(trainVectors[i].X, x);
        }
        return s;
    }

    // --- Getteri pentru serializare ---

    /** @return multiplicatorii duali alpha */
    public double[] getAlpha() { return alpha; }

    /** @return bias-ul b */
    public double getB() { return b; }

    /** @return etichetele in {-1,+1} */
    public int[] getLabels() { return labels; }

    /** @return vectorii de antrenament */
    public Vector[] getTrainVectors() { return trainVectors; }

    /**
     * Restaureaza starea clasificatorului dupa deserializare.
     * @param alpha        multiplicatorii duali
     * @param b            bias-ul
     * @param labels       etichetele in {-1,+1}
     * @param trainVectors vectorii de antrenament
     */
    public void setState(double[] alpha, double b, int[] labels, Vector[] trainVectors) {
        this.alpha        = alpha;
        this.b            = b;
        this.labels       = labels;
        this.trainVectors = trainVectors;
        this.N            = alpha.length;
        this.dim          = (trainVectors != null && trainVectors.length > 0)
                            ? trainVectors[0].getDimension() : 0;
    }
}
