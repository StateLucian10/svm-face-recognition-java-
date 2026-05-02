package face;

/**
 * HOG - Histogram of Oriented Gradients
 *
 * Algoritm de extragere a vectorului de trasaturi dintr-o imagine.
 * Descris de Dalal si Triggs (2005) pentru detectia persoanelor.
 *
 * Pasi:
 *   1. Convertim imaginea la tonuri de gri (grayscale)
 *   2. Calculam gradientii orizontal si vertical pentru fiecare pixel
 *   3. Calculam magnitudinea si orientarea gradientului per pixel
 *   4. Impartim imaginea in celule (cellSize x cellSize pixeli)
 *   5. Pentru fiecare celula construim o histograma a orientarilor (nbins directii)
 *   6. Grupam celulele in blocuri (blockSize x blockSize celule) si normalizam L2
 *   7. Concatenam toate histogramele normalizate in vectorul final de trasaturi
 *
 * Parametrii impliciți pentru imagini 128x128:
 *   cellSize  = 8  => 16x16 celule
 *   blockSize = 2  => blocuri de 2x2 celule
 *   nbins     = 9  => 9 directii in [0, 180) grade (gradiente nesemnate)
 *   Dimensiune vector = (16-1)*(16-1) * 2*2 * 9 = 15*15*36 = 8100
 */
public class HOG {

    /** Dimensiunea unei celule in pixeli (ex: 8 => celula 8x8 pixeli) */
    private int cellSize;

    /** Dimensiunea unui bloc in celule (ex: 2 => bloc 2x2 celule) */
    private int blockSize;

    /** Numarul de directii (bins) in histograma de orientari [0, 180) grade */
    private int nbins;

    /** Epsilon pentru stabilitate numerica la normalizare L2 */
    private static final double EPS = 1e-6;

    /**
     * Constructorul implicit cu parametrii recomandati de Dalal & Triggs.
     * Potrivit pentru imagini de 128x128 pixeli.
     */
    public HOG() {
        this.cellSize  = 8;
        this.blockSize = 2;
        this.nbins     = 9;
    }

    /**
     * Constructor cu parametrii configurabili.
     * @param cellSize  dimensiunea celulei in pixeli
     * @param blockSize dimensiunea blocului in celule
     * @param nbins     numarul de directii in histograma
     */
    public HOG(int cellSize, int blockSize, int nbins) {
        this.cellSize  = cellSize;
        this.blockSize = blockSize;
        this.nbins     = nbins;
    }

    /**
     * Extrage vectorul HOG dintr-o imagine reprezentata ca array 2D de pixeli RGB.
     * Acesta este punctul de intrare principal al algoritmului.
     * @param pixels matricea de pixeli (pixels[y][x] = valoare RGB packed int)
     * @param width  latimea imaginii in pixeli
     * @param height inaltimea imaginii in pixeli
     * @return vectorul de trasaturi HOG ca array de float
     */
    public float[] extract(int[][] pixels, int width, int height) {
        // Pasul 1: convertim imaginea la tonuri de gri
        float[][] gray = toGrayscale(pixels, width, height);

        // Pasul 2 si 3: calculam gradientii si obtinem magnitude si orientare
        float[][] magnitude   = new float[height][width];
        float[][] orientation = new float[height][width];
        computeGradients(gray, width, height, magnitude, orientation);

        // Pasul 4 si 5: calculam histogramele de orientari per celula
        int nCellsX = width  / cellSize; // numarul de celule pe orizontala
        int nCellsY = height / cellSize; // numarul de celule pe verticala
        // histCell[cy][cx][bin] = valoarea bin-ului pentru celula (cx, cy)
        float[][][] histCell = computeCellHistograms(magnitude, orientation,
                                                     nCellsX, nCellsY);

        // Pasul 6 si 7: normalizam blocurile si concatenam in vectorul final
        return normalizeAndConcatenate(histCell, nCellsX, nCellsY);
    }

    /**
     * Extrage vectorul HOG dintr-o imagine reprezentata ca array 1D de pixeli RGB.
     * Suprasarcina convenabila pentru formatul java.awt.image.BufferedImage.getRGB().
     * @param pixels array 1D de pixeli RGB (row-major: index = y*width + x)
     * @param width  latimea imaginii
     * @param height inaltimea imaginii
     * @return vectorul HOG
     */
    public float[] extract(int[] pixels, int width, int height) {
        // Convertim array-ul 1D in matrice 2D pentru procesare uniforma
        int[][] pixels2D = new int[height][width];
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                pixels2D[y][x] = pixels[y * width + x]; // row-major -> matrice
        return extract(pixels2D, width, height);
    }

    /**
     * Converteste o imagine RGB la tonuri de gri folosind formula luminantei:
     *   gray = 0.299*R + 0.587*G + 0.114*B
     * Aceasta formula aproximeaza perceptia umana a luminozitatii.
     * @param pixels matricea de pixeli RGB (packed int: 0xAARRGGBB)
     * @param width  latimea imaginii
     * @param height inaltimea imaginii
     * @return matricea de valori gri in [0, 255]
     */
    private float[][] toGrayscale(int[][] pixels, int width, int height) {
        float[][] gray = new float[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = pixels[y][x];
                // Extragem componentele R, G, B din int-ul packed
                int r = (rgb >> 16) & 0xFF; // biti 16-23
                int g = (rgb >>  8) & 0xFF; // biti 8-15
                int b =  rgb        & 0xFF; // biti 0-7
                // Aplicam formula luminantei ITU-R BT.601
                gray[y][x] = 0.299f * r + 0.587f * g + 0.114f * b;
            }
        }
        return gray;
    }

    /**
     * Calculeaza gradientii imaginii folosind filtrele centrate [-1, 0, 1]:
     *   Gx[y][x] = gray[y][x+1] - gray[y][x-1]  (gradient orizontal)
     *   Gy[y][x] = gray[y+1][x] - gray[y-1][x]  (gradient vertical)
     *
     * Magnitudinea: M[y][x] = sqrt(Gx^2 + Gy^2)
     * Orientarea:  theta[y][x] = atan2(|Gy|, |Gx|) in grade, in [0, 180)
     *              Folosim orientari nesemnate (unsigned) — 0 si 180 sunt identice.
     *
     * Pixelii de pe margine (y=0, y=H-1, x=0, x=W-1) primesc gradient 0
     * deoarece nu au vecini pe ambele parti.
     *
     * @param gray      imaginea grayscale de intrare
     * @param width     latimea
     * @param height    inaltimea
     * @param magnitude matricea de magnitudini (output)
     * @param orient    matricea de orientari in grade [0,180) (output)
     */
    private void computeGradients(float[][] gray, int width, int height,
                                   float[][] magnitude, float[][] orient) {
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                // Filtrul centrat [-1, 0, +1] pe orizontala si verticala
                float gx = gray[y][x + 1] - gray[y][x - 1]; // gradient orizontal
                float gy = gray[y + 1][x] - gray[y - 1][x]; // gradient vertical

                // Magnitudinea gradientului
                magnitude[y][x] = (float) Math.sqrt(gx * gx + gy * gy);

                // Orientarea in [0, 180) grade (nesemnata — ignoram directia)
                // atan2 returneaza in [-pi, pi], luam valoarea absoluta -> [0, pi]
                // Impartim la pi si inmultim cu 180 pentru a obtine grade
                double angle = Math.toDegrees(Math.atan2(Math.abs(gy), Math.abs(gx)));
                // Clipam in [0, 180) pentru siguranta
                if (angle >= 180.0) angle -= 180.0;
                orient[y][x] = (float) angle;
            }
        }
        // Marginile raman 0 (initializare implicita Java pentru float[][])
    }

    /**
     * Calculeaza histogramele de orientari pentru fiecare celula.
     * Fiecare celula are nbins directii distribuite uniform in [0, 180) grade.
     * Fiecare pixel contribuie la cele 2 bin-uri vecine proportional cu distanta
     * (interpolare bilineara pe orientare — soft binning).
     *
     * @param magnitude matricea de magnitudini
     * @param orient    matricea de orientari in grade [0, 180)
     * @param nCellsX   numarul de celule pe orizontala
     * @param nCellsY   numarul de celule pe verticala
     * @return histCell[cy][cx][bin] — histogramele per celula
     */
    private float[][][] computeCellHistograms(float[][] magnitude, float[][] orient,
                                               int nCellsX, int nCellsY) {
        float[][][] histCell = new float[nCellsY][nCellsX][nbins];
        double binWidth = 180.0 / nbins; // latimea unui bin in grade (ex: 20 grade pentru 9 bins)

        for (int cy = 0; cy < nCellsY; cy++) {
            for (int cx = 0; cx < nCellsX; cx++) {
                // Coordonatele pixelilor din aceasta celula
                int yStart = cy * cellSize;
                int xStart = cx * cellSize;

                for (int py = yStart; py < yStart + cellSize; py++) {
                    for (int px = xStart; px < xStart + cellSize; px++) {
                        float mag = magnitude[py][px]; // magnitudinea pixelului
                        float ang = orient[py][px];    // orientarea in [0, 180)

                        // Calculam bin-ul de baza si fractia pentru interpolare
                        double binFloat = ang / binWidth - 0.5; // bin fractionar
                        int    bin0     = (int) Math.floor(binFloat); // bin stanga
                        double frac     = binFloat - bin0;             // fractia -> bin dreapta

                        // Bin-ul din dreapta (cu wrap-around circular)
                        int bin1 = (bin0 + 1) % nbins;
                        // Bin-ul din stanga (cu wrap-around)
                        bin0 = ((bin0 % nbins) + nbins) % nbins;

                        // Distribuim magnitudinea proportional (soft binning)
                        histCell[cy][cx][bin0] += mag * (float)(1.0 - frac);
                        histCell[cy][cx][bin1] += mag * (float) frac;
                    }
                }
            }
        }
        return histCell;
    }

    /**
     * Normalizeaza histogramele la nivel de bloc si concateneaza in vectorul final.
     * Un bloc este format din blockSize x blockSize celule adiacente.
     * Normalizarea L2 reduce influenta variatiilor de iluminare.
     *
     * Formula normalizare L2-Hys (Dalal & Triggs):
     *   v_norm = v / sqrt(||v||^2 + eps^2)
     *   apoi clipam la 0.2 si renormalizam (L2-Hys)
     *
     * Blocurile se suprapun cu pasul 1 celula (stride = 1).
     *
     * @param histCell histogramele per celula
     * @param nCellsX  numarul de celule pe orizontala
     * @param nCellsY  numarul de celule pe verticala
     * @return vectorul HOG final (float[])
     */
    private float[] normalizeAndConcatenate(float[][][] histCell,
                                             int nCellsX, int nCellsY) {
        // Numarul de blocuri (cu stride 1, blocurile se suprapun)
        int nBlocksX = nCellsX - blockSize + 1; // ex: 16-2+1 = 15
        int nBlocksY = nCellsY - blockSize + 1; // ex: 16-2+1 = 15

        // Dimensiunea totala a vectorului HOG
        int vecSize = nBlocksY * nBlocksX * blockSize * blockSize * nbins;
        float[] hog = new float[vecSize]; // vectorul de trasaturi final
        int idx = 0; // indexul curent in vectorul HOG

        for (int by = 0; by < nBlocksY; by++) {
            for (int bx = 0; bx < nBlocksX; bx++) {

                // Colectam toate valorile din blocul (bx, by)
                // Un bloc are blockSize*blockSize celule, fiecare cu nbins valori
                float[] blockVec = new float[blockSize * blockSize * nbins];
                int k = 0;
                for (int dy = 0; dy < blockSize; dy++) {
                    for (int dx = 0; dx < blockSize; dx++) {
                        int cy = by + dy; // indexul celulei pe verticala
                        int cx = bx + dx; // indexul celulei pe orizontala
                        for (int bin = 0; bin < nbins; bin++) {
                            blockVec[k++] = histCell[cy][cx][bin];
                        }
                    }
                }

                // Normalizare L2-Hys (varianta recomandata de Dalal & Triggs)
                // Pasul 1: normalizare L2 initiala
                double norm = 0.0;
                for (int i = 0; i < blockVec.length; i++)
                    norm += (double)blockVec[i] * blockVec[i];
                norm = Math.sqrt(norm + EPS * EPS); // adaugam EPS pentru stabilitate
                for (int i = 0; i < blockVec.length; i++)
                    blockVec[i] = (float)(blockVec[i] / norm);

                // Pasul 2: clipam la 0.2 (reduce influenta gradientilor foarte mari)
                for (int i = 0; i < blockVec.length; i++)
                    if (blockVec[i] > 0.2f) blockVec[i] = 0.2f;

                // Pasul 3: renormalizam dupa clipping
                norm = 0.0;
                for (int i = 0; i < blockVec.length; i++)
                    norm += (double)blockVec[i] * blockVec[i];
                norm = Math.sqrt(norm + EPS * EPS);
                for (int i = 0; i < blockVec.length; i++)
                    blockVec[i] = (float)(blockVec[i] / norm);

                // Copiem valorile normalizate in vectorul HOG final
                for (int i = 0; i < blockVec.length; i++)
                    hog[idx++] = blockVec[i];
            }
        }

        return hog; // vectorul complet de trasaturi HOG
    }

    /**
     * Calculeaza dimensiunea vectorului HOG pentru o imagine data.
     * Util pentru a pre-aloca structuri de date inainte de extragere.
     * @param width  latimea imaginii in pixeli
     * @param height inaltimea imaginii in pixeli
     * @return dimensiunea vectorului HOG
     */
    public int getVectorSize(int width, int height) {
        int nCellsX  = width  / cellSize;
        int nCellsY  = height / cellSize;
        int nBlocksX = nCellsX - blockSize + 1;
        int nBlocksY = nCellsY - blockSize + 1;
        return nBlocksY * nBlocksX * blockSize * blockSize * nbins;
    }

    // --- Getteri ---

    /** @return dimensiunea celulei in pixeli */
    public int getCellSize()  { return cellSize; }

    /** @return dimensiunea blocului in celule */
    public int getBlockSize() { return blockSize; }

    /** @return numarul de bins din histograma */
    public int getNbins()     { return nbins; }
}
