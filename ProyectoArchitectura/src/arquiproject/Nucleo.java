/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package arquiproject;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author CALI
 */
public class Nucleo extends Thread {
    private final CyclicBarrier lock;
    private String identificacion;
    private boolean hayTrabajo;
    public boolean ocupado;
    public Contexto contexto;

    int [] registros; 
    
    
    public Nucleo(CyclicBarrier lock, String identificacion){
        this.lock = lock;

        this.identificacion = identificacion;
        hayTrabajo = true;
        ocupado = true;
        registros = new int[33];
        for(int i = 0; i<33; i++){
            registros[i]=0;
        }
    }
    
    public void setContexto(Contexto contexto) {
        this.contexto = contexto;
    }
    
    public void setEstado(boolean hayTrabajo){
        this.hayTrabajo = hayTrabajo;
    }
    
    
    
    // Este método puede ser llamado en cualquier momento que se termine un ciclo
    private void avanzarReloj() throws InterruptedException{
        try {
            lock.await();
        } catch (BrokenBarrierException ex) {
            Logger.getLogger(Nucleo.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
    @Override
    public void run(){
        registros = Decodificador.decodificacion("8 1 4 9", registros);
        while (hayTrabajo) { // Seria hasta que ya no exista trabajo
            //System.out.println("Núcleo " + identificacion + " iniciando el ciclo");
        
            // AQUI SE MANEJARIA LA LOGICA DE CADA NUCLEO
            
            try {
                avanzarReloj();
            } catch (InterruptedException ex) {
                Logger.getLogger(Nucleo.class.getName()).log(Level.SEVERE, null, ex);
            }
            //System.out.println("Núcleo " + identificacion + " terminado ciclo");  
        }
    }        
}
