FROM eclipse-temurin:17.0.12_7-jre-alpine

LABEL maintainer="Bertrik Sikken bertrik@gmail.com"
LABEL org.opencontainers.image.source="https://github.com/bertrik/verkeersdrukte"
LABEL org.opencontainers.image.description="Collects traffic data from NDW and republishes it in a friendly format"
LABEL org.opencontainers.image.licenses="MIT"

ADD verkeersdrukte/build/distributions/verkeersdrukte.tar /opt/

WORKDIR /opt/verkeersdrukte
ENTRYPOINT ["/opt/verkeersdrukte/bin/verkeersdrukte"]

