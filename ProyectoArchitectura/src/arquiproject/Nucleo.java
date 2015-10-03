/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package arquiproject;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.ConcurrentLinkedQueue; 

/**
 *
 * @author CALI
 */
public class Nucleo extends Thread {
    private final CyclicBarrier barreraLock;
    public String id;
    public boolean ocupado;
    public Contexto contexto;
    int bloquesEnCache = 8; 
    int [][] cacheInstrucciones = new int[8][17];
    boolean esFin = false;
    private final Lock lockOcupado;
    public static int QUAMTUM = 20;
    public static int m = 2;
    public static int b = 2;
    private final ConcurrentLinkedQueue<Contexto> cola;
    private final ConcurrentLinkedQueue<Contexto> coladeTerminados;
    
    public Nucleo(CyclicBarrier lock, String id, Lock lockFin, ConcurrentLinkedQueue<Contexto> cola, ConcurrentLinkedQueue<Contexto> coladeTerminados){
        contexto = null;
        this.cola = cola;
        this.coladeTerminados = coladeTerminados;
        this.barreraLock = lock;
        this.lockOcupado = lockFin;
        this.id = id;
        ocupado = false;
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
        lockOcupado.lock();
        try {
            ocupado = true;
        } finally {
            lockOcupado.unlock();
        }
    }
    
   
    // Este método puede ser llamado en cualquier momento que se termine un ciclo
    private void avanzarReloj() throws InterruptedException{
        try {
            barreraLock.await();
        } catch (BrokenBarrierException ex) {
            System.out.println("Se reseteo la barrera");
        }
    }
    
    
    boolean estaEnCache(int numBloque){
        // revisamos si existe la tag en la columna 16 de la matriz de cache igual a numBloque        
        return cacheInstrucciones[numBloque%8][16] == numBloque; 
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
        boolean agregarATerminados = false;
        boolean completadoEnEsteCiclo = true;   // Para la primera entrega siempre es true
        int q = QUAMTUM;
        //System.out.println("EL QUAMTUM ES: " + q);
        while (q != 0)  {       // Ejecución de todas las instrucciones que comprenden un quantum
            System.out.println("El PC actual del nucleo " + id.toUpperCase() + " es: "+contexto.PC);       
            int numBloque = contexto.PC/16; // Bloque en memoria
            System.out.println("Buscando numero de bloque: "+numBloque);  
            int numPalabra = contexto.PC%16;
            
            if (estaEnCache(numBloque)) {       // la instrucción está en cache?
                System.out.println("Instruccion esta en cache"); 
                String hilillo = leerInstruccionDeCache(numBloque%8, numPalabra);
                Decodificador.decodificacion(hilillo, contexto);
                if (!completadoEnEsteCiclo) {   // 2da Entrega
                    // es una operación SW o LW pues dura mas de un ciclo
                    // Volver a calcular los ciclos de espera, pedir el bus, leer memoria, avanzar reloj cuando termine espera
                }else {
                    q--;                        // Disminuimos Quatum 
                    //lockFin.lock();
                    try {
                        esFin = Decodificador.esFin(hilillo);
                        if (esFin){
                            agregarATerminados = true;
                            HiloMaestro.terminarHilo(); 
                            System.out.println("El nucleo "+ id + " agrega el contexto del hilillo " + contexto.id +" a la cola de terminados");
                            coladeTerminados.add(contexto);
                            try {
                                avanzarReloj();
                            } catch (InterruptedException ex) {
                                System.out.println("Falla al avanzar el reloj cuando se ejecuta el fin");
                            }
                            break;
                        }
                    } finally {
                       // lockOcupado.unlock();
                    }
                    // Avanzar reloj 
                    try {
                        avanzarReloj();
                    } catch (InterruptedException ex) {
                        System.out.println("Falla al avanzar el reloj despues de ejecutar una instruccion");
                    }
                }
            } else {    // Instruccion no esta en cache 
                // ponemos en cache la tag del numero de bloque
                int nuevoNumBloque = numBloque%bloquesEnCache;  // averiguar donde colocar bloque
                System.out.println("Colocando el bloque: "+ numBloque + "en el bloque de cache: "+nuevoNumBloque);  
                cacheInstrucciones[nuevoNumBloque][16] = numBloque;
                int c = tiempoTrayendoBloque; 
                if (!HiloMaestro.attemptAccess()) {
                    System.out.println("Bus esta ocupado, buu");
                    // Avanzar reloj hasta que bus esté desocupado
                    try {
                        avanzarReloj();
                    } catch (InterruptedException ex) {
                        System.out.println("AVANCEEEE EN NUCLEO" + id);;
                    }
                } else {
                    // traer de memoria las 4 palabras del bloque 
                    System.out.println("Bus libre. Trayendo de memoria todo el bloque: "+nuevoNumBloque);  
                    for (int j=0; j<4; j++) {  // 4 palabras
                        for (int i=0; i<4; i++) {  // 4 campitos de 1 palabra
                            cacheInstrucciones[nuevoNumBloque][j*4+i]= HiloMaestro.leerMemoria((nuevoNumBloque*16)+4*j+i);
                        }
                    }
                    //imprimirCache();
                    //HiloMaestro.imprimirMemoria();
                    HiloMaestro.releaseAccess();
                    // mientras que la cantidad de ciclos nueva no termine 
                    while (c != 0){
                        c--;    // Solo avance reloj 
                        try {
                            avanzarReloj();
                        } catch (InterruptedException ex) {
                            System.out.println("Error en el trayendo datos de memoria");
                        }
                    } 
                    // Al salir de aquí ya cargó el bloque a cache
                }
            }
        }//final de whileç
        System.out.println("El nucleo "+ id+ " termina de procesar quantum. El contexto guardado es: PC = "+contexto.PC);  
        // Como aun no termina, se mete el contexto a la cola para luego volver a procesarlo  
        if (!agregarATerminados) {
            System.out.println("El nucleo "+ id + " agrega el contexto del hilillo " + contexto.id +" a la cola");
            cola.add(contexto); // Guardamos el contexto
        } 
        contexto = null; // Retiramos el contexto del nuecleo
    }
    
    
    @Override
    public void run(){
        while (HiloMaestro.hayTrabajo()) {    // Seria hasta que ya no exista trabajo
            // Caso de que no tenga un contexto asignado el debe seguir corriendo  
            if (contexto == null) {
                System.out.println("Nucleo " + id + " esta ocioso");
                try {
                    avanzarReloj();
                } catch (InterruptedException ex) {
                    System.out.println("Falla al avanzar el reloj cuando no hay contexto");
                }              
                continue;  // Salga de esta iteración hasta que exista un contexto
            }  
            System.out.println("Núcleo " + id + " iniciando el quantum");
            procesarQuantum(); // Se procesa un quantum 
            lockOcupado.lock();
            try {
                ocupado = false; 
            } finally {
                lockOcupado.unlock();
            }
            System.out.println("Núcleo " + id + " terminado quantum");  
        }
        System.out.println("TERMINO EL NUCLEO " +id);
    }        
}
