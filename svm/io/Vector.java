package io;

import svm.SVM;

public class Vector{
	public float[] X;
	public Clasa cl;
	
	public Vector(float[] X, Clasa cl){
		this.X = X;
		this.cl = cl;
	}	
		
	public int getDimension(){return X.length;}
	
	public static double norm(float[] X){
		double s = 0;
		for(int i = 0; i < X.length; i++)
			s += X[i] * X[i];
		return Math.sqrt(s);
	}	

	public static double maxNorm(Vector[] V){
		double max = -1;
		for(int i = 0; i < V.length; i++){
			double norm = norm(V[i].X);
			if(norm > max) max = norm;
		}
		return max;		
	}

}