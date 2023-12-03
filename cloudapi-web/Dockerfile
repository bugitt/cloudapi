FROM openjdk:11-bullseye

COPY ./build/libs/cloudapi-web-0.0.1-all.jar /usr/src/myapp/cloudapi.jar

COPY /binary/mc /bin/mc

WORKDIR /usr/src/myapp

EXPOSE 9999

CMD ["java", "-jar", "cloudapi.jar"]
