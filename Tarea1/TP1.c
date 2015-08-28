
#include <mpi.h>
#include <stdio.h>
#include <math.h>

// funcion que devuelve el maximo comun divisor de 2 numeros
// Recordar que el que escribe el usuarion pueda ser un long long int
int mcd(int a, int b)
{
	int part, aux;
	part = a%b;
	while (part != 0) {
		aux = b;
		b = part;
		part = aux%b;
	}
	return b; 
}
int main(int argc,char **argv)
{
    int myid, numprocs, i, *sendbuf, *recvbuf, root = 0;
	int cantidadPrimos;
	int inicio = 134217728;   // 2^27
    int final = 4294967296;   // 2^32
	int total = 4160749569; // total de números diferentes para repartir
   
    double startwtime, endwtime;
    int  namelen;
    char processor_name[MPI_MAX_PROCESSOR_NAME];

    MPI_Init(&argc,&argv);  /*  Se inicia el trabajo con MPI */
	MPI_Comm_size(MPI_COMM_WORLD,&numprocs);  /*  MPI almacena en numprocs el número total de procesos que se pusieron a correr */
	MPI_Comm_rank(MPI_COMM_WORLD,&myid); /*  MPI almacena en myid la identificación del proceso actual */
	MPI_Get_processor_name(processor_name,&namelen); /*  MPI almacena en processor_name en la computadora que corre el proceso actual, y en namelen la longitud de éste */

    fprintf(stdout,"Proceso %d de %d en %s\n", myid, numprocs, processor_name);
/*  Cada proceso despliega su identificación y el nombre de la computadora en la que corre*/
	
	
	int porcion = total/numprocs;  // tener cuidado de que si sobro uno o más bien falto uno	
	MPI_Barrier(MPI_COMM_WORLD); /* Barrera de sincronizacion.*/
        if (myid == root) {			
			 while (!n) {
				 printf("Digite un numero entre 2 y 18446744073709551615 ");
				 fflush(stdout);
				 scanf("%d",&n);
			}
			startwtime = MPI_Wtime();
        }		
		// todos deben conocer el numero digitado por el usuario
        MPI_Bcast(&n, 1, MPI_INT, 0, MPI_COMM_WORLD);

		int actual = inicio + mypid*porcion; // El valor actual que vamos a probar -en este caso el primero-
		int limiteSuperior;
		if (actual + procion <  final) {
			limiteSuperior = actual + procion;
		} else {
			limiteSuperior = final;
		}
		
		//Creacion de un array de ints por cada proceso
		sendbuf=(int*) malloc(porcion*sizeof(int));	
		int contador = 0;
		while (actual < limiteSuperior) { 
			int result = mcd(actual,n);
			if (result == 1) { // es el mcd es 1 si son primos relativos
				sendbuf[contador] = actual;
				contador = contador + 1;
			  }
			actual += actual +1;
		}
		
		//cada proceso envia la cantidad de primos que encontro para saber el tamaño del array que guarda los primos en el root
		MPI_Reduce(&contador, &cantidadPrimos, 1, MPI_DOUBLE, MPI_SUM, 0, MPI_COMM_WORLD);	
		if (myid == root) {
			recvbuf=(int*) malloc(cantidadPrimos*sizeof(int));	
		}	
		// cada proceso debe devolver los números que encontro
		MPI_Gather( sendbuf, contador, MPI_INT, recvbuf, total, MPI_INT, 0, MPI_COMM_WORLD); 
		free(sendbuf);  //Limpiamos el buffer
		
        if (myid == 0) {
            printf("el proceso 0 da la respuesta");
			endwtime = MPI_Wtime(); /* Se toma el tiempo actual, para luego calcular la duración del cálculo por 
		                        diferencia con el tiempo inicial*/
			printf("wall clock time = %f\n", endwtime-startwtime);	       
			fflush( stdout );
			FILE *archivo;
			archivo = fopen("respuesta.txt", w);
			for (int k = 0; k  < cantidadPrimos ; k++ ){
				fprintf(archivo, "%d \n", name);
			}		
			fclose(archivo);
			free(recvbuf);
	    }
           
    MPI_Finalize();
    return 0;
}
