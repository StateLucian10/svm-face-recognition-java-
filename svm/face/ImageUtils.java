package face;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.ImageIO;

/**
 * ImageUtils - utilitare pentru procesarea imaginilor.
 *
 * Furnizeaza operatii de baza necesare detectiei si recunoasterii faciale:
 *   - incarcare imagine din fisier
 *   - conversie la grayscale
 *   - redimensionare (resize) bilineara
 *   - decupare (crop) a unei regiuni dreptunghiulare
 *   - salvare imagine pe disc
 *   - extragere pixeli ca array pentru HOG
 *
 * Toate operatiile sunt implementate manual, fara biblioteci specializate,
 * conform cerintelor proiectului. Se foloseste doar java.awt si javax.imageio
 * pentru I/O de baza, permise explicit in cerinte.
 */
public class ImageUtils {

    // -----------------------------------------------------------------------
    // Incarcare si salvare
    // -----------------------------------------------------------------------

    /**
     * Incarca o imagine din fisier si o returneaza ca BufferedImage.
     * Suporta formatele JPG, PNG, BMP, GIF (standard javax.imageio).
     * @param path calea completa catre fisierul imagine
     * @return imaginea incarcata, sau null daca fisierul nu poate fi citit
     */
    public static BufferedImage load(String path) {
        try {
            // ImageIO.read decodifica formatul automat pe baza header-ului
            return ImageIO.read(new File(path));
        } catch (IOException e) {
            System.out.println("ImageUtils: nu pot incarca imaginea: " + path);
            return null;
        }
    }

    /**
     * Salveaza o imagine BufferedImage pe disc in format JPG.
     * @param img    imaginea de salvat
     * @param path   calea completa de destinatie (inclusiv extensia)
     * @return true daca salvarea a reusit, false altfel
     */
    public static boolean save(BufferedImage img, String path) {
        try {
            // Determinam extensia pentru a alege formatul corect
            String ext = "jpg";
            int dot = path.lastIndexOf('.');
            if (dot >= 0) ext = path.substring(dot + 1).toLowerCase();
            return ImageIO.write(img, ext, new File(path));
        } catch (IOException e) {
            System.out.println("ImageUtils: nu pot salva imaginea: " + path);
            return false;
        }
    }

    // -----------------------------------------------------------------------
    // Redimensionare bilineara
    // -----------------------------------------------------------------------

    /**
     * Redimensioneaza o imagine la dimensiunile specificate folosind
     * interpolarea bilineara.
     *
     * Interpolarea bilineara calculeaza valoarea unui pixel din imaginea
     * destinatie ca medie ponderata a celor 4 pixeli vecini din imaginea sursa,
     * proportional cu aria de suprapunere. Produce rezultate mai netede decat
     * interpolarea nearest-neighbor.
     *
     * @param src     imaginea sursa
     * @param newW    noua latime in pixeli
     * @param newH    noua inaltime in pixeli
     * @return noua imagine redimensionata
     */
    public static BufferedImage resize(BufferedImage src, int newW, int newH) {
        int srcW = src.getWidth();   // latimea sursei
        int srcH = src.getHeight();  // inaltimea sursei

        // Cream imaginea destinatie de tip RGB
        BufferedImage dst = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);

        // Raportul de scalare: cat de mult dintr-un pixel sursa corespunde unui pixel destinatie
        double scaleX = (double) srcW / newW; // factor de scalare orizontal
        double scaleY = (double) srcH / newH; // factor de scalare vertical

        for (int dy = 0; dy < newH; dy++) {
            for (int dx = 0; dx < newW; dx++) {
                // Coordonatele corespunzatoare in imaginea sursa (fractionale)
                double sx = dx * scaleX; // pozitia orizontala reala in sursa
                double sy = dy * scaleY; // pozitia verticala reala in sursa

                // Cei 4 vecini intregi din sursa
                int x0 = (int) sx;                           // vecinul stanga
                int y0 = (int) sy;                           // vecinul sus
                int x1 = Math.min(x0 + 1, srcW - 1);       // vecinul dreapta (clipat)
                int y1 = Math.min(y0 + 1, srcH - 1);       // vecinul jos (clipat)

                // Fractiile pentru interpolare (distantele pana la vecinii intregi)
                double fx = sx - x0; // fractia pe orizontala: 0=stanga, 1=dreapta
                double fy = sy - y0; // fractia pe verticala: 0=sus, 1=jos

                // Extragem culorile celor 4 vecini
                int c00 = src.getRGB(x0, y0); // stanga-sus
                int c10 = src.getRGB(x1, y0); // dreapta-sus
                int c01 = src.getRGB(x0, y1); // stanga-jos
                int c11 = src.getRGB(x1, y1); // dreapta-jos

                // Interpolam fiecare canal de culoare (R, G, B) separat
                int r = bilinear(r(c00), r(c10), r(c01), r(c11), fx, fy);
                int g = bilinear(g(c00), g(c10), g(c01), g(c11), fx, fy);
                int b = bilinear(b(c00), b(c10), b(c01), b(c11), fx, fy);

                // Scriem pixelul interpolat in imaginea destinatie
                dst.setRGB(dx, dy, rgb(r, g, b));
            }
        }
        return dst;
    }

    /**
     * Interpolare bilineara pentru un singur canal de culoare.
     * Formula: (1-fx)*(1-fy)*v00 + fx*(1-fy)*v10 + (1-fx)*fy*v01 + fx*fy*v11
     * unde v00, v10, v01, v11 sunt valorile celor 4 vecini.
     * @param v00 valoarea vecinului stanga-sus
     * @param v10 valoarea vecinului dreapta-sus
     * @param v01 valoarea vecinului stanga-jos
     * @param v11 valoarea vecinului dreapta-jos
     * @param fx  fractia orizontala in [0, 1)
     * @param fy  fractia verticala in [0, 1)
     * @return valoarea interpolata, rotunjita si clipata in [0, 255]
     */
    private static int bilinear(int v00, int v10, int v01, int v11,
                                 double fx, double fy) {
        // Interpolam mai intai pe orizontala (intre stanga si dreapta)
        double top    = v00 * (1.0 - fx) + v10 * fx; // randul de sus
        double bottom = v01 * (1.0 - fx) + v11 * fx; // randul de jos
        // Apoi interpolam pe verticala (intre sus si jos)
        double result = top * (1.0 - fy) + bottom * fy;
        // Clipam rezultatul in [0, 255] si rotunjim
        return (int) Math.max(0, Math.min(255, Math.round(result)));
    }

    // -----------------------------------------------------------------------
    // Decupare (crop)
    // -----------------------------------------------------------------------

    /**
     * Decupeaza o regiune dreptunghiulara dintr-o imagine.
     * Daca dreptunghiul iese din limitele imaginii, il clipam automat.
     * @param src imaginea sursa
     * @param x   coordonata x a coltului stanga-sus al regiunii
     * @param y   coordonata y a coltului stanga-sus al regiunii
     * @param w   latimea regiunii in pixeli
     * @param h   inaltimea regiunii in pixeli
     * @return sub-imaginea decupata
     */
    public static BufferedImage crop(BufferedImage src, int x, int y, int w, int h) {
        int srcW = src.getWidth();
        int srcH = src.getHeight();

        // Clipam coordonatele pentru a nu depasi limitele imaginii
        int x0 = Math.max(0, x);
        int y0 = Math.max(0, y);
        int x1 = Math.min(srcW, x + w); // coordonata dreapta (exclusiva)
        int y1 = Math.min(srcH, y + h); // coordonata jos (exclusiva)

        int cropW = x1 - x0; // latimea efectiva dupa clipping
        int cropH = y1 - y0; // inaltimea efectiva dupa clipping

        if (cropW <= 0 || cropH <= 0) {
            // Dreptunghiul e complet in afara imaginii — returnam imagine goala 1x1
            return new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        }

        // Cream imaginea destinatie si copiem pixelii
        BufferedImage dst = new BufferedImage(cropW, cropH, BufferedImage.TYPE_INT_RGB);
        for (int dy = 0; dy < cropH; dy++)
            for (int dx = 0; dx < cropW; dx++)
                dst.setRGB(dx, dy, src.getRGB(x0 + dx, y0 + dy));

        return dst;
    }

    // -----------------------------------------------------------------------
    // Conversie la grayscale
    // -----------------------------------------------------------------------

    /**
     * Converteste o imagine color la tonuri de gri.
     * Foloseste formula luminantei: gray = 0.299R + 0.587G + 0.114B.
     * Imaginea rezultata are tipul TYPE_BYTE_GRAY.
     * @param src imaginea color de intrare
     * @return imaginea in tonuri de gri
     */
    public static BufferedImage toGrayscale(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        // TYPE_BYTE_GRAY stocheaza un singur canal de 8 biti per pixel
        BufferedImage gray = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = src.getRGB(x, y);
                // Calculam luminanta
                int lum = (int)(0.299 * r(rgb) + 0.587 * g(rgb) + 0.114 * b(rgb));
                // Scriem ca pixel gri (R=G=B=lum)
                gray.setRGB(x, y, rgb(lum, lum, lum));
            }
        }
        return gray;
    }

    // -----------------------------------------------------------------------
    // Extragere pixeli pentru HOG
    // -----------------------------------------------------------------------

    /**
     * Extrage pixelii unei imagini ca array 1D de int-uri RGB.
     * Formatul este row-major: pixel(x,y) = array[y*width + x].
     * Aceasta forma e acceptata direct de HOG.extract(int[], int, int).
     * @param img imaginea de procesat
     * @return array-ul de pixeli RGB
     */
    public static int[] getPixels(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        int[] pixels = new int[w * h];
        // getRGB cu coordonate de start si dimensiuni umple array-ul direct
        img.getRGB(0, 0, w, h, pixels, 0, w);
        return pixels;
    }

    /**
     * Pipeline complet: incarca o imagine, o redimensioneaza la targetW x targetH
     * si extrage vectorul HOG. Folosit la antrenarea clasificatoarelor.
     * @param path    calea catre fisierul imagine
     * @param targetW latimea tinta (ex: 128)
     * @param targetH inaltimea tinta (ex: 128)
     * @param hog     instanta HOG configurata
     * @return vectorul HOG, sau null daca imaginea nu poate fi incarcata
     */
    public static float[] loadAndExtractHOG(String path, int targetW, int targetH, HOG hog) {
        BufferedImage img = load(path);
        if (img == null) return null;                       // fisier invalid
        BufferedImage resized = resize(img, targetW, targetH); // redimensionam la 128x128
        int[] pixels = getPixels(resized);                  // extragem pixelii
        return hog.extract(pixels, targetW, targetH);       // calculam HOG
    }

    /**
     * Pipeline pentru o portiune dintr-o imagine (crop + resize + HOG).
     * Folosit in sliding window si in recunoasterea faciala live.
     * @param src     imaginea sursa
     * @param x       coordonata x a regiunii
     * @param y       coordonata y a regiunii
     * @param w       latimea regiunii
     * @param h       inaltimea regiunii
     * @param targetW latimea tinta dupa resize
     * @param targetH inaltimea tinta dupa resize
     * @param hog     instanta HOG
     * @return vectorul HOG al regiunii
     */
    public static float[] cropResizeHOG(BufferedImage src,
                                         int x, int y, int w, int h,
                                         int targetW, int targetH, HOG hog) {
        BufferedImage cropped = crop(src, x, y, w, h);         // decupam regiunea
        BufferedImage resized = resize(cropped, targetW, targetH); // redimensionam
        int[] pixels = getPixels(resized);                      // extragem pixelii
        return hog.extract(pixels, targetW, targetH);           // calculam HOG
    }

    // -----------------------------------------------------------------------
    // Utilitare pentru manipularea culorilor (packed int RGB)
    // -----------------------------------------------------------------------

    /**
     * Extrage componenta rosie (R) dintr-un pixel packed int (0xAARRGGBB).
     * @param rgb pixelul packed
     * @return valoarea R in [0, 255]
     */
    public static int r(int rgb) { return (rgb >> 16) & 0xFF; }

    /**
     * Extrage componenta verde (G) dintr-un pixel packed int.
     * @param rgb pixelul packed
     * @return valoarea G in [0, 255]
     */
    public static int g(int rgb) { return (rgb >> 8) & 0xFF; }

    /**
     * Extrage componenta albastra (B) dintr-un pixel packed int.
     * @param rgb pixelul packed
     * @return valoarea B in [0, 255]
     */
    public static int b(int rgb) { return rgb & 0xFF; }

    /**
     * Construieste un pixel packed int din componentele R, G, B.
     * Formatul rezultat: 0x00RRGGBB (alpha=0, ignorat de BufferedImage RGB).
     * @param r componenta rosie [0, 255]
     * @param g componenta verde [0, 255]
     * @param b componenta albastra [0, 255]
     * @return pixelul packed
     */
    public static int rgb(int r, int g, int b) {
        return ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    /**
     * Verifica daca un fisier are extensie de imagine suportata.
     * @param filename numele fisierului
     * @return true daca extensia e jpg, jpeg, png, bmp sau gif
     */
    public static boolean isImageFile(String filename) {
        if (filename == null) return false;
        String lower = filename.toLowerCase();
        return lower.endsWith(".jpg")  || lower.endsWith(".jpeg") ||
               lower.endsWith(".png")  || lower.endsWith(".bmp")  ||
               lower.endsWith(".gif");
    }
}
