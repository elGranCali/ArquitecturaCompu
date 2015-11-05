/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package arquiproject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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
    static int cantidadMemDatos = 1408;
    static int [] memoriaInstrucciones = new int[640];
    static int [] memoriaDatos = new int [1408];
    private int ciclo;
    private static final CyclicBarrier lock = new CyclicBarrier(3);
    int quantumCiclos;
    int memoriaCiclos;
    int busCiclos; 
    private final Lock lockOcupado1 = new ReentrantLock();
    private final Lock lockOcupado2 = new ReentrantLock();
    private int inicioHilo = 0;
    private final static Semaphore busInstrucciones = new Semaphore(1);
    private final static Semaphore busDatos = new Semaphore(1);
    static Nucleo n1;
    static Nucleo n2; 
    /*
    Nucleo n1;
    Nucleo n2; 
    */
    private final ConcurrentLinkedQueue<Contexto> cola = new ConcurrentLinkedQueue<>();
    public static final ConcurrentLinkedQueue<Contexto> colaDeTerminados = new ConcurrentLinkedQueue<>();
    public static boolean stepByStep = false;
    public static javax.swing.JTextArea textArea;
    
    
   public static boolean attemptAccess() {
       return busInstrucciones.tryAcquire();
   }
   
   public static boolean releaseAccess() {
       busInstrucciones.release();
       return true;
   }
    
   public static boolean pedirBusDatos() {
       return busDatos.tryAcquire();
   }
   
   public static boolean soltarBusDatos() {
       busDatos.release();
       return true;
   }
   
    public static synchronized boolean hayTrabajo(){
        System.out.println("Hilos a procesar son: "+ hilosAprocesar);
        return !(hilosAprocesar==0);
    }
   
    public static synchronized void terminarHilo(){
        hilosAprocesar--;
    }
    
    public boolean nucleoVacio(Nucleo n) {
        
        if (n.id.equals("uno") ){
            lockOcupado1.lock();
            try {
                if (n.ocupado == false) {
                    //n.esFin = false;
                    return true;
                }
            } finally {
                lockOcupado1.unlock();
            }
            return false; 
        } else {
            lockOcupado2.lock();
            try {
                if (n.ocupado == false) {
                    //n.esFin = false;
                    return true;
                }
            } finally {
                lockOcupado2.unlock();
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
                String [] instrucciones = line.split(" ");
                for (String instruccion : instrucciones) {
                    int number = Integer.parseInt(instruccion);
                    memoriaInstrucciones[totalInstrucciones] = number;
                    totalInstrucciones++; 
                }
                inicioHilo++;                
            }
            System.out.println("Archivo "+myfile.getName()+" procesado.");
            System.out.println("Contexto agregado");
        }catch (IOException | NumberFormatException e) {
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
    
	 public static void initMemoriaDatos() {
        for (int i=0; i < cantidadMemDatos; i++) {
            memoriaDatos[i] = -1;
        }
    }
	
    public static void imprimirMemoria() {
        String cajita = "Memoria:\n";
        for (int i=0; i < cantidadMemInstrucciones; i++){
            cajita += "["+memoriaInstrucciones[i]+"] , ";   
        }
        System.out.println(cajita);
    }
	
    public static void imprimirMemDatos() {
    String cajita = "Memoria Datos:\n";
    for (int i=0; i < cantidadMemDatos; i++){
        cajita += "["+memoriaDatos[i]+"] , ";   
    }
    System.out.println(cajita);
    }
    
    public static int leerMemoria(int posicion) {
        int valor = memoriaInstrucciones[posicion];
        return valor; 
    }
	
    public static int leerMemoriaDatos(int posicion) {
        int valor = memoriaDatos[posicion];
        return valor;
    }
    
    public void setStepByStep(boolean step){
        stepByStep = step;
    }
    
    public void setTextField(javax.swing.JTextArea jTextArea1){
        HiloMaestro.textArea = jTextArea1;
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
    
    // Metodo que permite pedir la cache del otro nucleo,usando al hilo
    // maestro como intermediario
    public static boolean pedirCacheDelOtroNucleo(String idNucleo) {
        if( idNucleo.equalsIgnoreCase("uno")){
            return n2.pedirCache();
        }else{
            return n1.pedirCache();
        }
    }
	
     public HiloMaestro() {
        ciclo = 1;
        n1 = new Nucleo(lock, "uno", lockOcupado1, cola, colaDeTerminados);
        n2 = new Nucleo(lock, "dos", lockOcupado2, cola, colaDeTerminados); 
        System.out.println("Se crean los 2 nucleos");      
    }
     
    public static void escribirEnMemoria(int [] bloque, int numBloqueMemoria) {
        //Escriba en memoria el bloque corr
    }
    
    public static int [] leerDesdeMemoria(int direccion) {
        int [] bloque = new int[4];
        for (int i = 0 ; i < Nucleo.NUMERODEBLOQUESENCACHE; i++) {
            bloque[i] = memoriaDatos[direccion+i];
        }
        return bloque;
    }
    
     public static int [] leerDesdeLaOtraCache(String nucleoFuente, int numBloqueDatoM) {
        if( nucleoFuente.equalsIgnoreCase("uno")){
            return n2.getBloque(numBloqueDatoM%4);
        }else{
            return n1.getBloque(numBloqueDatoM%4);
        }
    }
    
    public static boolean spoofing (String nucleoFuente, int numBloqueDatoM) {
        if( nucleoFuente.equalsIgnoreCase("uno")){
            return n2.cacheHit(numBloqueDatoM);
        }else{
            return n1.cacheHit(numBloqueDatoM);
        }
    }
    
     
} // Final de clase
