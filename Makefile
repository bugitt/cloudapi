build:
	./gradlew clean build -x test

# 这里为了省事儿，先本地构建，直接将本地打包产物copy到Nginx镜像中
# 后面可以改成直接在docker build时编译代码
image: build
	docker build -t cloudapi -f ./Dockerfile .
	docker tag cloudapi harbor.scs.buaa.edu.cn/iobs/cloudapi:latest

push: image
	docker push harbor.scs.buaa.edu.cn/iobs/cloudapi:latest
