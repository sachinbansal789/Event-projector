FROM docker.allianzuk.cloud/azuk/azuk-java-11:latest

ARG user=sa
ARG group=sa
ARG uid=2000
ARG gid=2000

RUN addgroup -g ${gid} ${group} && adduser -h "/home/${user}" -u ${uid} -G ${group} -s /bin/bash -D ${user}



VOLUME /tmp

ARG VERSION

WORKDIR /etc/app

ENV LOG_FORMAT=json

ADD target/service.jar /etc/app/service.jar

# Add Allianz group CA public certs
ADD src/Allianz-Infrastructure-CA-V.crt /usr/local/share/ca-certificates/Allianz-Infrastructure-CA-V.crt
RUN update-ca-certificates

HEALTHCHECK NONE

EXPOSE 8080 8080

RUN echo '#!/bin/sh' >  ./entrypoint.sh \
 && echo 'exec java -jar /etc/app/service.jar'   >> ./entrypoint.sh

RUN chmod +x ./entrypoint.sh


RUN chown -R ${user}:${group} ./entrypoint.sh
# Run hardening script then remove
ADD harden_java.sh /opt/hardening_scripts/
RUN chmod u+x /opt/hardening_scripts/harden_java.sh
RUN /opt/hardening_scripts/harden_java.sh
RUN rm -r /opt/hardening_scripts

USER ${user}


ENTRYPOINT ["./entrypoint.sh"]
