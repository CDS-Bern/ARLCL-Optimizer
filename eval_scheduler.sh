#!/bin/bash

END=0
for i in $(seq 0 $END)
do /home/gouz/.jdks/adopt-openjdk-16.0.1/bin/java -jar /home/gouz/IdeaProjects/ARLCL-Sim/out/artifacts/ARLCL_Sim_jar/ARLCL-Sim.jar out_path=/media/gouz/5525a2b6-d208-4e52-b5eb-2f79b24261f5/Positioning/Export/ARLCL_Estimations/ rss_db_path=/media/gouz/5525a2b6-d208-4e52-b5eb-2f79b24261f5/Positioning/Node_Samples/ scenarios_path=/media/gouz/5525a2b6-d208-4e52-b5eb-2f79b24261f5/Positioning/Batches/b14.txt eval_id=$i seed=$i end_iter=100 opt_iter=100 threads=1 cycles=50
done