#!/bin/bash

find logs -depth 1 -exec grep -H -n -i avoiding {} \;
find logs -depth 1 -exec grep -H -n -i died {} \;
cat logs/trace_*.txt | grep -c "drops food to 512,512"
