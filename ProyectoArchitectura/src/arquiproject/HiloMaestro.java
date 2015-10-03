/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package arquiproject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentLinkedQueue; 
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
/**
 *
 * @author Pc
 */
public class HiloMaestro {
    static int totalInstrucciones = 0;
    public static int hilosAprocesar = 0;
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
    private final static Semaphore busInstrucciones = new Semaphore(1);
    Nucleo n1;
    Nucleo n2; 
    private final ConcurrentLinkedQueue<Contexto> cola = new ConcurrentLinkedQueue<>();
    public static final ConcurrentLinkedQueue<Contexto> colaDeTerminados = new ConcurrentLinkedQueue<>();
    
    
   public static boolean attemptAccess() {
       return busInstrucciones.tryAcquire();
   }
   
   public static boolean releaseAccess() {
       busInstrucciones.release();
       return true;
   }
    
    public static synchronized boolean hayTrabajo(){
        return !(hilosAprocesar==0);
    }
   
    public static synchronized void terminarHilo(){
        hilosAprocesar--;
    }
    
    public boolean nucleoVacio(Nucleo n) {
        
        if (n.id.equals("uno") ){
            lockFin1.lock();
            try {
                if (n.esFin == true || n.ocupado == false) {
                    n.esFin = false;
                    return true;
                }
            } finally {
                lockFin1.unlock();
            }
            return false; 
        } else {
            lockFin2.lock();
            try {
                if (n.esFin == true || n.ocupado == false) {
                    n.esFin = false;
                    return true;
                }
            } finally {
                lockFin2.unlock();
            }
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
    
    public String iniciar(){
        // debería preguntarse ademas de q si el nucleo esta ocupado.. de que si hay un contexto para cada nuclio
        // revisar q en la cola hayan hilos para asignar 2!! 
        asignarContexto(n1);
        asignarContexto(n2);
        iniciarHilos(); 
        while(true) {
            System.out.println("Avance hilo maestro"); 
            try {
                if (nucleoVacio(n1)) {
                    asignarContexto(n1);      
                }
                if (nucleoVacio(n2)) {
                    asignarContexto(n2);    
                }
                if (hayTrabajo()) {
                    lock.await(3,TimeUnit.SECONDS);
                } else {
                    if (lock.getNumberWaiting() != 0) {
                        lock.reset();
                    }
                    break;
                }
                ciclo++;
            } catch (InterruptedException ex) {
                System.out.println("Interrupcion");
            } catch (BrokenBarrierException ex) {
                System.out.println("Se rompio la barrera");
            } catch (TimeoutException ex) {
                System.out.println("Se acabo el tiempo");
            } 
        }
        String ans = "EL CICLO ES: " + ciclo + "\n";
        // Este es un delay para esperar que los hilillos se guarden en la cola de terminos y puedan ser impresos
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {
            System.out.println("Fallo en el delay de la cola de terminados");
        }
        while (colaDeTerminados.peek() != null) {
            ans += colaDeTerminados.poll().toString();
        }
        return ans;
    }
        
    /* Evento que se dispara cuando se seleccionan los hilos 
    * a correr (Carga de archivos)
    */
    public String ReadFile(String filename){
        String line = null;
        inicioHilo = inicioHilo * 4; 
        System.out.println("Inicio de hilo: "+totalInstrucciones); 
        cola.add(new Contexto(totalInstrucciones, hilosAprocesar+1));
        try {
            File myfile = new File(filename);
            FileReader filereader = new FileReader(myfile);
            BufferedReader  reader = new BufferedReader(filereader);
            
            while((line = reader.readLine()) != null) {
                String [] single = line.split(" ");
                for (String single1 : single) {
                    int number = Integer.parseInt(single1);
                    memoriaInstrucciones[totalInstrucciones] = number;
                    totalInstrucciones++; 
                }
                inicioHilo++;                
            }
            System.out.println("Archivo "+filename+" procesado.");
            System.out.println("Contexto agregado");
        }catch (Exception e) {
            System.out.println("Error al leer archivo");
        }
        hilosAprocesar++;
        return line;
    }
    
    public static void initMemoriaInstrucciones() {
        for (int i=0; i < cantidadMemInstrucciones; i++){
            memoriaInstrucciones[i] = 0;
        }
    }
    
    public static void imprimirMemoria() {
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
        System.out.println("q="+quantumCiclos+" m="+memoriaCiclos+" b="+busCiclos); 
    }
    
     public HiloMaestro() {
        ciclo = 1;
        n1 = new Nucleo(lock, "uno", lockFin1, cola, colaDeTerminados);
        n2 = new Nucleo(lock, "dos", lockFin2, cola, colaDeTerminados); 
        System.out.println("Se crean los 2 nucleos");      
    }
} // Final de clase
