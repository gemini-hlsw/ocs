import java.io.*;

public class filterEdit {
    public static void main(String[] args) throws Exception {
        String[] values;
	try {
	    BufferedReader in = new BufferedReader(new FileReader("CH4H4S.dat"));
	    String fileLine;
	    //double newVal;
	    BufferedWriter out = new BufferedWriter(new FileWriter("CH4H4S_new.dat"));
	    while ((fileLine = in.readLine()) != null) {
                values = fileLine.split("  ");
		for (int i=0; i < values.length; i++) {
		    try {
			double doubleVal = Double.valueOf(values[i].trim()).doubleValue();
			if (doubleVal <= 100.0) {
			    doubleVal = doubleVal*0.01;
			    out.write(Double.toString(doubleVal));
			    out.newLine();
			}
			else
			    out.write(values[i]+ " ");
			    
		    } catch (Exception e) {
			System.out.println("Cannot convert "+values[i]+" to Int");
			//out.write(values[i]);
		    }
		}
	    }
	    in.close();
	    out.close();
	} catch (Exception e) {
	    System.out.println("An exception has occurred!");
	}
    }
}