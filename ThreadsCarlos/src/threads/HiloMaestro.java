/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package threads;

import Nucleos.Contexto;
import Nucleos.Nucleo;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author CALI
 */
public class HiloMaestro {

    /**
     * @param args the command line arguments
     */
    private int ciclo;
    private static final CyclicBarrier lock = new CyclicBarrier(3);
    private static final Object lock2 = new Object();
    Nucleo n1;
    Nucleo n2; 
    private Queue<Contexto> cola = new ConcurrentLinkedQueue<Contexto>();
    
    public HiloMaestro() {
        ciclo = 1;
        n1 = new Nucleo(lock, "uno", lock2);
        n2 = new Nucleo(lock, "dos", lock2); 
    }
    
    private void iniciarHilos(){
        n1.start();
        n2.start();
    }
    
    private boolean hayTrabajo(){
        return !cola.isEmpty();
    }
    
    
    private void cargarHilos(){
        // Parte que habria q subir los hilos y darles un contexto
        agregarContexto(new Contexto());
        agregarContexto(new Contexto());
    }
    
    public void agregarContexto(Contexto nuevoContexto){
        if (cola.add(nuevoContexto)) {
            System.out.println("Contexto agregado");
        }
    }
    
    
    public void modificarVariableInstancia() {
            synchronized(lock2){
            try {
                lock2.wait();
            } catch (InterruptedException ex) {
                Logger.getLogger(Nucleo.class.getName()).log(Level.SEVERE, null, ex);
            }
            n1.trabajoActual = false;
        }
    }
    
    public void iniciar(){
        iniciarHilos(); // Siempre ejecutaran primero el wait
        cargarHilos();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {
            Logger.getLogger(HiloMaestro.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        while(hayTrabajo()) {     
            try {
                new Scanner(System.in).nextLine();
                System.out.println("El ciclo actual es " + ciclo);
                lock.await();
                ciclo++;
            } catch (InterruptedException ex) {
                Logger.getLogger(HiloMaestro.class.getName()).log(Level.SEVERE, null, ex);
            } catch (BrokenBarrierException ex) {
                Logger.getLogger(HiloMaestro.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
        
        
    
    public static void main(String[] args) {  
        HiloMaestro maestro = new HiloMaestro();
        maestro.iniciar();
     }
}
    
