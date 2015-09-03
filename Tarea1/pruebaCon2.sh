#!/bin/bash
#$ -cwd
#$ -j y
#$ -S /bin/bash
echo 2 | /opt/mpich2/gnu/bin/mpiexec -n 20 -f /home/B21684/Tareas/TP1 ./TP1 