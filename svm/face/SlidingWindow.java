package face;

import java.util.*;

/**
 * SlidingWindow - detectie prin fereastra glisanta.
 *
 * Parcurge o imagine la mai multe scari si pozitii cu o fereastra de dimensiune
 * fixa, returnand coordonatele ferestrelor candidate (potential capete).
 *
 * Algoritm:
 *   1. La fiecare scara, redimensionam imaginea cu factorul scaleFactor^k
 *   2. Glisam fereastra de dimensiune winW x winH cu pasul stepSize
 *   3. Fiecare fereastra e un candidat — clasificatorul decide daca e cap
 *   4. Candidatii pozitivi sunt procesati prin NMS (Non-Maximum Suppression)
 *      pentru a elimina detectiile redundante suprapuse
 *
 * Coordonatele returnate sunt intotdeauna in sistemul imaginii ORIGINALE,
 * nu al imaginii scalate.
 */
public class SlidingWindow {

    /** Latimea ferestrei de detectie in pixeli (imaginea scalata la 128x128) */
    private int winW;

    /** Inaltimea ferestrei de detectie in pixeli */
    private int winH;

    /** Pasul ferestrei pe orizontala si verticala (in pixeli) */
    private int stepSize;

    /** Factorul de reducere a imaginii la fiecare scala (ex: 1.25) */
    private double scaleFactor;

    /** Numarul minim de scari de cautare */
    private int minScale;

    /** Pragul NMS: doua detectii cu suprapunere > nmsThresh sunt considerate duplicate */
    private double nmsThresh;

    /**
     * O detectie candidata: pozitia in imaginea originala si scorul clasificatorului.
     */
    public static class Detection {
        /** Coordonatele in imaginea originala */
        public int x, y, w, h;
        /** Scorul clasificatorului (mai mare = mai sigur) */
        public double score;

        public Detection(int x, int y, int w, int h, double score) {
            this.x = x; this.y = y;
            this.w = w; this.h = h;
            this.score = score;
        }

        /** Aria patratului cap */
        public int area() { return w * h; }
    }

    /**
     * Constructor cu parametrii impliciți.
     * winW=winH=128 corespunde imaginilor 128x128 din cerinta.
     */
    public SlidingWindow() {
        this.winW        = 128;
        this.winH        = 128;
        this.stepSize    = 48;   // pas mare => mai putine ferestre => mai rapid
        this.scaleFactor = 2.0;  // doar 2-3 scari in piramida => mai rapid
        this.minScale    = 1;    // cel putin 1 scala
        this.nmsThresh   = 0.3;  // NMS standard
    }

    /**
     * Constructor cu parametrii configurabili.
     * @param winW        latimea ferestrei
     * @param winH        inaltimea ferestrei
     * @param stepSize    pasul glisarii
     * @param scaleFactor factorul de scalare intre nivele
     * @param nmsThresh   pragul NMS pentru suprapunere
     */
    public SlidingWindow(int winW, int winH, int stepSize,
                          double scaleFactor, double nmsThresh) {
        this.winW        = winW;
        this.winH        = winH;
        this.stepSize    = stepSize;
        this.scaleFactor = scaleFactor;
        this.minScale    = 1;
        this.nmsThresh   = nmsThresh;
    }

    /**
     * Ruleaza fereastra glisanta pe o imagine si returneaza detectiile finale.
     * @param img        imaginea pe care se face detectia (BufferedImage)
     * @param classifier functia de clasificare — primeste HOG si returneaza scorul
     * @param hog        instanta HOG pentru extragerea trasaturilor
     * @return lista de detectii dupa NMS, sortate descrescator dupa scor
     */
    public List<Detection> detect(java.awt.image.BufferedImage img,
                                   Classifier classifier, HOG hog) {
        int origW = img.getWidth();
        int origH = img.getHeight();

        List<Detection> candidates = new ArrayList<Detection>();

        // Parcurgem imaginea la mai multe scari
        double scale = 1.0; // incepem cu imaginea la scara originala
        java.awt.image.BufferedImage scaled = img;

        while (true) {
            int scaledW = scaled.getWidth();
            int scaledH = scaled.getHeight();

            // Daca imaginea scalata e mai mica decat fereastra, oprim
            if (scaledW < winW || scaledH < winH) break;

            // Glisam fereastra pe imaginea scalata
            for (int y = 0; y + winH <= scaledH; y += stepSize) {
                for (int x = 0; x + winW <= scaledW; x += stepSize) {

                    // Extragem vectorul HOG din fereastra curenta
                    float[] hogVec = ImageUtils.cropResizeHOG(
                        scaled, x, y, winW, winH, 128, 128, hog);

                    // Clasificam fereastra
                    double score = classifier.score(hogVec);

                    // Daca scorul e pozitiv, e un candidat cap
                    if (score > -0.90) { // prag mai strict
                        int origX = (int)(x * scale);
                        int origY = (int)(y * scale);
                        int origBoxW = (int)(winW * scale);
                        int origBoxH = (int)(winH * scale);
                        // Filtram detectiile prea mici (zgomot) sau prea mari (fundal)
                        if (origBoxW >= 80 && origBoxW <= origW * 3 / 4) {
                            candidates.add(new Detection(origX, origY,
                                                          origBoxW, origBoxH, score));
                        }
                    }
                }
            }

            // Trecem la urmatoarea scala (reducem imaginea)
            scale *= scaleFactor;
            int newW = (int)(origW / scale);
            int newH = (int)(origH / scale);
            if (newW < winW || newH < winH) break; // imaginea ar fi prea mica
            scaled = ImageUtils.resize(img, newW, newH);
        }

        // Aplicam NMS pentru a elimina detectiile redundante
        return nonMaxSuppression(candidates);
    }

    /**
     * Non-Maximum Suppression (NMS) — elimina detectiile suprapuse redundante.
     *
     * Algoritm greedy:
     *   1. Sortam detectiile descrescator dupa scor
     *   2. Luam detectia cu cel mai mare scor si o adaugam in rezultat
     *   3. Eliminam toate detectiile care se suprapun cu ea mai mult de nmsThresh
     *   4. Repetam cu detectia urmatoare neprocesata
     *
     * Masura de suprapunere: IoU (Intersection over Union)
     *   IoU = aria_intersectie / aria_reuniune
     *
     * @param detections lista de detectii candidate
     * @return lista filtrata de detectii distincte
     */
    public List<Detection> nonMaxSuppression(List<Detection> detections) {
        if (detections.isEmpty()) return detections;

        // Sortam descrescator dupa scor (cel mai bun candidat primul)
        Collections.sort(detections, new Comparator<Detection>() {
            public int compare(Detection a, Detection b) {
                return Double.compare(b.score, a.score); // descrescator
            }
        });

        List<Detection> result  = new ArrayList<Detection>();
        boolean[] suppressed    = new boolean[detections.size()]; // marcat ca eliminat

        for (int i = 0; i < detections.size(); i++) {
            if (suppressed[i]) continue; // deja eliminat

            Detection di = detections.get(i);
            result.add(di); // adaugam cea mai buna detectie ramasa

            // Eliminam toate detectiile care se suprapun prea mult cu di
            for (int j = i + 1; j < detections.size(); j++) {
                if (suppressed[j]) continue;
                Detection dj = detections.get(j);
                if (iou(di, dj) > nmsThresh)
                    suppressed[j] = true; // suprapunere prea mare — eliminam
            }
        }
        return result;
    }

    /**
     * Calculeaza IoU (Intersection over Union) intre doua detectii.
     * IoU = aria_intersectiei / aria_reuniunii
     * Valori: 0 = fara suprapunere, 1 = suprapunere perfecta
     * @param a prima detectie
     * @param b a doua detectie
     * @return valoarea IoU in [0, 1]
     */
    private double iou(Detection a, Detection b) {
        // Calculam coordonatele dreptunghiului de intersectie
        int interX1 = Math.max(a.x, b.x);               // stanga intersectie
        int interY1 = Math.max(a.y, b.y);               // sus intersectie
        int interX2 = Math.min(a.x + a.w, b.x + b.w);  // dreapta intersectie
        int interY2 = Math.min(a.y + a.h, b.y + b.h);  // jos intersectie

        // Daca nu se suprapun, intersectia e vida
        if (interX2 <= interX1 || interY2 <= interY1) return 0.0;

        // Aria intersectiei
        double interArea = (double)(interX2 - interX1) * (interY2 - interY1);

        // Aria reuniunii = suma ariilor - aria intersectiei (formula incluziune-excludere)
        double unionArea = (double)(a.area() + b.area()) - interArea;

        return interArea / unionArea; // IoU
    }

    /**
     * Gaseste detectia cu aria maxima dintr-o lista.
     * Folosita la cerinta 2: "patratul cap cu aria maxima".
     * @param detections lista de detectii
     * @return detectia cu aria maxima, sau null daca lista e goala
     */
    public static Detection getLargest(List<Detection> detections) {
        if (detections == null || detections.isEmpty()) return null;
        Detection largest = detections.get(0);
        for (int i = 1; i < detections.size(); i++) {
            if (detections.get(i).area() > largest.area())
                largest = detections.get(i);
        }
        return largest;
    }

    /**
     * Interfata functionala pentru clasificatorul folosit in sliding window.
     * Permite injectarea oricarui clasificator (SVMModel, SMO direct, etc.)
     */
    public interface Classifier {
        /**
         * Returneaza scorul functiei de decizie pentru vectorul HOG dat.
         * Scor > 0 => cap detectat, scor < 0 => non-cap.
         * @param hogVec vectorul HOG extras din fereastra curenta
         * @return scorul clasificatorului
         */
        double score(float[] hogVec);
    }

    // --- Getteri ---
    public int    getWinW()        { return winW; }
    public int    getWinH()        { return winH; }
    public int    getStepSize()    { return stepSize; }
    public double getScaleFactor() { return scaleFactor; }
    public double getNmsThresh()   { return nmsThresh; }
}
