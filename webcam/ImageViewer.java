package webcam;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import face.ImageUtils;

/**
 * ImageViewer - vizualizarea si stergerea imaginilor de antrenament.
 *
 * Implementeaza cerinta 4:
 *   "Programul va permite vizualizarea imaginilor salvate la (3) si stergerea
 *    imaginilor necorespunzatoare (neclare, incadrate rau, etc)."
 *
 * Interfata grafica:
 *   - Lista cu persoanele disponibile (subdirectoare din rootDir)
 *   - Imaginea curenta afisata mare in centru
 *   - Butoane: Inapoi / Inainte pentru navigare
 *   - Buton: Sterge (elimina imaginea curenta de pe disc)
 *   - Contor: "imagine X din Y - nume_fisier"
 */
public class ImageViewer extends Dialog implements ActionListener, ItemListener {

    /** Directorul radacina cu imaginile tuturor persoanelor */
    private String rootDir;

    /**
     * Lista persoanelor disponibile.
     * Folosim java.awt.List explicit pentru a evita conflictul cu java.util.List.
     */
    private java.awt.List personList;

    /** Numele persoanei selectate curent */
    private String currentPerson;

    /** Lista fisierelor imaginii persoanei curente */
    private java.util.List<File> imageFiles;

    /** Indexul imaginii curente in lista imageFiles */
    private int currentIndex;

    /** Zona de desenare a imaginii curente */
    private ImagePanel imagePanel;

    /** Eticheta cu informatii despre imaginea curenta */
    private Label infoLabel;

    /** Butoanele de navigare si stergere */
    private Button btnPrev, btnNext, btnDelete, btnClose;

    /** Dimensiunea de afisare a imaginii in panou */
    private static final int DISPLAY_SIZE = 256;

    /**
     * Constructor.
     * @param parent  fereastra parinte
     * @param rootDir directorul radacina cu imaginile persoanelor
     */
    public ImageViewer(Frame parent, String rootDir) {
        super(parent, "Image Viewer - Training Data", false);
        this.rootDir    = rootDir;
        this.imageFiles = new java.util.ArrayList<File>();
        this.currentIndex = 0;

        setBackground(Color.darkGray);
        setLayout(new BorderLayout(5, 5));
        resize(700, 520);
        move(100, 100);

        buildUI();
        populatePersonList();
    }

    // -----------------------------------------------------------------------
    // Constructia interfetei
    // -----------------------------------------------------------------------

    private void buildUI() {
        // --- Panoul stanga: lista persoanelor ---
        Panel leftPanel = new Panel(new BorderLayout());
        leftPanel.setBackground(Color.darkGray);

        Label persLabel = new Label("Persoane:", Label.CENTER);
        persLabel.setForeground(Color.white);
        leftPanel.add("North", persLabel);

        // java.awt.List explicit — evita conflictul cu java.util.List
        personList = new java.awt.List(10, false);
        personList.setBackground(Color.black);
        personList.setForeground(Color.green);
        personList.addItemListener(this);
        leftPanel.add("Center", personList);

        add("West", leftPanel);

        // --- Panoul central: imaginea curenta ---
        imagePanel = new ImagePanel();
        imagePanel.setBackground(Color.black);
        add("Center", imagePanel);

        // --- Panoul jos: controale ---
        Panel bottomPanel = new Panel(new BorderLayout(5, 5));
        bottomPanel.setBackground(Color.darkGray);

        infoLabel = new Label("Selectati o persoana din lista.", Label.CENTER);
        infoLabel.setForeground(Color.white);
        bottomPanel.add("North", infoLabel);

        Panel btnPanel = new Panel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        btnPanel.setBackground(Color.darkGray);

        btnPrev   = new Button("< Inapoi");
        btnNext   = new Button("Inainte >");
        btnDelete = new Button("Sterge");
        btnClose  = new Button("Inchide");

        btnPrev.setBackground(Color.gray);
        btnNext.setBackground(Color.gray);
        btnDelete.setBackground(new Color(180, 50, 50));
        btnClose.setBackground(Color.gray);

        btnPrev.setForeground(Color.white);
        btnNext.setForeground(Color.white);
        btnDelete.setForeground(Color.white);
        btnClose.setForeground(Color.white);

        btnPrev.addActionListener(this);
        btnNext.addActionListener(this);
        btnDelete.addActionListener(this);
        btnClose.addActionListener(this);

        btnPanel.add(btnPrev);
        btnPanel.add(btnNext);
        btnPanel.add(btnDelete);
        btnPanel.add(btnClose);

        bottomPanel.add("South", btnPanel);
        add("South", bottomPanel);
    }

    // -----------------------------------------------------------------------
    // Popularea listei de persoane
    // -----------------------------------------------------------------------

    private void populatePersonList() {
        personList.removeAll();
        File root = new File(rootDir);
        if (!root.exists()) {
            infoLabel.setText("Directorul " + rootDir + " nu exista!");
            return;
        }
        File[] dirs = root.listFiles(new FileFilter() {
            public boolean accept(File f) { return f.isDirectory(); }
        });
        if (dirs == null || dirs.length == 0) {
            infoLabel.setText("Nu exista persoane in " + rootDir);
            return;
        }
        Arrays.sort(dirs);
        for (File d : dirs) {
            int cnt = countImages(d);
            personList.add(d.getName() + " (" + cnt + ")");
        }
    }

    private int countImages(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return 0;
        int cnt = 0;
        for (File f : files)
            if (ImageUtils.isImageFile(f.getName())) cnt++;
        return cnt;
    }

    // -----------------------------------------------------------------------
    // Incarcare imagini pentru persoana selectata
    // -----------------------------------------------------------------------

    private void loadPersonImages(String personName) {
        currentPerson = personName;
        imageFiles.clear();
        currentIndex = 0;

        File dir = new File(rootDir + File.separator + personName);
        if (!dir.exists()) return;

        File[] files = dir.listFiles();
        if (files == null) return;

        for (File f : files)
            if (ImageUtils.isImageFile(f.getName()))
                imageFiles.add(f);

        Collections.sort(imageFiles);

        if (!imageFiles.isEmpty()) {
            showImage(0);
        } else {
            infoLabel.setText(personName + ": nicio imagine gasita.");
            imagePanel.setImage(null);
            imagePanel.repaint();
        }
    }

    // -----------------------------------------------------------------------
    // Afisare imagine
    // -----------------------------------------------------------------------

    private void showImage(int index) {
        if (imageFiles.isEmpty()) return;
        if (index < 0) index = 0;
        if (index >= imageFiles.size()) index = imageFiles.size() - 1;
        currentIndex = index;

        File f = imageFiles.get(currentIndex);
        BufferedImage img = ImageUtils.load(f.getAbsolutePath());

        if (img != null) {
            BufferedImage displayed = ImageUtils.resize(img, DISPLAY_SIZE, DISPLAY_SIZE);
            imagePanel.setImage(displayed);
            imagePanel.repaint();
        } else {
            imagePanel.setImage(null);
            imagePanel.repaint();
        }

        infoLabel.setText(currentPerson + "  |  Imaginea " + (currentIndex + 1)
                        + " din " + imageFiles.size()
                        + "  |  " + f.getName());

        btnPrev.setEnabled(currentIndex > 0);
        btnNext.setEnabled(currentIndex < imageFiles.size() - 1);
    }

    // -----------------------------------------------------------------------
    // Stergere imagine
    // -----------------------------------------------------------------------

    private void deleteCurrentImage() {
        if (imageFiles.isEmpty() || currentPerson == null) return;

        File toDelete = imageFiles.get(currentIndex);
        String name   = toDelete.getName();

        Dialog confirm = new Dialog((Frame)getParent(), "Confirmare stergere", true);
        confirm.setLayout(new BorderLayout(5, 5));
        confirm.setBackground(Color.darkGray);
        confirm.resize(350, 130);
        confirm.move(getLocation().x + 100, getLocation().y + 150);

        Label msg = new Label("Stergi imaginea: " + name + "?", Label.CENTER);
        msg.setForeground(Color.white);
        confirm.add("Center", msg);

        Panel btnP = new Panel(new FlowLayout());
        btnP.setBackground(Color.darkGray);
        Button btnYes = new Button("Da, sterge");
        Button btnNo  = new Button("Nu, pastreaza");
        btnYes.setBackground(new Color(180, 50, 50));
        btnYes.setForeground(Color.white);
        btnNo.setBackground(Color.gray);
        btnNo.setForeground(Color.white);

        final boolean[] confirmed = {false};
        btnYes.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                confirmed[0] = true;
                confirm.dispose();
            }
        });
        btnNo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { confirm.dispose(); }
        });

        btnP.add(btnYes);
        btnP.add(btnNo);
        confirm.add("South", btnP);
        confirm.setVisible(true);

        if (!confirmed[0]) return;

        boolean deleted = toDelete.delete();
        if (deleted) {
            imageFiles.remove(currentIndex);
            populatePersonList();
            if (imageFiles.isEmpty()) {
                infoLabel.setText(currentPerson + ": toate imaginile au fost sterse.");
                imagePanel.setImage(null);
                imagePanel.repaint();
            } else {
                if (currentIndex >= imageFiles.size())
                    currentIndex = imageFiles.size() - 1;
                showImage(currentIndex);
            }
        } else {
            infoLabel.setText("Eroare: nu am putut sterge " + name);
        }
    }

    // -----------------------------------------------------------------------
    // Tratare evenimente
    // -----------------------------------------------------------------------

    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if      (src == btnPrev)   showImage(currentIndex - 1);
        else if (src == btnNext)   showImage(currentIndex + 1);
        else if (src == btnDelete) deleteCurrentImage();
        else if (src == btnClose)  setVisible(false);
    }

    public void itemStateChanged(ItemEvent e) {
        if (e.getSource() == personList) {
            String selected = personList.getSelectedItem();
            if (selected != null) {
                int parenIdx = selected.lastIndexOf(" (");
                String name  = parenIdx >= 0
                             ? selected.substring(0, parenIdx)
                             : selected;
                loadPersonImages(name);
            }
        }
    }

    public boolean handleEvent(Event e) {
        if (e.id == Event.WINDOW_DESTROY) {
            setVisible(false);
            return true;
        }
        return super.handleEvent(e);
    }

    public void showViewer() {
        populatePersonList();
        setVisible(true);
    }

    // -----------------------------------------------------------------------
    // Panel de afisare imagine
    // -----------------------------------------------------------------------

    private static class ImagePanel extends Panel {
        private BufferedImage image;

        public ImagePanel() { setBackground(Color.black); }

        public void setImage(BufferedImage img) { this.image = img; }

        public void paint(Graphics g) {
            g.setColor(Color.black);
            g.fillRect(0, 0, getWidth(), getHeight());
            if (image == null) {
                g.setColor(Color.gray);
                g.drawString("Nicio imagine selectata",
                             getWidth()/2 - 60, getHeight()/2);
                return;
            }
            int x = (getWidth()  - image.getWidth())  / 2;
            int y = (getHeight() - image.getHeight()) / 2;
            g.drawImage(image, x, y, this);
            g.setColor(Color.darkGray);
            g.drawRect(x-1, y-1, image.getWidth()+1, image.getHeight()+1);
        }

        public void update(Graphics g) { paint(g); }
    }
}
