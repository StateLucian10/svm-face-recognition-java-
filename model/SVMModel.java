package model;

import java.io.*;
import io.Vector;
import alg.SMO;

/**
 * SVMModel - reprezinta un clasificator SVM antrenat, gata de serializare.
 *
 * Stocheaza toate datele necesare pentru a clasifica exemple noi:
 *   - multiplicatorii duali alpha
 *   - bias-ul b
 *   - etichetele vectorilor suport
 *   - vectorii de antrenament (necesari pentru kernel)
 *   - parametrii kernel-ului Sigmoid (gamma, coef0)
 *   - pseudonimul persoanei asociate (pentru clasificatoarele de recunoastere)
 *
 * Serializarea se face manual (fara ObjectOutputStream) prin scriere
 * campurie camp in fisier text/binar, conform cerintei de implementare integrala.
 *
 * Utilizare:
 *   - Un model per persoana (clasificator one-vs-all, cerinta 6)
 *   - Un model pentru detectia capului (clasificator cap/non-cap, cerinta 1)
 */
public class SVMModel implements Serializable {

    /** Versiunea pentru compatibilitate la deserializare */
    private static final long serialVersionUID = 1L;

    // --- Date clasificator ---

    /** Multiplicatorii duali alpha[i] >= 0 pentru fiecare vector suport */
    public double[] alpha;

    /** Bias-ul clasificatorului */
    public double b;

    /** Etichetele in {-1, +1} pentru fiecare exemplu de antrenament */
    public int[] labels;

    /** Vectorii de antrenament (necesari pentru calculul kernel-ului la clasificare) */
    public float[][] supportVectors;

    // --- Parametrii kernel Sigmoid ---

    /** Coeficientul de scalare al produsului scalar: K = tanh(gamma*dot + coef0) */
    public double gamma;

    /** Termenul liber al kernel-ului Sigmoid */
    public double coef0;

    // --- Metadate ---

    /**
     * Pseudonimul persoanei asociate acestui clasificator.
     * Gol ("") pentru clasificatorul de detectie cap.
     */
    public String personName;

    /** Numarul de vectori de antrenament */
    public int N;

    /** Dimensiunea vectorilor de trasaturi */
    public int dim;

    /**
     * Constructor implicit — necesar pentru deserializare.
     */
    public SVMModel() {
        this.personName = "";
    }

    /**
     * Constructor care construieste modelul dintr-un obiect SMO antrenat.
     * @param smo        clasificatorul SMO antrenat
     * @param personName pseudonimul persoanei (sau "" pentru detectia capului)
     */
    public SVMModel(SMO smo, String personName) {
        this.personName = personName;
        this.b          = smo.getB();
        this.alpha      = smo.getAlpha();
        this.labels     = smo.getLabels();
        this.gamma      = smo.gamma;
        this.coef0      = smo.coef0;

        Vector[] tv = smo.getTrainVectors();
        this.N   = tv.length;
        this.dim = tv[0].getDimension();

        // Copiem coordonatele vectorilor de antrenament (nu referintele)
        // pentru a nu depinde de structura io.Vector la deserializare
        this.supportVectors = new float[N][dim];
        for (int i = 0; i < N; i++)
            for (int j = 0; j < dim; j++)
                this.supportVectors[i][j] = tv[i].X[j];
    }

    // -----------------------------------------------------------------------
    // Clasificare
    // -----------------------------------------------------------------------

    /**
     * Calculeaza kernel-ul Sigmoid intre vectorii x si z.
     * Identic cu SMO.kernel() — duplicat intentionat pentru independenta de SMO.
     *   K(x, z) = tanh(gamma * dot(x,z) + coef0)
     * @param x primul vector
     * @param z al doilea vector
     * @return valoarea kernel-ului
     */
    private double kernel(float[] x, float[] z) {
        double dot = 0.0;
        for (int j = 0; j < x.length; j++)
            dot += (double)x[j] * (double)z[j];
        return Math.tanh(gamma * dot + coef0);
    }

    /**
     * Calculeaza scorul functiei de decizie pentru vectorul x:
     *   f(x) = sum_i( alpha[i] * y[i] * K(sv[i], x) ) + b
     * Scor > 0 => clasa +1 (persoana recunoscuta sau cap detectat)
     * Scor < 0 => clasa -1
     * @param x vectorul de trasaturi HOG de clasificat
     * @return valoarea reala a functiei de decizie
     */
    public double score(float[] x) {
        double s = b;
        for (int i = 0; i < N; i++) {
            if (alpha[i] == 0.0) continue; // sarim non-vectorii suport
            s += alpha[i] * labels[i] * kernel(supportVectors[i], x);
        }
        return s;
    }

    /**
     * Clasifica vectorul x.
     * @param x vectorul de trasaturi HOG
     * @return +1 daca e clasa pozitiva, -1 altfel
     */
    public int classify(float[] x) {
        return score(x) >= 0.0 ? 1 : -1;
    }

    /**
     * Elimina vectorii suport cu alpha foarte mic (contribuie neglijabil la clasificare).
     * Reduce numarul de calcule kernel la fiecare clasificare => creste viteza.
     * @param threshold vectorii cu alpha < threshold sunt eliminati
     */
    public void pruneSuportVectors(double threshold) {
        int kept = 0;
        for (int i = 0; i < N; i++)
            if (Math.abs(alpha[i]) >= threshold) kept++;

        double[]   newAlpha = new double[kept];
        int[]      newLabels = new int[kept];
        float[][]  newSV    = new float[kept][];

        int k = 0;
        for (int i = 0; i < N; i++) {
            if (Math.abs(alpha[i]) >= threshold) {
                newAlpha[k]  = alpha[i];
                newLabels[k] = labels[i];
                newSV[k]     = supportVectors[i];
                k++;
            }
        }
        System.out.println("SVMModel[" + personName + "]: vectori suport "
                         + N + " -> " + kept);
        alpha          = newAlpha;
        labels         = newLabels;
        supportVectors = newSV;
        N              = kept;
    }

    // -----------------------------------------------------------------------
    // Serializare manuala
    // -----------------------------------------------------------------------

    /**
     * Salveaza modelul intr-un fisier binar folosind DataOutputStream.
     * Formatul fisierului:
     *   [String]  personName (UTF)
     *   [double]  b
     *   [double]  gamma
     *   [double]  coef0
     *   [int]     N
     *   [int]     dim
     *   [double*N] alpha[0..N-1]
     *   [int*N]   labels[0..N-1]
     *   [float*N*dim] supportVectors[0..N-1][0..dim-1]
     *
     * @param path calea fisierului de iesire
     * @throws IOException daca scrierea esueaza
     */
    public void save(String path) throws IOException {
        DataOutputStream dos = new DataOutputStream(
            new BufferedOutputStream(new FileOutputStream(path)));
        try {
            dos.writeUTF(personName);   // scriem pseudonimul (lungime + UTF-8)
            dos.writeDouble(b);          // scriem bias-ul
            dos.writeDouble(gamma);      // scriem parametrul gamma
            dos.writeDouble(coef0);      // scriem termenul liber coef0
            dos.writeInt(N);             // scriem numarul de vectori
            dos.writeInt(dim);           // scriem dimensiunea

            // Scriem array-ul alpha (N double-uri)
            for (int i = 0; i < N; i++)
                dos.writeDouble(alpha[i]);

            // Scriem array-ul labels (N int-uri)
            for (int i = 0; i < N; i++)
                dos.writeInt(labels[i]);

            // Scriem matricea supportVectors (N*dim float-uri)
            for (int i = 0; i < N; i++)
                for (int j = 0; j < dim; j++)
                    dos.writeFloat(supportVectors[i][j]);
        } finally {
            dos.close(); // inchidem intotdeauna, chiar daca apare exceptie
        }
    }

    /**
     * Incarca un model dintr-un fisier binar (formatul descris in save()).
     * @param path calea fisierului de intrare
     * @return modelul incarcat
     * @throws IOException daca citirea esueaza sau formatul e invalid
     */
    public static SVMModel load(String path) throws IOException {
        DataInputStream dis = new DataInputStream(
            new BufferedInputStream(new FileInputStream(path)));
        try {
            SVMModel model     = new SVMModel();
            model.personName   = dis.readUTF();      // citim pseudonimul
            model.b            = dis.readDouble();    // citim bias-ul
            model.gamma        = dis.readDouble();    // citim gamma
            model.coef0        = dis.readDouble();    // citim coef0
            model.N            = dis.readInt();       // citim N
            model.dim          = dis.readInt();       // citim dim

            // Citim alpha
            model.alpha = new double[model.N];
            for (int i = 0; i < model.N; i++)
                model.alpha[i] = dis.readDouble();

            // Citim labels
            model.labels = new int[model.N];
            for (int i = 0; i < model.N; i++)
                model.labels[i] = dis.readInt();

            // Citim supportVectors
            model.supportVectors = new float[model.N][model.dim];
            for (int i = 0; i < model.N; i++)
                for (int j = 0; j < model.dim; j++)
                    model.supportVectors[i][j] = dis.readFloat();

            return model;
        } finally {
            dis.close();
        }
    }

    /**
     * Salveaza o lista de modele (toate clasificatoarele de recunoastere faciala)
     * intr-un singur fisier binar.
     * Formatul: [int] nrModele, urmat de fiecare model serialzat consecutiv.
     * @param models  array-ul de modele de salvat
     * @param path    calea fisierului de iesire
     * @throws IOException daca scrierea esueaza
     */
    public static void saveAll(SVMModel[] models, String path) throws IOException {
        DataOutputStream dos = new DataOutputStream(
            new BufferedOutputStream(new FileOutputStream(path)));
        try {
            dos.writeInt(models.length); // scriem numarul de modele
            for (SVMModel m : models) {
                dos.writeUTF(m.personName);
                dos.writeDouble(m.b);
                dos.writeDouble(m.gamma);
                dos.writeDouble(m.coef0);
                dos.writeInt(m.N);
                dos.writeInt(m.dim);
                for (int i = 0; i < m.N; i++) dos.writeDouble(m.alpha[i]);
                for (int i = 0; i < m.N; i++) dos.writeInt(m.labels[i]);
                for (int i = 0; i < m.N; i++)
                    for (int j = 0; j < m.dim; j++)
                        dos.writeFloat(m.supportVectors[i][j]);
            }
        } finally {
            dos.close();
        }
    }

    /**
     * Incarca o lista de modele dintr-un fisier binar (formatul saveAll()).
     * @param path calea fisierului de intrare
     * @return array-ul de modele incarcate
     * @throws IOException daca citirea esueaza
     */
    public static SVMModel[] loadAll(String path) throws IOException {
        DataInputStream dis = new DataInputStream(
            new BufferedInputStream(new FileInputStream(path)));
        try {
            int n = dis.readInt(); // citim numarul de modele
            SVMModel[] models = new SVMModel[n];
            for (int mi = 0; mi < n; mi++) {
                SVMModel m   = new SVMModel();
                m.personName = dis.readUTF();
                m.b          = dis.readDouble();
                m.gamma      = dis.readDouble();
                m.coef0      = dis.readDouble();
                m.N          = dis.readInt();
                m.dim        = dis.readInt();
                m.alpha      = new double[m.N];
                for (int i = 0; i < m.N; i++) m.alpha[i] = dis.readDouble();
                m.labels     = new int[m.N];
                for (int i = 0; i < m.N; i++) m.labels[i] = dis.readInt();
                m.supportVectors = new float[m.N][m.dim];
                for (int i = 0; i < m.N; i++)
                    for (int j = 0; j < m.dim; j++)
                        m.supportVectors[i][j] = dis.readFloat();
                models[mi] = m;
            }
            return models;
        } finally {
            dis.close();
        }
    }

    /**
     * Salveaza modelele per persoana SI vectorii HOG intr-un SINGUR fisier binar.
     * Respecta cerinta 6: "vor fi salvate intr-un fisier, prin serializare".
     *
     * Formatul fisierului combinat:
     *   [int]     nrModele
     *   [model_0] ... [model_N] - modelele SVM per persoana
     *   [int]     nrVectori     - numarul total de vectori HOG
     *   [int]     dimVector     - dimensiunea vectorului HOG
     *   [int*N]   etichete      - etichetele vectorilor (+1 sau -1)
     *   [float*N*dim] vectori   - vectorii HOG
     *
     * @param models   modelele SVM per persoana
     * @param hogData  vectorii HOG de antrenament (TrainingData)
     * @param path     calea fisierului de iesire
     * @throws IOException daca scrierea esueaza
     */
    public static void saveAllWithHOG(SVMModel[] models,
                                       model.TrainingData hogData,
                                       String path) throws IOException {
        DataOutputStream dos = new DataOutputStream(
            new BufferedOutputStream(new FileOutputStream(path)));
        try {
            // Scriem numarul de modele
            dos.writeInt(models.length);

            // Scriem fiecare model SVM
            for (SVMModel m : models) {
                dos.writeUTF(m.personName);
                dos.writeDouble(m.b);
                dos.writeDouble(m.gamma);
                dos.writeDouble(m.coef0);
                dos.writeInt(m.N);
                dos.writeInt(m.dim);
                for (int i = 0; i < m.N; i++) dos.writeDouble(m.alpha[i]);
                for (int i = 0; i < m.N; i++) dos.writeInt(m.labels[i]);
                for (int i = 0; i < m.N; i++)
                    for (int j = 0; j < m.dim; j++)
                        dos.writeFloat(m.supportVectors[i][j]);
            }

            // Scriem vectorii HOG dupa modele (in acelasi fisier)
            int n   = hogData.getN();
            int dim = hogData.getDim();
            dos.writeInt(n);   // numarul de vectori HOG
            dos.writeInt(dim); // dimensiunea vectorului HOG

            // Scriem etichetele
            for (int i = 0; i < n; i++)
                dos.writeInt(hogData.getY(i));

            // Scriem vectorii HOG
            for (int i = 0; i < n; i++)
                for (int j = 0; j < dim; j++)
                    dos.writeFloat(hogData.getX(i)[j]);

        } finally {
            dos.close();
        }
        System.out.println("SVMModel: salvat " + models.length
                         + " modele + " + hogData.getN()
                         + " vectori HOG in " + path);
    }

    /**
     * Incarca modelele si vectorii HOG dintr-un singur fisier binar.
     * @param path   calea fisierului
     * @param result array de 2 elemente: result[0]=SVMModel[], result[1]=TrainingData
     * @throws IOException daca citirea esueaza
     */
    public static Object[] loadAllWithHOG(String path) throws IOException {
        DataInputStream dis = new DataInputStream(
            new BufferedInputStream(new FileInputStream(path)));
        try {
            // Citim modelele
            int nModels = dis.readInt();
            SVMModel[] models = new SVMModel[nModels];
            for (int mi = 0; mi < nModels; mi++) {
                SVMModel m   = new SVMModel();
                m.personName = dis.readUTF();
                m.b          = dis.readDouble();
                m.gamma      = dis.readDouble();
                m.coef0      = dis.readDouble();
                m.N          = dis.readInt();
                m.dim        = dis.readInt();
                m.alpha      = new double[m.N];
                for (int i = 0; i < m.N; i++) m.alpha[i] = dis.readDouble();
                m.labels     = new int[m.N];
                for (int i = 0; i < m.N; i++) m.labels[i] = dis.readInt();
                m.supportVectors = new float[m.N][m.dim];
                for (int i = 0; i < m.N; i++)
                    for (int j = 0; j < m.dim; j++)
                        m.supportVectors[i][j] = dis.readFloat();
                models[mi] = m;
            }

            // Citim vectorii HOG
            int n   = dis.readInt();
            int dim = dis.readInt();
            model.TrainingData td = new model.TrainingData(n);
            io.Clasa cls1 = new io.Clasa("+1", 1, java.awt.Color.RED);
            io.Clasa cls0 = new io.Clasa("-1", 0, java.awt.Color.BLUE);
            int[] labels = new int[n];
            for (int i = 0; i < n; i++) labels[i] = dis.readInt();
            for (int i = 0; i < n; i++) {
                float[] x = new float[dim];
                for (int j = 0; j < dim; j++) x[j] = dis.readFloat();
                td.add(x, labels[i]);
            }

            return new Object[]{models, td};
        } finally {
            dis.close();
        }
    }

    @Override
    public String toString() {
        return "SVMModel[person=" + personName + ", N=" + N + ", dim=" + dim
             + ", gamma=" + gamma + ", coef0=" + coef0 + ", b=" + b + "]";
    }
}
