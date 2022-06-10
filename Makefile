# 这里为了省事儿，先本地构建，直接将本地打包产物copy到Nginx镜像中
# 后面可以改成直接在docker build时编译代码
image:
	./gradlew clean build -x test
	docker build -t harbor.scs.buaa.edu.cn/iobs/cloudapi:latest -f ./Dockerfile .

push: image
	docker push harbor.scs.buaa.edu.cn/iobs/cloudapi:latest
