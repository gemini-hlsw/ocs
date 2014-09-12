package jsky.app.ot.ui.util;


public class ProgressDialogTest {

    public static void main(String[] args) throws InterruptedException {

        ProgressModel model = new ProgressModel("Preparing to count...", 10);
        ProgressDialog dialog = new ProgressDialog(null, "Count to 10", false, model);
        dialog.setVisible(true);

        for (int i = 1; i <= 10; i++) {
            Thread.sleep(1000);
            model.work();
            model.setMessage("The count is " + i);
            if (model.isCancelled()) {
                System.out.println("Cancelled!");
                break;
            }
        }
        Thread.sleep(1000);

        dialog.setVisible(false);
        dialog.dispose();

    }

}
