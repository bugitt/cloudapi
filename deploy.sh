#!/usr/bin/env bash

./gradlew clean bootJar -x test

scp build/libs/cloudapi-0.0.1-SNAPSHOT.jar kmaster:/opt/cloudapi/cloudapi.jar

ssh kmaster 'docker rm -f cloudapi'

ssh kmaster 'docker run -d --restart=always --name cloudapi -v "/opt/cloudapi":/usr/src/myapp -w /usr/src/myapp -p 9090:8080 openjdk:17 java -jar cloudapi.jar'

