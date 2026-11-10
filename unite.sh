#!/bin/bash

counter=1

find . -mindepth 2 -type f -name "dataset_*.bin" | sort | while read -r file; do
    dir=$(dirname "$file")
    new_name=$(printf "dataset_%04d.bin" "$counter")

    mv "$file" "$dir/$new_name"

    echo "$file -> $dir/$new_name"

    ((counter++))
done