FROM eclipse-temurin:8-jre-jammy

COPY ./build/libs/vcenter-0.0.1-all.jar /usr/src/myapp/vcenter.jar

WORKDIR /usr/src/myapp

EXPOSE 9977

CMD ["java", "-jar", "vcenter.jar"]
