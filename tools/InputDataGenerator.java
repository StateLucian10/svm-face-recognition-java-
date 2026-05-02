package tools;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import svm.SVM;

public class InputDataGenerator extends Dialog{
	SVM svm;
	TextField attributes_count, vectors_count, min, max, classes_count;
	
	//L3 start
	TextField mg;
	Label mg_label;
	Checkbox als,cs,rs;
	Label als_label,cs_label,rs_label;	
	//L3 end
	Checkbox liniar;
	Label attributes_count_label, vectors_count_label, min_label, max_label, classes_count_label, liniar_label;
	Button generate, save;
	public TextArea ta;
	String dir = ".\\svm\\data", path;
	
	public InputDataGenerator(SVM svm){
		super(svm, "Input Data Generator", true);
		this.svm = svm;
		setBackground(svm.settings.background_color_default);
		setResizable(false);
		resize(640,480);
		move((svm.res.width-640)/2,(svm.res.height-480)/2);			
		setLayout(null);
		
		int x1 = 10, x2 = 380;
		int y = 35;
		attributes_count_label = new Label("Attributes Count:");
		attributes_count_label.setBounds(x1,y,150,20);
		attributes_count_label.setForeground(Color.white);
		add(attributes_count_label);
		attributes_count = new TextField("2");
		attributes_count.setBounds(x1+150,y,100,20);
		add(attributes_count);
		
		vectors_count_label = new Label("Vectors Count:");
		vectors_count_label.setBounds(x2,y,150,20);
		vectors_count_label.setForeground(Color.white);
		add(vectors_count_label);
		vectors_count = new TextField("1000");
		vectors_count.setBounds(x2+150,y,100,20);
		add(vectors_count);
		
		y+=30;
		min_label = new Label("Minimum Coordinates:");
		min_label.setBounds(x1,y,150,20);
		min_label.setForeground(Color.white);
		add(min_label);
		min = new TextField("-1000");
		min.setBounds(x1+150,y,100,20);
		add(min);	

		max_label = new Label("Maximum Coordinates:");
		max_label.setBounds(x2,y,150,20);
		max_label.setForeground(Color.white);
		add(max_label);
		max = new TextField("1000");
		max.setBounds(x2+150,y,100,20);
		add(max);
		
		y+=30;
		classes_count_label = new Label("Classes Count:");
		classes_count_label.setBounds(x1,y,150,20);
		classes_count_label.setForeground(Color.white);
		add(classes_count_label);
		classes_count = new TextField("2");
		classes_count.setBounds(x1+150,y,100,20);
		add(classes_count);	
		classes_count.enable(false);
		
		liniar_label = new Label("Liniar separated:");
		liniar_label.setBounds(x2,y,150,20);
		liniar_label.setForeground(Color.white);
		add(liniar_label);
		liniar = new Checkbox("");
		liniar.setBounds(x2+150,y,20,20);
		liniar.setState(true);
		add(liniar);
		
		//L3 start
		y += 30;  // Move to next row
		mg_label = new Label("Margin:");
		mg_label.setBounds(x1, y, 150, 20);
		mg_label.setForeground(Color.white);
		add(mg_label);

		mg = new TextField("50");
		mg.setBounds(x1 + 150, y, 100, 20);
		add(mg);

		// Add als checkbox in same row as margin (or move to next row for better spacing)
		y += 30;  // New row for als
		als_label = new Label("Almost linear separated:");
		als_label.setBounds(x1, y, 150, 20);
		als_label.setForeground(Color.white);
		add(als_label);

		als = new Checkbox("");
		als.setBounds(x1 + 150, y, 20, 20);
		add(als);

		y += 30;  // New row for cs
		cs_label = new Label("Circular separated:");
		cs_label.setBounds(x1, y, 150, 20);
		cs_label.setForeground(Color.white);
		add(cs_label);

		cs = new Checkbox("");
		cs.setBounds(x1 + 150, y, 20, 20);
		add(cs);

		y += 30;  // New row for rs
		rs_label = new Label("Random separated:");
		rs_label.setBounds(x1, y, 150, 20);
		rs_label.setForeground(Color.white);
		add(rs_label);

		rs = new Checkbox("");
		rs.setBounds(x1 + 150, y, 20, 20);
		add(rs);
		//L3 end
		
		y+=30;
		generate = new Button("Generate");
		generate.setBounds(x1,y,250,20);
		generate.setBackground(svm.settings.button_color_default);
		generate.setForeground(svm.settings.button_label_default);	
		add(generate);	
		
		save = new Button("Save");
		save.setBounds(x2,y,250,20);
		save.setBackground(svm.settings.button_color_default);
		save.setForeground(svm.settings.button_label_default);	
		add(save);
		
		y+=30;
		ta = new TextArea("");
		ta.setBounds(x1,y,size().width-2*x1,size().height-y-x1);
		ta.setBackground(svm.settings.button_color_default);
		ta.setForeground(svm.settings.string_color_default);
		add(ta);		

		show();
	}
	
	public boolean handleEvent(Event e) {
		if (e.id == Event.WINDOW_DESTROY) {
			dispose();
		} else if (e.id == Event.ACTION_EVENT && e.target == generate) {
			generateData();
			return true;
		} else if (e.id == Event.ACTION_EVENT && e.target == save) {
			saveGeneratedData();
			return true;
			//L3 start
		} else if (e.id == Event.ACTION_EVENT && e.target == liniar) {
			if (liniar.getState()) {
				als.setState(false);
				cs.setState(false);
				rs.setState(false);
				classes_count.setText("2");
				classes_count.setEnabled(false);  // Only 2 classes for linear
			}
			return true;
		} else if (e.id == Event.ACTION_EVENT && e.target == als) {
			if (als.getState()) {
				liniar.setState(false);
				cs.setState(false);
				rs.setState(false);
				classes_count.setText("2");
				classes_count.setEnabled(false);  // Only 2 classes for almost linear
			}
			return true;
		} else if (e.id == Event.ACTION_EVENT && e.target == cs) {
			if (cs.getState()) {
				liniar.setState(false);
				als.setState(false);
				rs.setState(false);
				classes_count.setText("2");
				classes_count.setEnabled(false);  // Only 2 classes for circular
			}
			return true;
		} else if (e.id == Event.ACTION_EVENT && e.target == rs) {
			if (rs.getState()) {
				liniar.setState(false);
				als.setState(false);
				cs.setState(false);
				classes_count.setEnabled(true);   // Multiple classes allowed for random
			}
			return true;
		}
		return super.handleEvent(e);
	}//L3 end
	
	//L3 start
	void generateData() {
		ta.setText("");
		
		// Validate inputs before generating
		try {
			int N = Integer.parseInt(vectors_count.getText());
			int n = Integer.parseInt(attributes_count.getText());
			int MIN = Integer.parseInt(min.getText());
			int MAX = Integer.parseInt(max.getText());
			float margin = Float.parseFloat(mg.getText());
			
			if (N <= 1) {
				ta.append("Error: Vectors count must be > 1\n");
				return;
			}
			if (n <= 1) {
				ta.append("Error: Attributes count must be > 1\n");
				return;
			}
			if (MIN >= MAX) {
				ta.append("Error: Minimum must be less than Maximum\n");
				return;
			}
			
			if (liniar.getState()) generateLiniarData();
			else if (als.getState()) generateAlmostLinearData();
			else if (cs.getState()) generateCircularData();
			else if (rs.getState()) generateRandomData();
			else {
				ta.append("Please select a generation type\n");
			}
		} catch (NumberFormatException ex) {
			ta.append("Error: Please enter valid numbers in all fields\n");
		}
	}//L3 end
	
	//L3 start
	void generateLiniarData() {
		int N = Integer.parseInt(vectors_count.getText());
		int n = Integer.parseInt(attributes_count.getText());
		int MIN = Integer.parseInt(min.getText());
		int MAX = Integer.parseInt(max.getText());
		float margin = Float.parseFloat(mg.getText());
		if (N <= 1 || n <= 1 || MIN >= MAX) {
			mesaj();
			return;
		}
		ta.append("% attributes count = " + n + "\n");
		ta.append("% vectors count = " + N + "\n");
		for(int i=1; i<=n; i++)ta.append("@attribute attrib_"+i+" numeric\n");
		ta.append("@attribute class {0, 1}\n");
		ta.append("@data\n");
		if(N > 1 && n > 1 && MIN < MAX){
			float[] w = new float[n+1];
			for(int i=0; i<=n; i++)w[i] = MIN + (float)Math.random()*(MAX-MIN);
			int generated = 0;
			while(generated < N){
				float[] x = new float[n];
				String s = "";
				for(int i=0; i<n; i++){
					x[i] = MIN + (float)Math.random()*(MAX-MIN);
					s += x[i] + ",";
				}
				float z = 0;
				for(int i=0; i<n; i++) z += w[i]*x[i];
				z += w[n];
				int y;
				if(z > margin) y = 1;
				else if(z < -margin) y = 0;
				else continue;
				s += y + "\n";
				ta.append(s);
				generated++;
			}
		} else mesaj();
	}
	
	//L3 start
	void generateAlmostLinearData() {
		int N = Integer.parseInt(vectors_count.getText());
		int n = Integer.parseInt(attributes_count.getText());
		int MIN = Integer.parseInt(min.getText());
		int MAX = Integer.parseInt(max.getText());
		float margin = Float.parseFloat(mg.getText());

		// Add validation
		if (N <= 1 || n <= 1 || MIN >= MAX) {
			mesaj();
			return;
		}

		ta.append("% attributes count = " + n + "\n");
		ta.append("% vectors count = " + N + "\n");
		ta.append("% Almost linear separated with margin = " + margin + "\n");
		
		for (int i = 1; i <= n; i++)
			ta.append("@attribute attrib_" + i + " numeric\n");
		ta.append("@attribute class {0, 1}\n");
		ta.append("@data\n");

		// Generate random hyperplane
		float[] w = new float[n + 1];
		for (int i = 0; i <= n; i++)
			w[i] = MIN + (float) Math.random() * (MAX - MIN);

		int generated = 0;
		while (generated < N) {
			float[] x = new float[n];
			String s = "";
			
			for (int i = 0; i < n; i++) {
				x[i] = MIN + (float) Math.random() * (MAX - MIN);
				s += x[i] + ",";
			}

			// Calculate distance from hyperplane
			float z = 0;
			for (int i = 0; i < n; i++)
				z += w[i] * x[i];
			z += w[n];

			int y;
			if (Math.abs(z) < margin) {
				// Inside margin region - assign random class (makes data non-separable)
				y = (Math.random() < 0.5) ? 0 : 1;
			} else {
				// Outside margin - assign class based on hyperplane
				y = (z >= 0) ? 1 : 0;
			}

			s += y + "\n";
			ta.append(s);
			generated++;
		}
	}

	//L3 end
	void generateRandomData() {
		int N = Integer.parseInt(vectors_count.getText());
		int n = Integer.parseInt(attributes_count.getText());
		int MIN = Integer.parseInt(min.getText());
		int MAX = Integer.parseInt(max.getText());
		int C = Integer.parseInt(classes_count.getText());
		if (N <= 1 || n <= 1 || MIN >= MAX) {
			mesaj();
			return;
		}
		ta.append("% attributes count = " + n + "\n");
		ta.append("% vectors count = " + N + "\n");
		for(int i=1; i<=n; i++) ta.append("@attribute attrib_"+i+" numeric\n");
		String ss = "";
		for(int i=0; i<C-1; i++) ss += i + ", ";
		ss += (C-1);
		ta.append("@attribute class {"+ss+"}\n");
		ta.append("@data\n");
		if(N > 1 && n > 1 && MIN < MAX && C > 1){
			for(int k=0; k<N; k++){
				String s = "";
				for(int i=0; i<n; i++){ s += (MIN + (float)Math.random()*(MAX-MIN)) + ","; }
				s += (int)(Math.random()*C)+"\n";
				ta.append(s);
			}
		}else mesaj();
	}
	
	//L3 start
	void generateCircularData() {
		int N = Integer.parseInt(vectors_count.getText());
		int n = Integer.parseInt(attributes_count.getText());
		int MIN = Integer.parseInt(min.getText());
		int MAX = Integer.parseInt(max.getText());
		float radius = Float.parseFloat(mg.getText());

		if (N <= 1 || n <= 1 || MIN >= MAX) {
			mesaj();
			return;
		}

		ta.append("% attributes count = " + n + "\n");
		ta.append("% vectors count = " + N + "\n");
		ta.append("% Circular separated with radius = " + radius + "\n");
		ta.append("% Center at [" + (MIN+MAX)/2.0f + " (all dimensions)]\n");

		for (int i = 1; i <= n; i++)
			ta.append("@attribute attrib_" + i + " numeric\n");

		ta.append("@attribute class {0, 1}\n");
		ta.append("@data\n");

		// Center of sphere is at the midpoint of the range
		float[] center = new float[n];
		for (int i = 0; i < n; i++)
			center[i] = (MIN + MAX) / 2.0f;

		int generated = 0;
		int inside_count = 0;
		int outside_count = 0;
		
		while (generated < N) {
			float[] x = new float[n];
			String s = "";

			for (int i = 0; i < n; i++) {
				x[i] = MIN + (float) Math.random() * (MAX - MIN);
				s += x[i] + ",";
			}

			// Calculate distance from center
			float distSquared = 0;
			for (int i = 0; i < n; i++)
				distSquared += (x[i] - center[i]) * (x[i] - center[i]);
			
			float dist = (float) Math.sqrt(distSquared);

			// Inside sphere -> class 1, outside -> class 0
			int cl = (dist <= radius) ? 1 : 0;
			
			// Count for verification
			if (cl == 1) inside_count++;
			else outside_count++;

			s += cl + "\n";
			ta.append(s);
			generated++;
		}
		
		// Add summary at the end for verification
		ta.append("% Generated: " + inside_count + " points inside sphere, " + 
				  outside_count + " points outside sphere\n");
		ta.append("% Sphere radius: " + radius + ", Center at [" + center[0] + ", ...]\n");
	}//L3 end	
	
	
	
	
	
	
	
	void saveGeneratedData(){
		if(!ta.getText().equals("")){
			try{ 	
				FileDialog fd=new FileDialog(this, "Save Generated Input Data", 1);
				if(dir!=null) fd.setDirectory(dir);
				fd.setFile("*.csv");
				fd.setVisible(true);
				if(fd.getFile() != null) {
					dir = fd.getDirectory();
					String fisier = fd.getFile();
					path = dir + fisier;
					File file = new File(path); 
					BufferedWriter bw = null;
					if(file.exists()) file.delete();
					try{
						bw = new BufferedWriter(new FileWriter(file));
						bw.write(ta.getText());
						bw.close();
					}
					catch(IOException e){e.printStackTrace();}				
				}
			}
			catch(Exception e) {e.printStackTrace();}	
		}		
	}
		
	void mesaj(){
		System.out.println("Vectors Dimension must be > 1.");
		System.out.println("Vectors Count must be > 1.");
		System.out.println("It is necessary that Minimum Coordonates < Maximum Coordonates.");		
	}
	
	
	
}