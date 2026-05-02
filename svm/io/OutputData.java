package io;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import svm.SVM;

public class OutputData extends Dialog{
	
	public String dataInputFile;
	public int vectors_count;
	public int attributes_count;	
	public String algorithm;
	public long max_stages_count;
	public long stages_count;
	public long computing_time;	//milis
	
	public float[] w;
	public int[] alpha;
	public float b;
	
	public int accuracy;
	public int testing_vectors_count;
	
	SVM svm;
	public Button bSave, bTest, bReserved1, bReserved2, bReserved3;
	public TextArea ta;

	public OutputData(SVM svm){
		super(svm, "Output Data", false);
		this.svm = svm;
		setBackground(svm.settings.background_color_default);
		setResizable(false);		
		resize(640,480);
		move((svm.res.width-640)/2,(svm.res.height-480)/2);				
		setLayout(null);

		int w = (size().width-10)/5;
		bSave = new Button("Save Output Data");
		bSave.setBounds(8,30,w-5,30);
		bSave.setBackground(svm.settings.button_color_default);
		bSave.setForeground(svm.settings.button_label_default);	
		add(bSave);			
	
		bTest = new Button("Classifier Testing");
		bTest.setBounds(8 + w,30,w-5,30);
		bTest.setBackground(svm.settings.button_color_default);
		bTest.setForeground(svm.settings.button_label_default);	
		add(bTest);			
		
		bReserved1 = new Button("...reserved...");
		bReserved1.setBounds(8 + 2*w,30,w-5,30);
		bReserved1.setBackground(svm.settings.button_color_default);
		bReserved1.setForeground(svm.settings.button_label_default);	
		add(bReserved1);

		bReserved2 = new Button("...reserved...");
		bReserved2.setBounds(8 + 3*w,30,w-5,30);
		bReserved2.setBackground(svm.settings.button_color_default);
		bReserved2.setForeground(svm.settings.button_label_default);	
		add(bReserved2);

		bReserved3 = new Button("...reserved...");
		bReserved3.setBounds(8 + 4*w,30,w-5,30);
		bReserved3.setBackground(svm.settings.button_color_default);
		bReserved3.setForeground(svm.settings.button_label_default);	
		add(bReserved3);		
		
		ta = new TextArea("");
		ta.setBounds(8,65,size().width-16,size().height-73);
		ta.setBackground(svm.settings.button_color_default);
		ta.setForeground(svm.settings.string_color_default);
		add(ta);
	}
	
	public void print(){
		System.out.println("Input Data File = " + dataInputFile);
		System.out.println("Vectors Count = " + vectors_count);
		System.out.println("Attributes Count = " + attributes_count);
		System.out.println("=====================================");
		System.out.println("Algorithm = " + algorithm);
		System.out.println("Stages Count = " + stages_count);
		System.out.println("Computing Time = " + computing_time + " ms");
		System.out.println("=====================================");
		System.out.println("Classifier:");
		for(int j = 0; j < w.length; j++) System.out.println("w["+j+"] = " + w[j]);
		System.out.println("=====================================");
		System.out.println("Accuracy = " + accuracy + "%");
		System.out.println("Testing Vectors Count = " + testing_vectors_count+"\n");
	}
	
	public void showInputData(){
		svm.control.showInputData(dataInputFile, vectors_count+"", attributes_count+"", algorithm, max_stages_count+"");
	}	
	
	public void showOutputData(){
		ta.append("Input Data File = " + dataInputFile+"\n");
		ta.append("Vectors Count = " + vectors_count+"\n");
		ta.append("Attributes Count = " + attributes_count+"\n");
		ta.append("Algorithm = " + algorithm+"\n");
		ta.append("Computing Time = " + computing_time+" ms\n");
		ta.append("Stages Count = " + stages_count+"\n");
		String s = w[0] + " * x[" + 0 + "] ";
		for(int j = 1; j < attributes_count; j++) {
			if(w[j] == 0) continue;
			s += w[j]<0 ? "- " : "+ ";
			s += Math.abs(w[j]) + " * x[" + j + "] ";				
		}
		if(w[attributes_count] != 0){
			s += w[attributes_count]<0 ? "- " : "+ ";
			s += Math.abs(w[attributes_count]) + " ";			
		}
		ta.append("Classifier:    " + s + "< 0 ? 0 : 1" +"\n");
		ta.append("Accuracy = " + accuracy+"%\n");
		ta.append("Testing Vectors Count = " + testing_vectors_count+"\n");	
		ta.append("====================================="+"\n");
		ta.append("====================================="+"\n");

		svm.control.showOutputData(stages_count+"", computing_time+" ms", w);		
	}
	
	public void saveOutputData(){ 
		try{ 	
			FileDialog fd = new FileDialog(this, "Save Output Data", 1);
			if(svm.ind.dir!=null) fd.setDirectory(svm.ind.dir+"svm\\data");
			fd.setFile(svm.ind.input_file.substring(0,svm.ind.input_file.indexOf("."))+"_output.txt");
			fd.setVisible(true);
			if(fd.getFile() != null) {
				svm.ind.dir = fd.getDirectory();
				String fisier = fd.getFile();
				svm.ind.path = svm.ind.dir + fisier;	                            
				File file = new File(svm.ind.path); 
				if(file.exists()) file.delete();
				BufferedWriter bw = new BufferedWriter(new FileWriter(file));
				bw.write(ta.getText());
				bw.close();				
			}
		}
		catch(IOException e) {e.printStackTrace();}
	}
	
	public boolean handleEvent(Event e){
		if(e.id==Event.WINDOW_DESTROY){
			svm.mb.getMenu(2).getItem(2).setLabel("Show Output Data");
			dispose();
		}else if(e.id==Event.ACTION_EVENT && e.target == bSave){
			saveOutputData();
           	return true;						
		}else if(e.id==Event.ACTION_EVENT && e.target == bTest){

           	return true;				
		}else return false;	
		return super.handleEvent(e);
	}	
		
}