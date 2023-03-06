package com.example.image.handler;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ConnectionManager {

    private static final ResponseStatusException SFTP_CONNECTION_EXCEPTION =
    new ResponseStatusException(INTERNAL_SERVER_ERROR, "Failed to connect to remote server");

    private static final JSch jSch = new JSch();

    private static final ConcurrentHashMap<Session, AtomicInteger> sessionChannels = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Session, AtomicInteger> invalidSessions = new ConcurrentHashMap<>();

    private static String remoteUsername;
    private static String remoteAddress;

    private static int maxChannelCount;
    private static int minSessionCount;

    public static void init(
        final byte[] privateKey,
        final String username, final String address,
        final int minSessionCount, final int maxChannelCount) throws Exception {

        // Connection Optimization
        JSch.setConfig("PreferredAuthentications", "publickey");
        JSch.setConfig("StrictHostKeyChecking", "no");

        jSch.addIdentity("id_rsa", privateKey, null, null);

        ConnectionManager.remoteUsername = username;
        ConnectionManager.remoteAddress = address;

        ConnectionManager.minSessionCount = minSessionCount;
        ConnectionManager.maxChannelCount = maxChannelCount;

        createSession(0);
    }

    // connect new channel: 50ms
    public static Mono<ChannelSftp> connect() {

        return Mono
        .just(sessionChannels)
        .flatMap(sessions -> {

            ChannelSftp channel = null;

            for (final Entry<Session, AtomicInteger> e: sessions.entrySet())
            if (e.getValue().getAndIncrement() < maxChannelCount) {

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

    public static void disconnect(final ChannelSftp channel)
    { disconnect(channel, null); }

    public static void disconnect(final ChannelSftp channel, Session session) {

        try {

            if (channel!=null) channel.disconnect();
            if (session==null&&channel!=null) session = channel.getSession();
            if (session==null) return;

            boolean sessionIsDeletable = false;

            if (sessionChannels.containsKey(session))
            sessionIsDeletable =
            maxChannelCount <= sessionChannels
            .get(session)
            .updateAndGet(
                x->
                minSessionCount<sessionChannels.size()&&x==1? maxChannelCount:x-1);

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

    private static Session createSession(final int initialValue) {

        Session session = null;
        try {

            (session = jSch.getSession(remoteUsername, remoteAddress)).connect();

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

    private static ChannelSftp openChannel(final Session session) {

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
