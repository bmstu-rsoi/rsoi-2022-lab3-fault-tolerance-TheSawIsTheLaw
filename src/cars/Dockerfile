FROM adoptopenjdk/openjdk11
ARG JAR_FILE=/build/libs/*.jar
COPY $JAR_FILE cars.jar
ENTRYPOINT ["java", "-jar", "/cars.jar"]