package face;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import model.SVMModel;
import model.TrainingData;
import alg.SMO;
import io.Vector;

/**
 * FaceDetector - antreneaza si foloseste clasificatorul SVM pentru detectia capului.
 *
 * Implementeaza cerintele 1 si 2 din proiect:
 *   1. Antreneaza un clasificator SVM binar (cap / non-cap) pe baza unui set
 *      de imagini pozitive (fete) si negative (non-fete) preluate din Internet.
 *   2. Pentru o imagine data, detecteaza toate patratele cap si returneaza
 *      portiunea cu aria maxima, scalata la 128x128 pixeli.
 *
 * Antrenarea:
 *   - Pozitive: imagini cu fete (eticheta +1), redimensionate la 128x128
 *   - Negative: patch-uri aleatoare din imagini fara fete (eticheta -1)
 *   - Extrage vectorul HOG din fiecare imagine si antreneaza SMO
 *   - Salveaza modelul antrenat intr-un fisier pentru reutilizare
 *
 * Detectia:
 *   - Foloseste SlidingWindow pentru a gasi candidatii
 *   - Aplica NMS pentru a elimina duplicatele
 *   - Returneaza patratul cu aria maxima (cerinta 2)
 */
public class FaceDetector {

    /** Instanta HOG pentru extragerea trasaturilor (128x128, cell=8, block=2, bins=9) */
    private HOG hog;

    /** Instanta SlidingWindow pentru parcurgerea imaginii */
    private SlidingWindow sw;

    /** Modelul SVM antrenat pentru detectia capului */
    private SVMModel model;

    /** Dimensiunea imaginilor de antrenament (patrate) */
    private static final int IMG_SIZE = 128;

    /** Calea implicita pentru salvarea/incarcarea modelului de detectie cap */
    public static final String DEFAULT_MODEL_PATH = "svm/face_detector.model";

    /** Calea implicita pentru salvarea vectorilor HOG de antrenament (cerinta 7) */
    public static final String DEFAULT_HOG_PATH   = "svm/face_detector_hog.dat";

    /**
     * Constructor implicit.
     */
    public FaceDetector() {
        this.hog   = new HOG(8, 2, 9);           // parametrii recomandati Dalal & Triggs
        this.sw    = new SlidingWindow(128, 128, 48, 2.0, 0.3); // fereastra 128x128
        this.model = null; // modelul se incarca sau se antreneaza explicit
    }

    // -----------------------------------------------------------------------
    // Cerinta 1: Antrenare clasificator SVM pentru detectia capului
    // -----------------------------------------------------------------------

    /**
     * Antreneaza clasificatorul SVM pe baza imaginilor din doua directoare:
     *   - positivesDir: imagini cu fete (eticheta +1)
     *   - negativesDir: imagini fara fete, din care extragem patch-uri (eticheta -1)
     *
     * Pasii antrenarii:
     *   1. Incarcam imaginile pozitive, le redimensionam la 128x128, extragem HOG
     *   2. Extragem patch-uri aleatoare din imaginile negative, extragem HOG
     *   3. Antrenam SMO cu toti vectorii HOG si etichetele lor
     *   4. Salvam modelul si vectorii HOG pe disc
     *
     * @param positivesDir  directorul cu imagini de fete (pozitive)
     * @param negativesDir  directorul cu imagini fara fete (negative)
     * @param negativesPerImage numarul de patch-uri extrase din fiecare imagine negativa
     * @param modelPath     calea pentru salvarea modelului antrenat
     * @param hogPath       calea pentru salvarea vectorilor HOG (cerinta 7)
     */
    public void train(String positivesDir, String negativesDir,
                      int negativesPerImage, String modelPath, String hogPath) {

        System.out.println("FaceDetector: incepe antrenarea...");
        TrainingData td = new TrainingData(2000); // setul de date de antrenament

        // --- Pasul 1: Imagini pozitive (fete) ---
        System.out.println("FaceDetector: procesez imagini pozitive din " + positivesDir);
        File posDir = new File(positivesDir);
        File[] posFiles = posDir.listFiles(); // lista fisierelor din director
        int posCount = 0;
        if (posFiles != null) {
            for (File f : posFiles) {
                if (!ImageUtils.isImageFile(f.getName())) continue;
                float[] hogVec = ImageUtils.loadAndExtractHOG(
                    f.getAbsolutePath(), IMG_SIZE, IMG_SIZE, hog);
                if (hogVec == null) continue;
                td.add(hogVec, +1);
                posCount++;
                if (posCount % 100 == 0)
                    System.out.println("  pozitive procesate: " + posCount);
            }
        }
        System.out.println("FaceDetector: " + posCount + " imagini pozitive procesate.");

        // --- Pasul 2: Patch-uri negative (non-fete) ---
        System.out.println("FaceDetector: procesez imagini negative din " + negativesDir);
        File negDir = new File(negativesDir);
        File[] negFiles = negDir.listFiles();
        int negCount = 0;
        Random rnd = new Random(42); // seed fix pentru reproductibilitate
        if (negFiles != null) {
            for (File f : negFiles) {
                if (!ImageUtils.isImageFile(f.getName())) continue;
                BufferedImage negImg = ImageUtils.load(f.getAbsolutePath());
                if (negImg == null) continue;
                int imgW = negImg.getWidth();
                int imgH = negImg.getHeight();
                if (imgW < IMG_SIZE || imgH < IMG_SIZE) continue; // prea mica

                // Extragem patch-uri aleatoare din aceasta imagine negativa
                for (int k = 0; k < negativesPerImage; k++) {
                    int px = rnd.nextInt(imgW - IMG_SIZE);
                    int py = rnd.nextInt(imgH - IMG_SIZE);
                    float[] hogVec = ImageUtils.cropResizeHOG(
                        negImg, px, py, IMG_SIZE, IMG_SIZE, IMG_SIZE, IMG_SIZE, hog);
                    td.add(hogVec, -1);
                    negCount++;
                }
                if (negCount % 500 == 0)
                    System.out.println("  negative procesate: " + negCount);
            }
        }
        System.out.println("FaceDetector: " + negCount + " patch-uri negative procesate.");
        System.out.println("FaceDetector: total exemple: " + td.getN()
                         + " (+" + td.countPositive() + " / -" + td.countNegative() + ")");

        // --- Pasul 3: Antrenam SMO ---
        // Verificam ca avem suficiente exemple inainte de antrenare
        if (td.getN() == 0 || td.countPositive() == 0 || td.countNegative() == 0) {
            System.out.println("FaceDetector: EROARE - nu sunt suficiente imagini!");
            System.out.println("  Asigurati-va ca:");
            System.out.println("  - Directorul 'positives' contine imagini .jpg cu fete");
            System.out.println("  - Directorul 'negatives' contine imagini .jpg fara fete");
            return;
        }

        System.out.println("FaceDetector: antrenez SMO...");
        Vector[] vectors = td.toVectors(); // convertim la format io.Vector

        // Cream SMO standalone (fara GUI)
        SMO smo = SMO.createStandalone(
            1.0,    // C: regularizare moderata
            0.001,  // gamma: scala produsului scalar
            -1.0,   // coef0: termen liber kernel sigmoid
            0.001,  // tol: toleranta KKT
            10000   // maxIter: 10000 iteratii
        );
        smo.train(vectors); // antrenam pe toti vectorii HOG
        System.out.println("FaceDetector: antrenare SMO finalizata.");

        // --- Pasul 4: Salvam modelul si vectorii HOG ---
        this.model = new SVMModel(smo, "");
        // Eliminam vectorii suport cu contributie neglijabila (alpha < 0.01)
        // Reduce numarul de calcule kernel => detectie mai rapida
        this.model.pruneSuportVectors(0.01);
        try {
            model.save(modelPath);
            System.out.println("FaceDetector: model salvat in " + modelPath);
        } catch (IOException e) {
            System.out.println("FaceDetector: eroare la salvare model: " + e.getMessage());
        }

        // Salvam vectorii HOG (cerinta 7 — folderul cu vectori HOG)
        try {
            td.save(hogPath);
            System.out.println("FaceDetector: vectori HOG salvati in " + hogPath);
        } catch (IOException e) {
            System.out.println("FaceDetector: eroare la salvare HOG: " + e.getMessage());
        }
    }

    /**
     * Incarca un model de detectie cap pre-antrenat din fisier.
     * Apelata la pornirea aplicatiei daca modelul exista pe disc.
     * @param modelPath calea fisierului model
     * @return true daca incarcarea a reusit
     */
    public boolean loadModel(String modelPath) {
        try {
            model = SVMModel.load(modelPath);
            System.out.println("FaceDetector: model incarcat din " + modelPath);
            return true;
        } catch (IOException e) {
            System.out.println("FaceDetector: nu pot incarca modelul: " + e.getMessage());
            return false;
        }
    }

    // -----------------------------------------------------------------------
    // Cerinta 2: Detectia patratului cap cu aria maxima
    // -----------------------------------------------------------------------

    /**
     * Detecteaza toate patratele cap dintr-o imagine si returneaza
     * portiunea cu aria maxima, scalata la 128x128 pixeli (cerinta 2).
     *
     * @param img imaginea de intrare (contine una sau mai multe persoane)
     * @return imaginea 128x128 cu capul detectat (aria maxima),
     *         sau null daca nu s-a detectat niciun cap
     */
    public BufferedImage detectLargestHead(BufferedImage img) {
        if (model == null) {
            System.out.println("FaceDetector: modelul nu e incarcat!");
            return null;
        }

        // Obtinem lista tuturor detectiilor dupa NMS
        List<SlidingWindow.Detection> detections = detectAll(img);
        if (detections.isEmpty()) return null; // niciun cap detectat

        // Gasim detectia cu aria maxima (cerinta 2)
        SlidingWindow.Detection largest = SlidingWindow.getLargest(detections);

        // Decupam si redimensionam la 128x128
        BufferedImage head = ImageUtils.crop(img,
            largest.x, largest.y, largest.w, largest.h);
        return ImageUtils.resize(head, 128, 128);
    }

    /**
     * Detecteaza toate patratele cap dintr-o imagine si le returneaza ca lista.
     * Folosita la cerinta 8: deseneaza patratele cap pe imaginea live.
     * @param img imaginea de intrare
     * @return lista de detectii (pozitie + dimensiune + scor)
     */
    public List<SlidingWindow.Detection> detectAll(BufferedImage img) {
        if (model == null) return new ArrayList<SlidingWindow.Detection>();

        // Definim clasificatorul ca lambda/clasa anonima ce apeleaza model.score()
        SlidingWindow.Classifier classifier = new SlidingWindow.Classifier() {
            public double score(float[] hogVec) {
                return model.score(hogVec); // scorul SVMModel
            }
        };

        return sw.detect(img, classifier, hog);
    }

    /**
     * Verifica daca modelul de detectie cap este incarcat si gata de utilizare.
     * @return true daca modelul e disponibil
     */
    public boolean isReady() {
        return model != null;
    }

    // --- Getteri ---
    public HOG          getHog()   { return hog; }
    public SVMModel     getModel() { return model; }
    public SlidingWindow getSW()   { return sw; }
}
