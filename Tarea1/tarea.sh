#!/bin/bash
#$ -cwd
#$ -j y
#$ -S /bin/bash
echo 3 | /opt/mpich2/gnu/bin/mpiexec -n 20 -f /home/A75952/distribucion ./primos 
