#!/usr/bin/env bash

openapi-generator generate -g kotlin -i ./harbor_api.yaml -c config.yaml -o ./tmp

src_dir="./tmp/src/main/kotlin/cn/edu/buaa/scs/sdk/harbor/"
des_dir="../../src/main/kotlin/cn/edu/buaa/scs/sdk/harbor"

mkdir -p $des_dir

cp -r $src_dir $des_dir

rm -rf ./tmp
