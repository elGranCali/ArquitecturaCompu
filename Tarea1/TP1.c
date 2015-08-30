
#include <mpi.h>
#include <stdio.h>
#include <math.h>
#include<string.h>

/*
	mcd  (Maximo Comun Divisor)
	Descripcion: Funcion que devuelve el maximo comun divisor de 2 numeros.
	Recordar que el que escribe el usuarion pueda ser un long long int

*/
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
    int myid, numprocs, i = 0, *sendbuf, *recvbuf, n = 0, root = 0;
	int porcion = 0, resto = 0;   
	int cantidadPrimos = 0;
	int inicio = 0;   // 2^27
    int final = 100;   // 2^32
	int total = 100; // total de numeros diferentes para repartir
	int s = 0; // division exacta de numeros para cada proceso
	/*int inicio = 134217728;   // 2^27
    int final = 4294967296;   // 2^32
	int total = 4160749569; // total de numeros diferentes para repartir
    */
    double startwtime, endwtime;
    int  namelen;
    char processor_name[MPI_MAX_PROCESSOR_NAME];

    MPI_Init(&argc,&argv);  /*  Se inicia el trabajo con MPI */
	MPI_Comm_size(MPI_COMM_WORLD,&numprocs);  /*  MPI almacena en numprocs el numero total de procesos que se pusieron a correr */
	MPI_Comm_rank(MPI_COMM_WORLD,&myid); /*  MPI almacena en myid la identificacion del proceso actual */
	MPI_Get_processor_name(processor_name,&namelen); /*  MPI almacena en processor_name en la computadora que corre el proceso actual, y en namelen la longitud de este */

    fprintf(stdout,"Proceso %d de %d en %s\n", myid, numprocs, processor_name);
	/*  Cada proceso despliega su identificacion y el nombre de la computadora en la que corre*/
	 
	MPI_Barrier(MPI_COMM_WORLD); /* Barrera de sincronizacion.*/
	if (myid == root) {			
		 while (!n) {
			 printf("Digite un numero entre 2 y 18446744073709551615: ");
			 fflush(stdout);
			 scanf("%d",&n);  /* n es el numero al cual se le buscaran los primos relativos */
		}
		printf("Usted digito: %d\n", n);
		startwtime = MPI_Wtime();  /* inicia el tiempo que dure el programa */
		
	}
	
	porcion = total/numprocs; 
	resto = total%numprocs; 
		
		if(resto == 0) {           /* si no sobran elementos  el arreglo se mantiene de tamano porcion */
			sendbuf = (int *)malloc((porcion)*sizeof(int));  /* asignación dinámica del arreglo inicial para ordenar*/
		}
			
		else { /* Si sobran r > 0 elementos, se debe  repartir en partes iguales a los procesos*/
			sendbuf = (int *)malloc((porcion)*sizeof(int)); /* asignación dinámica del arreglo inicial para ordenar*/
			for(i= total ;i<total+numprocs-resto;i++)                   /*Se ponen 0's en los últimos porcion - resto elementos del arreglo */
				sendbuf[i]=0;
		}
		s = porcion/numprocs;
		
        MPI_Bcast(&n, 1, MPI_INT, 0, MPI_COMM_WORLD);	/* todos deben conocer el numero digitado por el usuario */
		
		int actual = inicio + myid*porcion; /* El valor actual que vamos a probar segun el proceso */

		int limiteSuperior;
		if (actual + porcion <  final) {
			limiteSuperior = actual + porcion;			/* calculo del limite superior para cada proceso */ 
		} else {
			limiteSuperior = final;
		}
		
		int contador = 0;   // contador de primos encontrados 
		
		/* se crea un archivo de nombre respuesta+ el identificador del proceso actual */
		char copyname[50];
		sprintf(copyname, "Respuesta%i.txt", myid);
		FILE *archivo;
		archivo = fopen(copyname,"w");
		
		while (actual < limiteSuperior) { 
			int result = mcd(actual,n);
			if (result == 1) { // si el mcd es 1 son primos relativos
				sendbuf[contador] = actual;
				//printf("x   Proceso: %d Primo encontrado: %d\n", myid, sendbuf[contador]);  /* para corroborar en consola */ 
				fprintf(archivo, "Primo encontrado: %d\n", sendbuf[contador]);
				contador += 1;
			  }
			actual += 1;
		}
		fclose(archivo);
		
		//Cada proceso envia la cantidad de primos que encontro para saber el tamano del array que guarda los primos en el root
		MPI_Reduce(&contador, &cantidadPrimos, 1, MPI_INT, MPI_SUM, 0, MPI_COMM_WORLD);	
		
        if (myid == root) {
			printf("Proceso %d da la respuesta. Cantidad de primos encontrados: %d \n", myid, cantidadPrimos);
			printf("Ver los archivos creados en esta carpeta para saber los primos encontrados. \n");
			endwtime = MPI_Wtime(); 
			printf("Tiempo de reloj: %f\n", endwtime-startwtime);	       
			fflush( stdout );
			free(sendbuf);  //Limpiamos el buffer
	    }
           
    MPI_Finalize();
    return 0;
}
