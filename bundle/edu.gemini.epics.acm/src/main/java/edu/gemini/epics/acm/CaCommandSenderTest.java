package edu.gemini.epics.acm;

import gov.aps.jca.CAException;
import gov.aps.jca.TimeoutException;

public class CaCommandSenderTest {
    public static void main(String[] args) throws CAException {
        CaService.setAddressList("172.16.2.20");
        CaService service = CaService.getInstance();

        CaApplySender apply;
        apply = service.createApplySender("apply", "tc1:apply", "tc1:applyC");

        CaCommandSender cs = service.createCommandSender("sourceA", apply, null);
        try {
            CaParameter<String> parSystem = cs.addString("system", "tc1:sourceA.B");
            CaParameter<String> parCoord1 = cs.addString("theta1", "tc1:sourceA.C");
            CaParameter<String> parCoord2 = cs.addString("theta2", "tc1:sourceA.D");
            parSystem.set("AzEl");
            parCoord1.set(new Double(Math.random()*360.0).toString());
            parCoord2.set(new Double(Math.random()*50.0 + 30.0).toString());
            CaCommandMonitor cm = cs.postCallback(new CaCommandListener() {
                
                @Override
                public void onSuccess() {
                    // TODO Auto-generated method stub
                    System.out.println("Command completed successfully.");
                }
                
                @Override
                public void onFailure(Exception cause) {
                    // TODO Auto-generated method stub
                    System.out.println("Command completed with error " + cause.getMessage());
                }
            });
            cm.waitDone();
        } catch (CaException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (CAException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (TimeoutException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        try {
            service.unbind();
        } catch (CAException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
