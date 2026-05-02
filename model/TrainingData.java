package model;

import java.io.*;

/**
 * TrainingData - stocheaza vectorii HOG si etichetele pentru antrenament.
 *
 * Aceasta clasa colecteaza vectorii de trasaturi extrasi prin HOG
 * si etichetele corespunzatoare (+1 / -1), formand multimea de invatare
 * folosita de clasificatoarele SVM per persoana (cerinta 5 si 6).
 *
 * Serializarea se face manual prin DataOutputStream/DataInputStream,
 * la fel ca SVMModel, fara ObjectOutputStream.
 *
 * Structura fisierului salvat:
 *   [int]    N     - numarul de exemple
 *   [int]    dim   - dimensiunea vectorului HOG
 *   [int*N]  y     - etichetele (+1 sau -1)
 *   [float*N*dim] X - vectorii HOG
 */
public class TrainingData {

    /** Numarul curent de exemple stocate */
    private int N;

    /** Dimensiunea vectorului HOG (setata la primul add()) */
    private int dim;

    /** Vectorii HOG — X[i] este vectorul exemplului i */
    private float[][] X;

    /** Etichetele: y[i] = +1 sau -1 */
    private int[] y;

    /** Capacitatea initiala si pasul de crestere a array-urilor */
    private static final int INITIAL_CAPACITY = 1000;
    private static final int GROW_FACTOR       = 2;

    /**
     * Constructor implicit — creeaza un set de date gol.
     */
    public TrainingData() {
        N   = 0;
        dim = 0;
        X   = new float[INITIAL_CAPACITY][];
        y   = new int[INITIAL_CAPACITY];
    }

    /**
     * Constructor cu capacitate initiala specificata.
     * @param initialCapacity capacitatea initiala estimata
     */
    public TrainingData(int initialCapacity) {
        N   = 0;
        dim = 0;
        X   = new float[initialCapacity][];
        y   = new int[initialCapacity];
    }

    // -----------------------------------------------------------------------
    // Adaugare exemple
    // -----------------------------------------------------------------------

    /**
     * Adauga un exemplu nou (vector HOG + eticheta) in setul de date.
     * Daca array-urile interne sunt pline, le dublam capacitatea (dynamic array).
     * @param hog     vectorul de trasaturi HOG al imaginii
     * @param label   eticheta: +1 (clasa pozitiva) sau -1 (clasa negativa)
     */
    public void add(float[] hog, int label) {
        // La primul exemplu, setam dimensiunea vectorului
        if (N == 0) dim = hog.length;

        // Verificam daca trebuie sa crestem capacitatea
        if (N >= X.length) grow();

        // Copiem vectorul HOG (nu stocam referinta, ci o copie)
        X[N] = new float[dim];
        for (int j = 0; j < dim; j++)
            X[N][j] = hog[j];

        y[N] = label; // stocam eticheta
        N++;          // incrementam contorul
    }

    /**
     * Dubleaza capacitatea array-urilor interne cand sunt pline.
     * Aloca noi array-uri mai mari si copiaza datele existente.
     */
    private void grow() {
        int newCap = X.length * GROW_FACTOR; // noua capacitate
        float[][] newX = new float[newCap][];
        int[]     newY = new int[newCap];
        // Copiem datele existente in noile array-uri
        for (int i = 0; i < N; i++) {
            newX[i] = X[i];
            newY[i] = y[i];
        }
        X = newX;
        y = newY;
    }

    /**
     * Adauga toate exemplele dintr-un alt TrainingData in acesta.
     * Util pentru a combina date pozitive si negative intr-un singur set.
     * @param other setul de date de adaugat
     */
    public void addAll(TrainingData other) {
        for (int i = 0; i < other.N; i++)
            add(other.X[i], other.y[i]);
    }

    // -----------------------------------------------------------------------
    // Conversie pentru SMO
    // -----------------------------------------------------------------------

    /**
     * Converteste datele in format io.Vector[] pentru a fi folosite de SMO.
     * Fiecare Vector contine coordonatele HOG si o eticheta de clasa.
     * Eticheta +1 => clasa cu Y=1, eticheta -1 => clasa cu Y=0.
     * @return array de io.Vector gata de dat la SMO.train()
     */
    public io.Vector[] toVectors() {
        io.Clasa cls1 = new io.Clasa("+1", 1, java.awt.Color.RED);   // clasa pozitiva
        io.Clasa cls0 = new io.Clasa("-1", 0, java.awt.Color.BLUE);  // clasa negativa
        io.Vector[] vectors = new io.Vector[N];
        for (int i = 0; i < N; i++) {
            // Copiem vectorul HOG
            float[] xi = new float[dim];
            for (int j = 0; j < dim; j++) xi[j] = X[i][j];
            // Convertim eticheta: +1 -> cls1 (Y=1), -1 -> cls0 (Y=0)
            vectors[i] = new io.Vector(xi, y[i] == 1 ? cls1 : cls0);
        }
        return vectors;
    }

    // -----------------------------------------------------------------------
    // Serializare manuala
    // -----------------------------------------------------------------------

    /**
     * Salveaza vectorii HOG si etichetele intr-un fisier binar.
     * Formatul: [int N] [int dim] [int*N etichete] [float*N*dim vectori]
     * @param path calea fisierului de iesire
     * @throws IOException daca scrierea esueaza
     */
    public void save(String path) throws IOException {
        DataOutputStream dos = new DataOutputStream(
            new BufferedOutputStream(new FileOutputStream(path)));
        try {
            dos.writeInt(N);    // numarul de exemple
            dos.writeInt(dim);  // dimensiunea vectorului HOG

            // Scriem etichetele
            for (int i = 0; i < N; i++)
                dos.writeInt(y[i]);

            // Scriem vectorii HOG
            for (int i = 0; i < N; i++)
                for (int j = 0; j < dim; j++)
                    dos.writeFloat(X[i][j]);
        } finally {
            dos.close();
        }
        System.out.println("TrainingData: salvat " + N + " exemple in " + path);
    }

    /**
     * Incarca vectorii HOG si etichetele dintr-un fisier binar.
     * @param path calea fisierului de intrare
     * @return setul de date incarcat
     * @throws IOException daca citirea esueaza
     */
    public static TrainingData load(String path) throws IOException {
        DataInputStream dis = new DataInputStream(
            new BufferedInputStream(new FileInputStream(path)));
        try {
            int n   = dis.readInt(); // numarul de exemple
            int dim = dis.readInt(); // dimensiunea vectorilor
            TrainingData td = new TrainingData(n);
            td.N   = n;
            td.dim = dim;
            td.X   = new float[n][dim];
            td.y   = new int[n];

            // Citim etichetele
            for (int i = 0; i < n; i++)
                td.y[i] = dis.readInt();

            // Citim vectorii HOG
            for (int i = 0; i < n; i++)
                for (int j = 0; j < dim; j++)
                    td.X[i][j] = dis.readFloat();

            System.out.println("TrainingData: incarcat " + n + " exemple din " + path);
            return td;
        } finally {
            dis.close();
        }
    }

    // -----------------------------------------------------------------------
    // Acces si statistici
    // -----------------------------------------------------------------------

    /** @return numarul de exemple stocate */
    public int getN()   { return N; }

    /** @return dimensiunea vectorului HOG */
    public int getDim() { return dim; }

    /** @return vectorul HOG al exemplului i */
    public float[] getX(int i) { return X[i]; }

    /** @return eticheta exemplului i (+1 sau -1) */
    public int getY(int i) { return y[i]; }

    /**
     * Returneaza numarul de exemple pozitive (+1) din set.
     * @return numarul de exemple cu eticheta +1
     */
    public int countPositive() {
        int cnt = 0;
        for (int i = 0; i < N; i++)
            if (y[i] == 1) cnt++;
        return cnt;
    }

    /**
     * Returneaza numarul de exemple negative (-1) din set.
     * @return numarul de exemple cu eticheta -1
     */
    public int countNegative() {
        int cnt = 0;
        for (int i = 0; i < N; i++)
            if (y[i] == -1) cnt++;
        return cnt;
    }

    @Override
    public String toString() {
        return "TrainingData[N=" + N + ", dim=" + dim
             + ", pozitive=" + countPositive()
             + ", negative=" + countNegative() + "]";
    }
}
