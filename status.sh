#!/bin/bash

#find logs -depth 1 -exec grep -H -n -i avoiding {} \;
#find logs -depth 1 -exec grep -H -n -i died {} \;
#find logs -depth 1 -exec grep -H -n -i check {} \;
cat logs/info.txt | grep "turning into a guard"
echo -n "give up: "; cat logs/info.txt | grep -c "turning into a guard"
echo -n "food: "; cat logs/trace_*.txt | grep -c "drops food to 512,512"
