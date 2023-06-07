package com.example.image.handler;

import com.example.image.properties.SftpProperties;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class ConnectionManager implements ApplicationRunner {

    private static final ResponseStatusException SFTP_CONNECTION_EXCEPTION =
    new ResponseStatusException(INTERNAL_SERVER_ERROR, "Failed to connect to remote server");

    private static final JSch jSch = new JSch();

    private static final ConcurrentHashMap<Session, AtomicInteger> sessionChannels = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Session, AtomicInteger> invalidSessions = new ConcurrentHashMap<>();

    private final SftpProperties sftpProperties;

    @Override
    public void run(ApplicationArguments args) throws Exception {

        // Connection Optimization
        JSch.setConfig("PreferredAuthentications", "publickey");
        JSch.setConfig("StrictHostKeyChecking", "no");

        jSch.addIdentity("id_rsa", sftpProperties.getPrivateKeyBytes(), null, null);

        createSession(0);
    }

    // connect new channel: 50ms
    public Mono<ChannelSftp> connect() {

        return Mono
        .just(sessionChannels)
        .flatMap(sessions -> {

            ChannelSftp channel = null;

            for (final Entry<Session, AtomicInteger> e: sessions.entrySet())
            if (e.getValue().getAndIncrement() < sftpProperties.getMaxChannelCount()) {

                if ((channel = openChannel(e.getKey()))!=null) break;
            }
            else e.getValue().decrementAndGet();

            return Mono.justOrEmpty(channel);
        })
        .switchIfEmpty(
            Mono.defer(
                () -> Mono
                .justOrEmpty(createSession(1))
                .flatMap(session -> Mono.justOrEmpty(openChannel(session)))
                .switchIfEmpty(Mono.error(SFTP_CONNECTION_EXCEPTION))));
    }

    public void disconnect(final ChannelSftp channel)
    { disconnect(channel, null); }

    public void disconnect(final ChannelSftp channel, Session session) {

        try {

            if (channel!=null) channel.disconnect();
            if (session==null&&channel!=null) session = channel.getSession();
            if (session==null) return;

            boolean sessionIsDeletable = false;

            if (sessionChannels.containsKey(session))
            sessionIsDeletable =
            sftpProperties.getMaxChannelCount() <= sessionChannels
            .get(session)
            .updateAndGet(
                x->
                sftpProperties.getMinSessionCount()<sessionChannels.size()&&
                x==1? sftpProperties.getMaxChannelCount():x-1);

            else if (invalidSessions.contains(session))
            sessionIsDeletable = invalidSessions.get(session).decrementAndGet()==0;

            else if (session!=null) session.disconnect();

            if (sessionIsDeletable) {

                sessionChannels.remove(session);
                invalidSessions.remove(session);
                session.disconnect();
            }

        } catch (Exception e) {

            if (channel!=null) channel.disconnect();
            e.printStackTrace();
        }
    }

    private Session createSession(final int initialValue) {

        Session session = null;
        try {

            (session = jSch.getSession(
                sftpProperties.getRemoteUsername(),
                sftpProperties.getRemoteAddress()))
            .connect();

            sessionChannels.put(session, new AtomicInteger(initialValue));

        } catch (Exception e) {

            if (session!=null) {

                sessionChannels.remove(session);
                disconnect(null, session);
            }
            e.printStackTrace();
        }

        return session;
    }

    private ChannelSftp openChannel(final Session session) {

        ChannelSftp channel = null;
        if (session!=null) try {

            (channel = (ChannelSftp) session.openChannel("sftp")).connect();

        } catch (Exception e) {

            if (session!=null) {

                invalidSessions.put(session, sessionChannels.remove(session));
                disconnect(channel, session);
            }
            e.printStackTrace();
        }

        return channel;
    }
}
