package webcam;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.*;
import java.util.*;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import face.*;
import model.*;
import alg.SMO;
import io.Vector;

/**
 * FaceRecognizer - antreneaza clasificatoare per persoana si face recunoastere live.
 *
 * Implementeaza cerintele 6, 7 si 8:
 *
 * Cerinta 6: Antreneaza cate un clasificator SVM pentru fiecare persoana (one-vs-all).
 *   - Imaginile persoanei curente => eticheta +1
 *   - Imaginile tuturor celorlalte persoane => eticheta -1
 *   - Salveaza clasificatoarele si vectorii HOG prin serializare manuala
 *
 * Cerinta 7: Algoritmul SMO cu kernel Sigmoid (implementat in alg/SMO.java).
 *
 * Cerinta 8: Recunoastere faciala live de la camera web la 10 FPS.
 *   - Detecteaza patratele cap cu FaceDetector (cerinta 1)
 *   - Deseneaza patratele cu verde cu OpenCV (conditia 3: OpenCV permis si pentru desenare)
 *   - Pentru fiecare patrat cap, extrage HOG si aplica clasificatoarele per persoana
 *   - Daca un clasificator returneaza +1, scrie pseudonimul deasupra patratului
 *
 * Nota: conform conditiei 3, OpenCV este permis atat pentru preluarea imaginilor
 * de la camera, cat si pentru desenarea patratelor pe imagine. Toata desenarea
 * (dreptunghiuri verzi + text pseudonim) se face cu Imgproc din OpenCV.
 */
public class FaceRecognizer {

    /** Instanta HOG pentru extragerea trasaturilor */
    private HOG hog;

    /** Detectorul de cap (cerinta 1) */
    private FaceDetector faceDetector;

    /** Camera web */
    private WebcamCapture webcam;

    /** Modelele SVM per persoana */
    private SVMModel[] personModels;

    /** Directorul cu imaginile de antrenament ale persoanelor */
    private String trainingDir;

    /**
     * Calea fisierului combinat care contine modelele SVM per persoana
     * SI vectorii HOG de antrenament intr-un singur fisier (cerinta 6).
     */
    public static final String MODELS_PATH = "svm/face_models_hog.dat";

    /** Dimensiunea imaginilor de antrenament */
    private static final int IMG_SIZE = 128;

    /**
     * Culoarea verde in format BGR (Blue=0, Green=255, Red=0).
     * OpenCV foloseste intern ordinea BGR, nu RGB.
     */
    private static final Scalar GREEN = new Scalar(0, 255, 0);

    /** Grosimea liniei dreptunghiului in pixeli */
    private static final int LINE_THICKNESS = 2;

    /** Scala fontului pentru pseudonim (1.0 = marime normala) */
    private static final double FONT_SCALE = 0.7;

    /** Flag pentru oprirea recunoasterii live (volatile = vizibil din orice thread) */
    private volatile boolean running;

    /** Listener pentru trimiterea frame-urilor procesate catre GUI */
    private FrameListener frameListener;

    /**
     * Interfata pentru trimiterea frame-urilor procesate catre GUI.
     */
    public interface FrameListener {
        /**
         * Apelata dupa procesarea fiecarui frame.
         * @param frame frame-ul cu patratele si textul desenate cu OpenCV
         * @param names lista pseudonimelor persoanelor recunoscute
         */
        void onFrame(BufferedImage frame, String[] names);
    }

    /**
     * Constructor principal.
     * @param faceDetector detectorul de cap antrenat (cerinta 1)
     * @param trainingDir  directorul cu imaginile de antrenament
     */
    public FaceRecognizer(FaceDetector faceDetector, String trainingDir) {
        this.faceDetector = faceDetector;
        this.trainingDir  = trainingDir;
        this.hog          = new HOG(8, 2, 9);
        this.webcam       = new WebcamCapture(0, 640, 480, 10);
        this.personModels = null;
        this.running      = false;
    }

    // -----------------------------------------------------------------------
    // Cerinta 6: Antrenare clasificatoare per persoana (one-vs-all)
    // -----------------------------------------------------------------------

    /**
     * Antreneaza cate un clasificator SVM pentru fiecare persoana din trainingDir.
     * Strategie one-vs-all: imaginile persoanei P => +1, restul => -1.
     * @param modelsPath calea pentru salvarea modelelor
     * @param hogPath    calea pentru salvarea vectorilor HOG (cerinta 7)
     */
    public void trainAll(String modelsPath) {
        System.out.println("FaceRecognizer: incep antrenarea clasificatoarelor...");

        // Citim persoanele disponibile (fiecare subdirector = o persoana)
        File rootDir = new File(trainingDir);
        File[] personDirs = rootDir.listFiles(new FileFilter() {
            public boolean accept(File f) { return f.isDirectory(); }
        });

        if (personDirs == null || personDirs.length == 0) {
            System.out.println("FaceRecognizer: nicio persoana in " + trainingDir);
            return;
        }
        Arrays.sort(personDirs); // ordine alfabetica consistenta

        int nPersons = personDirs.length;
        System.out.println("FaceRecognizer: " + nPersons + " persoane gasite.");

        // Extragem vectorii HOG pentru imaginile fiecarei persoane
        TrainingData[] allData  = new TrainingData[nPersons];
        String[]       allNames = new String[nPersons];

        for (int p = 0; p < nPersons; p++) {
            allNames[p] = personDirs[p].getName(); // pseudonimul persoanei
            allData[p]  = new TrainingData(600);
            System.out.println("FaceRecognizer: extrag HOG pentru " + allNames[p]);

            File[] imgFiles = personDirs[p].listFiles();
            if (imgFiles == null) continue;

            for (File f : imgFiles) {
                if (!ImageUtils.isImageFile(f.getName())) continue;
                // Incarcam imaginea, o redimensionam la 128x128 si extragem HOG
                float[] hogVec = ImageUtils.loadAndExtractHOG(
                    f.getAbsolutePath(), IMG_SIZE, IMG_SIZE, hog);
                if (hogVec != null)
                    allData[p].add(hogVec, +1); // eticheta temporara +1
            }
            System.out.println("  " + allData[p].getN() + " imagini procesate.");
        }

        // Pentru fiecare persoana, construim setul one-vs-all si antrenam SMO
        personModels = new SVMModel[nPersons];
        TrainingData allHogData = new TrainingData(nPersons * 600); // pentru cerinta 7

        for (int p = 0; p < nPersons; p++) {
            System.out.println("FaceRecognizer: antrenez clasificator pentru "
                             + allNames[p] + "...");

            TrainingData td = new TrainingData(nPersons * 600);

            // Imaginile persoanei p => eticheta +1 (clasa pozitiva)
            for (int i = 0; i < allData[p].getN(); i++)
                td.add(allData[p].getX(i), +1);

            // Imaginile tuturor celorlalte persoane => eticheta -1 (clasa negativa)
            for (int q = 0; q < nPersons; q++) {
                if (q == p) continue; // sarim persoana curenta
                for (int i = 0; i < allData[q].getN(); i++)
                    td.add(allData[q].getX(i), -1);
            }

            System.out.println("  pozitive: " + td.countPositive()
                             + ", negative: " + td.countNegative());

            // Antrenam SMO cu kernel Sigmoid (cerinta 7)
            Vector[] vectors = td.toVectors();
            SMO smo = SMO.createStandalone(1.0, 0.001, -1.0, 0.001, 10000);
            smo.train(vectors);

            // Cream modelul si il salvam in array
            personModels[p] = new SVMModel(smo, allNames[p]);
            // Eliminam vectorii suport cu contributie neglijabila
            personModels[p].pruneSuportVectors(0.001);
            System.out.println("FaceRecognizer: clasificator antrenat pentru "
                             + allNames[p]);

            // Adaugam vectorii HOG in setul global pentru cerinta 7
            allHogData.addAll(td);
        }

        // Salvam modelele SI vectorii HOG intr-un SINGUR fisier (cerinta 6)
        // "Datele clasificatoarelor, impreuna cu datele vectorilor de invatare,
        //  vor fi salvate intr-un fisier, prin serializare."
        try {
            SVMModel.saveAllWithHOG(personModels, allHogData, modelsPath);
            System.out.println("FaceRecognizer: modele + HOG salvate in " + modelsPath);
        } catch (IOException e) {
            System.out.println("FaceRecognizer: eroare salvare: " + e.getMessage());
        }

        System.out.println("FaceRecognizer: antrenare completa pentru "
                         + nPersons + " persoane.");
    }

    /**
     * Incarca modelele per persoana dintr-un fisier binar.
     * @param modelsPath calea fisierului
     * @return true daca incarcarea a reusit
     */
    public boolean loadModels(String modelsPath) {
        try {
            Object[] result = SVMModel.loadAllWithHOG(modelsPath);
            personModels = (SVMModel[]) result[0];
            System.out.println("FaceRecognizer: incarcate " + personModels.length
                             + " modele din " + modelsPath);
            return true;
        } catch (IOException e) {
            System.out.println("FaceRecognizer: nu pot incarca modelele: "
                             + e.getMessage());
            return false;
        }
    }

    // -----------------------------------------------------------------------
    // Cerinta 8: Recunoastere faciala live cu desenare OpenCV
    // -----------------------------------------------------------------------

    /**
     * Porneste recunoasterea faciala live de la camera web.
     *
     * Conform conditiei 3 din cerinte: OpenCV este permis pentru preluarea
     * imaginilor de la camera SI pentru desenarea patratelor pe imagine.
     * Prin urmare, Imgproc.rectangle() si Imgproc.putText() sunt utilizari legale.
     *
     * Toti ceilalti algoritmi (HOG, SMO, sliding window, NMS) sunt implementati
     * integral in Java, fara biblioteci specializate.
     */
    public void startLive() {
        if (personModels == null || personModels.length == 0) {
            System.out.println("FaceRecognizer: nu exista modele incarcate!");
            return;
        }
        if (!faceDetector.isReady()) {
            System.out.println("FaceRecognizer: detectorul de cap nu e pregatit!");
            return;
        }

        running = true;

        if (!webcam.isOpened()) {
            if (!webcam.open()) {
                System.out.println("FaceRecognizer: nu pot deschide camera!");
                running = false;
                return;
            }
        }

        System.out.println("FaceRecognizer: recunoastere live pornita la 10 FPS...");

        while (running) {
            // Pasul 1: Capturam frame de la camera (OpenCV intern)
            BufferedImage frame = webcam.captureFrameWithDelay();
            if (frame == null) continue;

            // Pasul 2: Detectam patratele cap cu Java pur (SlidingWindow + SVM)
            java.util.List<SlidingWindow.Detection> detections =
                faceDetector.detectAll(frame);

            // Pastram doar detectia cu aria maxima — un singur patrat pe ecran
            SlidingWindow.Detection largest = SlidingWindow.getLargest(detections);
            detections = new java.util.ArrayList<SlidingWindow.Detection>();
            if (largest != null) detections.add(largest);

            // Convertim BufferedImage in Mat OpenCV pentru desenare
            Mat mat = bufferedImageToMat(frame);

            java.util.List<String> recognizedNames = new ArrayList<String>();

            for (SlidingWindow.Detection det : detections) {

                // Pasul 3: Desenam patratul cap cu verde folosind OpenCV
                // Imgproc.rectangle primeste Mat, doua puncte, culoare si grosime
                Imgproc.rectangle(
                    mat,
                    new org.opencv.core.Point(det.x, det.y),         // colt stanga-sus
                    new org.opencv.core.Point(det.x + det.w,
                                              det.y + det.h),         // colt dreapta-jos
                    GREEN,          // culoarea: verde BGR (0,255,0)
                    LINE_THICKNESS  // grosimea liniei: 2 pixeli
                );

                // Pasul 4a: Decupam portiunea capului si o redimensionam la 128x128
                BufferedImage headImg = ImageUtils.crop(
                    frame, det.x, det.y, det.w, det.h);
                headImg = ImageUtils.resize(headImg, IMG_SIZE, IMG_SIZE);

                // Pasul 4b: Extragem vectorul HOG din capul detectat (Java pur)
                int[]   pixels = ImageUtils.getPixels(headImg);
                float[] hogVec = hog.extract(pixels, IMG_SIZE, IMG_SIZE);

                // Pasul 4c: Aplicam toti clasificatorii per persoana
                // Alegem persoana cu scorul maxim pozitiv (cel mai sigur clasificator)
                String recognized = null;
                double bestScore  = 0.0;

                for (SVMModel m : personModels) {
                    double score = m.score(hogVec); // f(x) = sum(alpha*y*K) + b
                    // Prag minim de 1.0 pentru a evita fals pozitivele
                    // Doar daca clasificatorul e suficient de sigur scriem numele
                    if (score > 0.6 && score > bestScore) { // prag ridicat: evita false positive // prag pentru modelul C=1.0
                        bestScore  = score;       // scorul maxim pozitiv
                        recognized = m.personName; // pseudonimul asociat
                    }
                }

                // Pasul 5: Scriem pseudonimul deasupra patratului cu OpenCV
                if (recognized != null) {
                    // Imgproc.putText scrie text pe Mat cu fontul si scala date
                    Imgproc.putText(
                        mat,
                        recognized,                          // textul de scris
                        new org.opencv.core.Point(           // pozitia: deasupra patratului
                            det.x,
                            Math.max(det.y - 8, 15)          // min 15px de sus sa nu iasa
                        ),
                        Imgproc.FONT_HERSHEY_SIMPLEX,        // fontul OpenCV (sans-serif)
                        FONT_SCALE,                          // scala: 0.7x
                        GREEN,                               // culoarea: verde
                        2                                    // grosimea textului
                    );
                    recognizedNames.add(recognized);
                    System.out.println("FaceRecognizer: recunoscut " + recognized
                        + " (scor=" + String.format("%.3f", bestScore) + ")");
                }
            }

            // Pasul 6: Convertim Mat inapoi in BufferedImage pentru afisare in GUI
            BufferedImage annotated = matToBufferedImage(mat);
            mat.release(); // eliberam memoria nativa a Mat (nu Java heap)

            // Trimitem frame-ul procesat la GUI prin listener
            if (frameListener != null) {
                String[] names = recognizedNames.toArray(new String[0]);
                frameListener.onFrame(annotated, names);
            }
        }

        webcam.close();
        System.out.println("FaceRecognizer: recunoastere live oprita.");
    }

    /**
     * Porneste recunoasterea live intr-un thread daemon (non-blocking pentru GUI).
     * Thread daemon: se opreste automat la inchiderea aplicatiei principale.
     */
    public void startLiveAsync() {
        Thread t = new Thread(new Runnable() {
            public void run() { startLive(); }
        });
        t.setDaemon(true); // se opreste automat cu aplicatia
        t.start();
    }

    /**
     * Opreste recunoasterea live. Thread-safe (volatile boolean).
     */
    public void stopLive() {
        running = false;
        System.out.println("FaceRecognizer: oprire solicitata.");
    }

    // -----------------------------------------------------------------------
    // Conversii BufferedImage <-> Mat OpenCV
    // -----------------------------------------------------------------------

    /**
     * Converteste un BufferedImage Java intr-o matrice Mat OpenCV.
     * Necesara pentru a putea folosi Imgproc.rectangle si Imgproc.putText.
     *
     * Formatul intern Mat OpenCV: CV_8UC3, canale in ordine BGR.
     * Formatul TYPE_3BYTE_BGR al Java are aceeasi ordine de bytes => conversie directa.
     *
     * @param img imaginea Java de convertit
     * @return matricea OpenCV CV_8UC3
     */
    private Mat bufferedImageToMat(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();

        // Asiguram formatul TYPE_3BYTE_BGR (compatibil direct cu Mat CV_8UC3)
        BufferedImage bgr;
        if (img.getType() == BufferedImage.TYPE_3BYTE_BGR) {
            bgr = img; // deja in formatul corect, nu copiem inutil
        } else {
            // Convertim la TYPE_3BYTE_BGR prin desenare pe o imagine noua
            bgr = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
            Graphics g = bgr.getGraphics();
            g.drawImage(img, 0, 0, null);
            g.dispose();
        }

        // Obtinem array-ul de bytes bruti din DataBuffer-ul imaginii
        byte[] pixels = ((DataBufferByte) bgr.getRaster().getDataBuffer()).getData();

        // Cream matricea Mat de tip CV_8UC3 (unsigned byte, 3 canale)
        Mat mat = new Mat(h, w, CvType.CV_8UC3);

        // Copiem bytes-ii din Java in memoria nativa a Mat
        mat.put(0, 0, pixels);

        return mat;
    }

    /**
     * Converteste o matrice Mat OpenCV inapoi intr-un BufferedImage Java.
     * Folosita dupa desenarea cu Imgproc pentru a trimite rezultatul la GUI.
     *
     * @param mat matricea OpenCV CV_8UC3 cu patratele si textul desenate
     * @return imaginea Java corespunzatoare
     */
    private BufferedImage matToBufferedImage(Mat mat) {
        int w = mat.cols(); // latimea in pixeli
        int h = mat.rows(); // inaltimea in pixeli

        // Cream BufferedImage in format BGR (acelasi cu Mat)
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);

        // Obtinem referinta directa la bytes-ii imaginii Java
        byte[] pixels = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();

        // Copiem datele din memoria nativa Mat in array-ul Java
        mat.get(0, 0, pixels);

        return img;
    }

    // -----------------------------------------------------------------------
    // Getteri si setteri
    // -----------------------------------------------------------------------

    /** Seteaza listener-ul pentru frame-urile procesate */
    public void setFrameListener(FrameListener listener) {
        this.frameListener = listener;
    }

    /** @return true daca recunoasterea live ruleaza */
    public boolean isRunning() { return running; }

    /** @return numarul de persoane in model */
    public int getPersonCount() {
        return personModels != null ? personModels.length : 0;
    }

    /** @return pseudonimele persoanelor din model */
    public String[] getPersonNames() {
        if (personModels == null) return new String[0];
        String[] names = new String[personModels.length];
        for (int i = 0; i < personModels.length; i++)
            names[i] = personModels[i].personName;
        return names;
    }

    /** @return modelele per persoana */
    public SVMModel[] getPersonModels() { return personModels; }
}
