package gui;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import svm.SVM;

/**
 * Design - panoul principal de desenare al aplicatiei.
 *
 * Extins fata de versiunea de la laborator cu suport pentru afisarea
 * frame-urilor live de la camera web (cerintele 3 si 8).
 *
 * Daca liveFrame != null, panoul afiseaza frame-ul live scalat sa umple
 * suprafata disponibila, in loc de panoul SVM clasic cu puncte si axe.
 * Daca liveFrame == null, comportamentul e identic cu versiunea originala.
 */
public class Design extends Panel implements MouseListener, MouseMotionListener {

    SVM svm;
    Image im;
    Graphics img;
    int ww, hh;
    int Ox, Oy, cx, cy, ccx, ccy;
    boolean init = true;
    String coords = "";
    public boolean show_coords = false;
    public boolean show_line   = false;
    public boolean calculates  = false;
    public int x1, y1, x2, y2;

    /**
     * Frame-ul live de la camera web.
     * Daca e != null, se afiseaza in locul panoului SVM clasic.
     * Setat de FaceRecognizer si DataCollector prin SVM.java.
     */
    private BufferedImage liveFrame = null;

    /** True cand DataCollector colecteaza imagini — afiseaza chenarul de ghidare */
    private boolean collectingMode = false;

    public Design(SVM svm) {
        this.svm = svm;
        addMouseListener(this);
        addMouseMotionListener(this);
    }

    /**
     * Seteaza frame-ul live de afisat.
     * Apelata de FaceRecognizer (cerinta 8) si DataCollector (cerinta 3).
     * @param frame frame-ul de afisat, sau null pentru a reveni la modul SVM
     */
    /**
     * Activeaza/dezactiveaza modul de colectare cu chenar de ghidare.
     * @param collecting true = afiseaza chenar, false = ascunde
     */
    public void setCollectingMode(boolean collecting) {
        this.collectingMode = collecting;
    }

    public void setLiveFrame(BufferedImage frame) {
        this.liveFrame = frame;
    }

    public void initO() {
        Ox = ww / 2; Oy = hh / 2;
        cx = 0; cy = 0;
        repaint();
    }

    public void paint(Graphics g) { update(g); }

    public void update(Graphics g) {
        if (init) {
            ww  = size().width;
            hh  = size().height;
            im  = createImage(ww, hh);
            img = im.getGraphics();
            initO();
            init = false;
        }

        // Daca avem un frame live, il afisam in locul panoului SVM
        if (liveFrame != null) {
            drawLiveFrame(img);
        } else {
            drawSVMPanel(img);
        }

        g.drawImage(im, 0, 0, this);
    }

    /**
     * Deseneaza frame-ul live de la camera, scalat sa umple panoul.
     * Pastreaza proportiile imaginii (letterbox daca e nevoie).
     * @param g contextul grafic al buffer-ului off-screen
     */
    private void drawLiveFrame(Graphics g) {
        // Fundalul negru
        g.setColor(Color.black);
        g.fillRect(0, 0, ww, hh);

        if (liveFrame == null) return;

        int imgW = liveFrame.getWidth();
        int imgH = liveFrame.getHeight();

        // Calculam scalarea pentru a umple panoul pastrand proportiile
        double scaleX = (double) ww / imgW;
        double scaleY = (double) hh / imgH;
        double scale  = Math.min(scaleX, scaleY); // letterbox

        int drawW = (int)(imgW * scale);
        int drawH = (int)(imgH * scale);
        int drawX = (ww - drawW) / 2; // centram orizontal
        int drawY = (hh - drawH) / 2; // centram vertical

        // Desenam frame-ul scalat
        g.drawImage(liveFrame, drawX, drawY, drawW, drawH, this);

        // Afisam un indicator ca modul live e activ
        g.setColor(Color.green);
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.drawString("LIVE", 10, 20);

        // Daca e modul colectare, desenam chenarul de ghidare
        if (collectingMode) {
            // Calculam chenarul centrat in imagine (60% din dimensiunea minima)
            int guideSize = (int)(Math.min(drawW, drawH) * 0.6);
            int guideX = drawX + (drawW - guideSize) / 2;
            int guideY = drawY + (int)((drawH - guideSize) * 0.35);
            g.setColor(Color.green);
            Graphics2D g2d = (g instanceof Graphics2D) ? (Graphics2D) g : null;
            if (g2d != null) {
                float[] dash = {10f, 5f};
                g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_MITER, 10f, dash, 0f));
                g2d.drawRect(guideX, guideY, guideSize, guideSize);
                g2d.setStroke(new BasicStroke(1));
            } else {
                g.drawRect(guideX, guideY, guideSize, guideSize);
            }
            g.setFont(new Font("SansSerif", Font.BOLD, 11));
            g.drawString("Pozitioneaza fata in chenar", guideX, guideY - 8);
        }
    }

    /**
     * Deseneaza panoul SVM clasic (axe, puncte, linie de separare).
     * Identic cu implementarea originala de la laborator.
     * @param g contextul grafic al buffer-ului off-screen
     */
    private void drawSVMPanel(Graphics g) {
        g.setColor(svm.settings.background_color);
        g.fillRect(0, 0, ww, hh);

        if (svm.ind.V != null) {
            drawAxis(g);

            if (show_line) {
                g.setColor(svm.settings.line_color);
                g.drawLine(Ox + x1, Oy - y1, Ox + x2, Oy - y2);
            }

            for (int i = 0; i < svm.ind.V.length; i++) {
                Point p = new Point(
                    Ox + (int)(svm.ind.V[i].X[0] + 0.5),
                    Oy - (int)(svm.ind.V[i].X[1] + 0.5));
                g.setColor(svm.ind.V[i].cl.color);
                int r = svm.settings.point_radius;
                g.fillOval(p.x - r, p.y - r, 2 * r, 2 * r);
                g.setColor(Color.black);
                g.drawOval(p.x - r, p.y - r, 2 * r, 2 * r);
            }

            if (show_coords) {
                g.setColor(svm.settings.string_color);
                g.drawString(coords, ccx + 15, ccy + 30);
            }
        }
    }

    public void drawAxis(Graphics g) {
        if (svm.settings.grid) {
            g.setColor(svm.settings.grid_color);
            for (int i = Ox + svm.settings.axis_min;
                     i <= Ox + svm.settings.axis_max;
                     i += svm.settings.grid_size)
                g.drawLine(i, Oy + svm.settings.axis_min,
                           i, Oy + svm.settings.axis_max);
            for (int j = Oy + svm.settings.axis_min;
                     j <= Oy + svm.settings.axis_max;
                     j += svm.settings.grid_size)
                g.drawLine(Ox + svm.settings.axis_min, j,
                           Ox + svm.settings.axis_max, j);
        }
        if (svm.settings.axis) {
            g.setColor(svm.settings.axis_color);
            g.drawLine(Ox + svm.settings.axis_min, Oy,
                       Ox + svm.settings.axis_max, Oy);
            g.drawLine(Ox, Oy + svm.settings.axis_min,
                       Ox, Oy + svm.settings.axis_max);
            if (svm.settings.gradations) {
                for (int i = Ox; i <= Ox + svm.settings.axis_max;
                         i += svm.settings.axis_gradations)
                    g.drawLine(i, Oy - 2, i, Oy + 2);
                for (int i = Ox; i >= Ox + svm.settings.axis_min;
                         i -= svm.settings.axis_gradations)
                    g.drawLine(i, Oy - 2, i, Oy + 2);
                for (int j = Oy; j <= Oy + svm.settings.axis_max;
                         j += svm.settings.axis_gradations)
                    g.drawLine(Ox - 2, j, Ox + 2, j);
                for (int j = Oy; j >= Oy + svm.settings.axis_min;
                         j -= svm.settings.axis_gradations)
                    g.drawLine(Ox - 2, j, Ox + 2, j);
            }
        }
    }

    public void setPointsOfLine(float[] w) {
        show_line = true;
        if (Math.abs(w[0]) < Math.abs(w[1])) {
            x1 = svm.settings.axis_min;
            y1 = (int)((-w[w.length - 1] - w[0] * x1) / w[1] + 0.5);
            x2 = svm.settings.axis_max;
            y2 = (int)((-w[w.length - 1] - w[0] * x2) / w[1] + 0.5);
        } else {
            y1 = svm.settings.axis_min;
            x1 = (int)((-w[w.length - 1] - w[1] * y1) / w[0] + 0.5);
            y2 = svm.settings.axis_max;
            x2 = (int)((-w[w.length - 1] - w[1] * y2) / w[0] + 0.5);
        }
        repaint();
    }

    public void mouseClicked(MouseEvent me)  { if (liveFrame == null) initO(); }
    public void mouseEntered(MouseEvent me)  {}
    public void mouseExited(MouseEvent me)   {}

    public void mouseMoved(MouseEvent me) {
        if (liveFrame != null) return; // ignoram mouse in modul live
        ccx    = me.getX(); ccy = me.getY();
        coords = "(" + (ccx - Ox) + "," + (Oy - ccy) + ")";
        if (ccx <= 2 || ccx >= ww - 5 || ccy <= 5 || ccy >= hh - 5) coords = "";
        repaint();
    }

    public void mousePressed(MouseEvent me) {
        cx = me.getX(); cy = me.getY();
        coords = "";
    }

    public void mouseDragged(MouseEvent me) {
        if (liveFrame != null) return; // ignoram drag in modul live
        int x = me.getX(), y = me.getY();
        if (svm.ind.V != null) {
            cx = x - cx; cy = y - cy;
            Ox += cx; Oy += cy;
            cx = x; cy = y;
            coords = "";
            repaint();
        }
    }

    public void mouseReleased(MouseEvent me) {}
}
