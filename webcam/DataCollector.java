package webcam;

import java.awt.image.BufferedImage;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import face.FaceDetector;
import face.ImageUtils;

/**
 * DataCollector - colecteaza imagini de antrenament pentru recunoasterea faciala.
 *
 * Implementeaza cerinta 3:
 *   "Folosind functia de la (2), pentru o persoana aflata in fata camerei web,
 *    programul va permite preluarea unui numar de imagini (de exemplu 500),
 *    identificarea patratului cap in fiecare imagine si salvarea imaginilor
 *    returnate intr-un director. Numele unei imagini va incepe cu pseudonimul
 *    persoanei respective si va fi urmat de data si ora la care imaginea a
 *    fost salvata. Se repeta operatia pentru mai multe persoane."
 *
 * Comportament:
 *   - Porneste camera web
 *   - Captureaza imagini continuu
 *   - Detecteaza patratul cap cu aria maxima in fiecare imagine (cerinta 2)
 *   - Salveaza imaginile de 128x128 pixeli in directorul persoanei
 *   - Numele fisierului: pseudonim_YYYYMMDD_HHmmss_SSS.jpg
 *   - Se poate apela pentru mai multe persoane consecutiv
 */
public class DataCollector {

    /** Camera web folosita pentru captura */
    private WebcamCapture webcam;

    /** Detectorul de cap (clasificatorul SVM antrenat la cerinta 1) */
    private FaceDetector detector;

    /** Directorul radacina unde se salveaza imaginile tuturor persoanelor */
    private String rootDir;

    /** Numarul de imagini de capturat per persoana */
    private int imagesPerPerson;

    /** Formatul datei si orei folosit in numele fisierelor */
    private static final SimpleDateFormat DATE_FORMAT =
        new SimpleDateFormat("yyyyMMdd_HHmmss_SSS");

    /** Flag pentru oprirea colectarii din exterior (de ex. din GUI) */
    private volatile boolean stopRequested;

    /** Listener pentru a notifica GUI-ul despre progres */
    private ProgressListener progressListener;

    /**
     * Interfata pentru notificarea progresului catre GUI.
     * GUI-ul implementeaza aceasta interfata si primeste update-uri.
     */
    public interface ProgressListener {
        /**
         * Apelata dupa salvarea fiecarei imagini.
         * @param personName  pseudonimul persoanei curente
         * @param saved       numarul de imagini salvate pana acum
         * @param total       numarul total de imagini de salvat
         * @param lastFrame   ultimul frame capturat (pentru previzualizare in GUI)
         */
        void onProgress(String personName, int saved, int total,
                        BufferedImage lastFrame);

        /**
         * Apelata cand colectarea pentru o persoana s-a terminat.
         * @param personName pseudonimul persoanei
         * @param saved      numarul total de imagini salvate
         */
        void onDone(String personName, int saved);

        /**
         * Apelata cand in frame-ul curent nu s-a detectat niciun cap.
         * @param personName pseudonimul persoanei curente
         */
        void onNoFaceDetected(String personName);
    }

    /**
     * Constructor principal.
     * @param detector       detectorul de cap antrenat (cerinta 1)
     * @param rootDir        directorul radacina pentru salvarea imaginilor
     * @param imagesPerPerson numarul de imagini de capturat per persoana
     */
    public DataCollector(FaceDetector detector, String rootDir, int imagesPerPerson) {
        this.detector       = detector;
        this.rootDir        = rootDir;
        this.imagesPerPerson = imagesPerPerson;
        this.webcam         = new WebcamCapture(0, 320, 240, 10);
        this.stopRequested  = false;
    }

    /**
     * Constructor cu WebcamCapture configurata extern.
     * @param detector        detectorul de cap
     * @param webcam          instanta WebcamCapture deja configurata
     * @param rootDir         directorul radacina
     * @param imagesPerPerson numarul de imagini per persoana
     */
    public DataCollector(FaceDetector detector, WebcamCapture webcam,
                          String rootDir, int imagesPerPerson) {
        this.detector        = detector;
        this.webcam          = webcam;
        this.rootDir         = rootDir;
        this.imagesPerPerson = imagesPerPerson;
        this.stopRequested   = false;
    }

    // -----------------------------------------------------------------------
    // Colectare imagini
    // -----------------------------------------------------------------------

    /**
     * Colecteaza imaginile pentru o persoana.
     * Metoda blocheaza thread-ul curent pana la finalizare sau oprire.
     *
     * Pasii pentru fiecare frame:
     *   1. Capturam frame de la camera
     *   2. Detectam patratul cap cu aria maxima (FaceDetector.detectLargestHead)
     *   3. Daca s-a detectat un cap, salvam imaginea 128x128 cu numele corect
     *   4. Notificam GUI-ul prin ProgressListener
     *   5. Repetam pana la imagesPerPerson imagini salvate sau stop
     *
     * @param personName pseudonimul persoanei (ex: "ion", "maria")
     */
    public void collect(String personName) {
        stopRequested = false;

        // Cream directorul persoanei daca nu exista
        String personDir = rootDir + File.separator + personName;
        File dir = new File(personDir);
        if (!dir.exists()) {
            dir.mkdirs(); // cream si directoarele parinte daca lipsesc
            System.out.println("DataCollector: creat director " + personDir);
        }

        // Deschidem camera daca nu e deja deschisa
        boolean cameraOpenedHere = false;
        if (!webcam.isOpened()) {
            if (!webcam.open()) {
                System.out.println("DataCollector: nu pot deschide camera!");
                return;
            }
            cameraOpenedHere = true; // noi am deschis-o, noi o inchidem
        }

        System.out.println("DataCollector: incep colectarea pentru " + personName
                         + " (" + imagesPerPerson + " imagini)");

        int saved = 0; // numarul de imagini salvate pana acum

        while (saved < imagesPerPerson && !stopRequested) {
            // Pasul 1: Capturam un frame de la camera
            BufferedImage frame = webcam.captureFrameWithDelay();
            if (frame == null) {
                System.out.println("DataCollector: frame null, continui...");
                continue;
            }

            // Pasul 2: Detectam patratul cap cu aria maxima (cerinta 2)
            // Folosim functia detectLargestHead() conform cerintei 3
            BufferedImage headImg = detector.detectLargestHead(frame);

            if (headImg == null) {
                // Nu s-a detectat niciun cap in acest frame — sarim
                if (progressListener != null)
                    progressListener.onNoFaceDetected(personName);
                continue;
            }

            // Pasul 3: Construim numele fisierului
            // Format: pseudonim_YYYYMMDD_HHmmss_SSS.jpg
            String timestamp = DATE_FORMAT.format(new Date());
            String fileName  = personName + "_" + timestamp + ".jpg";
            String filePath  = personDir + File.separator + fileName;

            // Salvam imaginea 128x128 pe disc
            boolean ok = ImageUtils.save(headImg, filePath);
            if (ok) {
                saved++;
                System.out.println("DataCollector: salvat " + fileName
                                 + " (" + saved + "/" + imagesPerPerson + ")");

                // Pasul 4: Notificam GUI-ul despre progres
                if (progressListener != null)
                    progressListener.onProgress(personName, saved,
                                                imagesPerPerson, frame);
            } else {
                System.out.println("DataCollector: eroare la salvare " + fileName);
            }
        }

        // Inchidem camera doar daca noi am deschis-o
        if (cameraOpenedHere) webcam.close();

        System.out.println("DataCollector: colectare finalizata pentru "
                         + personName + ": " + saved + " imagini salvate.");

        // Notificam GUI-ul ca s-a terminat
        if (progressListener != null)
            progressListener.onDone(personName, saved);
    }

    /**
     * Colecteaza imagini pentru mai multe persoane consecutiv.
     * Pentru fiecare persoana, asteapta ca utilizatorul sa fie pozitionat
     * in fata camerei inainte de a incepe captura.
     *
     * @param personNames array cu pseudonimele persoanelor
     * @param delayBetween timpul de asteptare (ms) intre persoane (pentru reglaj pozitie)
     */
    public void collectMultiple(String[] personNames, long delayBetween) {
        // Deschidem camera o singura data pentru toate persoanele
        if (!webcam.isOpened()) {
            if (!webcam.open()) {
                System.out.println("DataCollector: nu pot deschide camera!");
                return;
            }
        }

        for (String name : personNames) {
            if (stopRequested) break;

            System.out.println("DataCollector: pregatire pentru persoana: " + name);
            System.out.println("DataCollector: astept " + delayBetween/1000
                             + " secunde pentru pozitionare...");

            // Asteptam ca utilizatorul sa se pozitioneze
            try {
                Thread.sleep(delayBetween);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            // Colectam imaginile pentru aceasta persoana
            collect(name);
        }

        // Inchidem camera dupa toate persoanele
        webcam.close();
        System.out.println("DataCollector: colectare completa pentru toate persoanele.");
    }

    /**
     * Opreste colectarea in curs (apelata din GUI la click pe "Stop").
     * Thread-safe: poate fi apelata din orice thread.
     */
    public void stop() {
        stopRequested = true;
        System.out.println("DataCollector: oprire solicitata.");
    }

    /**
     * Returneaza numarul de imagini salvate pentru o persoana.
     * @param personName pseudonimul persoanei
     * @return numarul de fisiere .jpg din directorul persoanei
     */
    public int countImages(String personName) {
        File dir = new File(rootDir + File.separator + personName);
        if (!dir.exists()) return 0;
        File[] files = dir.listFiles();
        if (files == null) return 0;
        int count = 0;
        for (File f : files)
            if (ImageUtils.isImageFile(f.getName())) count++;
        return count;
    }

    /**
     * Returneaza lista cu pseudonimele tuturor persoanelor din directorul radacina.
     * Fiecare subdirector corespunde unei persoane.
     * @return array de pseudonime, sau array gol daca nu exista nicio persoana
     */
    public String[] getPersonNames() {
        File root = new File(rootDir);
        if (!root.exists()) return new String[0];
        File[] dirs = root.listFiles(new FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory(); // doar subdirectoarele
            }
        });
        if (dirs == null) return new String[0];
        String[] names = new String[dirs.length];
        for (int i = 0; i < dirs.length; i++)
            names[i] = dirs[i].getName(); // numele directorului = pseudonimul
        return names;
    }

    /**
     * Returneaza calea catre directorul unei persoane.
     * @param personName pseudonimul persoanei
     * @return calea absoluta a directorului
     */
    public String getPersonDir(String personName) {
        return rootDir + File.separator + personName;
    }

    // --- Getteri si setteri ---

    /** Seteaza listener-ul de progres pentru notificarea GUI-ului */
    public void setProgressListener(ProgressListener listener) {
        this.progressListener = listener;
    }

    /** @return numarul de imagini per persoana */
    public int getImagesPerPerson() { return imagesPerPerson; }

    /** Seteaza numarul de imagini per persoana */
    public void setImagesPerPerson(int n) { this.imagesPerPerson = n; }

    /** @return directorul radacina */
    public String getRootDir() { return rootDir; }

    /** @return true daca o oprire a fost solicitata */
    public boolean isStopRequested() { return stopRequested; }
}