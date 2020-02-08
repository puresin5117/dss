package com.ztkmkoo.dss.network.http;

import com.ztkmkoo.dss.network.common.AbstractDssServer;
import com.ztkmkoo.dss.network.common.DssServerProperty;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;
import java.util.Optional;
import java.util.logging.Level;

public class DssHttpServerImpl extends AbstractDssServer {

    private static final String SCHEME_HTTP = "http";
    private static final String SCHEME_HTTPS = "https";

    @Override
    protected ServerBootstrap configServerBootstrap(DssServerProperty p, ServerBootstrap b) throws InterruptedException {

        if (p instanceof DssHttpServerProperty) {

            final DssHttpServerProperty property = (DssHttpServerProperty) p;

            final Optional<SslContext> sslContext = sslContextOptional(property);
            final String scheme = sslContext.isPresent() ? SCHEME_HTTPS : SCHEME_HTTP;

            b
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.DEBUG))
                    .childHandler(new DssHttpServerInitializer(sslContext.isPresent(), sslContext.orElse(null), property.getHandlerCreator()));

            final Object[] param = new Object[3];
            param[0] = scheme;
            param[1] = property.getHost();
            param[2] = property.getPort();
            logger.log(Level.INFO, "Http(s) server is binding to {0}://{1}:{2}", param);

            return b;

        } else {
            throw new InterruptedException("DssServerProperty invalid type. cannot set ssl context. " + p.toString());
        }
    }

    private Optional<SslContext> sslContextOptional(DssServerProperty property) {

        final DssHttpServerProperty httpServerProperty = (DssHttpServerProperty) property;
        if (!httpServerProperty.isSsl()) {
            return Optional.empty();
        }

        try {
            final SelfSignedCertificate ssc = new SelfSignedCertificate();
            return Optional.of(SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build());
        } catch (CertificateException | SSLException e) {
            logger.log(Level.WARNING, "Create SelfSignedCertificate error: ", e);
        }

        return Optional.empty();
    }
}
