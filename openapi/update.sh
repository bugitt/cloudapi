#!/usr/bin/env bash

openapi-generator generate -g kotlin-server -i cloudapi_v2.yaml -o tmp -c openapi_config.yaml

src_dir="./tmp/src/main/kotlin/cn/edu/buaa/scs/controller"
des_dir="../cloudapi-web/src/main/kotlin/cn/edu/buaa/scs/controller"

des_model_dir="$des_dir/models"
rm -rf $des_model_dir
mkdir -p $des_model_dir
cp -r "$src_dir/models/" $des_model_dir

#
#des_path_file="$des_dir/Paths.kt"
#cp "$src_dir/Paths.kt" $des_path_file

rm -rf ./tmp
