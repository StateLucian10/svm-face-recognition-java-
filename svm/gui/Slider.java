package gui;

import java.awt.*;
import svm.SVM;

public class Slider extends Panel implements Runnable {
    About about;
    Thread t;
    Image im;
    Graphics img;
    int w1, h1;
    int delay = 50;
    String[] students;
    Font fnt = new Font("Arial", 0, 12);
    int y, w = 10;
    boolean init = true, control;
    int ww, hh;

    public Slider(About about, int ww, int hh) {
    	this.about = about;
		this.ww = ww;
		this.hh = hh;    		
    	setStudents();
		setFont(fnt); 
		start();
    }

	public void setStudents(){
		String[] student = new String[55];
		student[0] = "AILENI ADELINA";
		student[1] = "ALEXANDRU CLAUDIU";
		student[2] = "ANDREI DIANA";
		student[3] = "ASANDEI MARIA";
		student[4] = "AVRAM FLAVIAN";
		student[5] = "BARON ADRIAN";
		student[6] = "BIGHIU RARE\u015E";
		student[7] = "BOI\u021A\u0102 ELENA";
		student[8] = "CIOBOTARIU MARIA";
		student[9] = "CO\u015EULEANU MARIA";
		student[10] = "COTLE\u021A CAMELIA";
		student[11] = "CRE\u021AU ALINA";
		student[12] = "CUMP\u0102RATU ERISA";
		student[13] = "DAMIAN AMINA";
		student[14] = "D\u0102NIL\u0102 CORINA";
		student[15] = "DUDA GABRIEL";
		student[16] = "EN\u0102CHESCU ALEXANDRA";
		student[17] = "FLOREA ANCA";
		student[18] = "FLOREA ROBERT";
		student[19] = "GIMIGA ANDREEA";
		student[20] = "G\u00CESC\u0102 DUMITRU";
		student[21] = "GRIGORA\u015E ANA";
		student[22] = "GRIGORIAN IULIANA";
		student[23] = "GROSU ANTONIA";
		student[24] = "HOV\u00CERNEANU \u015ETEFAN";
		student[25] = "ILIE IOANA";
		student[26] = "MANCIU EMANUEL";
		student[27] = "MANOLE R\u0102ZVAN";
		student[28] = "MAXIM DELIA";
		student[29] = "MEZDRIA EMANUEL";
		student[30] = "MIH\u0102IL\u0102 BIANCA";
		student[31] = "MOLOCI OANA";
		student[32] = "MORARU ROBERT";
		student[33] = "MURGEANU OANA";
		student[34] = "NECHIFOR MIHAI";
		student[35] = "NOEA IONU\u021A";
		student[36] = "ONI\u015EORU DANIEL";
		student[37] = "PA\u015EPARUG\u0102 MIHAI";
		student[38] = "PATRA\u015E VALERIA";
		student[39] = "PAV\u0102L MARIA";
		student[40] = "PIN\u021AOIU ANA";
		student[41] = "PODARIU TEODORA";
		student[42] = "RAIA ANDREEA";
		student[43] = "S\u0102CUIU ANDRA";
		student[44] = "SAMSON LUISA";
		student[45] = "SIMICIN ANDREAS";
		student[46] = "SION ALIN";
		student[47] = "STATE LUCIAN";
		student[48] = "SUMANDRU SEBASTIAN";
		student[49] = "TARADACIUC IONELA";
		student[50] = "TCACI EMANUEL";
		student[51] = "TINCU COSMIN";
		student[52] = "\u021AUGA \u015ETEFAN";
		student[53] = "VOINEA ALEXANDRU";
		student[54] = "VOUCIUC RUBEN";

		students = new String[student.length*100];
		for(int i = 0; i < students.length; i++)
			students[i] = student[i % student.length];
	}
	
    public void start() {
    	if(t == null){
    		t = new Thread(this); 
    		t.start();
	        try{Thread.sleep(1000);}
			catch(InterruptedException e) { }    			
    	}
    }
    	
    public void stop() {if(t != null){ t.interrupt() ; t = null;}}	
	
    public void run() {
	    do {
	        repaint();
			try {Thread.sleep(delay);}
			catch(InterruptedException e) {return;}
	    } while(true);
    }  	
	
	public void reset(){
		y = hh + 10;	
		repaint();
		stop();
	}

    public final void paint(Graphics g) {
    	if(init){
			im = createImage(ww, hh);
			img = im.getGraphics();	
			for(int i = 0; i < students.length; i++) {
				FontMetrics fm = img.getFontMetrics(fnt);
				h1 += fm.getHeight();
				if(fm.stringWidth(students[i]) > w1) w1 = fm.stringWidth(students[i]);
			}
			y = hh + 10; 	
			init = false;
		}
		Color color = null;
		for(int l = 0; l < students.length; l++) {
			float f = (float)hh / 4.0F;
			float f1 = 1.0F, f2 = 1.0F, f3 = 1.0F;
			int i1 = y + (int)(1.5 * getFont().getSize() * l);
			if(i1 >= 0 && i1 <= hh) {
				float ff = 0;
				if((float)i1 <= f)
					ff = (float)i1 / f;
				else if((float)i1 >= (float)hh - f)
					ff = ((float)hh - (float)i1) / f;
				else
					ff = 1.0F;
				color = new Color((int)((float)255 * ff), (int)((float)255 * ff), (int)((float)255 * ff));
			}else color = new Color(0, 0, 0);
			img.setColor(color);
			img.setFont(fnt);
			img.drawString(students[l], w, i1);
	    }
	    g.drawImage(im, 0, 0, this);
    }

    public final void update(Graphics g) {   
    	if(img!=null){	
			img.setColor(Color.black);
			img.fillRect(0,0,ww,hh);		
		}
		if(y < -(int)((float)h1*0.75f))
			y = hh + 10;
		else
			y --;
		paint(g);
	}		

}
