FROM adoptopenjdk/openjdk11
ARG JAR_FILE=/build/libs/*.jar
COPY $JAR_FILE rental.jar
ENTRYPOINT ["java", "-jar", "/rental.jar"]