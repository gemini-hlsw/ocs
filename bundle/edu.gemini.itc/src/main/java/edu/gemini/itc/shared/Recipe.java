package edu.gemini.itc.shared;

/**
 * Interface for ITC recipes.
 * By convention a recipe constructor should take as arguments a
 * HttpServletRequest containing the form data and a PrintWriter
 * to which the calculation results are written.
 */
public interface Recipe {
    /**
     * Writes results of the recipe calculation.
     * Format should be suitable for inclusion inside a web page.
     */
    void writeOutput();
}
