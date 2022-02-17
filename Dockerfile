# Definition of source image
# FROM openjdk:11.0-jre-slim
FROM openjdk:8-jre-alpine

# Deployment of software components
RUN mkdir /app 
#     && apt update 

COPY target/ais-service-1.0.0-SNAPSHOT.jar /app

# Run the container
EXPOSE 2201
WORKDIR /app
ENTRYPOINT ["sh", "-c"]
CMD ["java -jar /app/ais-service-1.0.0-SNAPSHOT.jar"]