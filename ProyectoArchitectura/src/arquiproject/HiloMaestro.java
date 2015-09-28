/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package arquiproject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Scanner;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentLinkedQueue; 
import java.util.concurrent.CyclicBarrier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
/**
 *
 * @author Pc
 */
public class HiloMaestro {
    static int totalInstrucciones = 0;
    static int cantidadHilosCargados = 0;
    static int cantidadMemInstrucciones = 640; 
    static int [] memoriaInstrucciones = new int[640];
    private int ciclo;
    private static final CyclicBarrier lock = new CyclicBarrier(3);
    int quantumCiclos;
    int memoriaCiclos;
    int busCiclos; 
    private final Lock lockFin1 = new ReentrantLock();
    private final Lock lockFin2 = new ReentrantLock();
    private int inicioHilo = 0;
    
    Nucleo n1;
    Nucleo n2; 
    private final ConcurrentLinkedQueue<Contexto> cola = new ConcurrentLinkedQueue<Contexto>();
   
    
    private boolean hayTrabajo(){
        return !(cantidadHilosCargados==0);
    }
   
    
    public boolean nucleoVacio(Nucleo n) {
        
        if (n.id.equals("uno") ){
            lockFin1.lock();
            if (n.esFin == true) {
                n.esFin = false; 
                return true;
            }
            lockFin1.unlock();
            return false; 
        } else {
            lockFin2.lock();
            if (n.esFin == true) {
                n.esFin = false; 
                return true;
            } 
            lockFin2.unlock();
            return false;
        } 
    }
    
    private void asignarContexto(Nucleo nucleo) {
        if (!nucleo.ocupado && !cola.isEmpty()) {
            nucleo.setContexto(cola.remove());
        }
    }
    
    
    // Se inician los hilos y se ejecutan el asignar contexto
      private void iniciarHilos(){
        System.out.println("Se inician los hilos (?)");
        n1.start();
        n2.start();
    }
    
    public void iniciar(){
        // debería preguntarse ademas de q si el nucleo esta ocupado.. de que si hay un contexto para cada nuclio
        // revisar q en la cola hayan hilos para asignar 2!! 
        asignarContexto(n1);
        asignarContexto(n2);
        iniciarHilos(); 
//        try {
//            Thread.sleep(2000);
//        } catch (InterruptedException ex) {
//            Logger.getLogger(HiloMaestro.class.getName()).log(Level.SEVERE, null, ex);
//        }
        while(hayTrabajo()) {
            System.out.println("Hay Trabajo"); 
            try {
//                new Scanner(System.in).nextLine();
//                System.out.println("El ciclo actual es " + ciclo);
                // mismo caso, deberia preguntarse primero si hay un contexto para asignar a ambos!! 
                // puede que en la cola solo haya 1 hilo mas y aqui asigna 2 
                if (nucleoVacio(n1)) {
                    asignarContexto(n1);      
                }
                if (nucleoVacio(n2)) {
                    asignarContexto(n2);    
                }
                lock.await();
                // procsar(); <-- ? 
                ciclo++;
            } catch (InterruptedException ex) {
                Logger.getLogger(HiloMaestro.class.getName()).log(Level.SEVERE, null, ex);
            } catch (BrokenBarrierException ex) {
                Logger.getLogger(HiloMaestro.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
        
    /* Evento que se dispara cuando se seleccionan los hilos 
    * a correr (Carga de archivos)
    */
    public String ReadFile(String filename){
        String line = null;
        System.out.println("Inicio de hilo: "+inicioHilo); 
        cola.add(new Contexto(inicioHilo));
        try {
            File myfile = new File(filename);
            FileReader filereader = new FileReader(myfile);
            BufferedReader  reader = new BufferedReader(filereader);
            
            while((line = reader.readLine()) != null) {
                String [] single = line.split(" ");
                for (int i=0; i < single.length; i++){
                    int number = Integer.parseInt(single[i]);
                    memoriaInstrucciones[totalInstrucciones] = number;
                    totalInstrucciones++; 
                }
                inicioHilo++;                
            }
            //imprimirMemoria();
            System.out.println("Archivo "+filename+" procesado.");
            System.out.println("Contexto agregado");
        }catch (Exception e) {
            
        }
        cantidadHilosCargados++;
        return line;
    }
    
    public static void initMemoriaInstrucciones() {
        for (int i=0; i < cantidadMemInstrucciones; i++){
            memoriaInstrucciones[i] = 0;
        }
    }
    
    public void imprimirMemoria() {
        String cajita = "";
        for (int i=0; i < cantidadMemInstrucciones; i++){
            cajita += "["+memoriaInstrucciones[i]+"] , ";   
        }
        System.out.println(cajita);
    }
    
    public static int leerMemoria(int posicion) {
        int valor = memoriaInstrucciones[posicion];
        return valor; 
    }
    
    public void setParametrosCiclos(int q, int m, int b) {
        quantumCiclos = q;
        memoriaCiclos = m;
        busCiclos = b;
        Nucleo.QUAMTUM = q; 
        Nucleo.m = m;
        Nucleo.b = b; 
        String r = "q="+quantumCiclos+" m="+memoriaCiclos+" b="+busCiclos; 
        System.out.println("q="+quantumCiclos+" m="+memoriaCiclos+" b="+busCiclos); 
    }
    
     public HiloMaestro() {
        ciclo = 1;
        n1 = new Nucleo(lock, "uno", lockFin1, cola);
        n2 = new Nucleo(lock, "dos", lockFin2, cola); 
        System.out.println("Se crean los 2 nucleos");
        
    }
} // Final de clase
