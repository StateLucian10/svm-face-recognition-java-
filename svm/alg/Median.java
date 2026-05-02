package alg;

import svm.SVM;

public class Median extends Algorithm{

	public Median(SVM svm){
		super(svm);
		if(svm.ind.V != null){
			name = "Median";
			svm.outd.algorithm = name;
			svm.outd.max_stages_count = 1;
			svm.outd.showInputData();
		}
	}
	
	public void run(){
		t = System.currentTimeMillis();
		float[] M0 = new float[dim];
		float[] M1 = new float[dim];
		float[] w = new float[dim+1];
		int k0 = 0, k1 = 0;		
		for(int i = 0; i < N; i++)
			if(svm.ind.V[i].cl.Y == 0) k0++; else k1++;
		for(int j = 0; j < dim; j++)
			for(int i = 0; i < N; i++)
				if(svm.ind.V[i].cl.Y == 0) 
					M0[j] += svm.ind.V[i].X[j];
				else 
					M1[j] += svm.ind.V[i].X[j];
		for(int j = 0; j < dim; j++){
			M0[j] /= k0;
			M1[j] /= k1;
		}
		float[] X0 = new float[dim];
		for(int j = 0; j < dim; j++){
			X0[j] = (M0[j] + M1[j])/2;
			w[j] =  M1[j] - M0[j];
			w[dim] -= w[j] * X0[j];
		}
		if(dim==2) svm.design.setPointsOfLine(w);
		svm.outd.stages_count = 1;
		svm.outd.max_stages_count = 1;
		svm.outd.computing_time = System.currentTimeMillis() - t;
		svm.outd.w = w;
		svm.outd.accuracy = getAccuracy(w);
		svm.outd.showInputData();
		svm.outd.showOutputData();
		svm.design.calculates = false;
		svm.design.repaint();
		svm.control.start.enable(false);		
	}
	
}