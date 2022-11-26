JAVA_HOME ?= ${JAVA_HOME}

VERSION = ${shell git describe --tags}
IMAGE = scs.buaa.edu.cn:8081/iobs/cloudapi:$(VERSION)

build:
	JAVA_HOME=$(JAVA_HOME) ./gradlew clean build -x test

image: build
	docker build -t $(IMAGE) -f ./Dockerfile .

push: image
	docker push $(IMAGE)
