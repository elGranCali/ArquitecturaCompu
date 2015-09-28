/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package arquiproject;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.ConcurrentLinkedQueue; 

/**
 *
 * @author CALI
 */
public class Nucleo extends Thread {
    private final CyclicBarrier barreraLock;
    public String id;
    private boolean hayTrabajo;
    public boolean ocupado;
    public Contexto contexto;
    int [] registros; 
    int bloquesEnCache = 8; 
    int [][] cacheInstrucciones = new int[8][17];
    boolean esFin = false;
    private Lock lockFin;
    public static int QUAMTUM = 20;
    public static int m = 2;
    public static int b = 2;
    private ConcurrentLinkedQueue<Contexto> cola;
    
    public Nucleo(CyclicBarrier lock, String id, Lock lockFin, ConcurrentLinkedQueue<Contexto> cola){
        contexto = null;
        this.cola = cola;
        this.barreraLock = lock;
        this.lockFin = lockFin;
        this.id = id;
        hayTrabajo = true;
        ocupado = false;
        registros = new int[33];
        for(int i = 0; i<33; i++){
            registros[i]=0;
        }
        // inicializacion de la cache en -1's
        for (int i=0; i < 8; i++){
            for (int j=0; j < 17; j++) {
                cacheInstrucciones[i][j] = -1;
            }
        }
        
    }
    
     public void imprimirCache() {
        String cajita = "";
        for (int i=0; i < 8; i++){
            for (int j=0; j < 17; j++) {
                 cajita += "["+cacheInstrucciones[i][j]+"] , ";
            }
        }
        System.out.println(cajita);
    }
    
    public void reiniciarQuatum(){
        
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
            barreraLock.await();
        } catch (BrokenBarrierException ex) {
            Logger.getLogger(Nucleo.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
    boolean estaEnCache(int numBloque){
    // revisamos si existe la tag en la columna 16 de la matriz de cache igual a numBloque
        for(int i=0; i < 8 ; i++) {  // Recorro el numero de bloques
            if (cacheInstrucciones[i][16] == numBloque) {
                return true;    
            }       
        }
        return false; 
    }
    
    String leerInstruccionDeCache(int numBloque, int numPalabra) {
        String hilillo;
        int op = cacheInstrucciones[numBloque][numPalabra];
        int reg1 = cacheInstrucciones[numBloque][numPalabra+1];
        int reg2 = cacheInstrucciones[numBloque][numPalabra+2];
        int result = cacheInstrucciones[numBloque][numPalabra+3];
        hilillo = op+" "+reg1+" "+reg2+" "+result; 
        return hilillo;
    }
    
    public void procesarQuantum () {
        int tiempoTrayendoBloque = 4*(b+m+b);
        System.out.println("El tiempo trayendo bloque para esta simulacion es: "+tiempoTrayendoBloque);       
        boolean busBusy = false;          
        boolean completadoEnEsteCiclo = true;   // Para la primera entrega siempre es true
        int q = QUAMTUM;
        System.out.println("EL QUAMTUM ES: " + q);
        while (q != 0)  {       // Ejecución de todas las instrucciones que comprenden un quantum
            int pc = contexto.PC; // dirección apartir de la cuál leer la instruccion
            System.out.println("El PC actual es: "+pc);       
            int numBloque = pc/16;
            System.out.println("Buscando numero de bloque: "+numBloque);  
            int numPalabra = pc%16;
            
            if (estaEnCache(numBloque)) {       // la instrucción está en cache?
                System.out.println("Instruccion esta en cache"); 
                String hilillo = leerInstruccionDeCache(numBloque, numPalabra);
                System.out.println("Hilillo actual: "+hilillo); 
                registros = Decodificador.decodificacion(hilillo, registros);
                if (!completadoEnEsteCiclo) {   // 2da Entrega
                    // es una operación SW o LW pues dura mas de un ciclo
                    // Volver a calcular los ciclos de espera, pedir el bus, leer memoria, avanzar reloj cuando termine espera
                }else {
                    q--;                        // Disminuimos Quatum 
                    pc = pc+4;                  // Aumentamos pc 
                    lockFin.lock();             
                    try {
                        esFin = Decodificador.esFin(hilillo);
                        System.out.println("Variable esFin esta en "+esFin);
                    } finally {
                        lockFin.unlock();
                    }
                    
                    // Avanzar reloj 
                    try {
                        avanzarReloj();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Nucleo.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } else {    // Instruccion no esta en cache 
                // ponemos en cache la tag del numero de bloque
                int nuevoNumBloque = numBloque%bloquesEnCache;  // averiguar donde colocar bloque
                System.out.println("Colocando el bloque: "+ numBloque + "en el bloque de cache: "+nuevoNumBloque);  
                cacheInstrucciones[nuevoNumBloque][16] = numBloque;
                int c = tiempoTrayendoBloque; 
                if (busBusy) {
                    // Avanzar reloj hasta que bus esté desocupado
                    try {
                        avanzarReloj();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Nucleo.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    
                    // traer de memoria las 4 palabras del bloque 
                    System.out.println("Trayendo de memoria todo el bloque: "+nuevoNumBloque);  
                    for (int j=0; j<4; j++) {  // 4 palabras
                        for (int i=0; i<4; i++) {  // 4 campitos de 1 palabra
                            cacheInstrucciones[nuevoNumBloque][j*4+i]= HiloMaestro.leerMemoria(4*j+i);
                        }
                    }
                    imprimirCache();
                    HiloMaestro.imprimirMemoria();
                    // mientras que la cantidad de ciclos nueva no termine 
                    if (c != 0){
                        c--;    // Solo avance reloj 
                        try {
                            avanzarReloj();
                        } catch (InterruptedException ex) {
                            Logger.getLogger(Nucleo.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    } 
                    // Al salir de aquí ya cargó el bloque a cache
                }
            }
            contexto.PC = pc;
        }//final de while
        System.out.println("Termina de procesar quantum. El contexto guardado es: PC = "+contexto.PC);  
        q = QUAMTUM;
        // Guardar el contexto actual
        contexto.registros = registros;
        // Como aun no termina, se mete el contexto a la cola para luego volver a procesarlo
        System.out.println("Se agrega contexto a la cola");  
        cola.add(contexto); // Guardamos el contexto
        contexto = null; // Retiramos el contexto del nuecleo
    }
    
    
    @Override
    public void run(){
        while (hayTrabajo) {    // Seria hasta que ya no exista trabajo
            // Caso de que no tenga un contexto asignado el debe seguir corriendo  
            if (contexto == null) {
                try {
                    avanzarReloj();
                } catch (InterruptedException ex) {
                    Logger.getLogger(Nucleo.class.getName()).log(Level.SEVERE, null, ex);
                }
                continue;  // Salga de esta iteración hasta que exista un contexto
            }  
            System.out.println("Núcleo " + id + " iniciando el quantum");
            procesarQuantum(); // Se procesa un quantum 
            System.out.println("Núcleo " + id + " terminado quantum");  
        }
    }        
}
