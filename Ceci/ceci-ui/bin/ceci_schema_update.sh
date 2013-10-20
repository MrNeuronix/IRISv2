#!/bin/bash
DIRECTORY="/opt/agocontrol/site/database"
FILES="$DIRECTORY/*.sql"

chgrp postgres $DIRECTORY
chmod g+rwx $DIRECTORY

echo "Updating schema:"

for FILE in $FILES
do
  echo "$FILE..."
  # take action on each file. $f store current file name
  LOG="$FILE.log"
  
  if [ ! -f $LOG ]
    then
      su - postgres -c "/usr/bin/psql -f $FILE > $LOG"
      echo "DONE"
    else
      echo "SKIP"
  fi

done
