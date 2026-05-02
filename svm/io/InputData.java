package io;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import svm.SVM;
import gui.*;
import alg.*;

public class InputData extends Dialog{
	SVM svm;
	public Button bOpen, bSave, bData, bAttributes, bStatistics;
	public TextArea ta;
	public AttributesPanel ap;
	public StatisticsPanel sp;
	public String[] comments, metadata, data;
	public String dir=".\\", path, input_file;
	public Clasa[] classes;
	public Attribute[] attributes;
	public Vector[] V, T, ALL;	

	public InputData(SVM svm){
		super(svm, "Input Data", false);
		this.svm = svm;
		setBackground(svm.settings.background_color_default);
		setResizable(false);		
		resize(640,480);
		move((svm.res.width-640)/2,(svm.res.height-480)/2);				
		setLayout(null);
		
		int w = (size().width-10)/5;
		bOpen = new Button("Load Input Data");
		bOpen.setBounds(8,30,w-5,30);
		bOpen.setBackground(svm.settings.button_color_default);
		bOpen.setForeground(svm.settings.button_label_default);	
		add(bOpen);	

		bSave = new Button("Save Input Data");
		bSave.setBounds(8 + w,30,w-5,30);
		bSave.setBackground(svm.settings.button_color_default);
		bSave.setForeground(svm.settings.button_label_default);	
		add(bSave);		

		bData = new Button("Data");
		bData.setBounds(8 + 2*w,30,w-5,30);
		bData.setBackground(svm.settings.button_color_default.darker());
		bData.setForeground(svm.settings.button_label_default);	
		add(bData);			
		
		bAttributes = new Button("Attributes");
		bAttributes.setBounds(8 + 3*w,30,w-5,30);
		bAttributes.setBackground(svm.settings.button_color_default);
		bAttributes.setForeground(svm.settings.button_label_default);	
		add(bAttributes);

		bStatistics = new Button("Statistics");
		bStatistics.setBounds(8 + 4*w,30,w-5,30);
		bStatistics.setBackground(svm.settings.button_color_default);
		bStatistics.setForeground(svm.settings.button_label_default);	
		add(bStatistics);

		ta = new TextArea("");
		ta.setBounds(8,65,size().width-16,size().height-73);
		ta.setBackground(svm.settings.button_color_default);
		ta.setForeground(svm.settings.string_color_default);
		add(ta);

		ap = new AttributesPanel(this);
		ap.setBounds(8,65,size().width-16,size().height-73);
		ap.setBackground(svm.settings.button_color_default);
		ap.setForeground(svm.settings.string_color_default);
		add(ap);	
		ap.setVisible(false);
		
		sp = new StatisticsPanel(this);
		sp.setBounds(8,65,size().width-16,size().height-73);
		sp.setBackground(svm.settings.button_color_default);
		sp.setForeground(svm.settings.string_color_default);
		add(sp);
		sp.setVisible(false);		
		
	}
		
	public void loadInputData(){ 
		try{ 	
			FileDialog fd=new FileDialog(this, "Load Input Data", 0);
			if(dir!=null) fd.setDirectory(dir+"svm\\data");
			fd.setFile("*.csv");
			fd.setVisible(true);
			if(fd.getFile() != null) {
				svm.init();
				dir = fd.getDirectory();
				input_file = fd.getFile();
				path = dir + input_file;	                            
				loadInputData(new File(path));  
			}
		}
		catch(Exception e) {e.printStackTrace();}
	}	
	
	void loadInputData(File file){	
		try {
			DataInputStream dis = new DataInputStream(new FileInputStream(file));
			String[] comments_ = new String[1000];
			String[] metadata_ = new String[100000];
			String[] data_ = new String[100000000];
			int ic = 0, im = 0, id = 0;
			String LINE = "", line = "";
			while ((LINE = line = dis.readLine()) != null) {
				line = line.trim();
				line = line.replaceAll("\'","");
				line = line.toLowerCase();
				if(line.equals("")) continue;
				if(line.startsWith("%")){
					comments_[ic] = LINE;
					ic++;
					continue;
				}
				if(line.startsWith("@")){
					metadata_[im] = LINE;
					im++;
					continue;
				}
				data_[id] = LINE;
				id++;			
			}
			if(ic>0){
				comments = new String[ic];
				System.arraycopy(comments_, 0, comments, 0, ic);
			}else comments = null;
			if(im>0){
				metadata = new String[im];
				System.arraycopy(metadata_, 0, metadata, 0, im);	
			}else metadata = null;
			if(id>0){
				data = new String[id];
				System.arraycopy(data_, 0, data, 0, id);
			}else data = null;
			dis.close();	
			System.gc();			
		} 
		catch (IOException e) {}

		if(metadata!=null){
			Attribute[] attributes_ = new Attribute[100000];
			int ia = 0;			
			for(int i = 0; i < metadata.length; i++){
				if(metadata[i].startsWith("@relation") || metadata[i].startsWith("@data")) continue;
				String info = metadata[i].replaceAll(" ", "");
				if(info.startsWith("@attributeclass")){
					String s = info.replace('{',' ');
					s = s.substring(0,s.length()-1).split(" ")[1];
					String[] ss = s.split(",");
					classes = new Clasa[ss.length];
					if(ss.length == 2){
						classes[0] = new Clasa(ss[0], 0, svm.settings.class0_point_color);
						classes[1] = new Clasa(ss[1], 1, svm.settings.class1_point_color);
					}else{
						for(int j = 0; j < ss.length; j++)
							classes[j] = new Clasa(ss[j], j+1, new Color(128+(int)(Math.random()*128), 128+(int)(Math.random()*128), 128+(int)(Math.random()*128)));
					}
					continue;
				}else if(info.startsWith("@attribute")){
					String atr_name = metadata[i].split(" ")[1].trim();
					String atr_type = metadata[i].split(" ")[2].trim().replace('{',' ');
					attributes_[ia] = new Attribute(atr_name, atr_type);
					ia++;
					continue;
				}
			}
			if(ia>0){
				attributes = new Attribute[ia];
				System.arraycopy(attributes_, 0, attributes, 0, ia);
			}else attributes = null;			
		}

		ALL = new Vector[data.length];
		int n = 0, k = 0;
		for(int i=0; i<data.length; i++){
			String[] coords = data[i].split(",");
			boolean b = true;
			if(b){
				n = coords.length;
				b = false;
			} else if(coords.length != n){
				System.out.println("There are vectors of different sizes!");
				ALL = null;
				break;
			}
			float[] X = new float[coords.length-1];
			String cl = coords[coords.length-1].trim();
			
			int t = -1;
			for(int u = 0; u < classes.length; u++)
				if(classes[u].name.startsWith(coords[coords.length-1].trim())) {t = u; break;}
			for(int j = 0; j <= coords.length-2; j++){
				try{X[j] = Float.parseFloat(coords[j]);}
				catch(NumberFormatException e){System.out.println("The vector " + k + " is not correct!"); break;}
			}
			if(t>-1){
				ALL[k] = new Vector(X, classes[t]);
				k++;
			}
		}
		split();
		getStatistics();
		ta.setText("");
		if(comments!=null) for(int i = 0; i < comments.length; i++) ta.append(comments[i] + "\n");	
		if(metadata!=null) for(int i = 0; i < metadata.length; i++) ta.append(metadata[i] + "\n");	
		if(data!=null) for(int i = 0; i < data.length; i++) ta.append(data[i] + "\n");	
		svm.control.show();
		svm.mb.getMenu(2).getItem(0).setLabel("Hide Simulation Control");	
	} 
	
	public void init(){
		svm.design.initO();
		svm.control.showInputData(svm.outd.dataInputFile, svm.outd.vectors_count+"", svm.outd.attributes_count+"", "", "");
		svm.control.showOutputData("", "", null);
		svm.control.ta.setText("");
		svm.control.start.enable(true);	
	}	
		
	public void split(){
		int nL =  (svm.settings.percentage_inputData * ALL.length)/100;
		int nT = ALL.length - nL;		
		if(nL>0) V = new Vector[nL];		
		if(nT>0) T = new Vector[nT];
		for(int i = 0; i < ALL.length; i++){
			if(i<nL) V[i] = ALL[i];
			else T[i-nL] = ALL[i];
		}
		svm.outd.dataInputFile = input_file;
		svm.outd.vectors_count = V.length;
		svm.outd.attributes_count = V[0].getDimension();
		if(T!=null) svm.outd.testing_vectors_count = T.length;
		init();
	}
	
	public void getStatistics(){
		for(int i=0; i<attributes.length; i++){
			attributes[i].class_statistic = new Statistic[classes.length];
			for(int j=0; j<classes.length; j++)
				attributes[i].class_statistic[j] = new Statistic();
			attributes[i].attr_statistic = new Statistic();
			
			float Ma = 0, Sa = 0;
			int ma = 0;
			float mina = Float.MAX_VALUE, maxa = Float.MIN_VALUE;
			for(int j=0; j<classes.length; j++){
				attributes[i].class_statistic[j].statistic_name = attributes[i].attribute_name + ": Class " + classes[j].Y;
				float min = Float.MAX_VALUE, max = Float.MIN_VALUE;
				float M = 0, S = 0;
				int m = 0;
				for(int k = 0; k < V.length; k++){
					if(V[k].cl.Y == classes[j].Y){
						if(V[k].X[i] < min) min = V[k].X[i];
						if(V[k].X[i] > max) max = V[k].X[i];
						M += V[k].X[i];
						m++;
					}
				}
				if(min < mina) mina = min;
				if(max > maxa) maxa = max;
				ma += m; Ma += M;
				M = M/m;
				attributes[i].class_statistic[j].objects_count = m;
				attributes[i].class_statistic[j].min_value = min;
				attributes[i].class_statistic[j].max_value = max;
				attributes[i].class_statistic[j].mean = M;
				for(int k = 0; k < V.length; k++)
					if(V[k].cl.Y == classes[j].Y)
						S += (V[k].X[i]-M)*(V[k].X[i]-M);
				S = (float)Math.sqrt(S/m);
				attributes[i].class_statistic[j].stdDev = S;
			}
			Ma = Ma/ma;
			attributes[i].attr_statistic.statistic_name = attributes[i].attribute_name;
			attributes[i].attr_statistic.objects_count = ma;
			attributes[i].attr_statistic.min_value = mina;
			attributes[i].attr_statistic.max_value = maxa;
			attributes[i].attr_statistic.mean = Ma;	
			for(int k = 0; k < V.length; k++)
				Sa += (V[k].X[i]-Ma)*(V[k].X[i]-Ma);
			Sa = (float)Math.sqrt(Sa/ma);
			attributes[i].attr_statistic.stdDev = Sa;	
		}
	}

	public void saveInputData(){ 
		if(input_file!=null){
			try{ 	
				FileDialog fd=new FileDialog(this, "Save Input Data", 1);
				if(dir!=null) fd.setDirectory(dir+"svm\\data");
				fd.setFile(input_file.substring(0,input_file.indexOf("."))+"_modified.csv");
				fd.setVisible(true);
				if(fd.getFile() != null) {
					svm.init();
					dir = fd.getDirectory();
					input_file = fd.getFile();
					path = dir + input_file;	                            
					File file = new File(path); 
					if(file.exists()) file.delete();					
					BufferedWriter bw = new BufferedWriter(new FileWriter(file));
					bw.write(ta.getText());
					bw.close();	
				}
			}
			catch(IOException e) {e.printStackTrace();}
		}
	}			
	
	public boolean handleEvent(Event e){
		if(e.id==Event.WINDOW_DESTROY){
			svm.mb.getMenu(2).getItem(1).setLabel("Show Input Data");
			dispose();
		}else if(e.id==Event.ACTION_EVENT && e.target == bOpen){
			loadInputData();
			if(attributes != null) {
				ap.populatesList();
				sp.populatesList();
			}
           	return true;						
		}else if(e.id==Event.ACTION_EVENT && e.target == bSave){
			saveInputData();
           	return true;
		}else if(e.id==Event.ACTION_EVENT && e.target == bData){
			bData.setBackground(svm.settings.button_color_default.darker());
			bAttributes.setBackground(svm.settings.button_color_default);
			bStatistics.setBackground(svm.settings.button_color_default);
			ta.setVisible(true);
			ap.setVisible(false);
			sp.setVisible(false);
           	return true;			
		}else if(e.id==Event.ACTION_EVENT && e.target == bAttributes){
			if(attributes != null) ap.populatesList(); 
			bData.setBackground(svm.settings.button_color_default);
			bAttributes.setBackground(svm.settings.button_color_default.darker());
			bStatistics.setBackground(svm.settings.button_color_default);			
			ta.setVisible(false);
			ap.setVisible(true);
			sp.setVisible(false);
           	return true;
		}else if(e.id==Event.ACTION_EVENT && e.target == bStatistics){
			if(attributes != null) sp.populatesList();
			bData.setBackground(svm.settings.button_color_default);
			bAttributes.setBackground(svm.settings.button_color_default);
			bStatistics.setBackground(svm.settings.button_color_default.darker());		
			ta.setVisible(false);
			ap.setVisible(false);
			sp.setVisible(true);
           	return true;			
		}else return false;	
		return super.handleEvent(e);
	}	
	
}