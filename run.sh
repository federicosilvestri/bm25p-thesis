#!/bin/bash

TERRIER_DIR="../terrier/"

if [ -z $1 ]
then
  echo "You must set the number of processes!"
  exit 1;
fi

a=0
while [ $a -lt "$1" ]
do
  a=$(($a+1));

  # creating Terrier environment
  mkdir "terrier_$a/";
  cp -r "$TERRIER_DIR/etc" "terrier_$a/";
  cp -r "$TERRIER_DIR/var" "terrier_$a/";
  cp -r "$TERRIER_DIR/share" "terrier_$a/";
  cp -r "$TERRIER_DIR/start.jar" "terrier_$a/";
  cd "terrier_$a";
  java -Xms100M -Xmx5G -jar start.jar nlings $1 $(($a -1))&
  cd ..
done

echo "All instances started!"