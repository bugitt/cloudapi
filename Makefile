JAVA_HOME ?= ${JAVA_HOME}

VERSION = ${shell git describe --tags}
IMAGE = scs.buaa.edu.cn:8081/iobs/cloudapi:$(VERSION)

.PHONY: all build image push

build:
	JAVA_HOME=$(JAVA_HOME) ./gradlew clean build -x test

image:
	docker build -t $(IMAGE) -f ./Dockerfile .

push:
	docker push $(IMAGE)

all: build image push
