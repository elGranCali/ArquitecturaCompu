/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package arquiproject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 *
 * @author Pc
 */
public class HiloMaestro {
    static int totalInstrucciones = 0; 
    static int cantidadMemInstrucciones = 640; 
    static int [] memoriaInstrucciones = new int[640];
    private int ciclo;
    private static final CyclicBarrier lock = new CyclicBarrier(3);
    private static final Object lock2 = new Object();
    int quantumCiclos;
    int memoriaCiclos;
    int busCiclos; 
    
    
    Nucleo n1;
    Nucleo n2; 
    private Queue<Contexto> cola = new ConcurrentLinkedQueue<Contexto>();
    

    private void iniciarHilos(){
        n1.start();
        n2.start();
    }
    
    private boolean hayTrabajo(){
        return !cola.isEmpty();
    }
    
    /*private void guardarContextos(){
        // Parte que habria q subir los hilos y darles un contexto
        agregarContexto(new Contexto());
        agregarContexto(new Contexto());
    }
    public void agregarContexto(Contexto nuevoContexto){
        if (cola.add(nuevoContexto)) {
            //System.out.println("Contexto agregado");
        }
    }
    */
    
     private void asignarContexto(Nucleo nucleo) {
        if (!nucleo.ocupado) {
            nucleo.setContexto(cola.remove());
        }
    }
    
    public void modificarVariableInstancia() {
            synchronized(lock2){
            try {
                lock2.wait();
            } catch (InterruptedException ex) {
                Logger.getLogger(Nucleo.class.getName()).log(Level.SEVERE, null, ex);
            }
            n1.ocupado = false;
        }
    }
    
    public void iniciar(){
        iniciarHilos(); // Siempre ejecutaran primero el wait
        asignarContexto(n1);
        asignarContexto(n2);
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
                asignarContexto(n1);    
                asignarContexto(n2);

                
            } catch (InterruptedException ex) {
                Logger.getLogger(HiloMaestro.class.getName()).log(Level.SEVERE, null, ex);
            } catch (BrokenBarrierException ex) {
                Logger.getLogger(HiloMaestro.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
        
     
    public String ReadFile(String filename){
        String line = null;
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
            }
            //imprimirMemoria();
            System.out.println("Archivo "+filename+" procesado.");
            cola.add(new Contexto());
            System.out.println("Contexto agregado");
        }catch (Exception e) {
            
        }
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
        String r = "q="+quantumCiclos+" m="+memoriaCiclos+" b="+busCiclos; 
        System.out.println("q="+quantumCiclos+" m="+memoriaCiclos+" b="+busCiclos);
        //new interfaz().imprima(r); 
    }
    
     public HiloMaestro() {
        ciclo = 1;
        n1 = new Nucleo(lock, "uno");
        n2 = new Nucleo(lock, "dos"); 
    }
    
    
     /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        initMemoriaInstrucciones();
        java.awt.EventQueue.invokeLater(new Runnable() {
        public void run() {
                new interfaz().setVisible(true);
            }
        });
        
        HiloMaestro maestro = new HiloMaestro();
        maestro.iniciar();
    }

  
    
} // Final de clase
