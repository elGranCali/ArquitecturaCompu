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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
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
    int quantumCiclos;
    int memoriaCiclos;
    int busCiclos; 
    private Lock lockFin1 = new ReentrantLock();
    private Lock lockFin2 = new ReentrantLock();
    private int inicioHilo = 0;
    
    Nucleo n1;
    Nucleo n2; 
    private ConcurrentLinkedQueue<Contexto> cola = new ConcurrentLinkedQueue<Contexto>();
    

    private void iniciarHilos(){
        n1.start();
        n2.start();
    }
    
    private boolean hayTrabajo(){
        return !cola.isEmpty();
    }
    
     private void asignarContexto(Nucleo nucleo) {
        if (!nucleo.ocupado) {
            nucleo.setContexto(cola.remove());
        }
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
    
    public void procesar() {
        
        n1.procesar();
        n2.procesar();
        
    }
    
    public void iniciar(){
        iniciarHilos(); // Siempre ejecutaran primero el wait
        asignarContexto(n1);
        asignarContexto(n2);
        procesar();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {
            Logger.getLogger(HiloMaestro.class.getName()).log(Level.SEVERE, null, ex);
        }
        while(hayTrabajo()) {     
            try {
                new Scanner(System.in).nextLine();
                System.out.println("El ciclo actual es " + ciclo);
                
                if (nucleoVacio(n1)) {
                    asignarContexto(n1);      
                }
                if (nucleoVacio(n2)) {
                    asignarContexto(n2);    
                }
                lock.await();
                
                ciclo++;
            } catch (InterruptedException ex) {
                Logger.getLogger(HiloMaestro.class.getName()).log(Level.SEVERE, null, ex);
            } catch (BrokenBarrierException ex) {
                Logger.getLogger(HiloMaestro.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
        
    
    public String ReadFile(String filename){
        String line = null;
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
        Nucleo.q = q; 
        Nucleo.m = m;
        Nucleo.b = b;
        
        String r = "q="+quantumCiclos+" m="+memoriaCiclos+" b="+busCiclos; 
        System.out.println("q="+quantumCiclos+" m="+memoriaCiclos+" b="+busCiclos);
        //new interfaz().imprima(r); 
    }
    
     public HiloMaestro() {
        ciclo = 1;
        n1 = new Nucleo(lock, "uno", lockFin1, cola);
        n2 = new Nucleo(lock, "dos", lockFin2, cola); 
    }
    
    
     /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        initMemoriaInstrucciones();
        java.awt.EventQueue.invokeLater(new Runnable() {
        public void run() {
                interfaz inter = new interfaz();
                inter.setVisible(true);
            }
        });
        
        HiloMaestro maestro = new HiloMaestro();
        maestro.iniciar();
        
    }

  
    
} // Final de clase
