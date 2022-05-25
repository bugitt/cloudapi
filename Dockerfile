FROM openjdk:17

COPY ./build/libs/cloudapi-0.0.1-all.jar /usr/src/myapp/cloudapi.jar

WORKDIR /usr/src/myapp

EXPOSE 9999

CMD ["java", "-jar", "cloudapi.jar"]