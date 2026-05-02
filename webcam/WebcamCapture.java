package webcam;

import org.opencv.core.*;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.awt.image.BufferedImage;

/**
 * WebcamCapture - captureaza imagini de la camera web folosind OpenCV.
 *
 * Aceasta clasa este singurul loc din proiect unde se foloseste OpenCV
 * pentru captura video, conform conditiei 3:
 * "Este permisa utilizarea functiilor native doar pentru legatura cu camera web."
 *
 * Conversia BGR->RGB din Mat in BufferedImage se face in Java pur,
 * fara Imgproc, respectand conditia 3 care interzice procesarea imaginii cu OpenCV.
 */
public class WebcamCapture {

    /** Obiectul OpenCV de captura video */
    private VideoCapture capture;

    /** Indexul camerei (0 = camera implicita a sistemului) */
    private int cameraIndex;

    /** Latimea frame-ului captat (pixeli) */
    private int width;

    /** Inaltimea frame-ului captat (pixeli) */
    private int height;

    /** FPS-ul dorit (cadre pe secunda) */
    private int fps;

    /** True daca camera este deschisa si functionala */
    private boolean opened;

    /** Flag pentru incarcarea bibliotecii OpenCV (se face o singura data) */
    private static boolean opencvLoaded = false;

    /**
     * Incarca biblioteca nativa OpenCV.
     * Trebuie apelata inainte de orice utilizare a WebcamCapture.
     */
    public static void loadOpenCV() {
        if (!opencvLoaded) {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
            opencvLoaded = true;
            System.out.println("WebcamCapture: biblioteca OpenCV incarcata.");
        }
    }

    /**
     * Constructor cu parametrii impliciți.
     * Camera 0, rezolutie 320x240, 10 FPS.
     */
    public WebcamCapture() {
        this(0, 320, 240, 10);
    }

    /**
     * Constructor cu parametrii configurabili.
     * @param cameraIndex indexul camerei (0 = implicita)
     * @param width       latimea dorita a frame-ului
     * @param height      inaltimea dorita a frame-ului
     * @param fps         numarul de cadre pe secunda
     */
    public WebcamCapture(int cameraIndex, int width, int height, int fps) {
        this.cameraIndex = cameraIndex;
        this.width       = width;
        this.height      = height;
        this.fps         = fps;
        this.opened      = false;
    }

    // -----------------------------------------------------------------------
    // Deschidere si inchidere camera
    // -----------------------------------------------------------------------

    /**
     * Deschide camera web si o configureaza.
     * @return true daca camera a fost deschisa cu succes
     */
    public boolean open() {
        loadOpenCV();

        capture = new VideoCapture(cameraIndex);

        if (!capture.isOpened()) {
            System.out.println("WebcamCapture: nu pot deschide camera " + cameraIndex);
            opened = false;
            return false;
        }

        capture.set(Videoio.CAP_PROP_FRAME_WIDTH,  width);
        capture.set(Videoio.CAP_PROP_FRAME_HEIGHT, height);
        capture.set(Videoio.CAP_PROP_FPS, fps);

        opened = true;
        System.out.println("WebcamCapture: camera " + cameraIndex + " deschisa ("
                         + width + "x" + height + " @ " + fps + "fps)");
        return true;
    }

    /**
     * Inchide camera web si elibereaza resursele OpenCV.
     */
    public void close() {
        if (capture != null && capture.isOpened()) {
            capture.release();
        }
        opened = false;
        System.out.println("WebcamCapture: camera inchisa.");
    }

    // -----------------------------------------------------------------------
    // Capturare frame
    // -----------------------------------------------------------------------

    /**
     * Captureaza un singur frame de la camera si il returneaza ca BufferedImage.
     * Conversia BGR->RGB se face in Java pur, fara Imgproc (conditia 3).
     * @return frame-ul captat ca BufferedImage, sau null daca captura a esuat
     */
    public BufferedImage captureFrame() {
        if (!opened || capture == null || !capture.isOpened()) {
            System.out.println("WebcamCapture: camera nu este deschisa!");
            return null;
        }

        Mat frame = new Mat();
        boolean success = capture.read(frame); // citim frame de la camera cu OpenCV

        if (!success || frame.empty()) {
            System.out.println("WebcamCapture: nu am putut citi frame-ul.");
            frame.release();
            return null;
        }

        // Convertim Mat BGR in BufferedImage RGB in Java pur (fara Imgproc)
        BufferedImage img = matToBufferedImageBGR(frame);

        frame.release(); // eliberam memoria nativa
        return img;
    }

    /**
     * Converteste o matrice Mat OpenCV (format BGR) intr-un BufferedImage Java (format RGB).
     * Conversia se face complet in Java pur, fara nicio functie OpenCV,
     * respectand conditia 3 care permite OpenCV doar pentru captura si desenare patrate.
     *
     * OpenCV stocheaza pixelii in ordine BGR: [B0,G0,R0, B1,G1,R1, ...]
     * BufferedImage TYPE_INT_RGB asteapta ordinea: 0x00RRGGBB per pixel.
     * Inversam bytes-ii B si R manual pentru fiecare pixel.
     *
     * @param mat matricea OpenCV CV_8UC3 in format BGR
     * @return imaginea Java in format RGB
     */
    private BufferedImage matToBufferedImageBGR(Mat mat) {
        int w = mat.cols(); // latimea in pixeli
        int h = mat.rows(); // inaltimea in pixeli

        // Extragem toti bytes-ii bruti din Mat intr-un array Java
        // Fiecare pixel = 3 bytes in ordine BGR
        byte[] bgrData = new byte[w * h * 3];
        mat.get(0, 0, bgrData); // copiem din memoria nativa OpenCV in Java

        // Cream BufferedImage de tip RGB
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        // Convertim fiecare pixel din BGR in RGB prin inversarea canalelor B si R
        for (int i = 0; i < w * h; i++) {
            // Extragem cele 3 canale din bytes-ii BGR
            // & 0xFF converteste byte signed (-128..127) in int unsigned (0..255)
            int b = bgrData[i * 3]     & 0xFF; // Blue  (primul byte in BGR)
            int g = bgrData[i * 3 + 1] & 0xFF; // Green (al doilea byte)
            int r = bgrData[i * 3 + 2] & 0xFF; // Red   (al treilea byte in BGR)

            // Construim pixelul RGB packed: 0x00RRGGBB
            // Deplasam R cu 16 biti, G cu 8 biti, B ramane pe pozitia 0
            int rgb = (r << 16) | (g << 8) | b;

            // Setam pixelul in imaginea Java la coordonatele (x, y)
            img.setRGB(i % w, i / w, rgb);
        }

        return img;
    }

    /**
     * Captureaza un frame si asteapta intervalul corespunzator FPS-ului.
     * Intervalul tinta = 1000ms / fps (ex: 100ms pentru 10 FPS).
     * @return frame-ul captat, sau null daca captura a esuat
     */
    public BufferedImage captureFrameWithDelay() {
        long startTime = System.currentTimeMillis();
        BufferedImage frame = captureFrame();

        long elapsed    = System.currentTimeMillis() - startTime;
        long targetInterval = 1000L / fps;
        long waitTime   = targetInterval - elapsed;

        if (waitTime > 0) {
            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        return frame;
    }

    // -----------------------------------------------------------------------
    // Getteri si setteri
    // -----------------------------------------------------------------------

    /** @return true daca camera este deschisa */
    public boolean isOpened() { return opened; }

    /** @return latimea frame-ului */
    public int getWidth() { return width; }

    /** @return inaltimea frame-ului */
    public int getHeight() { return height; }

    /** @return FPS-ul setat */
    public int getFps() { return fps; }

    /**
     * Seteaza un nou FPS.
     * @param fps noul FPS dorit
     */
    public void setFps(int fps) {
        this.fps = fps;
        if (opened && capture != null)
            capture.set(Videoio.CAP_PROP_FPS, fps);
    }
}
