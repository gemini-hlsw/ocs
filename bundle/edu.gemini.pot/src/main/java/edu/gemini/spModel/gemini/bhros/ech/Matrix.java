package edu.gemini.spModel.gemini.bhros.ech;

import java.io.InputStream;
import java.util.Arrays;


/**
 * Matrix class optimized for performance at the expense of Java-ness.
 * Specifically, the math operations take an output matrix to cut down on
 * copying, and the class provides some crazy setters for the specific needs of
 * the Echellogram widget.
 */
public final class Matrix {
    
    private final int nRows;          
    private final int nCols;           
    private final double[] pcells;      

    public Matrix(int rows, int cols) {
        this.nRows = rows;
        this.nCols = cols;
        pcells = new double[rows * cols];
    }
    
    public double cell(int row, int col) {
        return pcells[row * nCols + col];
    }
    
    public void set(int row, int col, double value) {
        pcells[row * nCols + col] = value;
    }
    
    public void set2(double a, double b) {
        assert nRows * nCols == 2;
        pcells[0] = a;
        pcells[1] = b;
    }

    public void set3(double a, double b, double c) {
        assert nRows * nCols == 3;
        pcells[0] = a;
        pcells[1] = b;
        pcells[2] = c;
    }

    public void set4(double a, double b, double c, double d) {
        assert nRows * nCols == 4;
        pcells[0] = a;
        pcells[1] = b;
        pcells[2] = c;
        pcells[3] = d;
    }
    

    public void set6(double a, double b, double c, double d, double e, double f) {
        assert nRows * nCols == 6;
        pcells[0] = a;
        pcells[1] = b;
        pcells[2] = c;
        pcells[3] = d;
        pcells[4] = e;
        pcells[5] = f;
    }

	/**
	 * Set values by copying them from another matrix, starting at the specified cell.
	 * Probably only makes sense for 1-dimensional matrices.
	 * @param other
	 * @param pos
	 */
    public void set(Matrix other, int pos) {
		for (int i = 0; i < pcells.length; i++)
			pcells[i] = other.pcells[pos++];
    }

    public void set(Matrix other) {
		if (other.nRows != nRows || other.nCols != nCols)
			throw new IllegalArgumentException("Can't copy an array of different size.");
        System.arraycopy(other.pcells, 0, pcells, 0, pcells.length);
    }
    
    public void addInto(Matrix matB, Matrix matSum){
        for (int i = 0; i < pcells.length; i++)
            matSum.pcells[i] = pcells[i] + matB.pcells[i];
    }
 
    public void multiplyInto(Matrix matB, Matrix matProduct){
        int row, col, i;
        double sigma;      
        for (row=0; row < matProduct.nRows; row++) {
            for (col = 0; col < matProduct.nCols; col++) {
                sigma = 0.0;
                for (i=0; i< this.nCols; i++) {
                    sigma += this.pcells[row * this.nCols + i] * matB.pcells[i * matB.nCols + col];
                }
                matProduct.pcells[row * matProduct.nCols + col] = sigma;
            }
        }
    }

    public void copyRowFrom(Matrix other, int sourceRow, int destRow) {
        assert(nCols == other.nCols);
        int src = sourceRow * other.nCols;
        int dst = destRow * nCols;
        for (int i = 0; i < nCols; i++)
            pcells[dst++] = other.pcells[src++];
    }

    public void scale(double factor) {
        for (int i = 0; i < pcells.length; i++)
            pcells[i] *= factor;
    }

    public void negate() {
        for (int i = 0; i < pcells.length; i++)
            pcells[i] = -pcells[i];
    }

    public void zero() {
        Arrays.fill(pcells, 0);
    }

    private int currentReadRow;
    
    public synchronized void read(InputStream is) {
        currentReadRow = 0;
	    new ArrayReader() {
            public Object build(String args) {
                String[] parts = args.split("\\s+");
                if (parts.length != nCols)
                    throw new RuntimeException("Expected " + nCols + " cols; found " + parts.length);
                for (int i = 0; i < nCols; i++) {
                    pcells[currentReadRow * nCols + i] = Double.parseDouble(parts[i]);
                }
                ++currentReadRow;
                return null;
            }
        }.readArray(is, new Object[0]);
        if (currentReadRow != nRows)
            throw new RuntimeException("Expected " +nRows + " rows; found " + currentReadRow);
    }

	public int nRows() {
		return nRows;
	}

	public int nCols() {
		return nCols;
	}

}
