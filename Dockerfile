FROM openjdk:17-bullseye

ARG RPKI_IRR_BGP_STATS_DIST=target/rpki-irr-bgp-stats-dist.tar.gz

COPY ${RPKI_IRR_BGP_STATS_DIST} /build/rpki-irr-bgp-stats-dist.tar.gz
RUN tar -zxf /build/rpki-irr-bgp-stats-dist.tar.gz -C /opt \
    && rm -rf /build

ENV PATH="${PATH}:/opt/rpki-irr-bgp-stats"
