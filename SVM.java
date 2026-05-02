package svm;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import gui.*;
import alg.*;
import tools.*;
import io.*;
import face.*;
import webcam.*;
import model.*;

/**
 * SVM - fereastra principala a aplicatiei.
 *
 * Extinde aplicatia de la laborator cu functionalitati de detectie
 * si recunoastere faciala, conform cerintelor proiectului.
 *
 * Meniuri noi adaugate:
 *   - Algorithms > SMO Sigmoid
 *   - Face Detection > Train Head Detector / Load Head Detector
 *   - Face Recognition > Collect Training Data / View Training Data /
 *                        Train Recognizer / Load Recognizer / Start Live
 */
public class SVM extends Frame {

    public Toolkit   tool;
    public MenuBar   mb;
    public Dimension res;
    public Image     ico, bkg, color, calculates;

    public Design           design;
    public Settings         settings;
    public SimulationControl control;
    public About            about;
    public Options          options;

    public OutputData outd;
    public InputData  ind;

    public Algorithm algorithm;

    // --- Componente noi pentru detectie si recunoastere faciala ---

    /** Detectorul de cap (cerinta 1 si 2) */
    public FaceDetector faceDetector;

    /** Colectorul de imagini de antrenament (cerinta 3) */
    public DataCollector dataCollector;

    /** Vizualizatorul de imagini (cerinta 4) */
    public ImageViewer imageViewer;

    /** Clasificatorul de recunoastere faciala (cerintele 6, 7, 8) */
    public FaceRecognizer faceRecognizer;

    /** Directorul cu imaginile de antrenament ale persoanelor */
    public static final String TRAINING_DIR = "training_data";

    /** Numarul de imagini de capturat per persoana (cerinta 3) */
    public static final int IMAGES_PER_PERSON = 500;

    public static void main(String args[]) { new SVM(); }

    public SVM() {
        tool = getToolkit();
        res  = tool.getScreenSize();
        loadImages();
        setIconImage(ico);
        setTitle("SVM Simulator - Face Recognition");
        adaugaMenuBar();

        design = new Design(this);
        add("Center", design);

        settings = new Settings(this);
        settings.resize(376, 600);
        settings.move((res.width - 376) / 2, (res.height - 600) / 2);

        about = new About(this);
        about.resize(712, 410);
        about.move((res.width - 712) / 2, (res.height - 410) / 2);

        control = new SimulationControl(this, 400, res.height - 80);
        control.resize(400, res.height - 80);
        control.move(res.width - 405, 35);

        options = new Options(this);

        outd = new OutputData(this);
        ind  = new InputData(this);

        // Initializam componentele de recunoastere faciala
        initFaceComponents();

        setResizable(false);
        setBackground(settings.background_color);
        resize(res.width, res.height - 40);
        move(0, 0);
        show();
    }

    /**
     * Initializeaza componentele de detectie si recunoastere faciala.
     * Incearca sa incarce modelele existente de pe disc.
     */
    private void initFaceComponents() {
        // Initializam detectorul de cap
        faceDetector = new FaceDetector();

        // Incercam sa incarcam modelul de detectie cap daca exista
        File modelFile = new File(FaceDetector.DEFAULT_MODEL_PATH);
        if (modelFile.exists()) {
            faceDetector.loadModel(FaceDetector.DEFAULT_MODEL_PATH);
            System.out.println("SVM: model detectie cap incarcat.");
        } else {
            System.out.println("SVM: modelul de detectie cap nu exista inca.");
            System.out.println("     Folositi Face Detection > Train Head Detector.");
        }

        // Initializam recunoscatorul facial
        faceRecognizer = new FaceRecognizer(faceDetector, TRAINING_DIR);

        // Incercam sa incarcam modelele per persoana daca exista
        File recogFile = new File(FaceRecognizer.MODELS_PATH);
        if (recogFile.exists()) {
            faceRecognizer.loadModels(FaceRecognizer.MODELS_PATH);
            System.out.println("SVM: modele recunoastere incarcate: "
                             + faceRecognizer.getPersonCount() + " persoane.");
        }

        // Colectorul de date (cerinta 3)
        dataCollector = new DataCollector(faceDetector, TRAINING_DIR, IMAGES_PER_PERSON);

        // Vizualizatorul de imagini (cerinta 4)
        imageViewer = new ImageViewer(this, TRAINING_DIR);

        // Listener pentru frame-urile live (cerinta 8) — afiseaza in Design
        faceRecognizer.setFrameListener(new FaceRecognizer.FrameListener() {
            public void onFrame(BufferedImage frame, String[] names) {
                // Trimitem frame-ul catre panoul de desenare
                design.setLiveFrame(frame);
                design.repaint();
            }
        });
    }

    /**
     * Construieste bara de meniu cu toate optiunile aplicatiei.
     */
    void adaugaMenuBar() {
        mb = new MenuBar();

        // --- Meniu File ---
        Menu file = new Menu("File");
        file.add("Load Input Data");
        file.add("-");
        file.add("Exit");
        mb.add(file);

        // --- Meniu Algorithms (existent + SMO nou) ---
        Menu algorithms = new Menu("Algorithms");
        algorithms.add("Median");
        algorithms.add("Perceptron");
        algorithms.add("Median-Perceptron");
        algorithms.add("Dual Perceptron");
        algorithms.add("Dual Perceptron NS");
        algorithms.add("-");
        algorithms.add("SMO Sigmoid");  // NOU: algoritmul cerut in cerinta 7
        mb.add(algorithms);

        // --- Meniu View (existent) ---
        Menu view = new Menu("View");
        view.add("Show Simulation Control");
        view.add("Show Input Data");
        view.add("Show Output Data");
        view.add("-");
        view.add("Show Cursor Coordinates");
        mb.add(view);

        // --- Meniu Face Detection (NOU: cerintele 1 si 2) ---
        Menu faceDetMenu = new Menu("Face Detection");
        faceDetMenu.add("Train Head Detector");  // cerinta 1: antreneaza SVM cap
        faceDetMenu.add("Load Head Detector");   // incarca model existent
        mb.add(faceDetMenu);

        // --- Meniu Face Recognition (NOU: cerintele 3, 4, 6, 7, 8) ---
        Menu faceRecMenu = new Menu("Face Recognition");
        faceRecMenu.add("Collect Training Data"); // cerinta 3: captura imagini
        faceRecMenu.add("View Training Data");    // cerinta 4: vizualizare + stergere
        faceRecMenu.add("-");
        faceRecMenu.add("Train Recognizer");      // cerinta 6: antreneaza per persoana
        faceRecMenu.add("Load Recognizer");       // incarca modele existente
        faceRecMenu.add("-");
        faceRecMenu.add("Start Live Recognition"); // cerinta 8: recunoastere live
        faceRecMenu.add("Stop Live Recognition");  // oprire recunoastere
        mb.add(faceRecMenu);

        // --- Meniu Tools (existent) ---
        Menu tools = new Menu("Tools");
        tools.add("Input Data Generator");
        tools.add("-");
        tools.add("Settings");
        mb.add(tools);

        // --- Meniu Help (existent) ---
        Menu help = new Menu("Help");
        help.add("Help");
        help.add("About");
        mb.add(help);

        setMenuBar(mb);
    }

    public URL getResources(String s) { return this.getClass().getResource(s); }

    public void loadImages() {
        try {
            bkg        = tool.getImage(getResources("res/bkg.jpg"));
            ico        = tool.getImage(getResources("res/ico.png"));
            color      = tool.getImage(getResources("res/color.png"));
            calculates = tool.getImage(getResources("res/calculates.gif"));
        } catch (Throwable e) {
            System.out.println("Eroare la incarcarea imaginilor!");
        }
    }

    public boolean handleEvent(Event e) {
        if (e.id == Event.WINDOW_DESTROY) {
            // Oprim recunoasterea live daca ruleaza
            if (faceRecognizer != null && faceRecognizer.isRunning())
                faceRecognizer.stopLive();
            System.exit(0);

        } else if (e.id == Event.ACTION_EVENT && e.target instanceof MenuItem) {
            String cmd = (String) e.arg;

            // ---------------------------------------------------------------
            // Meniu File
            // ---------------------------------------------------------------
            if ("Exit".equals(cmd)) {
                if (faceRecognizer != null && faceRecognizer.isRunning())
                    faceRecognizer.stopLive();
                System.exit(0);

            } else if ("Load Input Data".equals(cmd)) {
                ind.loadInputData();
                return true;

            // ---------------------------------------------------------------
            // Meniu Algorithms — algoritmi existenti
            // ---------------------------------------------------------------
            } else if ("Median".equals(cmd)) {
                if (ind.V != null) {
                    resetAlgorithm();
                    algorithm = new Median(this);
                    showControl();
                }
                return true;

            } else if ("Perceptron".equals(cmd)) {
                if (ind.V != null) {
                    resetAlgorithm();
                    algorithm = new Perceptron(this);
                    showControl();
                }
                return true;

            } else if ("Median-Perceptron".equals(cmd)) {
                if (ind.V != null) {
                    resetAlgorithm();
                    algorithm = new MPerceptron(this);
                    showControl();
                }
                return true;

            } else if ("Dual Perceptron".equals(cmd)) {
                if (ind.V != null) {
                    resetAlgorithm();
                    algorithm = new DualPerceptron(this);
                    showControl();
                }
                return true;

            } else if ("Dual Perceptron NS".equals(cmd)) {
                if (ind.V != null) {
                    resetAlgorithm();
                    algorithm = new DualPerceptronNS(this);
                    showControl();
                }
                return true;

            // --- SMO Sigmoid (NOU) ---
            } else if ("SMO Sigmoid".equals(cmd)) {
                if (ind.V != null) {
                    resetAlgorithm();
                    algorithm = new SMO(this); // SMO cu parametrii impliciți
                    showControl();
                }
                return true;

            // ---------------------------------------------------------------
            // Meniu View
            // ---------------------------------------------------------------
            } else if ("Show Simulation Control".equals(cmd)) {
                control.show();
                mb.getMenu(2).getItem(0).setLabel("Hide Simulation Control");
                return true;
            } else if ("Hide Simulation Control".equals(cmd)) {
                control.hide();
                mb.getMenu(2).getItem(0).setLabel("Show Simulation Control");
                return true;
            } else if ("Show Input Data".equals(cmd)) {
                ind.show();
                mb.getMenu(2).getItem(1).setLabel("Hide Input Data");
                return true;
            } else if ("Hide Input Data".equals(cmd)) {
                ind.hide();
                mb.getMenu(2).getItem(1).setLabel("Show Input Data");
                return true;
            } else if ("Show Output Data".equals(cmd)) {
                outd.show();
                mb.getMenu(2).getItem(2).setLabel("Hide Output Data");
                return true;
            } else if ("Hide Output Data".equals(cmd)) {
                outd.hide();
                mb.getMenu(2).getItem(2).setLabel("Show Output Data");
                return true;
            } else if ("Show Cursor Coordinates".equals(cmd)) {
                design.show_coords = true;
                design.repaint();
                mb.getMenu(2).getItem(4).setLabel("Hide Cursor Coordinates");
                return true;
            } else if ("Hide Cursor Coordinates".equals(cmd)) {
                design.show_coords = false;
                design.repaint();
                mb.getMenu(2).getItem(4).setLabel("Show Cursor Coordinates");
                return true;

            // ---------------------------------------------------------------
            // Meniu Face Detection (NOU)
            // ---------------------------------------------------------------

            } else if ("Train Head Detector".equals(cmd)) {
                // Cerinta 1: antreneaza SVM pentru detectia capului
                handleTrainHeadDetector();
                return true;

            } else if ("Load Head Detector".equals(cmd)) {
                // Incarca un model de detectie cap existent
                boolean ok = faceDetector.loadModel(FaceDetector.DEFAULT_MODEL_PATH);
                showMessage(ok ? "Model detectie cap incarcat cu succes!"
                              : "Eroare: modelul nu a putut fi incarcat.\n"
                              + "Verificati ca fisierul '"
                              + FaceDetector.DEFAULT_MODEL_PATH + "' exista.");
                return true;

            // ---------------------------------------------------------------
            // Meniu Face Recognition (NOU)
            // ---------------------------------------------------------------

            } else if ("Collect Training Data".equals(cmd)) {
                // Cerinta 3: colecteaza imagini de antrenament
                handleCollectTrainingData();
                return true;

            } else if ("View Training Data".equals(cmd)) {
                // Cerinta 4: vizualizeaza si sterge imagini
                imageViewer.showViewer();
                return true;

            } else if ("Train Recognizer".equals(cmd)) {
                // Cerinta 6: antreneaza clasificatoare per persoana
                handleTrainRecognizer();
                return true;

            } else if ("Load Recognizer".equals(cmd)) {
                // Incarca modelele per persoana existente
                boolean ok = faceRecognizer.loadModels(FaceRecognizer.MODELS_PATH);
                showMessage(ok ? "Modele incarcate: "
                              + faceRecognizer.getPersonCount() + " persoane."
                              : "Eroare: modelele nu au putut fi incarcate.");
                return true;

            } else if ("Start Live Recognition".equals(cmd)) {
                // Cerinta 8: porneste recunoasterea live
                handleStartLive();
                return true;

            } else if ("Stop Live Recognition".equals(cmd)) {
                // Opreste recunoasterea live
                faceRecognizer.stopLive();
                design.setLiveFrame(null);
                design.repaint();
                return true;

            // ---------------------------------------------------------------
            // Meniu Tools
            // ---------------------------------------------------------------
            } else if ("Input Data Generator".equals(cmd)) {
                new InputDataGenerator(this);
                return true;
            } else if ("Settings".equals(cmd)) {
                settings.loadSettings();
                settings.show();
                return true;

            // ---------------------------------------------------------------
            // Meniu Help
            // ---------------------------------------------------------------
            } else if ("Help".equals(cmd)) {
                File helpFile = new File("svm/SVM.pdf");
                try {
                    if (helpFile.toString().endsWith(".pdf"))
                        Runtime.getRuntime().exec(
                            "rundll32 url.dll,FileProtocolHandler " + helpFile);
                    else
                        Desktop.getDesktop().open(helpFile);
                } catch (IOException ex) {
                    System.out.println("No application registered for PDFs!");
                }
                return true;
            } else if ("About".equals(cmd)) {
                about.show();
                return true;
            }

        } else {
            return false;
        }
        return super.handleEvent(e);
    }

    // -----------------------------------------------------------------------
    // Handlere pentru functionalitati noi
    // -----------------------------------------------------------------------

    /**
     * Trateaza comanda "Train Head Detector" (cerinta 1).
     * Deschide un dialog pentru a specifica directoarele cu imagini pozitive
     * si negative, apoi antreneaza SVM-ul de detectie cap intr-un thread separat.
     */
    private void handleTrainHeadDetector() {
        if (!faceDetector.isReady()) {
            showMessage("ATENTIE: Antrenarea poate dura mult timp (minute).\n"
                      + "Asigurati-va ca aveti:\n"
                      + "  - Directorul 'positives/' cu imagini de fete\n"
                      + "  - Directorul 'negatives/' cu imagini fara fete\n\n"
                      + "Antrenarea incepe acum in fundal.");
        }
        // Antrenam intr-un thread separat pentru a nu bloca GUI-ul
        Thread t = new Thread(new Runnable() {
            public void run() {
                faceDetector.train(
                    "positives",          // directorul cu imagini pozitive
                    "negatives",          // directorul cu imagini negative
                    1,                    // 1 patch negativ per imagine (echilibru cu pozitivele)
                    FaceDetector.DEFAULT_MODEL_PATH,
                    FaceDetector.DEFAULT_HOG_PATH
                );
                showMessage("Antrenare detector cap finalizata!\n"
                          + "Model salvat in: " + FaceDetector.DEFAULT_MODEL_PATH);
            }
        });
        t.setDaemon(true);
        t.start();
    }

    /**
     * Trateaza comanda "Collect Training Data" (cerinta 3).
     * Cere pseudonimul persoanei si porneste colectarea in fundal.
     */
    private void handleCollectTrainingData() {
        if (!faceDetector.isReady()) {
            showMessage("Eroare: Detectorul de cap nu este incarcat!\n"
                      + "Folositi mai intai Face Detection > Train/Load Head Detector.");
            return;
        }

        // Dialog pentru introducerea pseudonimului
        final Dialog dlg = new Dialog(this, "Colectare imagini", true);
        dlg.setLayout(new BorderLayout(5, 5));
        dlg.setBackground(Color.darkGray);
        dlg.resize(320, 160);
        dlg.move((res.width - 320) / 2, (res.height - 160) / 2);

        Panel centerP = new Panel(new GridLayout(2, 2, 5, 5));
        centerP.setBackground(Color.darkGray);

        Label lbl = new Label("Pseudonim persoana:");
        lbl.setForeground(Color.white);
        final TextField tf = new TextField("", 20);
        tf.setBackground(Color.black);
        tf.setForeground(Color.green);

        Label lblN = new Label("Numar imagini:");
        lblN.setForeground(Color.white);
        final TextField tfN = new TextField(IMAGES_PER_PERSON + "", 10);
        tfN.setBackground(Color.black);
        tfN.setForeground(Color.green);

        centerP.add(lbl); centerP.add(tf);
        centerP.add(lblN); centerP.add(tfN);
        dlg.add("Center", centerP);

        Panel btnP = new Panel(new FlowLayout());
        btnP.setBackground(Color.darkGray);
        Button btnOk  = new Button("Start colectare");
        Button btnCancel = new Button("Anuleaza");
        btnOk.setBackground(new Color(50, 120, 50));
        btnOk.setForeground(Color.white);
        btnCancel.setBackground(Color.gray);
        btnCancel.setForeground(Color.white);

        final boolean[] ok = {false};
        btnOk.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { ok[0] = true; dlg.dispose(); }
        });
        btnCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { dlg.dispose(); }
        });

        btnP.add(btnOk); btnP.add(btnCancel);
        dlg.add("South", btnP);
        dlg.setVisible(true); // blocheaza pana la raspuns

        if (!ok[0]) return;

        final String personName = tf.getText().trim();
        if (personName.isEmpty()) {
            showMessage("Pseudonimul nu poate fi gol!");
            return;
        }

        // Citim numarul de imagini
        int nImages = IMAGES_PER_PERSON;
        try {
            nImages = Integer.parseInt(tfN.getText().trim());
        } catch (NumberFormatException ex) {
            nImages = IMAGES_PER_PERSON;
        }
        final int finalN = nImages;

        dataCollector.setImagesPerPerson(finalN);

        // Setam listener de progres pentru afisare in consola
        dataCollector.setProgressListener(new DataCollector.ProgressListener() {
            public void onProgress(String name, int saved, int total,
                                   BufferedImage lastFrame) {
                System.out.println("Colectare " + name + ": "
                                 + saved + "/" + total);
                // Afisam frame-ul curent in panoul Design
                design.setLiveFrame(lastFrame);
                design.setCollectingMode(true);
                design.repaint();
            }
            public void onDone(String name, int saved) {
                design.setLiveFrame(null);
                design.setCollectingMode(false);
                design.repaint();
                showMessage("Colectare finalizata pentru " + name
                          + ": " + saved + " imagini salvate.");
            }
            public void onNoFaceDetected(String name) {
                // Nu facem nimic special — mesajul ar fi prea frecvent
            }
        });

        // Pornim colectarea intr-un thread separat
        Thread t = new Thread(new Runnable() {
            public void run() { dataCollector.collect(personName); }
        });
        t.setDaemon(true);
        t.start();

        showMessage("Colectare pornita pentru: " + personName
                  + "\nPozitionati-va in fata camerei.\n"
                  + "Vor fi salvate " + finalN + " imagini.");
    }

    /**
     * Trateaza comanda "Train Recognizer" (cerinta 6).
     * Antreneaza clasificatoarele per persoana intr-un thread separat.
     */
    private void handleTrainRecognizer() {
        File trainDir = new File(TRAINING_DIR);
        if (!trainDir.exists() || trainDir.listFiles() == null) {
            showMessage("Eroare: directorul '" + TRAINING_DIR + "' nu exista!\n"
                      + "Folositi mai intai Face Recognition > Collect Training Data.");
            return;
        }

        showMessage("Antrenarea clasificatoarelor incepe acum in fundal.\n"
                  + "Poate dura cateva minute in functie de numarul de persoane.");

        Thread t = new Thread(new Runnable() {
            public void run() {
                faceRecognizer.trainAll(FaceRecognizer.MODELS_PATH);
                showMessage("Antrenare finalizata!\n"
                          + faceRecognizer.getPersonCount()
                          + " clasificatoare salvate.");
            }
        });
        t.setDaemon(true);
        t.start();
    }

    /**
     * Trateaza comanda "Start Live Recognition" (cerinta 8).
     */
    private void handleStartLive() {
        if (!faceDetector.isReady()) {
            showMessage("Eroare: Detectorul de cap nu este incarcat!");
            return;
        }
        if (faceRecognizer.getPersonCount() == 0) {
            showMessage("Eroare: Nu exista modele de recunoastere incarcate!\n"
                      + "Folositi Face Recognition > Train/Load Recognizer.");
            return;
        }
        if (faceRecognizer.isRunning()) {
            showMessage("Recunoasterea live ruleaza deja!");
            return;
        }
        faceRecognizer.startLiveAsync(); // porneste in thread daemon
        showMessage("Recunoastere live pornita la 10 FPS.\n"
                  + "Persoane in model: " + faceRecognizer.getPersonCount()
                  + "\nFolositi Stop Live Recognition pentru oprire.");
    }

    // -----------------------------------------------------------------------
    // Utilitare GUI
    // -----------------------------------------------------------------------

    /**
     * Afiseaza un mesaj informativ intr-un dialog simplu.
     * @param msg mesajul de afisat
     */
    private void showMessage(String msg) {
        // Rulam pe EDT pentru siguranta
        final String message = msg;
        // Adaugam 'final' aici pentru a putea folosi 'dlg' in interiorul butonului
        final Dialog dlg = new Dialog(this, "Informatie", true); 
        dlg.setLayout(new BorderLayout(5, 5));
        dlg.setBackground(Color.darkGray);
        dlg.resize(380, 160);
        dlg.move((res.width - 380) / 2, (res.height - 160) / 2);

        TextArea ta = new TextArea(message, 4, 40,
                                    TextArea.SCROLLBARS_VERTICAL_ONLY);
        ta.setBackground(Color.black);
        ta.setForeground(Color.white);
        ta.setEditable(false);
        dlg.add("Center", ta);

        Panel p = new Panel(new FlowLayout());
        p.setBackground(Color.darkGray);
        Button btn = new Button("OK");
        btn.setBackground(Color.gray);
        btn.setForeground(Color.white);
        
        // Reparam actiunea butonului:
        btn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e2) {
                dlg.dispose(); // Acum se va inchide corect!
            }
        });
        
        p.add(btn);
        dlg.add("South", p);
        dlg.setVisible(true);
    }

    /**
     * Opreste algoritmul curent si reseteaza starea GUI-ului.
     */
    private void resetAlgorithm() {
        if (algorithm != null) {
            algorithm.stop_();
            algorithm = null;
            init2();
        }
    }

    /**
     * Afiseaza panoul de control al simularii.
     */
    private void showControl() {
        control.show();
        mb.getMenu(2).getItem(0).setLabel("Hide Simulation Control");
    }

    public void init() {
        if (algorithm != null) {
            algorithm.stop_();
            algorithm = null;
        }
        ind.input_file = null;
        ind.V          = null;
        design.show_line   = false;
        design.calculates  = false;
        control.init       = true;
        control.start.setLabel("Start Simulation");
        design.repaint();
    }

    public void init2() {
        design.show_line  = false;
        design.calculates = false;
        control.init      = true;
        control.start.setLabel("Start Simulation");
        design.repaint();
        ind.init();
    }
}
