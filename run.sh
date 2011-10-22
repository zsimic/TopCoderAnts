#!/bin/bash
java -ea -cp lib/ants-api.jar:lib/ants-server.jar:ants-zoran/build/libs/ants-zoran.jar org/linkedin/contest/ants/server/AntServer -B -p1 org.linkedin.contest.ants.zoran.ZoranAnt -p2 org.linkedin.contest.ants.zoran.ZoranAnt

#main class: org/linkedin/contest/ants/server/AntServer
#args: -B -p1 org.linkedin.contest.ants.zoran.ZoranAnt -p2 org.linkedin.contest.ants.zoran.DoNothingAnt
