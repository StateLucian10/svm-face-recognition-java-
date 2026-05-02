package gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import svm.SVM;

public class SimulationControl extends Dialog{
	SVM svm;
	public TextArea ta, ta1;
	public Button start, options, reserved;
	public Button view, test, reserved1;
	public Label ilabel1, ilabel2, ilabel2_, ilabel3, ilabel3_, ilabel4, ilabel4_, ilabel5, ilabel5_, ilabel6, ilabel6_;
	public Label olabel1, olabel2, olabel2_, olabel3, olabel3_, olabel4;
	public boolean init = true;
	
	public SimulationControl(SVM svm, int width, int height){
		super(svm, "Simulation Control", false);
		this.svm = svm;
		setBackground(svm.settings.background_color_default);
		setResizable(false);
		setLayout(null);
		
		int a = 30;
		ilabel1 = new Label("Input Data");
		ilabel1.setBounds(20,a,110,20);
		ilabel1.setForeground(new Color(255,200,0));
		add(ilabel1);
		
		a+=20;
		ilabel2 = new Label("Input Data File:");
		ilabel2.setBounds(20,a,110,20);
		ilabel2.setForeground(Color.white);
		add(ilabel2);
		ilabel2_ = new Label("");
		ilabel2_.setBounds(130,a,180,20);
		ilabel2_.setForeground(Color.white);
		add(ilabel2_);	
		
		a+=20;
		ilabel3 = new Label("Vectors Count:");
		ilabel3.setBounds(20,a,110,20);
		ilabel3.setForeground(Color.white);
		add(ilabel3);
		ilabel3_ = new Label("");
		ilabel3_.setBounds(130,a,110,20);
		ilabel3_.setForeground(Color.white);
		add(ilabel3_);	
		
		a+=20;
		ilabel4 = new Label("Attributes Count:");
		ilabel4.setBounds(20,a,110,20);
		ilabel4.setForeground(Color.white);
		add(ilabel4);
		ilabel4_ = new Label("");
		ilabel4_.setBounds(130,a,110,20);
		ilabel4_.setForeground(Color.white);
		add(ilabel4_);	
		
		a+=20;
		ilabel5 = new Label("Algorithm:");
		ilabel5.setBounds(20,a,110,20);
		ilabel5.setForeground(Color.white);
		add(ilabel5);
		ilabel5_ = new Label("");
		ilabel5_.setBounds(130,a,180,20);
		ilabel5_.setForeground(Color.white);
		add(ilabel5_);	
		
		a+=20;
		ilabel6 = new Label("Max Stages Count:");
		ilabel6.setBounds(20,a,110,20);
		ilabel6.setForeground(Color.white);
		add(ilabel6);
		ilabel6_ = new Label("");
		ilabel6_.setBounds(130,a,110,20);
		ilabel6_.setForeground(Color.white);
		add(ilabel6_);			
		
		a+=28;	
		start = new Button("Start Simulation");
		start.setBounds((width-360)/2,a,110,30);
		start.setBackground(svm.settings.button_color_default);
		start.setForeground(svm.settings.button_label_default);		
		add(start);
		start.enable(false);

		options = new Button("Options");
		options.setBounds((width-360)/2+120,a,110,30);
		options.setBackground(svm.settings.button_color_default);
		options.setForeground(svm.settings.button_label_default);		
		add(options);
		
		reserved = new Button("...reserved...");
		reserved.setBounds((width-360)/2+240,a,110,30);
		reserved.setBackground(svm.settings.button_color_default);
		reserved.setForeground(svm.settings.button_label_default);		
		add(reserved);		

		ta = new TextArea("");
		ta.setBounds(0,200,width,height-400);
		ta.setBackground(svm.settings.button_color_default);
		ta.setForeground(svm.settings.string_color_default);
		add(ta);
		
		int b = height-195;
		olabel1 = new Label("Output Data");
		olabel1.setBounds(20,b,110,20);
		olabel1.setForeground(new Color(255,200,0));
		add(olabel1);
		
		b+=20;
		olabel2 = new Label("Stages Count:");
		olabel2.setBounds(20,b,110,20);
		olabel2.setForeground(Color.white);
		add(olabel2);	
		olabel2_ = new Label("");
		olabel2_.setBounds(130,b,110,20);
		olabel2_.setForeground(Color.white);
		add(olabel2_);	

		b+=20;
		olabel3 = new Label("Computing Time:");
		olabel3.setBounds(20,b,110,20);
		olabel3.setForeground(Color.white);
		add(olabel3);	
		olabel3_ = new Label("");
		olabel3_.setBounds(130,b,110,20);
		olabel3_.setForeground(Color.white);
		add(olabel3_);	

		b+=20;
		olabel4 = new Label("Classifier:");
		olabel4.setBounds(20,b,110,20);
		olabel4.setForeground(new Color(255,200,0));
		add(olabel4);
		
		b+=28;
		ta1 = new TextArea("");
		ta1.setBounds(0,b,width,40);
		ta1.setBackground(svm.settings.button_color_default);
		ta1.setForeground(svm.settings.string_color_default);
		add(ta1);		

		view = new Button("View Output Data");
		view.setBounds((width-360)/2,height-50,110,30);
		view.setBackground(svm.settings.button_color_default);
		view.setForeground(svm.settings.button_label_default);
		add(view);	
		
		test = new Button("Classifier Testing");
		test.setBounds((width-360)/2+120,height-50,110,30);
		test.setBackground(svm.settings.button_color_default);
		test.setForeground(svm.settings.button_label_default);
		add(test);			

		reserved1 = new Button("...reserved...");
		reserved1.setBounds((width-360)/2+240,height-50,110,30);
		reserved1.setBackground(svm.settings.button_color_default);
		reserved1.setForeground(svm.settings.button_label_default);
		add(reserved1);	

		
		
	}
	
	public void showInputData(String inputFile, String vectors_count, String vectors_dim, String algorithm, String max_epochs){
		ilabel2_.setForeground(Color.white);
		ilabel5_.setForeground(Color.white);
		ilabel2_.setText(inputFile);
		ilabel3_.setText(vectors_count);
		ilabel4_.setText(vectors_dim);
		ilabel5_.setText(algorithm);
		ilabel6_.setText(max_epochs);
	}
	
	public void showOutputData(String epocs_count, String computing_time, float[] w){
		olabel2_.setText(epocs_count);
		olabel3_.setText(computing_time);
		ta.append("Stage: " + epocs_count + "   Time: " + computing_time + "   Accuracy: " + svm.outd.accuracy + "%\n");
		if(w!=null) {
			String s = w[0] + " * x[" + 0 + "] ";
			int n = w.length;
			for(int j = 1; j < n-1; j++) {
				if(w[j] == 0) continue;
				s += w[j]<0 ? "- " : "+ ";
				s += Math.abs(w[j]) + " * x[" + j + "] ";				
			}
			if(w[n-1] != 0){
				s += w[n-1]<0 ? "- " : "+ ";
				s += Math.abs(w[n-1]) + " < 0 ? 0 : 1";			
			}			
			ta1.setText(s);
		}else ta1.setText("");
	}	
	
	public void start_simulation(){
		if(svm.ind.input_file == null){
			ilabel2_.setForeground(Color.yellow);
			ilabel2_.setText("Please load an input data file.");
		}else{
			if(svm.algorithm == null){
				ilabel5_.setForeground(Color.yellow);
				ilabel5_.setText("Please select an algorithm.");
			}else{	
				if(start.getLabel().startsWith("Start Simulation")){
					if(init){
						ta.setText("");
						svm.algorithm.start_simulation();
						init = false;
					}else svm.algorithm.resume_();
					start.setLabel("Stop Simulation");
				}else{
					svm.algorithm.suspend_();
					start.setLabel("Start Simulation");
				}			
			}
		}		
	}
	
	
	public boolean handleEvent(Event e){
		if(e.id==Event.WINDOW_DESTROY){
			svm.mb.getMenu(2).getItem(0).setLabel("Show Simulation Control");
			dispose();
		}else if(e.id==Event.ACTION_EVENT && e.target == reserved){
           	return true;				
		}else if(e.id==Event.ACTION_EVENT && e.target == start){
			start_simulation();
           	return true;	
		}else if(e.id==Event.ACTION_EVENT && e.target == options){
			svm.options.setValue();
			svm.options.eta_tf.setText(""+svm.settings.learning_rate);
			svm.options.show();
           	return true;			
		}else if(e.id==Event.ACTION_EVENT && e.target == view){
			svm.mb.getMenu(2).getItem(2).setLabel("Hide Output Data");
			svm.outd.show();
           	return true;				
		}	
		return super.handleEvent(e);
	}		


}